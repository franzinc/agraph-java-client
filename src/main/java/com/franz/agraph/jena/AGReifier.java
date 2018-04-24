/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import com.franz.agraph.repository.AGBooleanQuery;
import com.franz.agraph.repository.AGGraphQuery;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.AlreadyReifiedException;
import org.apache.jena.shared.CannotReifyException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;

public class AGReifier {

    private AGGraph graph;

    AGReifier(AGGraph graph) {
        this.graph = graph;
    }

    public Triple getTriple(Node n) {
        String queryString = "construct {?s ?p ?o} where { ?st rdf:subject ?s . ?st rdf:predicate ?p . ?st rdf:object ?o . }";
        AGRepositoryConnection conn = graph.getConnection();
        AGValueFactory vf = conn.getValueFactory();
        AGGraphQuery bq = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        GraphQueryResult result;
        try {
            bq.setBinding("st", vf.asValue(n));
            bq.setDataset(graph.getDataset());
            result = bq.evaluate();
            if (result.hasNext()) {
                Triple t = AGNodeFactory.asTriple(result.next());
                if (result.hasNext()) {
                    // see testOverspecificationSuppressesReification
                    return null;
                } else {
                    return t;
                }
            } else {
                return null;
            }
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
    }


    public ExtendedIterator<Triple> find(Triple m) {
        return graph.graphBaseFind(m);
    }


    public ExtendedIterator<Triple> findExposed(Triple m) {
        if (matchesReification(m)) {
            if (m.getMatchPredicate() != null) {
                return graph.graphBaseFind(m);
            } else {
                //String queryString = "construct {?s ?p ?o} where { {?s ?p ?o . FILTER (?p IN (rdf:subject, rdf:predicate, rdf:object)) } UNION {?s rdf:type rdf:Statement} }";
                String queryString = "construct {?s ?p ?o} where { {?s ?p ?o . FILTER (?p = rdf:subject || ?p = rdf:predicate || ?p = rdf:object)} UNION {?s ?p ?o . FILTER (?p = rdf:type && ?o = rdf:Statement)} }";
                AGRepositoryConnection conn = graph.getConnection();
                AGValueFactory vf = conn.getValueFactory();
                AGGraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
                GraphQueryResult result;
                try {
                    if (m.getMatchSubject() != null) {
                        gq.setBinding("s", vf.asValue(m.getMatchSubject()));
                    }
                    if (m.getMatchObject() != null) {
                        gq.setBinding("o", vf.asValue(m.getMatchObject()));
                    }
                    gq.setDataset(graph.getDataset());
                    result = gq.evaluate();
                    return new AGTripleIteratorGQ(graph, result);
                } catch (QueryEvaluationException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            return NullIterator.instance();
        }
    }

    public ExtendedIterator<Triple> findEither(Triple m, boolean showHidden) {
        return showHidden ? NullIterator.instance() : find(m);
    }


    public int size() {
        return 0;
    }


    public Graph getParentGraph() {
        return graph;
    }

    public Node reifyAs(Node n, Triple t) {
        Triple tn = getTriple(n);
        if (tn != null && !tn.equals(t)) {
            // per AbstractTestReifier#testException, only throw an
            // AlreadyReifiedException if n is already reifying a
            // *different* triple
            throw new AlreadyReifiedException(n);
        }
        if (tn == null) {
            // TODO do this in a single server request?
            String queryString = "select ?v { ?st ?p ?v . FILTER (?v != ?tv) }";
            AGRepositoryConnection conn = graph.getConnection();
            AGTupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            TupleQueryResult result;
            try {
                q.setDataset(graph.getDataset());
                // Check for existing n with rdf:subject other than t's subject
                // see AbstractTestReifier#testKevinCaseB
                q.setBinding("st", graph.vf.asValue(n));
                q.setBinding("p", graph.vf.asValue(RDF.Nodes.subject));
                q.setBinding("tv", graph.vf.asValue(t.getSubject()));
                result = q.evaluate();
                if (result.hasNext()) {
                    // TODO: convey that it's because of ?v
                    throw new CannotReifyException(n);
                }
                // Check for existing n with rdf:predicate other than t's predicate
                q.setBinding("p", graph.vf.asValue(RDF.Nodes.predicate));
                q.setBinding("tv", graph.vf.asValue(t.getPredicate()));
                result = q.evaluate();
                if (result.hasNext()) {
                    throw new CannotReifyException(n);
                }
                // Check for existing n with rdf:object other than t's object
                q.setBinding("p", graph.vf.asValue(RDF.Nodes.object));
                q.setBinding("tv", graph.vf.asValue(t.getObject()));
                result = q.evaluate();
                if (result.hasNext()) {
                    throw new CannotReifyException(n);
                }
            } catch (QueryEvaluationException e) {
                throw new RuntimeException(e);
            }
            graph.add(Triple.create(n, RDF.Nodes.subject, t.getSubject()));
            graph.add(Triple.create(n, RDF.Nodes.predicate, t.getPredicate()));
            graph.add(Triple.create(n, RDF.Nodes.object, t.getObject()));
            graph.add(Triple.create(n, RDF.Nodes.type, RDF.Nodes.Statement));
        }
        return n;
    }


    public boolean hasTriple(Node n) {
        boolean result;
        String queryString = "ask { ?st rdf:type rdf:Statement . }";
        AGRepositoryConnection conn = graph.getConnection();
        AGValueFactory vf = conn.getValueFactory();
        AGBooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
        try {
            bq.setBinding("st", vf.asValue(n));
            bq.setDataset(graph.getDataset());
            result = bq.evaluate();
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
        return result;
    }


    public boolean hasTriple(Triple t) {
        boolean result;
        String queryString = "ask { ?st rdf:subject ?s . ?st rdf:predicate ?p . ?st rdf:object ?o . }";
        AGRepositoryConnection conn = graph.getConnection();
        AGValueFactory vf = conn.getValueFactory();
        AGBooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
        try {
            bq.setBinding("s", vf.asValue(t.getSubject()));
            bq.setBinding("p", vf.asValue(t.getPredicate()));
            bq.setBinding("o", vf.asValue(t.getObject()));
            bq.setDataset(graph.getDataset());
            result = bq.evaluate();
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
        return result;
    }


    public ExtendedIterator<Node> allNodes() {
        String queryString = "select ?st where { ?st rdf:type rdf:Statement }";
        AGRepositoryConnection conn = graph.getConnection();
        AGTupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result;
        try {
            q.setDataset(graph.getDataset());
            result = q.evaluate();
            return new AGNodeIterator(result);
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
    }


    public ExtendedIterator<Node> allNodes(Triple t) {
        String queryString = "select ?st where { ?st rdf:type rdf:Statement . ?st rdf:subject ?s . ?st rdf:predicate ?p . ?st rdf:object ?o . }";
        AGRepositoryConnection conn = graph.getConnection();
        AGValueFactory vf = conn.getValueFactory();
        AGTupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult result;
        try {
            q.setDataset(graph.getDataset());
            q.setBinding("s", vf.asValue(t.getSubject()));
            q.setBinding("p", vf.asValue(t.getPredicate()));
            q.setBinding("o", vf.asValue(t.getObject()));
            result = q.evaluate();
            return new AGNodeIterator(result);
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
    }


    public void remove(Node n, Triple t) {
        // TODO: do this in a single server request
        graph.delete(Triple.create(n, RDF.Nodes.subject, t.getSubject()));
        graph.delete(Triple.create(n, RDF.Nodes.predicate, t.getPredicate()));
        graph.delete(Triple.create(n, RDF.Nodes.object, t.getObject()));
        graph.delete(Triple.create(n, RDF.Nodes.type, RDF.Nodes.Statement));
    }


    public void remove(Triple t) {
        // TODO: do this in a single server request?
        String queryString = "select { ?st rdf:type rdf:Statement . ?st rdf:subject ?s . ?st rdf:predicate ?p . ?st rdf:object ?o . }";
        AGRepositoryConnection conn = graph.getConnection();
        AGValueFactory vf = conn.getValueFactory();
        AGTupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        try {
            q.setDataset(graph.getDataset());
            q.setBinding("s", vf.asValue(t.getSubject()));
            q.setBinding("p", vf.asValue(t.getPredicate()));
            q.setBinding("o", vf.asValue(t.getObject()));
            TupleQueryResult result = q.evaluate();
            while (result.hasNext()) {
                remove(AGNodeFactory.asNode(result.next().getValue("st")), t);
            }
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
    }


    public boolean handledAdd(Triple t) {
        return false;
    }


    public boolean handledRemove(Triple t) {
        return false;
    }


    public void close() {
    }

    /**
     * Answer true iff <code>m</code> might match a reification triple.
     */
    private boolean matchesReification(Triple m) {
        Node predicate = m.getPredicate();
        return !predicate.isConcrete() || predicate.equals(RDF.Nodes.subject)
                || predicate.equals(RDF.Nodes.predicate)
                || predicate.equals(RDF.Nodes.object)
                || predicate.equals(RDF.Nodes.type)
                && matchesStatement(m.getObject());
    }

    private boolean matchesStatement(Node x) {
        return !x.isConcrete() || x.equals(RDF.Nodes.Statement);
    }

}
