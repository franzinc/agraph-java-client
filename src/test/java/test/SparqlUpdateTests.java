/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecution;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import junit.framework.Assert;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SparqlUpdateTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void testSesameUpdate() throws Exception {
        IRI s = vf.createIRI("http://example/book1");
        IRI p = vf.createIRI("http://purl.org/dc/elements/1.1/title");
        Literal o_wrong = vf.createLiteral("Fundamentals of Compiler Desing");
        Literal o_right = vf.createLiteral("Fundamentals of Compiler Design");
        IRI g = vf.createIRI("http://example/bookStore");
        conn.add(s, p, o_wrong, g);

        // Perform a sequence of SPARQL UPDATE queries in one request to correct the title
        String queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
                + "DELETE DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } } ; \n"
                + "\n"
                + "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
                + "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";

        Update u = conn.prepareUpdate(QueryLanguage.SPARQL, queryString);
        u.execute();
        Assert.assertTrue("Title should be correct", conn.hasStatement(s, p, o_right, false, g));
        Assert.assertFalse("Incorrect title should be gone", conn.hasStatement(s, p, o_wrong, false, g));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testSesameUpdateViaBooleanQuery() throws Exception {
        IRI s = vf.createIRI("http://example/book1");
        IRI p = vf.createIRI("http://purl.org/dc/elements/1.1/title");
        Literal o_wrong = vf.createLiteral("Fundamentals of Compiler Desing");
        Literal o_right = vf.createLiteral("Fundamentals of Compiler Design");
        IRI g = vf.createIRI("http://example/bookStore");
        conn.add(s, p, o_wrong, g);

        // Perform a sequence of SPARQL UPDATE queries in one request to correct the title
        String queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
                + "DELETE DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } } ; \n"
                + "\n"
                + "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
                + "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";

        // SPARQL Update queries can also be executed using a BooleanQuery (for side effect)
        // Useful for older versions of Sesame that don't have a prepareUpdate method.
        conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate();
        Assert.assertTrue("Title should be correct", conn.hasStatement(s, p, o_right, false, g));
        Assert.assertFalse("Incorrect title should be gone", conn.hasStatement(s, p, o_wrong, false, g));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testJenaUpdate() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getUnionOfAllGraphs());
        AGModel model = closeLater(new AGModel(graph));
        model.read(Util.resourceAsStream("/test/example.nq"), null, "NQUADS");
        Assert.assertEquals("expected size 10", 10, model.size());
        Assert.assertTrue("Bob should be there", model.contains(model.createResource("http://example.org/bob/foaf.rdf#me"), FOAF.name, model.createLiteral("Bob")));
        Assert.assertFalse("Robert should not be there", model.contains(model.createResource("http://example.org/bob/foaf.rdf#me"), FOAF.name, model.createLiteral("Robert")));

        // Perform a sequence of SPARQL UPDATE queries in one request to correct the title
        String queryString = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
                + "DELETE DATA { GRAPH <http://example.org/bob/foaf.rdf> { <http://example.org/bob/foaf.rdf#me>  foaf:name  \"Bob\" } } ; \n"
                + "\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
                + "INSERT DATA { GRAPH <http://example.org/bob/foaf.rdf> { <http://example.org/bob/foaf.rdf#me>  foaf:name  \"Robert\" } }";

        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            qe.execUpdate();
        } finally {
            qe.close();
        }
        Assert.assertTrue("Robert should be there", model.contains(model.createResource("http://example.org/bob/foaf.rdf#me"), FOAF.name, model.createLiteral("Robert")));
        Assert.assertFalse("Bob should not be there", model.contains(model.createResource("http://example.org/bob/foaf.rdf#me"), FOAF.name, model.createLiteral("Bob")));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testJenaDeleteWithoutGraphs() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getGraph());
        AGModel model = closeLater(new AGModel(graph));
        Resource subject = model.createResource("http://franz.com/s");
        Property predicate = model.createProperty("http://franz.com/p");
        String object = "object";

        model.add(subject, predicate, object);

        Assert.assertTrue("Triple should be there",
                model.contains(subject, predicate, object));

        String queryString = "DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            qe.execUpdate();
        } finally {
            qe.close();
        }
        Assert.assertFalse("Triple should not be there",
                model.contains(subject, predicate, object));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testJenaDeleteUsingGraphs() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph defaultGraph = closeLater(maker.getGraph());
        AGGraph g1 = closeLater(maker.createGraph("http://franz.com/g1"));
        AGGraph g2 = closeLater(maker.createGraph("http://franz.com/g2"));
        AGGraph defaultAndG1 = closeLater(maker.createUnion(defaultGraph, g1));

        AGModel defaultModel = closeLater(new AGModel(defaultGraph));
        AGModel model1 = closeLater(new AGModel(g1));
        AGModel model2 = closeLater(new AGModel(g2));
        AGModel defaultAndM1 = closeLater(new AGModel(defaultAndG1));

        Resource subject = defaultModel.createResource("http://franz.com/s");
        Property predicate = defaultModel.createProperty("http://franz.com/p");
        String o1 = "1";
        String o2 = "2";
        String o3 = "3";

        defaultModel.add(subject, predicate, o1);
        model1.add(subject, predicate, o2);
        model2.add(subject, predicate, o3);

        // Check queries
        Assert.assertTrue("Triple 2 should be visible in graph DEFAULT + G1",
                defaultAndM1.contains(subject, predicate, o2));

        String queryString = "DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, defaultAndM1);
        try {
            qe.execUpdate();
        } finally {
            qe.close();
        }
        Assert.assertFalse("Triple 1 should not be there",
                defaultModel.contains(subject, predicate, o1));
        Assert.assertFalse("Triple 2 should not be there",
                model1.contains(subject, predicate, o2));
        Assert.assertTrue("Triple 3 should still be there",
                model2.contains(subject, predicate, o3));
    }
}
