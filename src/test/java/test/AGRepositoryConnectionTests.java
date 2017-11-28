/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRDFFormat;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import test.RepositoryConnectionTest.RepositoryConnectionTests;
import test.TestSuites.NonPrepushTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class AGRepositoryConnectionTests extends RepositoryConnectionTests {

    private static final String TEST_REPO_1 = "testRepo1";
    private String oldUseAddStatementBuffer;
    private String oldAddStatementBufferMaxSize;

    @Before
    public void setUp()
            throws Exception {
        super.setUp();

        oldUseAddStatementBuffer = System.getProperty(AGRepositoryConnection.PROP_USE_ADD_STATEMENT_BUFFER, null);
        oldAddStatementBufferMaxSize = System.getProperty(AGRepositoryConnection.PROP_ADD_STATEMENT_BUFFER_MAX_SIZE, null);
    }

    @After
    public void tearDown()
            throws Exception {
        if (oldUseAddStatementBuffer != null) {
            System.setProperty(AGRepositoryConnection.PROP_USE_ADD_STATEMENT_BUFFER, oldUseAddStatementBuffer);
        } else {
            System.clearProperty(AGRepositoryConnection.PROP_USE_ADD_STATEMENT_BUFFER);
        }
        if (oldAddStatementBufferMaxSize != null) {
            System.setProperty(AGRepositoryConnection.PROP_ADD_STATEMENT_BUFFER_MAX_SIZE, oldAddStatementBufferMaxSize);
        } else {
            System.clearProperty(AGRepositoryConnection.PROP_ADD_STATEMENT_BUFFER_MAX_SIZE);
        }

        super.tearDown();
    }

    protected Repository createRepository() throws Exception {
        AGServer server = new AGServer(AGAbstractTest.findServerUrl(), AGAbstractTest.username(), AGAbstractTest.password());
        AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
        if (catalog == null) {
            throw new Exception("Test catalog " + AGAbstractTest.CATALOG_ID + " not available");
        }
        AGRepository repo = catalog.createRepository(TEST_REPO_1);
        return repo;
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

    // Add statements to the repository from STREAM. STREAM contains data formatted
    // as FORMAT.
    public void addInputStream(InputStream stream, RDFFormat format) throws Exception {
        testCon.add(stream, "", format);
    }

    // Add statements to the repository from the file pointed to by SOURCE. SOUCE
    // contains data formatted as FORMAT.
    public void addInputFile(File source, RDFFormat format) throws Exception {
        try (InputStream stream = new FileInputStream(source)) {
            addInputStream(stream, format);
        }
    }

    public File createTempGzipFileFrom(File from) throws IOException {
        File gz = File.createTempFile(from.getName() + ".", ".gz");
        gz.deleteOnExit();
        Util.gzip(from, gz);
        return gz;
    }

    public File createTempZipFileFrom(File from) throws IOException {
        File zip = File.createTempFile(from.getName() + ".", ".zip");
        zip.deleteOnExit();
        Util.zip(from, zip);
        return zip;
    }

    @Test
    public void testAddGzipInputStreamNTriples() throws Exception {
        // add file default-graph.nt.gz to repository, no context
        File nt = Util.resourceAsTempFile(TEST_DIR_PREFIX + "default-graph.nt");
        addInputFile(createTempGzipFileFrom(nt), RDFFormat.NTRIPLES);

        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameBob, false));
        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameAlice, false));
    }

    @Test
    public void testAddZipFileNTriples() throws Exception {
        InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "graphs.nt.zip");
        addInputStream(in, RDFFormat.NTRIPLES);

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
        InputStream defaultGraphStream = Util.resourceAsStream(TEST_DIR_PREFIX
                + "default-graph.nt");

        try (Reader defaultGraph = new InputStreamReader(defaultGraphStream, "UTF-8")) {
            testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
        }

        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameBob, false));
        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameAlice, false));

        // add file graph1.nt to context1
        InputStream graph1Stream = Util.resourceAsStream(TEST_DIR_PREFIX
                + "graph1.nt");
        try (Reader graph1 = new InputStreamReader(graph1Stream, "UTF-8")) {
            testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
        }

        // add file graph2.nt to context2
        InputStream graph2Stream = Util.resourceAsStream(TEST_DIR_PREFIX
                + "graph2.nt");
        try (Reader graph2 = new InputStreamReader(graph2Stream, "UTF-8")) {
            testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
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

        try (InputStream defaultGraph =
                     Util.resourceAsStream(TEST_DIR_PREFIX + "default-graph.nt")) {
            testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
        }

        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameBob, false));
        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameAlice, false));

        // add file graph1.nt to context1
        try (InputStream graph1 = Util.resourceAsStream(TEST_DIR_PREFIX + "graph1.nt")) {
            testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
        }

        // add file graph2.nt to context2
        try (InputStream graph2 = Util.resourceAsStream(TEST_DIR_PREFIX + "graph2.nt")) {
            testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
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

    @Test
    public void testImportAllSupportedFormats() throws Exception {

        class SampleInput {
            public String file;
            public RDFFormat type;

            public SampleInput(RDFFormat format, String sampleFile) {
                type = format;
                file = sampleFile;
            }
        }

        AGRepository repo = (AGRepository) createRepository();
        AGRepositoryConnection conn = repo.getConnection();

        SampleInput[] formats = {new SampleInput(RDFFormat.NTRIPLES, "default-graph.nt"),
                new SampleInput(RDFFormat.NQUADS, "example.nq"),
                // import via 'application/trig'
                new SampleInput(AGRDFFormat.TRIG, "sample.trig"),
                // import via 'application/x-trig'
                new SampleInput(RDFFormat.TRIG, "sample.trig"),
                new SampleInput(RDFFormat.TRIX, "sample.trix"),
                new SampleInput(RDFFormat.TURTLE, "default-graph.ttl"),
                new SampleInput(RDFFormat.RDFXML, "tutorial-test8-expected.rdf"),
                new SampleInput(AGRDFFormat.NQX, "sample.nqx")
        };

        for (SampleInput format : formats) {
            // delete all triples.
            conn.remove((Resource) null, (URI) null, (Value) null, (Resource) null);
            // Import data file. If no exception is thrown, we consider the test successful
            try (InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + format.file)) {
                addInputStream(in, format.type);
            }
        }

    }

    @Test
    public void testSimpleTupleQuerySparql() throws Exception {
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

    @Test
    public void testSimpleTupleQueryUnicodeSparql() throws Exception {
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

    @Test
    public void testPreparedTupleQuerySparql() throws Exception {
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

    @Test
    public void testPreparedTupleQueryUnicodeSparql() throws Exception {
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

    @Test
    public void testSimpleGraphQuerySparql() throws Exception {
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

    @Test
    public void testPreparedGraphQuerySparql() throws Exception {
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

    @Test
    public void testBegin() throws Exception {
        assertFalse(testCon.isActive());
        testCon.begin();
        assertTrue(testCon.isActive());
        testCon.add(bob, name, nameBob);
        testCon.commit();
        // Sesame 2.6 transaction semantics are inverse of Sesame 2.7 transaction semantics.
        // Therefore, this test as it passes for 2.6 will fail in 2.7
        // So I have commented out below assert statement
        // assertFalse(testCon.isActive());
    }

    @Test
    public void testBeginRollback() throws Exception {
        assertFalse(testCon.isActive());
        testCon.begin();
        assertTrue(testCon.isActive());
        testCon.add(bob, name, nameBob);
        testCon.rollback();
        // Sesame 2.6 transaction semantics are inverse of Sesame 2.7 transaction semantics.
        // Therefore, this test as it passes for 2.6 will fail in 2.7
        // So I have commented out below assert statement
        // assertFalse(testCon.isActive());
    }

    @Test
    public void testisActive() throws Exception {
        assertFalse(testCon.isActive());
        testCon.begin();
        assertTrue(testCon.isActive());
        testCon.add(bob, name, nameBob);
        testCon.commit();
        // Sesame 2.6 transaction semantics are inverse of Sesame 2.7 transaction semantics.
        // Therefore, this test as it passes for 2.6 will fail in 2.7
        // So I have commented out below assert statement
        // assertFalse(testCon.isActive());
    }

    @Test
    public void testAddFile() throws Exception {
        // add file default-graph.nt to repository, no context
        Util.add(testCon, TEST_DIR_PREFIX + "default-graph.nt", "", RDFFormat.NTRIPLES);

        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameBob, false));
        assertTrue("Repository should contain newly added statements", testCon
                .hasStatement(null, publisher, nameAlice, false));

        // add file graph1.nt to context1
        String graph1 = TEST_DIR_PREFIX + "graph1.nt";


        Util.add(testCon, graph1, "", RDFFormat.NTRIPLES, context1);


        // add file graph2.nt to context2
        String graph2 = TEST_DIR_PREFIX + "graph2.nt";


        Util.add(testCon, graph2, "", RDFFormat.NTRIPLES, context2);


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
    public void testAddNQXFile() throws Exception {
        // add file sample.nqx to repository, no context

        AGRepository repo = (AGRepository) createRepository();
        AGRepositoryConnection conn = repo.getConnection();

        // define all attributes used in the import data set.
        conn.new AttributeDefinition("color").add();

        try (final InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "sample.nqx")) {
            conn.add(in, null, AGRDFFormat.NQX);
        }
    }

    @Test
    public void testClientImportWithAttributes() throws Exception {
        AGRepository repo = (AGRepository) createRepository();
        AGRepositoryConnection conn = repo.getConnection();

        // assume attributes already defined.

        // load ntriples w/ default attributes
        try (InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "default-graph.nt")) {
            conn.add(in, null, RDFFormat.NTRIPLES, new JSONObject("{ color: blue }"));
        }

        // load turtle w/ default attributes
        try (InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "default-graph.ttl")) {
            conn.add(in, null, RDFFormat.TURTLE, new JSONObject("{ color: green }"));
        }

        conn.clear();

        // load turtle w/ default attributes
        try (InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "rdftransaction.xml")) {
            conn.sendRDFTransaction(in, new JSONObject("{ color: red }"));
        }
    }

    // Test rollback via the x-rollback request header.
    // See RepositoryConnectionTest#testSizeRollback() for related tests.
    @Test
    public void testSizeXRollback() throws Exception {
        AGRepository repo = (AGRepository) createRepository();
        AGRepositoryConnection conn = repo.getConnection();

        assertEquals(0, conn.size());
        conn.setAutoCommit(false);
        conn.add(bob, name, nameBob);
        assertEquals(1, conn.size());
        conn.prepareHttpRepoClient().setSendRollbackHeader(true);
        assertEquals(0, conn.size());
        conn.prepareHttpRepoClient().setSendRollbackHeader(false);
        conn.add(bob, name, nameBob);
        assertEquals(1, conn.size());
        conn.rollback();
    }

    @Test
    public void testNoBufferedAddStatementsInTransactionByDefault()
            throws Exception {
        int bufSize = 5;
        System.setProperty(AGRepositoryConnection.PROP_ADD_STATEMENT_BUFFER_MAX_SIZE, "" + bufSize);
        // Not setting AGRepositoryConnection.PROP_USE_ADD_STATEMENT_BUFFER to "true"

        try (AGRepositoryConnection testConBuffered = (AGRepositoryConnection) testRepository.getConnection()) {
            assertEquals(0, testConBuffered.getNumBufferedAddStatements());

            testConBuffered.begin();
            assertEquals(0, testConBuffered.getNumBufferedAddStatements());

            testConBuffered.add(bob, mbox, mboxBob);
            assertEquals(0, testConBuffered.getNumBufferedAddStatements());

            testConBuffered.commit();
            assertEquals(0, testConBuffered.getNumBufferedAddStatements());
        }
    }

    @Test
    public void testNoBufferedAddStatementsInAutocommit()
            throws Exception {
        int bufSize = 5;
        System.setProperty(AGRepositoryConnection.PROP_USE_ADD_STATEMENT_BUFFER, "true");
        System.setProperty(AGRepositoryConnection.PROP_ADD_STATEMENT_BUFFER_MAX_SIZE, "" + bufSize);

        try (AGRepositoryConnection testConBuffered = (AGRepositoryConnection) testRepository.getConnection()) {
            testConBuffered.setAutoCommit(true);

            for (int i = 0; i < 2 * bufSize; i++) {
                testConBuffered.add(bob, mbox, vf.createLiteral("bob" + i + "@example.org"));
                assertEquals("Buffer should not be used in autocommit mode", false, testConBuffered.isUseAddStatementBuffer());
                assertEquals("Buffer should be empty in autocommit mode", 0, testConBuffered.getNumBufferedAddStatements());
                assertEquals("testCon sees the statements immediately", i + 1, getTotalStatementCount(testCon));
            }
        }
    }

    @Test
    public void testBufferedAddStatementInTransaction()
            throws Exception {
        int bufSize = 5;
        System.setProperty(AGRepositoryConnection.PROP_USE_ADD_STATEMENT_BUFFER, "true");
        System.setProperty(AGRepositoryConnection.PROP_ADD_STATEMENT_BUFFER_MAX_SIZE, "" + bufSize);

        try (AGRepositoryConnection testConBuffered = (AGRepositoryConnection) testRepository.getConnection()) {
            testConBuffered.begin();

            assertEquals(0, testConBuffered.getNumBufferedAddStatements());

            int numLoops = 10;
            for (int loopIter = 0; loopIter < numLoops; loopIter++) {
                // Fill buffer
                for (int i = 0; i < bufSize - 1; i++) {
                    testConBuffered.add(bob, mbox, vf.createLiteral("bob" + loopIter + "-" + i + "@example.org"));
                    assertEquals(i + 1, testConBuffered.getNumBufferedAddStatements());
                    // testCon is independent, so does not see the buffered inserts
                    assertEquals(0, ((AGRepositoryConnection) testCon).getNumBufferedAddStatements());
                }
                // Add one more into the buffer, so the buffer reaches max size and will be sent over
                testConBuffered.add(bob, mbox, vf.createLiteral("bob" + loopIter + "-" + bufSize + "@example.org"));
                // Buffer is empty again
                assertEquals(0, testConBuffered.getNumBufferedAddStatements());

                // testCon does not see the statements yet, because not committed
                assertEquals(0, getTotalStatementCount(testCon));
            }

            testConBuffered.commit();
            assertEquals(numLoops * bufSize, getTotalStatementCount(testCon));
            assertEquals(numLoops * bufSize, getTotalStatementCount(testConBuffered));
        }
    }

    @Test
    public void testBufferedAddStatementsInCancelledTransaction()
            throws Exception {
        int bufSize = 5;
        System.setProperty(AGRepositoryConnection.PROP_ADD_STATEMENT_BUFFER_MAX_SIZE, "" + bufSize);
        System.setProperty(AGRepositoryConnection.PROP_USE_ADD_STATEMENT_BUFFER, "true");

        try (AGRepositoryConnection testConBuffered = (AGRepositoryConnection) testRepository.getConnection()) {
            testConBuffered.begin();

            testConBuffered.add(bob, mbox, mboxBob);
            assertEquals(1, testConBuffered.getNumBufferedAddStatements());

            testConBuffered.rollback();
            assertEquals(0, testConBuffered.getNumBufferedAddStatements());
            assertEquals(0, getTotalStatementCount(testConBuffered));
        }
    }

    @Test
    public void testBufferedAddStatementsHandled()
            throws Exception {
        int bufSize = 5;
        System.setProperty(AGRepositoryConnection.PROP_ADD_STATEMENT_BUFFER_MAX_SIZE, "" + bufSize);
        System.setProperty(AGRepositoryConnection.PROP_USE_ADD_STATEMENT_BUFFER, "true");

        for (int i = 0; ; i++) {
            try (AGRepositoryConnection testConBuffered = (AGRepositoryConnection) testRepository.getConnection()) {
                testConBuffered.begin();

                testConBuffered.add(bob, mbox, mboxBob);
                assertEquals(1, testConBuffered.getNumBufferedAddStatements());

                // Calling a method on testConBuffered that does HTTP interaction (except .add(stmt))
                // triggers buffer upload. Try a variety of methods:
                switch (i) {
                    case 0:
                        testConBuffered.prepareHttpRepoClient();
                        break;
                    case 1:
                        testConBuffered.getUserAttributes();
                        break;
                    case 2:
                        testConBuffered.getAttributeDefinitions();
                        break;
                    case 3:
                        testConBuffered.getContextIDs();
                        break;
                    case 4:
                        testConBuffered.getNamespaces();
                        break;
                    case 5:
                        testConBuffered.remove(alice, mbox, mboxAlice, context1); // no such stmt
                        break;
                    case 6:
                        testConBuffered.commit();
                        // After this, the connection is in autocommit mode again...
                        break;
                    case 7:
                        // ... therefore stop the test: asserts below don't hold anymore
                        return;
                }

                assertEquals(0, testConBuffered.getNumBufferedAddStatements());
                assertEquals("In iter #" + i + " there should be 1 statement", 1, getTotalStatementCount(testConBuffered));
            }
        }
    }

    @Test
    public void testBufferedAddStatementsCanBeRemoved()
            throws Exception {
        int bufSize = 5;
        System.setProperty(AGRepositoryConnection.PROP_ADD_STATEMENT_BUFFER_MAX_SIZE, "" + bufSize);
        System.setProperty(AGRepositoryConnection.PROP_USE_ADD_STATEMENT_BUFFER, "true");

        try (AGRepositoryConnection testConBuffered = (AGRepositoryConnection) testRepository.getConnection()) {
            testConBuffered.begin();
            testConBuffered.add(bob, mbox, mboxBob);
            assertEquals("item should be buffered", 1, testConBuffered.getNumBufferedAddStatements());
            testConBuffered.remove(bob, mbox, mboxBob);
            assertEquals("item should not be buffered, because remove() calls prepareHttpRepoClient()",
                    0, testConBuffered.getNumBufferedAddStatements());
            testConBuffered.commit();

            assertEquals("Nothing should be added in the end", 0, getTotalStatementCount(testCon));
        }
    }

    @Test
    public void testBufferedAddStatementsHandledWhenStartingAutoCommit()
            throws Exception {
        int bufSize = 5;
        System.setProperty(AGRepositoryConnection.PROP_ADD_STATEMENT_BUFFER_MAX_SIZE, "" + bufSize);
        System.setProperty(AGRepositoryConnection.PROP_USE_ADD_STATEMENT_BUFFER, "true");

        try (AGRepositoryConnection testConBuffered = (AGRepositoryConnection) testRepository.getConnection()) {

            int numStatements = 0;

            assertEquals("Starting in autocommit is the default", true, testConBuffered.isAutoCommit());
            assertEquals("Buffering is initially disabled", false, testConBuffered.isUseAddStatementBuffer());
            assertEquals("Buffer is initially empty", 0, testConBuffered.getNumBufferedAddStatements());

            testConBuffered.add(bob, mbox, mboxBob);
            assertEquals("item should not be buffered", 0, testConBuffered.getNumBufferedAddStatements());
            assertEquals("Stmt #1 should be visible to other connection", 1, getTotalStatementCount(testCon));

            testConBuffered.setAutoCommit(false);
            assertEquals("Disabling autocommit should enable buffer", true, testConBuffered.isUseAddStatementBuffer());
            assertEquals("Disabling autocommit should start with empty buffer", 0, testConBuffered.getNumBufferedAddStatements());
            testConBuffered.add(alice, mbox, mboxAlice);

            assertEquals(1, testConBuffered.getNumBufferedAddStatements());
            assertEquals("Stmt #2 should not yet be visible to other connection", 1, getTotalStatementCount(testCon));

            testConBuffered.setAutoCommit(true);
            assertEquals("Enabling autocomit should handle and then disable buffering", 0, testConBuffered.getNumBufferedAddStatements());
            assertEquals("Stmt #2 should now be visible to other connection", 2, getTotalStatementCount(testCon));
        }
    }

    @Test
    public void testBufferedAddStatementsProgrammaticallyDisableNoop()
            throws Exception {
        try (AGRepositoryConnection testConUnbuffered = (AGRepositoryConnection) testRepository.getConnection()) {
            testConUnbuffered.add(alice, mbox, mboxAlice);

            assertEquals(1, getTotalStatementCount(testCon));
            assertEquals(0, testConUnbuffered.getNumBufferedAddStatements());

            testConUnbuffered.setAddStatementBufferEnabled(false); // no effect, was already disabled
            testConUnbuffered.add(bob, mbox, mboxBob);

            assertEquals(2, getTotalStatementCount(testCon));
            assertEquals(0, testConUnbuffered.getNumBufferedAddStatements());

            testConUnbuffered.clear();
            assertEquals(0, getTotalStatementCount(testCon));
        }
    }

    @Test
    public void testBufferedAddStatementsProgrammaticallyEnableDisable()
            throws Exception {
        try (AGRepositoryConnection c = (AGRepositoryConnection) testRepository.getConnection()) {
            assertEquals(false, c.isUseAddStatementBuffer());

            c.add(alice, mbox, mboxAlice);

            assertEquals("Unbuffered stmt should be seen", 1, getTotalStatementCount(testCon));
            assertEquals("No buffer applicable", 0, c.getNumBufferedAddStatements());

            c.setAddStatementBufferEnabled(true);
            assertEquals("buffering now enabled, but no transaction started so buffer should have no effect",
                    false, c.isUseAddStatementBuffer());

            c.add(bob, mbox, mboxBob);

            assertEquals(2, getTotalStatementCount(testCon));
            assertEquals(0, c.getNumBufferedAddStatements());

            c.clear();
            c.add(alice, mbox, mboxAlice);
            c.begin();
            assertEquals("now in a transaction, so buffering should be active", true, c.isUseAddStatementBuffer());

            c.add(bob, mbox, mboxBob);
            assertEquals(1, getTotalStatementCount(testCon));
            assertEquals(1, c.getNumBufferedAddStatements());

            c.setAddStatementBufferEnabled(false);
            assertEquals("Disabling buffer should send pending stmt", 0, c.getNumBufferedAddStatements());
            assertEquals("Disabling buffer should send pending stmt", 1, getTotalStatementCount(testCon));
        }
    }

    @Test
    public void testBufferedAddStatementsProgrammaticallyResize()
            throws Exception {
        try (AGRepositoryConnection c = (AGRepositoryConnection) testRepository.getConnection()) {
            c.setAddStatementBufferEnabled(true);
            c.setAddStatementBufferMaxSize(10);
            c.begin();
            // buffer enabled, 10 items

            for (int i = 0; i < 8; i++) {
                c.add(alice, mbox, vf.createLiteral("alice" + i + "@example.org"));
            }

            assertEquals(0, getTotalStatementCount(testCon));
            assertEquals(8, c.getNumBufferedAddStatements());

            c.setAddStatementBufferMaxSize(15);
            assertEquals("upsizing buffer should keep items", 8, c.getNumBufferedAddStatements());

            c.setAddStatementBufferMaxSize(9);
            assertEquals("downsizing buffer should keep items", 8, c.getNumBufferedAddStatements());

            c.setAddStatementBufferMaxSize(7);
            assertEquals("downsizing buffer should forcee all items out", 0, c.getNumBufferedAddStatements());
            assertEquals("not committed yet", 0, getTotalStatementCount(testCon));

            c.commit();
            assertEquals("downsizing buffer should force all items out", 8, getTotalStatementCount(testCon));
        }
    }

    @Test
    public void testBufferedAddStatementsProgrammaticallyWithContext()
            throws Exception {
        try (AGRepositoryConnection c = (AGRepositoryConnection) testRepository.getConnection()) {

            // Add in one connection, remove in another, to check that context is included properly
            // First without buffering, sanity check
            c.add(alice, mbox, mboxAlice, context1);
            assertEquals(1, getTotalStatementCount(testCon));

            testCon.remove(alice, mbox, mboxAlice, context2);
            assertEquals("Remove with different context should have no effect", 1, getTotalStatementCount(testCon));
            testCon.remove(alice, mbox, mboxAlice, context1);
            assertEquals("Remove with matching context should have effect", 0, getTotalStatementCount(testCon));

            // Now with buffering
            c.setAddStatementBufferMaxSize(3);
            c.setAddStatementBufferEnabled(true);
            c.begin();
            assertEquals(true, c.isUseAddStatementBuffer());
            c.add(alice, mbox, mboxAlice, context1);
            c.add(bob, mbox, mboxBob, context1);
            c.add(bob, mbox, mboxBob, context2);
            assertEquals("3 statements should have filled the buffer", 0, c.getNumBufferedAddStatements());

            assertEquals("Statements should not yet be committed", 0, getTotalStatementCount(testCon));
            c.commit();

            assertEquals(3, getTotalStatementCount(testCon));
            testCon.remove(alice, mbox, mboxAlice, context1);
            assertEquals(2, getTotalStatementCount(testCon));
            testCon.remove(bob, mbox, mboxBob, context1);
            assertEquals(1, getTotalStatementCount(testCon));
            testCon.remove(bob, mbox, mboxBob, context2);
            assertEquals(0, getTotalStatementCount(testCon));
        }
    }

    @Test
    public void testBufferedAddStatementsAndPool()
            throws Exception {
        assertEquals(0, getTotalStatementCount(testCon));

        AGConnPool pool =
                AGConnPool.create(AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
                        AGConnProp.username, AGAbstractTest.username(),
                        AGConnProp.password, AGAbstractTest.password(),
                        AGConnProp.catalog, AGAbstractTest.CATALOG_ID,
                        AGConnProp.repository, TEST_REPO_1, // not AGAbstractTest.REPO_ID
                        AGConnProp.session, AGConnProp.Session.TX,
                        AGPoolProp.maxActive, 1,
                        AGPoolProp.initialSize, 1);

        AGRepositoryConnection conn = pool.borrowConnection();
        assertEquals(0, getTotalStatementCount(conn));
        conn.setAddStatementBufferEnabled(true);
        conn.setAddStatementBufferMaxSize(10);
        conn.add(bob, mbox, mboxBob);
        conn.close();

        conn = pool.borrowConnection();
        assertEquals(0, getTotalStatementCount(conn));
    }

    @RunWith(Categories.class)
    @ExcludeCategory(NonPrepushTest.class)
    @SuiteClasses( {AGRepositoryConnectionTests.class})
    public static class Prepush {
    }

    @RunWith(Categories.class)
    @IncludeCategory(TestSuites.Broken.class)
    @SuiteClasses( {AGRepositoryConnectionTests.class})
    public static class Broken {
    }
}
