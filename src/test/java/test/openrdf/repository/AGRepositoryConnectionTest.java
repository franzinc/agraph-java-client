package test.openrdf.repository;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.StatementImpl;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.DatasetImpl;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnectionTest;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.contextaware.ContextAwareConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.junit.Ignore;
import org.junit.Test;
import test.AGAbstractTest;
import test.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class AGRepositoryConnectionTest extends RepositoryConnectionTest {

    /**
     * Location of local test data that isn't provided via TEST_DIR_PREFIX
     */
    public static final String TEST_DATA_DIR = "/test/";


    public AGRepositoryConnectionTest() {
        super(IsolationLevels.SNAPSHOT);
    }


    @Override
    protected Repository createRepository() throws Exception {
        return AGAbstractTest.sharedRepository();
    }

    @Override
    public void testDeleteDefaultGraph() throws Exception {
        super.testDeleteDefaultGraph();
    }

    @Override
    public void testDefaultContext() throws Exception {
        super.testDefaultContext();
    }

    @Override
    public void testDefaultInsertContext() throws Exception {
        super.testDefaultInsertContext();
    }

    @Test
    public void testDefaultInsertContextNull()
            throws Exception {
        ContextAwareConnection con = new ContextAwareConnection(testCon);
        IRI defaultGraph = null;
        con.setInsertContext(defaultGraph);
        con.add(vf.createIRI("urn:test:s1"), vf.createIRI("urn:test:p1"), vf.createIRI("urn:test:o1"));
        con.prepareUpdate("INSERT DATA { <urn:test:s2> <urn:test:p2> \"l2\" }").execute();
        assertEquals(2, Iterations.asList(con.getStatements(null, null, null)).size());
        assertEquals(2, Iterations.asList(con.getStatements(null, null, null, defaultGraph)).size());
        assertEquals(2, size(defaultGraph));
        con.add(vf.createIRI("urn:test:s3"), vf.createIRI("urn:test:p3"), vf.createIRI("urn:test:o3"), (Resource) null);
        con.add(vf.createIRI("urn:test:s4"), vf.createIRI("urn:test:p4"), vf.createIRI("urn:test:o4"), vf.createIRI("urn:test:other"));
        assertEquals(4, Iterations.asList(con.getStatements(null, null, null)).size());
        assertEquals(3, Iterations.asList(con.getStatements(null, null, null, defaultGraph)).size());
        assertEquals(4, Iterations.asList(testCon.getStatements(null, null, null, true)).size());
        assertEquals(3, size(defaultGraph));
        assertEquals(1, size(vf.createIRI("urn:test:other")));
        con.prepareUpdate("DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }").execute();
        assertEquals(0, Iterations.asList(con.getStatements(null, null, null)).size());
        assertEquals(0, Iterations.asList(testCon.getStatements(null, null, null, true)).size());
        assertEquals(0, size(defaultGraph));
        assertEquals(0, size(vf.createIRI("urn:test:other")));
    }

    private int size(IRI defaultGraph)
            throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        TupleQuery qry = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { ?s ?p ?o }");
        DatasetImpl dataset = new DatasetImpl();
        dataset.addDefaultGraph(defaultGraph);
        qry.setDataset(dataset);
        TupleQueryResult result = qry.evaluate();
        try {
            int count = 0;
            while (result.hasNext()) {
                result.next();
                count++;
            }
            return count;
        } finally {
            result.close();
        }
    }

    @Ignore
    @Override
    public void testExclusiveNullContext() throws Exception {
        //super.testExclusiveNullContext();
        //ignore
    }

    /**
     * TODO: query.evaluate() needs to be inside the try below, in the
     * parent test it's not.
     * <p>
     * TODO: 512 statements are added all at once here, in the parent
     * test there are 512 single adds (far slower, consider rfe10261 to
     * improve the performance of the unmodified parent test).
     */
    public void testOrderByQueriesAreInterruptable() throws Exception {
        testCon.setAutoCommit(false);
        Collection<Statement> stmts = new ArrayList<Statement>();
        for (int index = 0; index < 512; index++) {
            stmts.add(new StatementImpl(RDFS.CLASS, RDFS.COMMENT, testCon.getValueFactory().createBNode()));
        }
        testCon.add(stmts);
        testCon.setAutoCommit(true);

        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL,
                "SELECT * WHERE { ?s ?p ?o . ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 . ?s3 ?p3 ?o3 } ORDER BY ?s1 ?p1 ?o1 LIMIT 1000");
        query.setMaxQueryTime(2);

        long startTime = System.currentTimeMillis();
        try {
            TupleQueryResult result = query.evaluate();
            result.hasNext();
            fail("Query should have been interrupted");
        } catch (QueryInterruptedException e) {
            // Expected
            long duration = System.currentTimeMillis() - startTime;

            assertTrue("Query not interrupted quickly enough, should have been ~2s, but was "
                    + (duration / 1000) + "s", duration < 5000);
        }
    }

    /*@Override
    public void testGetNamespaces() throws Exception {
        super.testGetNamespaces();
    }*/

    @Override
    public void testXmlCalendarZ() throws Exception {
        super.testXmlCalendarZ();
    }

    @Override
    public void testSES713() throws Exception {
        super.testSES713();
    }

    public void testBaseURIInQueryString() throws Exception {
        testCon.add(vf.createIRI("urn:test:s1"), vf.createIRI("urn:test:p1"), vf.createIRI("urn:test:o1"));
        TupleQueryResult rs = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "BASE <urn:test:s1> SELECT * { <> ?p ?o }").evaluate();
        try {
            assertTrue(rs.hasNext());
        } finally {
            rs.close();
        }
    }

    public void testBaseURIInParam() throws Exception {
        testCon.add(vf.createIRI("http://example.org/s1"), vf.createIRI("urn:test:p1"), vf.createIRI("urn:test:o1"));
        TupleQueryResult rs = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { <s1> ?p ?o }", "http://example.org").evaluate();
        try {
            assertTrue(rs.hasNext());
        } finally {
            rs.close();
        }
    }

    public void testBaseURIInParamWithTrailingSlash() throws Exception {
        testCon.add(vf.createIRI("http://example.org/s1"), vf.createIRI("urn:test:p1"), vf.createIRI("urn:test:o1"));
        TupleQueryResult rs = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { <s1> ?p ?o }", "http://example.org/").evaluate();
        try {
            assertTrue(rs.hasNext());
        } finally {
            rs.close();
        }
    }

    @Test
    public void testHasStatementWithoutBNodes() throws Exception {
        testCon.add(name, name, nameBob);

        assertTrue("Repository should contain newly added statement", testCon
                .hasStatement(name, name, nameBob, false));
    }

    @Test
    public void testHasStatementWithBNodes() throws Exception {
        testCon.add(bob, name, nameBob);

        assertTrue("Repository should contain newly added statement", testCon
                .hasStatement(bob, name, nameBob, false));

    }

    @Test
    public void testAddGzipInputStreamNTriples() throws Exception {
        // add file default-graph.nt.gz to repository, no context
        File gz = File.createTempFile("default-graph.nt-", ".gz");
        File nt = new File(TEST_DATA_DIR + "default-graph.nt");
        Util.gzip(nt, gz);
        InputStream defaultGraph = new FileInputStream(gz);
        //RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "default-graph.nt.gz");
        try {
            testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
        } finally {
            defaultGraph.close();
        }

        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameBob, false));
        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameAlice, false));

    }

    @Test
    public void testAddZipFileNTriples() throws Exception {
        InputStream in = Util.resourceAsStream(TEST_DATA_DIR + "graphs.nt.zip");

        testCon.add(in, "", RDFFormat.NTRIPLES);

        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameBob, false));
        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameAlice, false));

        assertTrue("alice should be known in the store", testCon.hasStatement(
                null, name, nameAlice, false));

        assertTrue("bob should be known in the store", testCon.hasStatement(
                null, name, nameBob, false));
    }

    @Test
    public void testAddReaderNTriples() throws Exception {
        InputStream defaultGraphStream = Util.resourceAsStream(TEST_DATA_DIR
                + "default-graph.nt");
        Reader defaultGraph = new InputStreamReader(defaultGraphStream, "UTF-8");

        testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);

        defaultGraph.close();

        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameBob, false));
        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameAlice, false));

        // add file graph1.nt to context1
        InputStream graph1Stream = Util.resourceAsStream(TEST_DATA_DIR
                + "graph1.nt");
        Reader graph1 = new InputStreamReader(graph1Stream, "UTF-8");

        try {
            testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
        } finally {
            graph1.close();
        }

        // add file graph2.nt to context2
        InputStream graph2Stream = Util.resourceAsStream(TEST_DATA_DIR
                + "graph2.nt");
        Reader graph2 = new InputStreamReader(graph2Stream, "UTF-8");

        try {
            testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
        } finally {
            graph2.close();
        }

        assertTrue("alice should be known in the store", testCon.hasStatement(
                null, name, nameAlice, false));

        assertFalse("alice should not be known in context1", testCon
                .hasStatement(null, name, nameAlice, false, context1));
        assertTrue("alice should be known in context2", testCon.hasStatement(
                null, name, nameAlice, false, context2));

        assertTrue("bob should be known in the store", testCon.hasStatement(
                null, name, nameBob, false));

        assertFalse("bob should not be known in context2", testCon
                .hasStatement(null, name, nameBob, false, context2));
        assertTrue("bib should be known in context1", testCon.hasStatement(
                null, name, nameBob, false, context1));

    }

    @Test
    public void testAddInputStreamNTriples() throws Exception {
        // add file default-graph.nt to repository, no context
        InputStream defaultGraph = Util.resourceAsStream(TEST_DATA_DIR
                + "default-graph.nt");

        try {
            testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
        } finally {
            defaultGraph.close();
        }

        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameBob, false));
        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameAlice, false));

        // add file graph1.nt to context1
        InputStream graph1 = Util.resourceAsStream(TEST_DATA_DIR + "graph1.nt");

        try {
            testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
        } finally {
            graph1.close();
        }

        // add file graph2.nt to context2
        InputStream graph2 = Util.resourceAsStream(TEST_DATA_DIR + "graph2.nt");

        try {
            testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
        } finally {
            graph2.close();
        }

        assertTrue("alice should be known in the store", testCon.hasStatement(
                null, name, nameAlice, false));

        assertFalse("alice should not be known in context1", testCon
                .hasStatement(null, name, nameAlice, false, context1));
        assertTrue("alice should be known in context2", testCon.hasStatement(
                null, name, nameAlice, false, context2));

        assertTrue("bob should be known in the store", testCon.hasStatement(
                null, name, nameBob, false));

        assertFalse("bob should not be known in context2", testCon
                .hasStatement(null, name, nameBob, false, context2));
        assertTrue("bib should be known in context1", testCon.hasStatement(
                null, name, nameBob, false, context1));

    }

    @Test
    public void testRecoverFromParseErrorNTriples() throws RepositoryException,
            IOException {
        String invalidData = "bad";
        String validData = "<http://example.org/foo#a> <http://example.org/foo#b> <http://example.org/foo#c> .";

        try {
            testCon.add(new StringReader(invalidData), "", RDFFormat.NTRIPLES);
            fail("Invalid data should result in an exception");
        } catch (RDFParseException e) {
            // Expected behaviour
        }

        try {
            testCon.add(new StringReader(validData), "", RDFFormat.NTRIPLES);
        } catch (RDFParseException e) {
            fail("Valid data should not result in an exception");
        }

        assertEquals("Repository contains incorrect number of statements", 1,
                testCon.size());
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
    public void testSimpleTupleQuery() throws Exception {
        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);

        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
        queryBuilder.append(" SELECT ?name ?mbox");
        queryBuilder.append(" WHERE { ?x foaf:name ?name .");
        queryBuilder.append("         ?x foaf:mbox ?mbox .}");

        TupleQueryResult result = testCon.prepareTupleQuery(
                QueryLanguage.SPARQL, queryBuilder.toString()).evaluate();

        try {
            assertTrue(result != null);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                BindingSet solution = result.next();
                assertTrue(solution.hasBinding("name"));
                assertTrue(solution.hasBinding("mbox"));

                Value nameResult = solution.getValue("name");
                Value mboxResult = solution.getValue("mbox");

                assertTrue((nameAlice.equals(nameResult) || nameBob
                        .equals(nameResult)));
                assertTrue((mboxAlice.equals(mboxResult) || mboxBob
                        .equals(mboxResult)));
            }
        } finally {
            result.close();
        }
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
    public void testSimpleTupleQueryUnicode() throws Exception {
        testCon.add(alexander, name, Александър);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
        queryBuilder.append(" SELECT ?person");
        queryBuilder.append(" WHERE { ?person foaf:name '");
        queryBuilder.append(Александър.getLabel()).append("' .}");

        TupleQueryResult result = testCon.prepareTupleQuery(
                QueryLanguage.SPARQL, queryBuilder.toString()).evaluate();

        try {
            assertTrue(result != null);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                BindingSet solution = result.next();
                assertTrue(solution.hasBinding("person"));
                assertEquals(alexander, solution.getValue("person"));
            }
        } finally {
            result.close();
        }
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
    public void testPreparedTupleQuery() throws Exception {
        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);

        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
        queryBuilder.append(" SELECT ?name ?mbox");
        queryBuilder.append(" WHERE { ?x foaf:name ?name .");
        queryBuilder.append("         ?x foaf:mbox ?mbox .}");

        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL,
                queryBuilder.toString());
        query.setBinding("name", nameBob);

        TupleQueryResult result = query.evaluate();

        try {
            assertTrue(result != null);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                BindingSet solution = result.next();
                assertTrue(solution.hasBinding("name"));
                assertTrue(solution.hasBinding("mbox"));

                Value nameResult = solution.getValue("name");
                Value mboxResult = solution.getValue("mbox");

                assertEquals("unexpected value for name: " + nameResult,
                        nameBob, nameResult);
                assertEquals("unexpected value for mbox: " + mboxResult,
                        mboxBob, mboxResult);
            }
        } finally {
            result.close();
        }
    }

    public void testPreparedTupleQuery2()
            throws Exception {
        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);

        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
        queryBuilder.append(" SELECT ?name ?mbox");
        queryBuilder.append(" WHERE { ?x foaf:name ?name;");
        queryBuilder.append("            foaf:mbox ?mbox .}");

        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryBuilder.toString());
        query.setBinding("x", bob);

        TupleQueryResult result = query.evaluate();

        try {
            assertTrue(result != null);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                BindingSet solution = result.next();
                assertTrue(solution.hasBinding("name"));
                assertTrue(solution.hasBinding("mbox"));

                Value nameResult = solution.getValue("name");
                Value mboxResult = solution.getValue("mbox");

                assertEquals("unexpected value for name: " + nameResult, nameBob, nameResult);
                assertEquals("unexpected value for mbox: " + mboxResult, mboxBob, mboxResult);
            }
        } finally {
            result.close();
        }
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
    public void testPreparedTupleQueryUnicode() throws Exception {
        testCon.add(alexander, name, Александър);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
        queryBuilder.append(" SELECT ?person");
        queryBuilder.append(" WHERE { ?person foaf:name '");
        queryBuilder.append(Александър.getLabel()).append("' .}");

        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL,
                queryBuilder.toString());
        query.setBinding("name", Александър);

        TupleQueryResult result = query.evaluate();

        try {
            assertTrue(result != null);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                BindingSet solution = result.next();
                assertTrue(solution.hasBinding("person"));
                assertEquals(alexander, solution.getValue("person"));
            }
        } finally {
            result.close();
        }
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
    public void testSimpleGraphQuery() throws Exception {
        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);

        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
        queryBuilder.append(" CONSTRUCT { ?x foaf:name ?name .");
        queryBuilder.append("         ?x foaf:mbox ?mbox .}");
        queryBuilder.append(" WHERE { ?x foaf:name ?name .");
        queryBuilder.append("         ?x foaf:mbox ?mbox .}");

        GraphQueryResult result = testCon.prepareGraphQuery(
                QueryLanguage.SPARQL, queryBuilder.toString()).evaluate();

        try {
            assertTrue(result != null);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                Statement st = result.next();
                if (name.equals(st.getPredicate())) {
                    assertTrue(nameAlice.equals(st.getObject())
                            || nameBob.equals(st.getObject()));
                } else {
                    assertTrue(mbox.equals(st.getPredicate()));
                    assertTrue(mboxAlice.equals(st.getObject())
                            || mboxBob.equals(st.getObject()));
                }
            }
        } finally {
            result.close();
        }
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    @Override
    public void testPreparedGraphQuery() throws Exception {
        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);

        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
        queryBuilder.append(" CONSTRUCT { ?x foaf:name ?name .");
        queryBuilder.append("         ?x foaf:mbox ?mbox .}");
        queryBuilder.append(" WHERE { ?x foaf:name ?name .");
        queryBuilder.append("         ?x foaf:mbox ?mbox .}");

        GraphQuery query = testCon.prepareGraphQuery(QueryLanguage.SPARQL,
                queryBuilder.toString());
        query.setBinding("name", nameBob);

        GraphQueryResult result = query.evaluate();

        try {
            assertTrue(result != null);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                Statement st = result.next();
                assertTrue(name.equals(st.getPredicate())
                        || mbox.equals(st.getPredicate()));
                if (name.equals(st.getPredicate())) {
                    assertTrue("unexpected value for name: " + st.getObject(),
                            nameBob.equals(st.getObject()));
                } else {
                    assertTrue(mbox.equals(st.getPredicate()));
                    assertTrue("unexpected value for mbox: " + st.getObject(),
                            mboxBob.equals(st.getObject()));
                }

            }
        } finally {
            result.close();
        }
    }

    /**
     * AllegroGraph doesn't support SeRQL; test passes if
     * server reports that SeRQL is unsupported.
     */
    @Test
    @Override
    public void testPrepareSeRQLQuery() throws Exception {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" SELECT person");
        queryBuilder.append(" FROM {person} foaf:name {").append(Александър.getLabel()).append("}");
        queryBuilder.append(" USING NAMESPACE foaf = <" + FOAF_NS + ">");

        try {
            testCon.prepareQuery(QueryLanguage.SERQL, queryBuilder.toString());
        } catch (UnsupportedOperationException e) {
            // expected
        } catch (ClassCastException e) {
            fail("unexpected query object type: " + e.getMessage());
        }

    }

    @Override
    public void testGetStatementsInMultipleContexts()
            throws Exception {
        testCon.clear();

        testCon.begin();
        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);
        testCon.commit();

        // get statements with either no context or context2
        CloseableIteration<? extends Statement, RepositoryException> iter = testCon.getStatements(null, null,
                null, false, null, context2);

        try {
            int count = 0;
            while (iter.hasNext()) {
                count++;
                Statement st = iter.next();
                assertThat(st.getContext(), anyOf(is(nullValue(Resource.class)), is(equalTo((Resource) context2))));
            }

            assertEquals("there should be three statements", 3, count);
        } finally {
            iter.close();
        }

        // get all statements with context1 or context2. Note that context1 and
        // context2 are both known
        // in the store because they have been created through the store's own
        // value vf.
        iter = testCon.getStatements(null, null, null, false, context1, context2);

        try {
            int count = 0;
            while (iter.hasNext()) {
                count++;
                Statement st = iter.next();
                // we should have _only_ statements from context2
                assertThat(st.getContext(), is(equalTo((Resource) context2)));
            }
            assertEquals("there should be two statements", 2, count);
        } finally {
            iter.close();
        }

        // get all statements with unknownContext or context2.
        iter = testCon.getStatements(null, null, null, false, unknownContext, context2);

        try {
            int count = 0;
            while (iter.hasNext()) {
                count++;
                Statement st = iter.next();
                // we should have _only_ statements from context2
                assertThat(st.getContext(), is(equalTo((Resource) context2)));
            }
            assertEquals("there should be two statements", 2, count);
        } finally {
            iter.close();
        }

        // add statements to context1
        testCon.begin();
        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);
        testCon.commit();

        iter = testCon.getStatements(null, null, null, false, context1);
        try {
            assertThat(iter, is(notNullValue()));
            assertThat(iter.hasNext(), is(equalTo(true)));
        } finally {
            iter.close();
        }

        // get statements with either no context or context2
        iter = testCon.getStatements(null, null, null, false, null, context2);
        try {
            int count = 0;
            while (iter.hasNext()) {
                count++;
                Statement st = iter.next();
                // we should have _only_ statements from context2, or without
                // context
                assertThat(st.getContext(), anyOf(is(nullValue(Resource.class)), is(equalTo((Resource) context2))));
            }
            assertEquals("there should be four statements", 4, count);
        } finally {
            iter.close();
        }

        // get all statements with context1 or context2
        iter = testCon.getStatements(null, null, null, false, context1, context2);

        try {
            int count = 0;
            while (iter.hasNext()) {
                count++;
                Statement st = iter.next();
                assertThat(st.getContext(),
                        anyOf(is(equalTo((Resource) context1)), is(equalTo((Resource) context2))));
            }
            assertEquals("there should be four statements", 4, count);
        } finally {
            iter.close();
        }
    }

    @Override
    public void testOptionalFilter()
            throws Exception {
        String optional = "{ ?s :p1 ?v1 OPTIONAL {?s :p2 ?v2 FILTER(?v1<3) } }";
        IRI s = vf.createIRI("urn:test:s");
        IRI p1 = vf.createIRI("urn:test:p1");
        IRI p2 = vf.createIRI("urn:test:p2");
        Value v1 = vf.createLiteral(1);
        Value v2 = vf.createLiteral(2);
        Value v3 = vf.createLiteral(3);
        testCon.add(s, p1, v1);
        testCon.add(s, p2, v2);
        testCon.add(s, p1, v3);
        String qry = "PREFIX :<urn:test:> SELECT ?s ?v1 ?v2 WHERE " + optional;
        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, qry);
        TupleQueryResult result = query.evaluate();
        Set<List<Value>> set = new HashSet<List<Value>>();
        while (result.hasNext()) {
            BindingSet bindings = result.next();
            set.add(Arrays.asList(bindings.getValue("v1"), bindings.getValue("v2")));
        }
        result.close();
        assertThat(set, hasItem(Arrays.asList(v1, v2)));
        assertThat(set, hasItem(Arrays.asList(v3, null)));
    }

    @Override
    public void testOrPredicate()
            throws Exception {
        String union = "{ :s ?p :o FILTER (?p = :p1 || ?p = :p2) }";
        IRI s = vf.createIRI("urn:test:s");
        IRI p1 = vf.createIRI("urn:test:p1");
        IRI p2 = vf.createIRI("urn:test:p2");
        IRI o = vf.createIRI("urn:test:o");
        testCon.add(s, p1, o);
        testCon.add(s, p2, o);
        String qry = "PREFIX :<urn:test:> SELECT ?p WHERE " + union;
        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, qry);
        TupleQueryResult result = query.evaluate();
        List<Value> list = new ArrayList<Value>();
        while (result.hasNext()) {
            BindingSet bindings = result.next();
            list.add(bindings.getValue("p"));
        }
        result.close();
        assertThat(list, hasItem(p1));
        assertThat(list, hasItem(p2));
    }

    @Override
    public void testGraphSerialization()
            throws Exception {
        testCon.add(bob, name, nameBob);
        testCon.add(alice, name, nameAlice);

        Model graph;
        RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, true);
        try {
            graph = new LinkedHashModel(Iterations.asList(statements));
        } finally {
            statements.close();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(graph);
        out.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        Model deserializedGraph = (Model) in.readObject();
        in.close();

        assertThat(deserializedGraph.isEmpty(), is(equalTo(false)));

        for (Statement st : deserializedGraph) {
            assertThat(graph, hasItem(st));
            assertThat(testCon.hasStatement(st, true), is(equalTo(true)));
        }
    }


    @Override
    public void testGetNamespaces()
            throws Exception {
        setupNamespaces();
        Map<String, String> map = Namespaces.asMap(Iterations.asSet(testCon.getNamespaces()));
        assertThat(map.size(), is(equalTo(3)));
        assertThat(map.keySet(), hasItems("example", "rdfs", "rdf"));
        assertThat(map.get("example"), is(equalTo("http://example.org/")));
        assertThat(map.get("rdfs"), is(equalTo("http://www.w3.org/2000/01/rdf-schema#")));
        assertThat(map.get("rdf"), is(equalTo("http://www.w3.org/1999/02/22-rdf-syntax-ns#")));
    }

    private void setupNamespaces()
            throws IOException, RDFParseException, RepositoryException {
        testCon.setNamespace("example", "http://example.org/");
        testCon.setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        testCon.setNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

        // Translated from earlier RDF document. Is this line even necessary?
        testCon.add(vf.createIRI("http://example.org/", "Main"), vf.createIRI("http://www.w3.org/2000/01/rdf-schema#", "label"),
                vf.createLiteral("Main Node"));
    }

}
