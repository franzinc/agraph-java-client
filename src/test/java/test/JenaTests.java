/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGInfModel;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecution;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.jena.AGReasoner;
import com.franz.agraph.repository.AGRepositoryConnection;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileOutputStream;

public class JenaTests extends AGAbstractTest {

    private void addOne(AGModel model) throws RepositoryException {
        Assert.assertEquals(0, model.size());

        Resource bob = model.createResource("http://example.org/people/bob");
        Resource dave = model.createResource("http://example.org/people/dave");
        Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");

        model.add(bob, fatherOf, dave);

        Assert.assertEquals(1, model.size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaAutoCommitTrue() throws Exception {
        // this is the default, but try setting it explicitly
        conn.setAutoCommit(true);

        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getGraph());
        AGModel model = closeLater(new AGModel(graph));
        addOne(model);

        Assert.assertEquals("a different connection, triple was already committed", 1, getSize());

        model.commit();
        Assert.assertEquals(1, conn.size());

        Assert.assertEquals("a different connection", 1, getSize());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaAutoCommitFalse() throws Exception {
        //conn.setAutoCommit(false);

        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getGraph());
        AGModel model = closeLater(new AGModel(graph));
        try {
            // use begin instead of setAutoCommit(false)
            model.begin();

            addOne(model);

            Assert.assertEquals("a different connection, empty", 0, getSize());

            model.commit();
            Assert.assertEquals(1, conn.size());

            Assert.assertEquals("a different connection", 1, getSize());
        } catch (Exception e) {
            model.abort();
            throw e;
        }
    }

    // cf. rfe13511
    @Test
    @Category(TestSuites.Prepush.class)
    public void sparqlOrderByError_bug19157_rfe9971_no_check() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getGraph());
        AGModel model = closeLater(new AGModel(graph));

        Resource bob = model.createResource("http://example.org/people/bob");
        Resource dave = model.createResource("http://example.org/people/dave");
        Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");
        Property age = model.createProperty("http://example.org/ontology/age");
        Literal three = model.createTypedLiteral(3);

        model.add(bob, fatherOf, dave);
        model.add(dave, age, three);

