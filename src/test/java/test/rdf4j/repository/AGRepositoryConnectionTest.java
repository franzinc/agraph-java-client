package test.rdf4j.repository;

import com.franz.agraph.http.exception.AGMalformedDataException;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
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
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.contextaware.ContextAwareConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.testsuite.repository.RepositoryConnectionTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import test.Util;
import test.rdf4j.RDF4JTestsHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class AGRepositoryConnectionTest extends RepositoryConnectionTest {

    /**
     * Location of local test data that isn't provided via TEST_DIR_PREFIX
     */
    public static final String TEST_DATA_DIR = "/test/";

    @Override
    protected Repository createRepository(File dataDir) throws Exception {
        return RDF4JTestsHelper.getTestRepository();
    }

    @AfterAll
    public static void cleanUp() {
        RDF4JTestsHelper.clearTestRepository();
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Override
    public void testAddMalformedLiteralsDefaultConfig(IsolationLevel level) throws Exception {
        assertThrows(AGMalformedDataException.class, () ->
                super.testAddMalformedLiteralsStrictConfig(level)
        );
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Override
    public void testAddMalformedLiteralsStrictConfig(IsolationLevel level) throws Exception {
        assertThrows(AGMalformedDataException.class, () ->
                super.testAddMalformedLiteralsStrictConfig(level)
        );
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Override
    public void testGetStatementsMalformedTypedLiteral(IsolationLevel level) {
        // TODO: RDF4J expects the repo to handle malformed literal, but AG does not accept them
        assertThrows(AGMalformedDataException.class, () ->
                super.testAddMalformedLiteralsStrictConfig(level)
        );
    }

    @Disabled
    @ParameterizedTest
    @MethodSource({"parameters"})
    @Override
    public void testSizeDuplicateStatement(IsolationLevel level) {
        super.testSizeDuplicateStatement(level);
    }

    @ParameterizedTest
    @MethodSource({"parameters"})
    @Tag("Broken") // TODO: should be passing?
    @Override
    public void testImportNamespacesFromIterable(IsolationLevel level) {
        super.testSizeDuplicateStatement(level);
    }

    @Test
    public void testDefaultInsertContextNull() {
        super.setupTest(IsolationLevels.NONE);
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
        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { ?s ?p ?o }");
        SimpleDataset dataset = new SimpleDataset();
        dataset.addDefaultGraph(defaultGraph);
        query.setDataset(dataset);

        try (TupleQueryResult result = query.evaluate()) {
            int count = 0;
            while (result.hasNext()) {
                result.next();
                count++;
            }
            return count;
        }
    }

    /**
     * TODO: query.evaluate() needs to be inside the try below, in the
     * parent test it's not.
     * <p>
     * TODO: 512 statements are added all at once here, in the parent
     * test there are 512 single adds (far slower, consider rfe10261 to
     * improve the performance of the unmodified parent test).
     */
    @Disabled
    @Test
    public void testOrderByQueriesAreInterruptable() throws Exception {
        testCon.setAutoCommit(false);
        Collection<Statement> stmts = new ArrayList<>();
        for (int index = 0; index < 512; index++) {
            Statement stmt = vf.createStatement(RDFS.CLASS, RDFS.COMMENT, testCon.getValueFactory().createBNode());
            stmts.add(stmt);
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
            assertTrue(duration < 5000, "Query not interrupted quickly enough, should have been ~2s, but was " + (duration / 1000) + "s");
        }
    }

    @Test
    public void testBaseURIInQueryString() {
        super.setupTest(IsolationLevels.NONE);
        testCon.add(vf.createIRI("urn:test:s1"), vf.createIRI("urn:test:p1"), vf.createIRI("urn:test:o1"));
        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "BASE <urn:test:s1> SELECT * { <> ?p ?o }");
        try (TupleQueryResult rs = query.evaluate()) {
            assertTrue(rs.hasNext());
        }
    }

    @Test
    public void testBaseURIInParam() {
        super.setupTest(IsolationLevels.NONE);
        testCon.add(vf.createIRI("http://example.org/s1"), vf.createIRI("urn:test:p1"), vf.createIRI("urn:test:o1"));
        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { <s1> ?p ?o }", "http://example.org");
        try (TupleQueryResult rs = query.evaluate()) {
            assertTrue(rs.hasNext());
        }
    }

    @Test
    public void testBaseURIInParamWithTrailingSlash() {
        super.setupTest(IsolationLevels.NONE);
        testCon.add(vf.createIRI("http://example.org/s1"), vf.createIRI("urn:test:p1"), vf.createIRI("urn:test:o1"));
        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { <s1> ?p ?o }", "http://example.org/");
        try (TupleQueryResult rs = query.evaluate()) {
            assertTrue(rs.hasNext());
        }
    }

    @Test
    public void testHasStatementWithoutBNodes() {
        super.setupTest(IsolationLevels.NONE);
        testCon.add(name, name, nameBob);
        assertTrue(testCon.hasStatement(name, name, nameBob, false),
                "Repository should contain newly added statement");
    }

    @Test
    public void testHasStatementWithBNodes() {
        super.setupTest(IsolationLevels.NONE);
        testCon.add(bob, name, nameBob);
        assertTrue(testCon.hasStatement(bob, name, nameBob, false),
                "Repository should contain newly added statement");
    }

    @Test
    public void testAddGzipInputStreamNTriples() throws Exception {
        super.setupTest(IsolationLevels.NONE);
        // add file default-graph.nt.gz to repository, no context
        File gz = File.createTempFile("default-graph.nt-", ".gz");
        URL resourceUrl = Util.class.getResource(TEST_DATA_DIR + "default-graph.nt");
        assertNotNull(resourceUrl);
        File nt = Paths.get(resourceUrl.toURI()).toFile();

        Util.gzip(nt, gz);
        try (InputStream defaultGraph = Files.newInputStream(gz.toPath())) {
            testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
        }

        assertTrue(testCon.hasStatement(null, publisher, nameBob, false),
                "Repository should contain newly added statements");
        assertTrue(testCon.hasStatement(null, publisher, nameAlice, false),
                "Repository should contain newly added statements");

    }

    @Test
    public void testAddZipFileNTriples() throws Exception {
        super.setupTest(IsolationLevels.NONE);

        InputStream in = Util.resourceAsStream(TEST_DATA_DIR + "graphs.nt.zip");
        testCon.add(in, "", RDFFormat.NTRIPLES);

        assertTrue(testCon.hasStatement(null, publisher, nameBob, false),
                "Repository should contain newly added statements");
        assertTrue(testCon.hasStatement(null, publisher, nameAlice, false),
                "Repository should contain newly added statements");

        assertTrue(testCon.hasStatement(null, name, nameAlice, false),
                "alice should be known in the store");

        assertTrue(testCon.hasStatement(null, name, nameBob, false),
                "bob should be known in the store");
    }

    @Test
    public void testAddReaderNTriples() throws Exception {
        super.setupTest(IsolationLevels.NONE);
        InputStream defaultGraphStream = Util.resourceAsStream(TEST_DATA_DIR + "default-graph.nt");
        Reader defaultGraph = new InputStreamReader(defaultGraphStream, StandardCharsets.UTF_8);

        testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);

        defaultGraph.close();

        assertTrue(testCon.hasStatement(null, publisher, nameBob, false),
                "Repository should contain newly added statements");
        assertTrue(testCon.hasStatement(null, publisher, nameAlice, false),
                "Repository should contain newly added statements");

        // add file graph1.nt to context1
        InputStream graph1Stream = Util.resourceAsStream(TEST_DATA_DIR
                + "graph1.nt");

        try (Reader graph1 = new InputStreamReader(graph1Stream, StandardCharsets.UTF_8)) {
            testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
        }

        // add file graph2.nt to context2
        InputStream graph2Stream = Util.resourceAsStream(TEST_DATA_DIR
                + "graph2.nt");

        try (Reader graph2 = new InputStreamReader(graph2Stream, StandardCharsets.UTF_8)) {
            testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
        }

        assertTrue(testCon.hasStatement(null, name, nameAlice, false),
                "alice should be known in the store");

        assertFalse(testCon.hasStatement(null, name, nameAlice, false, context1),
                "alice should not be known in context1");
        assertTrue(testCon.hasStatement(null, name, nameAlice, false, context2),
                "alice should be known in context2");

        assertTrue(testCon.hasStatement(null, name, nameBob, false),
                "bob should be known in the store");

        assertFalse(testCon.hasStatement(null, name, nameBob, false, context2),
                "bob should not be known in context2");
        assertTrue(testCon.hasStatement(null, name, nameBob, false, context1),
                "bib should be known in context1");

    }

    @Test
    public void testAddInputStreamNTriples() throws Exception {

        super.setupTest(IsolationLevels.NONE);

        // add file default-graph.nt to repository, no context
        try (InputStream defaultGraph = Util.resourceAsStream(TEST_DATA_DIR + "default-graph.nt")) {
            testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
        }

        assertTrue(testCon.hasStatement(null, publisher, nameBob, false),
                "Repository should contain newly added statements");
        assertTrue(testCon.hasStatement(null, publisher, nameAlice, false),
                "Repository should contain newly added statements");

        // add file graph1.nt to context1

        try (InputStream graph1 = Util.resourceAsStream(TEST_DATA_DIR + "graph1.nt")) {
            testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
        }

        // add file graph2.nt to context2

        try (InputStream graph2 = Util.resourceAsStream(TEST_DATA_DIR + "graph2.nt")) {
            testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
        }

        assertTrue(testCon.hasStatement(null, name, nameAlice, false),
                "alice should be known in the store");

        assertFalse(testCon.hasStatement(null, name, nameAlice, false, context1),
                "alice should not be known in context1");
        assertTrue(testCon.hasStatement(null, name, nameAlice, false, context2),
                "alice should be known in context2");

        assertTrue(testCon.hasStatement(null, name, nameBob, false),
                "bob should be known in the store");

        assertFalse(testCon.hasStatement(null, name, nameBob, false, context2),
                "bob should not be known in context2");
        assertTrue(testCon.hasStatement(null, name, nameBob, false, context1), "bib should be known in context1");

    }

    @Test
    public void testRecoverFromParseErrorNTriples() throws RepositoryException, IOException {

        super.setupTest(IsolationLevels.NONE);

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

        assertEquals(1, testCon.size(), "Repository contains incorrect number of statements");
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    public void testSimpleTupleQuery() {

        super.setupTest(IsolationLevels.NONE);

        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);

        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);

        String queryString = """
                PREFIX foaf: <$FOAF_NS>
                SELECT ?name ?mbox
                WHERE { ?x foaf:name ?name .
                        ?x foaf:mbox ?mbox .}
                """
                .replace("$FOAF_NS", FOAF_NS);

        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

        try (TupleQueryResult result = query.evaluate()) {
            assertNotNull(result);
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
        }
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    public void testSimpleTupleQueryUnicode() {

        super.setupTest(IsolationLevels.NONE);

        testCon.add(alexander, name, Александър);

        String queryString = """
                    PREFIX foaf: <$FOAF_NS>
                    SELECT ?person WHERE { ?person foaf:name '$NAME' .}
                """
                .replace("$FOAF_NS", FOAF_NS)
                .replace("$NAME", Александър.getLabel());

        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        try (TupleQueryResult result = query.evaluate()) {
            assertNotNull(result);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                BindingSet solution = result.next();
                assertTrue(solution.hasBinding("person"));
                assertEquals(alexander, solution.getValue("person"));
            }
        }
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    public void testPreparedTupleQuery() {

        super.setupTest(IsolationLevels.NONE);

        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);

        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);

        String queryString = """
                        PREFIX foaf: <$FOAF_NS>
                        SELECT ?name ?mbox
                        WHERE { ?x foaf:name ?name .
                                ?x foaf:mbox ?mbox .}
                """
                .replace("$FOAF_NS", FOAF_NS);

        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        query.setBinding("name", nameBob);

        try (TupleQueryResult result = query.evaluate()) {
            assertNotNull(result);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                BindingSet solution = result.next();
                assertTrue(solution.hasBinding("name"));
                assertTrue(solution.hasBinding("mbox"));

                Value nameResult = solution.getValue("name");
                Value mboxResult = solution.getValue("mbox");

                assertEquals(nameBob, nameResult, "unexpected value for name: " + nameResult);
                assertEquals(mboxBob, mboxResult, "unexpected value for mbox: " + mboxResult);
            }
        }
    }

    @Test
    public void testPreparedTupleQuery2() {

        super.setupTest(IsolationLevels.NONE);

        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);

        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);

        String queryString = """
                PREFIX foaf: <$FOAF_NS>
                SELECT ?name ?mbox
                WHERE { ?x foaf:name ?name;
                           foaf:mbox ?mbox .}
                """
                .replace("$FOAF_NS", FOAF_NS);

        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        query.setBinding("x", bob);

        try (TupleQueryResult result = query.evaluate()) {
            assertNotNull(result);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                BindingSet solution = result.next();
                assertTrue(solution.hasBinding("name"));
                assertTrue(solution.hasBinding("mbox"));

                Value nameResult = solution.getValue("name");
                Value mboxResult = solution.getValue("mbox");

                assertEquals(nameBob, nameResult, "unexpected value for name: " + nameResult);
                assertEquals(mboxBob, mboxResult, "unexpected value for mbox: " + mboxResult);
            }
        }
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    public void testPreparedTupleQueryUnicode() {

        super.setupTest(IsolationLevels.NONE);

        testCon.add(alexander, name, Александър);

        String queryString = """
                PREFIX foaf: <$FOAF_NS>
                SELECT ?person WHERE { ?person foaf:name '$NAME' .}
                """
                .replace("$FOAF_NS", FOAF_NS)
                .replace("$NAME", Александър.getLabel());

        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        query.setBinding("name", Александър);

        try (TupleQueryResult result = query.evaluate()) {
            assertNotNull(result);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                BindingSet solution = result.next();
                assertTrue(solution.hasBinding("person"));
                assertEquals(alexander, solution.getValue("person"));
            }
        }
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    public void testSimpleGraphQuery() {

        super.setupTest(IsolationLevels.NONE);

        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);

        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);

        String queryString = """
                PREFIX foaf: <$FOAF_NS>
                CONSTRUCT { ?x foaf:name ?name .
                            ?x foaf:mbox ?mbox .}
                WHERE { ?x foaf:name ?name .
                         ?x foaf:mbox ?mbox .}
                """
                .replace("$FOAF_NS", FOAF_NS);

        GraphQuery query = testCon.prepareGraphQuery(QueryLanguage.SPARQL, queryString);

        try (GraphQueryResult result = query.evaluate()) {
            assertNotNull(result);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                Statement st = result.next();
                if (name.equals(st.getPredicate())) {
                    assertTrue(nameAlice.equals(st.getObject())
                            || nameBob.equals(st.getObject()));
                } else {
                    assertEquals(mbox, st.getPredicate());
                    assertTrue(mboxAlice.equals(st.getObject())
                            || mboxBob.equals(st.getObject()));
                }
            }
        }
    }

    /**
     * A rewrite using SPARQL rather than SeRQL.
     */
    @Test
    public void testPreparedGraphQuery() {

        super.setupTest(IsolationLevels.NONE);

        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);

        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);

        String queryString = """
                PREFIX foaf: <$FOAF_NS>
                CONSTRUCT { ?x foaf:name ?name .
                            ?x foaf:mbox ?mbox .}
                WHERE { ?x foaf:name ?name .
                        ?x foaf:mbox ?mbox .}
                """
                .replace("$FOAF_NS", FOAF_NS);

        GraphQuery query = testCon.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        query.setBinding("name", nameBob);

        try (GraphQueryResult result = query.evaluate()) {
            assertNotNull(result);
            assertTrue(result.hasNext());

            while (result.hasNext()) {
                Statement st = result.next();
                assertTrue(name.equals(st.getPredicate())
                        || mbox.equals(st.getPredicate()));
                if (name.equals(st.getPredicate())) {
                    assertEquals(nameBob, st.getObject(), "unexpected value for name: " + st.getObject());
                } else {
                    assertEquals(mbox, st.getPredicate());
                    assertEquals(mboxBob, st.getObject(), "unexpected value for mbox: " + st.getObject());
                }
            }
        }
    }


    @Test
    public void testGetStatementsInMultipleContexts() {

        super.setupTest(IsolationLevels.NONE);

        testCon.clear();

        testCon.begin();
        testCon.add(alice, name, nameAlice, context2);
        testCon.add(alice, mbox, mboxAlice, context2);
        testCon.add(context2, publisher, nameAlice);
        testCon.commit();

        // get statements with either no context or context2
        try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, null, context2)) {
            int count = 0;
            while (result.hasNext()) {
                count++;
                Resource context = result.next().getContext();
                assertTrue(context == null || context2.equals(context));
            }
            assertEquals(3, count, "there should be three statements");
        }

        // get all statements with context1 or context2. Note that context1 and
        // context2 are both known
        // in the store because they have been created through the store's own
        // value vf.
        try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, context1, context2)) {
            int count = 0;
            while (result.hasNext()) {
                count++;
                // we should have _only_ statements from context2
                Resource context = result.next().getContext();
                assertEquals(context2, context);
            }
            assertEquals(2, count, "there should be two statements");
        }

        // get all statements with unknownContext or context2.
        try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, unknownContext, context2)) {
            int count = 0;
            while (result.hasNext()) {
                count++;
                Resource context = result.next().getContext();
                // we should have _only_ statements from context2
                assertEquals(context2, context);
            }
            assertEquals(2, count,"there should be two statements");
        }

        // add statements to context1
        testCon.begin();
        testCon.add(bob, name, nameBob, context1);
        testCon.add(bob, mbox, mboxBob, context1);
        testCon.add(context1, publisher, nameBob);
        testCon.commit();

        try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, context1)) {
            assertNotNull(result);
            assertTrue(result.hasNext());
        }

        // get statements with either no context or context2
        try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, null, context2)) {
            int count = 0;
            while (result.hasNext()) {
                count++;
                Resource context = result.next().getContext();
                // we should have _only_ statements from context2, or without context
                assertTrue(context == null || context2.equals(context));
            }
            assertEquals(4, count, "there should be four statements");
        }

        // get all statements with context1 or context2
        try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, context1, context2)) {
            int count = 0;
            while (result.hasNext()) {
                count++;
                Resource context = result.next().getContext();
                assertTrue(context1.equals(context) || context2.equals(context));
            }
            assertEquals(4, count, "there should be four statements");
        }
    }

    @Test
    public void testOptionalFilter() {

        super.setupTest(IsolationLevels.NONE);

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
        Set<List<Value>> set = new HashSet<>();
        try (TupleQueryResult result = query.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindings = result.next();
                set.add(Arrays.asList(bindings.getValue("v1"), bindings.getValue("v2")));
            }
        }
        assertTrue(set.contains(Arrays.asList(v1, v2)));
        assertTrue(set.contains(Arrays.asList(v3, null)));
    }

    @Test
    public void testOrPredicate() {

        super.setupTest(IsolationLevels.NONE);

        String union = "{ :s ?p :o FILTER (?p = :p1 || ?p = :p2) }";
        IRI s = vf.createIRI("urn:test:s");
        IRI p1 = vf.createIRI("urn:test:p1");
        IRI p2 = vf.createIRI("urn:test:p2");
        IRI o = vf.createIRI("urn:test:o");
        testCon.add(s, p1, o);
        testCon.add(s, p2, o);
        String qry = "PREFIX :<urn:test:> SELECT ?p WHERE " + union;
        TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, qry);
        List<Value> list = new ArrayList<>();
        try (TupleQueryResult result = query.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindings = result.next();
                list.add(bindings.getValue("p"));
            }
        }
        assertTrue(list.contains(p1));
        assertTrue(list.contains(p2));
    }

    @Test
    public void testGraphSerialization() throws Exception {

        super.setupTest(IsolationLevels.NONE);

        testCon.add(bob, name, nameBob);
        testCon.add(alice, name, nameAlice);

        Model graph;
        try (RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, true)) {
            graph = new LinkedHashModel(Iterations.asList(statements));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(graph);
        out.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        Model deserializedGraph = (Model) in.readObject();
        in.close();

        assertFalse(deserializedGraph.isEmpty());
        for (Statement st : deserializedGraph) {
            assertTrue(graph.contains(st));
            assertTrue(testCon.hasStatement(st, true));
        }
    }


    @Test
    public void testGetNamespaces() {

        super.setupTest(IsolationLevels.NONE);

        testCon.setNamespace("example", "http://example.org/");
        testCon.setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        testCon.setNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

        // Translated from earlier RDF document. Is this line even necessary?
        testCon.add(
                vf.createIRI("http://example.org/", "Main"),
                vf.createIRI("http://www.w3.org/2000/01/rdf-schema#", "label"),
                vf.createLiteral("Main Node"));

        Map<String, String> map = Namespaces.asMap(Iterations.asSet(testCon.getNamespaces()));
        assertEquals(3, map.size());
        assertEquals("http://example.org/", map.get("example"));
        assertEquals("http://www.w3.org/2000/01/rdf-schema#", map.get("rdfs"));
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", map.get("rdf"));
    }
}
