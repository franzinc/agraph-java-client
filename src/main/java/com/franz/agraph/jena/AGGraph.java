/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGBooleanQuery;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.jena.graph.Capabilities;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphEvents;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.TransactionHandler;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.GraphBase;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.DeleteDeniedException;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.util.iterator.ClosableIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.DatasetImpl;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Implements the Jena Graph interface for AllegroGraph.
 */
public class AGGraph extends GraphBase implements Graph, Closeable {

    protected final AGGraphMaker maker;
    protected final Node graphNode;
    protected final Resource context;
    protected final Resource[] contexts;
    protected AGRepositoryConnection conn;
    protected AGValueFactory vf;
    protected String entailmentRegime = "false";

    AGGraph(AGGraphMaker maker, Node graphNode) {
        super();
        this.maker = maker;
        this.graphNode = graphNode;
        conn = maker.getRepositoryConnection();
        vf = conn.getValueFactory();
        context = vf.asResource(graphNode);
        contexts = new Resource[] {context};
    }

    AGGraph(AGGraphMaker maker, Resource context, Resource... contexts) {
        super();
        this.maker = maker;
        this.graphNode = null;
        this.conn = maker.getRepositoryConnection();
        this.vf = conn.getValueFactory();
        this.context = context;
        this.contexts = contexts;
    }

    /**
     * Returns a human-consumable representation of <code>graph</code>. The string
     * <code>prefix</code> will appear at the beginning of the string. Nodes
     * may be prefix-compressed using <code>graph</code>'s prefix-mapping. This
     * default implementation will display all the triples exposed by the graph,
     * including reification triples.
     *
     * @param prefix the prefix of the string returned
     * @param graph  the Graph to render into a string
     * @return String  a human-consumable representation of the argument Graph
     */
    public static String toString(String prefix, Graph graph) {
        // PrefixMapping pm = graph.getPrefixMapping();
        StringBuilder b = new StringBuilder(prefix + " {");
        String gap = "";
        ClosableIterator<Triple> it = GraphUtil.findAll(graph);
        while (it.hasNext()) {
            b.append(gap);
            gap = "; ";
            b.append(it.next().toString());
        }
        b.append("}");
        return b.toString();
    }

    AGGraphMaker getGraphMaker() {
        return maker;
    }

    Node getGraphNode() {
        return graphNode;
    }

    public String getName() {
        if (graphNode == null) {
            return "default-graph";
        }
        return graphNode.toString();
    }

    Resource getGraphContext() {
        return context;
    }

    Resource[] getGraphContexts() {
        return contexts;
    }

    AGRepositoryConnection getConnection() {
        return conn;
    }

    String getEntailmentRegime() {
        return entailmentRegime;
    }

    @Override
    public Capabilities getCapabilities() {
        if (capabilities == null) {
            capabilities = new AGCapabilities();
        }
        return capabilities;
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        return new AGPrefixMapping(this);
    }

    @Override
    public TransactionHandler getTransactionHandler() {
        return new AGTransactionHandler(this);
    }

    /*@Override
    public String toString() {
        if (graphNode == null)
            return "default-graph";
        return graphNode.toString();
    }*/
    @Override
    public String toString() {
        return toString(getName() + (closed ? " (closed) " : " (size: " + graphBaseSize() + ")."), this);
    }

    protected Dataset getDataset() {
        final DatasetImpl dataset = new DatasetImpl();
        // If we have any non-default contexts, construct a dataset.
        // Otherwise just use an empty dataset.
        // This is preferable since using 'null' to specify
        // the default graph is not supported by older versions
        // of AllegroGraph.
        if (Arrays.stream(contexts).anyMatch(x -> x != null)) {
            for (Resource c : contexts) {
                if (c == null) {
                    // null means "the default graph".
                    // This will not work in AG < 6.1.1
                    dataset.addDefaultGraph(null);
                } else if (c instanceof IRI) {
                    dataset.addDefaultGraph((IRI) c);
                    dataset.addNamedGraph((IRI) c);
                }
            }
        }
        return dataset;
    }

    /*================
     *
     * GraphBase methods that should be implemented or overridden
     *
     *================*/

    @Override
    protected ExtendedIterator<Triple> graphBaseFind(Triple m) {
        RepositoryResult<Statement> result;
        try {
            // TODO: allow arbitrary values in subject and predicate positions?
            Node s = m.getMatchSubject();
            Node p = m.getMatchPredicate();
            Node o = m.getMatchObject();
            // quickly return no results if RDF constraints for subject and predicate
            // are violated, as occurs in the Jena test suite for Graph.
            if ((s != null && s.isLiteral())
                    || (p != null && (p.isLiteral() || p.isBlank()))) {
                result = conn.createRepositoryResult(new ArrayList<Statement>());
            } else {
                StatementCollector collector = new StatementCollector();
                conn.prepareHttpRepoClient().getStatements(
                        vf.asResource(s), vf.asURI(p), vf.asValue(o),
                        entailmentRegime, collector, contexts);
                result = conn.createRepositoryResult(collector.getStatements());
            }
        } catch (AGHttpException | RDFHandlerException e) {
            throw new RuntimeException(e);
        }
        return new AGTripleIterator(this, result);
    }

    @Override
    public void performAdd(Triple t) {
        try {
            AGRepositoryConnection conn = maker.getRepositoryConnection();
            AGValueFactory vf = conn.getValueFactory();
            conn.add(vf.asResource(t.getSubject()), vf.asURI(t.getPredicate()),
                    vf.asValue(t.getObject()), context);
        } catch (UnauthorizedException e) {
            throw new AddDeniedException(e.getMessage());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void performDelete(Triple t) {
        try {
            conn.remove(vf.asResource(t.getSubject()), vf.asURI(t
                    .getPredicate()), vf.asValue(t.getObject()), contexts);
        } catch (UnauthorizedException e) {
            throw new DeleteDeniedException(e.getMessage());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int graphBaseSize() {
        // TODO deal with graphs bigger than int's.
        int size;
        try {
            size = (int) conn.size(contexts);
        } catch (RepositoryException e) {
            // TODO: proper exception to throw?
            throw new RuntimeException(e);
        }
        return size;
    }

    /**
     * Answer true iff this graph contains no triples.
     * <p>
     * Implemented using a SPARQL ASK for any triple in the graph's
     * dataset; on large databases this is faster than determining
     * whether the graph's size is zero.
     */
    @Override
    public boolean isEmpty() {
        String queryString = "ask {?s ?p ?o}";
        AGBooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
        bq.setDataset(getDataset());
        try {
            return !bq.evaluate();
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    // The default implementation of these methods tries to retrieve matching
    // nodes and delete one by one...

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        conn.remove((Resource) null, null, null);
        this.getEventManager().notifyEvent(this, GraphEvents.removeAll);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final Node s, final Node p, final Node o) {
        conn.remove(vf.asResource(s), vf.asURI(p), vf.asValue(o));
        this.getEventManager().notifyEvent(this, GraphEvents.remove(s, p, o));
    }
}