        AGQuery query = AGQueryFactory.create("select ?s ?p ?o where { ?s ?p ?o . } order by ?x ?s");
        {
            //query.setCheckVariables(false); // default is false
            AGQueryExecution qe = closeLater(AGQueryExecutionFactory.create(query, model));
            qe.execSelect();
            // extra var is ignored
        }
    }

    // cf. rfe13511
    @Test
    @Category(TestSuites.Prepush.class)
    public void sparqlOrderByError_bug19157_rfe9971_yes_check() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getGraph());
        AGModel model = closeLater(new AGModel(graph));

        Resource bob = model.createResource("http://example.org/people/bob");
        Resource dave = model.createResource("http://example.org/people/dave");
        Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");
        Property age = model.createProperty("http://example.org/ontology/age");
        Literal three = model.createTypedLiteral(3);

        model.add(bob, fatherOf, dave);
        model.add(dave, age, three);

        AGQuery query = AGQueryFactory.create("select ?s ?p ?o where { ?s ?p ?o . } order by ?z ?s");
        {
            query.setCheckVariables(true);
            AGQueryExecution qe = closeLater(AGQueryExecutionFactory.create(query, model));
            try {
                qe.execSelect();
                Assert.fail("query should have failed because of ?z");
            } catch (QueryException e) {
                if (!(e.getMessage().contains("Variables do not intersect with query: ?z") || e.getMessage().contains("unknown variable in order expression: ?z") || e.getMessage().contains("Unknown variable used in order expression: ?z"))) {
                    throw e;
                }
            }
        }
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaGraphs_bug19491() throws Exception {
        // This test is largely obsolete/superseded by jenaGraphScopedReasoning
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph defaultGraph = closeLater(maker.getGraph());
        AGModel defaultModel = closeLater(new AGModel(defaultGraph));
        addOne(defaultModel);

        AGGraph namedGraph = closeLater(maker.openGraph("http://example.com/named"));
        AGModel namedModel = closeLater(new AGModel(namedGraph));
        addOne(namedModel);

        AGReasoner reasoner = new AGReasoner();
        defaultGraph = closeLater(maker.getGraph());
        defaultModel = closeLater(new AGModel(defaultGraph));
        AGInfModel infModel = closeLater(new AGInfModel(reasoner, defaultModel));
        Assert.assertEquals("conn is full", 2, conn.size());
        Assert.assertEquals("infModel should be partial", 1,
                closeLater(infModel.listStatements((Resource) null, (Property) null, (RDFNode) null)).toList().size());
        Assert.assertEquals("defaultModel should be partial", 1,
                closeLater(defaultModel.listStatements((Resource) null, (Property) null, (RDFNode) null)).toList().size());
        // TODO: size is not correct for infModel, dunno why
        //Assert.assertEquals("infModel should be full", 2, infModel.size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaReasoning_bug19484() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getGraph());
        AGModel model = closeLater(new AGModel(graph));

        Resource bob = model.createResource("http://example.org/people/bob");
        Resource dave = model.createResource("http://example.org/people/dave");
        Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");
        Property hasFather = model.createProperty("http://example.org/ontology/hasFather");

        model.add(hasFather, OWL.inverseOf, fatherOf);
        model.add(bob, fatherOf, dave);

        AGQuery query = AGQueryFactory.create("select * where { <http://example.org/people/dave> <http://example.org/ontology/hasFather> ?o . }");
        {
            AGInfModel inf = closeLater(new AGInfModel(new AGReasoner(), model));

            StmtIterator stmts = closeLater(inf.listStatements(dave, hasFather, bob));
            Assert.assertTrue("with reasoning", stmts.hasNext());

            AGQueryExecution exe = closeLater(AGQueryExecutionFactory.create(query, inf));
            closeLater(exe);
            ResultSet results = exe.execSelect();
            Assert.assertTrue("with reasoning", results.hasNext());
        }
        {
            StmtIterator stmts = closeLater(model.listStatements(dave, hasFather, bob));
            Assert.assertFalse("without reasoning", stmts.hasNext());

            AGQueryExecution exe = closeLater(AGQueryExecutionFactory.create(query, model));
            closeLater(exe);
            ResultSet results = exe.execSelect();
            Assert.assertFalse("without reasoning", results.hasNext());
        }
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void savingModel_spr37167() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getGraph());
        AGModel model = closeLater(new AGModel(graph));

        Resource bob = model.createResource("http://example.org/people/bob");
        Resource dave = model.createResource("http://example.org/people/dave");
        Property fatherOf = model.createProperty("http://example.org/ontology/fatherOf");
        model.add(bob, fatherOf, dave);

        Resource blankNode = model.createResource();
        Property has = model.createProperty("http://example.org/ontology/has");
        model.add(blankNode, has, dave);

        File outputFile = File.createTempFile("JenaTest-", ".txt");
        outputFile.deleteOnExit();
        model.write(closeLater(new FileOutputStream(outputFile)));

        graph = closeLater(maker.getGraph());
        model = closeLater(new AGModel(graph));
        outputFile = File.createTempFile("JenaTest-", ".txt");
        outputFile.deleteOnExit();
        model.write(closeLater(new FileOutputStream(outputFile)));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaRestrictionReasoning() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getGraph());
        AGModel model = closeLater(new AGModel(graph));
        AGReasoner reasoner = AGReasoner.RESTRICTION;
        AGInfModel infmodel = closeLater(new AGInfModel(reasoner, model));
        Resource a = model.createResource("http://a");
        Resource c = model.createResource("http://C");
        Resource d = model.createResource("http://D");
        Property p = model.createProperty("http://p");
        Resource r = model.createResource("http://R");
        Resource v = model.createResource("http://v");
        Resource w = model.createResource("http://w");

        model.add(c, OWL.equivalentClass, r);
        model.add(r, RDF.type, OWL.Restriction);
        model.add(r, OWL.onProperty, p);
        model.add(r, OWL.hasValue, v);
        model.add(a, RDF.type, c);
        Assert.assertTrue("missing hasValue inference 1", infmodel.contains(a, p, v));

        model.removeAll();
        model.add(c, OWL.equivalentClass, r);
        model.add(r, RDF.type, OWL.Restriction);
        model.add(r, OWL.onProperty, p);
        model.add(r, OWL.hasValue, v);
        model.add(a, p, v);
        Assert.assertTrue("missing hasValue inference 2", infmodel.contains(a, RDF.type, c));

        model.removeAll();
        model.add(c, OWL.equivalentClass, r);
        model.add(r, RDF.type, OWL.Restriction);
        model.add(r, OWL.onProperty, p);
        model.add(r, OWL.someValuesFrom, d);
        model.add(a, p, v);
        model.add(a, p, w);
        model.add(v, RDF.type, d);
        Assert.assertTrue("missing someValuesFrom inference", infmodel.contains(a, RDF.type, c));
        Assert.assertFalse("unexpected someValuesFrom inference", infmodel.contains(w, RDF.type, d));

        model.removeAll();
        model.add(c, OWL.equivalentClass, r);
        model.add(r, RDF.type, OWL.Restriction);
        model.add(r, OWL.onProperty, p);
        model.add(r, OWL.allValuesFrom, d);
        model.add(a, p, v);
        model.add(a, RDF.type, c);
        Assert.assertTrue("missing allValuesFrom inference", infmodel.contains(v, RDF.type, d));

        // check for unsoundness
        model.removeAll();
        model.add(c, OWL.equivalentClass, r);
        model.add(r, RDF.type, OWL.Restriction);
        model.add(r, OWL.onProperty, p);
        model.add(r, OWL.allValuesFrom, d);
        model.add(a, p, v);
        model.add(a, p, w);
        model.add(v, RDF.type, d);
        model.add(w, RDF.type, d);
        Assert.assertFalse("unexpected allValuesFrom inference", infmodel.contains(a, RDF.type, c));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaGraphScopedReasoning() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph gd = closeLater(maker.getGraph());
        AGGraph g1 = closeLater(maker.createGraph("http://example.org/g1"));
        AGGraph g2 = closeLater(maker.createGraph("http://example.org/g2"));
        AGGraph g3 = closeLater(maker.createGraph("http://example.org/g3"));
        AGGraph gAll = closeLater(maker.getUnionOfAllGraphs());
        AGGraph gAllb = closeLater(maker.createUnion());
        AGGraph gd12 = closeLater(maker.createUnion(gd, g1, g2));
        AGGraph gd23 = closeLater(maker.createUnion(gd, g2, g3));
        AGGraph g123 = closeLater(maker.createUnion(g1, g2, g3));

        AGModel md = closeLater(new AGModel(gd));
        AGModel m1 = closeLater(new AGModel(g1));
        AGModel m2 = closeLater(new AGModel(g2));
        AGModel m3 = closeLater(new AGModel(g3));
        AGModel mAll = closeLater(new AGModel(gAll));
        AGModel mAllb = closeLater(new AGModel(gAllb));
        AGModel md12 = closeLater(new AGModel(gd12));
        AGModel md23 = closeLater(new AGModel(gd23));
        AGModel m123 = closeLater(new AGModel(g123));

        Resource a = md.createResource("http://a");
        Resource b = md.createResource("http://b");
        Resource c = md.createResource("http://c");
        Resource d = md.createResource("http://d");
        Property p = md.createProperty("http://p");
        Property q = md.createProperty("http://q");

        md.add(p, RDF.type, OWL.TransitiveProperty);
        m1.add(a, p, b);
        m1.add(p, RDFS.subPropertyOf, q);
        m2.add(b, p, c);
        m3.add(c, p, d);
        Assert.assertTrue("size of md", md.size() == 1);
        Assert.assertTrue("size of m1", m1.size() == 2);
        Assert.assertTrue("size of m2", m2.size() == 1);
        Assert.assertTrue("size of m3", m3.size() == 1);
        Assert.assertTrue("size of mAll", mAll.size() == 5);
        Assert.assertTrue("size of mAllb", mAllb.size() == 5);
        Assert.assertTrue("size of md12", md12.size() == 4);
        Assert.assertTrue("size of md23", md23.size() == 3);
        Assert.assertTrue("size of m123", m123.size() == 4);

        AGReasoner reasoner = AGReasoner.RDFS_PLUS_PLUS;
        AGInfModel infAll = closeLater(new AGInfModel(reasoner, mAll));
        AGInfModel infd = closeLater(new AGInfModel(reasoner, md));
        AGInfModel inf1 = closeLater(new AGInfModel(reasoner, m1));
        AGInfModel infd12 = closeLater(new AGInfModel(reasoner, md12));
        reasoner = AGReasoner.RESTRICTION;
        AGInfModel infd23 = closeLater(new AGInfModel(reasoner, md23));
        AGInfModel inf123 = closeLater(new AGInfModel(reasoner, m123));

        Assert.assertTrue("missing inference All", infAll.contains(a, p, d));
        Assert.assertFalse("unsound inference d", infd.contains(a, p, b));
        Assert.assertTrue("missing inference 1", inf1.contains(a, q, b));
        Assert.assertFalse("unsound inference 1", inf1.contains(a, p, c));
        Assert.assertTrue("missing inference d12", infd12.contains(a, p, c));
        Assert.assertFalse("unsound inference d12", infd12.contains(a, p, d));
        Assert.assertTrue("missing inference d23", infd23.contains(b, p, d));
        Assert.assertFalse("unsound inference d23", infd23.contains(a, p, d));
        Assert.assertTrue("missing inference 123", inf123.contains(b, p, c));
        Assert.assertFalse("unsound inference 123", inf123.contains(a, p, d));
        Statement s = inf123.createStatement(p, RDF.type, OWL.TransitiveProperty);
        inf123.add(s);
        Assert.assertTrue("missing added statement in m123", m123.contains(s));
        Assert.assertTrue("missing added statement in m1", m1.contains(s));
        Assert.assertTrue("missing added statement in md12", md12.contains(s));
        Assert.assertTrue("missing inference 123", inf123.contains(a, p, d));
        inf1.remove(a, p, b);
        Assert.assertFalse("unexpected statement in inf1", inf1.contains(a, p, b));
        Assert.assertFalse("unexpected statement in m1", m1.contains(a, p, b));
        Assert.assertTrue("missing statement in m1", m1.contains(s));
        Assert.assertFalse("unexpected statement in infAll", m1.contains(a, p, d));
        Assert.assertTrue("missing inference in infAll", infAll.contains(b, p, d));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testIsEmpty() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph gd = closeLater(maker.getGraph());
        AGGraph g1 = closeLater(maker.createGraph("http://example.org/g1"));
        AGGraph g2 = closeLater(maker.createGraph("http://example.org/g2"));
        AGGraph g3 = closeLater(maker.createGraph("http://example.org/g3"));
        AGGraph gAll = closeLater(maker.getUnionOfAllGraphs());
        AGGraph gd12 = closeLater(maker.createUnion(gd, g1, g2));
        AGGraph gd23 = closeLater(maker.createUnion(gd, g2, g3));
        AGGraph g123 = closeLater(maker.createUnion(g1, g2, g3));

        AGModel md = closeLater(new AGModel(gd));
        AGModel m1 = closeLater(new AGModel(g1));
        AGModel m2 = closeLater(new AGModel(g2));
        AGModel m3 = closeLater(new AGModel(g3));
        AGModel mAll = closeLater(new AGModel(gAll));
        AGModel md12 = closeLater(new AGModel(gd12));
        AGModel md23 = closeLater(new AGModel(gd23));
        AGModel m123 = closeLater(new AGModel(g123));

        AGReasoner reasoner = AGReasoner.RDFS_PLUS_PLUS;
        AGInfModel infAll = closeLater(new AGInfModel(reasoner, mAll));
        AGInfModel infd = closeLater(new AGInfModel(reasoner, md));
        AGInfModel inf1 = closeLater(new AGInfModel(reasoner, m1));
        AGInfModel infd12 = closeLater(new AGInfModel(reasoner, md12));
        reasoner = AGReasoner.RESTRICTION;
        AGInfModel infd23 = closeLater(new AGInfModel(reasoner, md23));
        AGInfModel inf123 = closeLater(new AGInfModel(reasoner, m123));

        Resource a = md.createResource("http://a");
        Resource b = md.createResource("http://b");
        Resource c = md.createResource("http://c");
        Resource d = md.createResource("http://d");
        Property p = md.createProperty("http://p");

        Assert.assertTrue("md", md.isEmpty());
        Assert.assertTrue("m1", m1.isEmpty());
        Assert.assertTrue("m2", m2.isEmpty());
        Assert.assertTrue("m3", m3.isEmpty());
        Assert.assertTrue("mAll", mAll.isEmpty());
        Assert.assertTrue("infd", infd.isEmpty());
        md.add(p, RDF.type, OWL.TransitiveProperty);
        Assert.assertFalse("md empty?", md.isEmpty());
        Assert.assertFalse("infd", infd.isEmpty());
        Assert.assertFalse("mAll empty?", mAll.isEmpty());
        Assert.assertFalse("md12 empty?", md12.size() == 4);
        Assert.assertFalse("md23 empty?", md23.size() == 3);
        Assert.assertTrue("m1", m1.isEmpty());
        Assert.assertTrue("inf1", inf1.isEmpty());
        Assert.assertTrue("m2", m2.isEmpty());
        Assert.assertTrue("m3", m3.isEmpty());
        Assert.assertTrue("m123", m123.isEmpty());
        m1.add(a, p, b);
        Assert.assertFalse("md empty?", md.isEmpty());
        Assert.assertFalse("mAll empty?", mAll.isEmpty());
        Assert.assertFalse("md12 empty?", md12.isEmpty());
        Assert.assertFalse("md23 empty?", md23.isEmpty());
        Assert.assertFalse("m1 empty?", m1.isEmpty());
        Assert.assertTrue("m2", m2.isEmpty());
        Assert.assertTrue("m3", m3.isEmpty());
        Assert.assertFalse("m123 empty?", m123.isEmpty());
        m2.add(b, p, c);
        Assert.assertFalse("md empty?", md.isEmpty());
        Assert.assertFalse("mAll empty?", mAll.isEmpty());
        Assert.assertFalse("md12 empty?", md12.isEmpty());
        Assert.assertFalse("md23 empty?", md23.isEmpty());
        Assert.assertFalse("m1 empty?", m1.isEmpty());
        Assert.assertFalse("m2 empty?", m2.isEmpty());
        Assert.assertTrue("m3", m3.isEmpty());
        Assert.assertFalse("m123 empty?", m123.isEmpty());
        m3.add(c, p, d);
        Assert.assertFalse("md empty?", md.isEmpty());
        Assert.assertFalse("mAll empty?", mAll.isEmpty());
        Assert.assertFalse("md12 empty?", md12.isEmpty());
        Assert.assertFalse("md23 empty?", md23.isEmpty());
        Assert.assertFalse("m1 empty?", m1.isEmpty());
        Assert.assertFalse("m2 empty?", m2.isEmpty());
        Assert.assertFalse("m3 empty?", m3.isEmpty());
        Assert.assertFalse("m123 empty?", m123.isEmpty());

        Assert.assertFalse("infAll empty?", infAll.isEmpty());
        Assert.assertFalse("infd empty?", infd.isEmpty());
        Assert.assertFalse("inf1 empty?", inf1.isEmpty());
        Assert.assertFalse("infd12 empty?", infd12.isEmpty());
        Assert.assertFalse("infd23 empty?", infd23.isEmpty());
        Assert.assertFalse("inf123 empty?", inf123.isEmpty());

        m1.remove(a, p, b);
        Assert.assertFalse("infAll empty?", infAll.isEmpty());
        Assert.assertFalse("infd empty?", infd.isEmpty());
        Assert.assertTrue("inf1 empty?", inf1.isEmpty());
        Assert.assertFalse("infd12 empty?", infd12.isEmpty());
        Assert.assertFalse("infd23 empty?", infd23.isEmpty());
        Assert.assertFalse("inf123 empty?", inf123.isEmpty());

    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaReadTurtle() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph g = closeLater(maker.getGraph());
        AGModel m = closeLater(new AGModel(g));

        m.read(Util.resourceAsStream("/test/default-graph.ttl"), null, "TURTLE");
        Assert.assertTrue("size of m", m.size() == 4);
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaDeleteQuads() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph unionGraph = closeLater(maker.getUnionOfAllGraphs());
        AGModel model = closeLater(new AGModel(unionGraph));

        // add quads somehow (here via the sesame api)
        IRI s = conn.getValueFactory().createIRI("http://s");
        IRI p = conn.getValueFactory().createIRI("http://p");
        IRI o = conn.getValueFactory().createIRI("http://o");
        IRI g = conn.getValueFactory().createIRI("http://g");
        conn.add(s, p, o, g);
        conn.add(g, p, o); // has the default "unnamed" graph
        Assert.assertEquals("unexpected model size", 2, model.size());

        // remove quads having subject http://g via Jena
        Resource r = model.createResource("http://g");
        model.removeAll(r, null, null);
        Assert.assertEquals("unexpected model size", 1, model.size());

        // remove quads having graph http://g via Jena
        AGGraph metadataGraph = closeLater(maker.createGraph("http://g"));
        AGModel metadataModel = closeLater(new AGModel(metadataGraph));
        Assert.assertEquals("unexpected metamodel size", 1, metadataModel.size());
        metadataModel.removeAll();
        Assert.assertEquals("unexpected metamodel size", 0, metadataModel.size());
        Assert.assertEquals("unexpected model size", 0, model.size());

        // add some quads again
        conn.add(s, p, o, g);
        conn.add(g, p, o); // has the default "unnamed" graph
        Assert.assertEquals("unexpected model size", 2, model.size());

        // test that removeAll works for models backed by union graphs
        model.removeAll();
        Assert.assertEquals("unexpected model size", 0, model.size());
    }

    /**
     * Creates a fresh connection and returns the repository size.
     */
    private long getSize() {
        try (final AGRepositoryConnection conn = getConnection()) {
            return conn.size();
        }
    }
}
