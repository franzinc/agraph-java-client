/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServerVersion;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.handler.AGResponseHandler;
import com.franz.agraph.http.handler.AGLongHandler;
import com.franz.agraph.http.handler.AGRDFHandler;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class RepositoryConnectionTest {

    public static final String TEST_DIR_PREFIX = "/test/";
    protected static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
    protected static final String DC_NS = "http://purl.org/dc/elements/1.1/";
    protected Repository testRepository;

    protected RepositoryConnection testCon;

    protected RepositoryConnection testCon2;

    protected ValueFactory vf;

    protected BNode bob;

    protected BNode alice;

    protected BNode alexander;

    protected IRI name;

    protected IRI mbox;

    protected IRI publisher;

    protected IRI unknownContext;

    protected IRI context1;

    protected IRI context2;

    protected Literal nameAlice;

    protected Literal nameBob;

    protected Literal nameTrudy;

    protected Literal mboxAlice;

    protected Literal mboxBob;

    protected Literal Александър;

    @BeforeEach
    public void setUp()
            throws Exception {
        testRepository = createRepository();
        testRepository.init();

        testCon = testRepository.getConnection();
        testCon.clear();
        testCon.clearNamespaces();

        testCon2 = testRepository.getConnection();

        vf = testRepository.getValueFactory();

        // Initialize values
        bob = vf.createBNode();
        alice = vf.createBNode();
        alexander = vf.createBNode();

        name = vf.createIRI(FOAF_NS + "name");
        mbox = vf.createIRI(FOAF_NS + "mbox");

        publisher = vf.createIRI(DC_NS + "publisher");

        nameAlice = vf.createLiteral("Alice");
        nameBob = vf.createLiteral("Bob");

        nameTrudy = vf.createLiteral("Evil \t \n \r \\ Trudy");

        mboxAlice = vf.createLiteral("alice@example.org");
        mboxBob = vf.createLiteral("bob@example.org");

        Александър = vf.createLiteral("Александър");

        unknownContext = SimpleValueFactory.getInstance().createIRI("urn:unknownContext");

        context1 = vf.createIRI("urn:x-local:graph1");
        context2 = vf.createIRI("urn:x-local:graph2");
    }

    @AfterEach
    public void tearDown()
            throws Exception {
        if (testCon2 != null) {
            testCon2.close();
            testCon2 = null;
        }
        if (testCon != null) {
            testCon.close();
            testCon = null;
        }
        if (testRepository != null) {
            testRepository.shutDown();
            testRepository = null;
        }
        vf = null;
    }

    /**
     * Gets an (uninitialized) instance of the repository that should be tested.
     *
     * @return an uninitialized repository.
     */
    protected abstract Repository createRepository()
            throws Exception;

    public static abstract class RepositoryConnectionTests extends RepositoryConnectionTest {

        @Test
        public void testAddStatement()
                throws Exception {
            testCon.add(bob, name, nameBob);

            assertTrue(testCon.hasStatement(bob, name, nameBob, false),
                    "Repository should contain newly added statement");

            Statement statement = vf.createStatement(alice, name, nameAlice);
            testCon.add(statement);

            assertTrue(testCon.hasStatement(statement, false), "Repository should contain newly added statement");
            assertTrue(testCon.hasStatement(alice, name, nameAlice, false),
                    "Repository should contain newly added statement");
            Repository tempRep = new SailRepository(new MemoryStore());
            tempRep.init();
            RepositoryConnection con = tempRep.getConnection();

            con.add(testCon.getStatements(null, null, null, false));

            assertTrue(con.hasStatement(bob, name, nameBob, false),
                    "Temp Repository should contain newly added statement");
            con.close();
            tempRep.shutDown();
        }

        @Test
        public void testTransactionIsolation() {
            testCon.begin();

            testCon.add(bob, name, nameBob);
            assertTrue(testCon.hasStatement(bob, name, nameBob, false));
            assertFalse(testCon2.hasStatement(bob, name, nameBob, false));

            testCon.commit();

            assertTrue(testCon.hasStatement(bob, name, nameBob, false));
            assertTrue(testCon2.hasStatement(bob, name, nameBob, false));
        }

        @Test
        public void testAddReader()
                throws Exception {
            InputStream defaultGraphStream = Util.resourceAsStream(TEST_DIR_PREFIX
                    + "default-graph.ttl");
            Reader defaultGraph = new InputStreamReader(defaultGraphStream, StandardCharsets.UTF_8);

            testCon.add(defaultGraph, "", RDFFormat.TURTLE);

            defaultGraph.close();

            assertTrue(testCon.hasStatement(null, publisher, nameBob, false),
                    "Repository should contain newly added statements");
            assertTrue(testCon.hasStatement(null, publisher, nameAlice, false),
                    "Repository should contain newly added statements");

            // add file graph1.ttl to context1
            InputStream graph1Stream = Util.resourceAsStream(TEST_DIR_PREFIX + "graph1.ttl");
            Reader graph1 = new InputStreamReader(graph1Stream, StandardCharsets.UTF_8);

            testCon.add(graph1, "", RDFFormat.TURTLE, context1);

            graph1.close();

            // add file graph2.ttl to context2
            InputStream graph2Stream = Util.resourceAsStream(TEST_DIR_PREFIX
                    + "graph2.ttl");
            Reader graph2 = new InputStreamReader(graph2Stream, StandardCharsets.UTF_8);

            testCon.add(graph2, "", RDFFormat.TURTLE, context2);

            graph2.close();

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
        public void testAddInputStream()
                throws Exception {
            // add file default-graph.ttl to repository, no context
            InputStream defaultGraph = Util.resourceAsStream(TEST_DIR_PREFIX
                    + "default-graph.ttl");

            testCon.add(defaultGraph, "", RDFFormat.TURTLE);

            defaultGraph.close();

            assertTrue(testCon.hasStatement(null, publisher, nameBob, false),
                    "Repository should contain newly added statements");
            assertTrue(testCon.hasStatement(null, publisher, nameAlice, false),
                    "Repository should contain newly added statements");

            // add file graph1.ttl to context1
            InputStream graph1 = Util.resourceAsStream(TEST_DIR_PREFIX + "graph1.ttl");

            testCon.add(graph1, "", RDFFormat.TURTLE, context1);

            graph1.close();

            // add file graph2.ttl to context2
            InputStream graph2 = Util.resourceAsStream(TEST_DIR_PREFIX + "graph2.ttl");

            testCon.add(graph2, "", RDFFormat.TURTLE, context2);

            graph2.close();

            assertTrue(testCon.hasStatement(null, name, nameAlice, false),
                    "alice should be known in the store");
            assertFalse(testCon.hasStatement(null, name, nameAlice, false, context1),
                    "alice should not be known in context1");
            assertTrue(testCon.hasStatement(null, name, nameAlice, false, context2),
                    "alice should be known in context2");

            assertTrue(testCon.hasStatement(null, name, nameBob, false), "bob should be known in the store");

            assertFalse(testCon.hasStatement(null, name, nameBob, false, context2),
                    "bob should not be known in context2");
            assertTrue(testCon.hasStatement(null, name, nameBob, false, context1),
                    "bib should be known in context1");

        }

        @Test
        public void testAddGzipInputStream()
                throws Exception {
            // add file default-graph.ttl to repository, no context
            final String path = TEST_DIR_PREFIX + "default-graph.ttl.gz";
            try (InputStream defaultGraph = Util.resourceAsStream(path)) {
                testCon.add(defaultGraph, "", RDFFormat.TURTLE);
            }

            assertTrue(testCon.hasStatement(null, publisher, nameBob, false),
                    "Repository should contain newly added statements");
            assertTrue(testCon.hasStatement(null, publisher, nameAlice, false),
                    "Repository should contain newly added statements");
        }

        @Test
        public void testAddZipFile()
                throws Exception {
            InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "graphs.ttl.zip");

            testCon.add(in, "", RDFFormat.TURTLE);

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
        public void testAutoCommit() {
            testCon.begin();
            testCon.add(alice, name, nameAlice);

            assertTrue(testCon.hasStatement(alice, name, nameAlice, false),
                    "Uncommitted update should be visible to own connection");

            testCon.commit();

            assertTrue(testCon.hasStatement(alice, name, nameAlice, false),
                    "Repository should contain statement after commit");

            testCon.setAutoCommit(true);
        }

        @Test
        public void testRollback()
                throws Exception {
            testCon.begin();
            testCon.add(alice, name, nameAlice);

            assertTrue(testCon.hasStatement(alice, name, nameAlice, false),
                    "Uncommitted updates should be visible to own connection");

            testCon.rollback();

            assertFalse(testCon.hasStatement(alice, name, nameAlice, false),
                    "Repository should not contain statement after rollback");

            testCon.setAutoCommit(true);
        }

        private static final String ASK_FOAF_NAME_QUERY = """
                    PREFIX foaf: <$FOAF_NS>
                    ASK { ?p foaf:name ?name }
                    """.replace("$FOAF_NS", FOAF_NS);

        @Test
        public void testSimpleBooleanQuery() {
            testCon.add(alice, name, nameAlice, context2);
            testCon.add(alice, mbox, mboxAlice, context2);
            testCon.add(context2, publisher, nameAlice);

            testCon.add(bob, name, nameBob, context1);
            testCon.add(bob, mbox, mboxBob, context1);
            testCon.add(context1, publisher, nameBob);

            boolean exists = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, ASK_FOAF_NAME_QUERY).evaluate();

            assertTrue(exists);
        }

        @Test
        public void testTSVTupleQueryWithSpecialSymbols() {

            AGServerVersion minVersion = new AGServerVersion("v6.6.0");
            if (((AGRepository)testRepository).getServer().getComparableVersion().compareTo(minVersion) >= 0) {
                testCon.add(alice, name, nameTrudy, context2);

                String query = """
                        PREFIX foaf: <$FOAF_NS>
                        SELECT ?name WHERE
                        { ?p foaf:name ?name }
                        """
                        .replace("$FOAF_NS", FOAF_NS);

                try (TupleQueryResult res = testCon.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
                    while (res.hasNext()) {
                        final BindingSet bs = res.next();
                        assertEquals(nameTrudy.getLabel(), bs.getValue("name").stringValue());
                    }
                }
            }
        }

        @Test
        public void testPreparedBooleanQuery() {
            testCon.add(alice, name, nameAlice, context2);
            testCon.add(alice, mbox, mboxAlice, context2);
            testCon.add(context2, publisher, nameAlice);

            testCon.add(bob, name, nameBob, context1);
            testCon.add(bob, mbox, mboxBob, context1);
            testCon.add(context1, publisher, nameBob);

            BooleanQuery query = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, ASK_FOAF_NAME_QUERY);
            query.setBinding("name", nameBob);

            assertTrue(query.evaluate());
        }

        @Test
        public void testDataset() {
            testCon.add(alice, name, nameAlice, context2);
            testCon.add(alice, mbox, mboxAlice, context2);
            testCon.add(context2, publisher, nameAlice);

            testCon.add(bob, name, nameBob, context1);
            testCon.add(bob, mbox, mboxBob, context1);
            testCon.add(context1, publisher, nameBob);

            BooleanQuery query = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, ASK_FOAF_NAME_QUERY);
            query.setBinding("name", nameBob);

            assertTrue(query.evaluate());

            SimpleDataset dataset = new SimpleDataset();

            // default graph: {context1}
            dataset.addDefaultGraph(context1);
            query.setDataset(dataset);
            assertTrue(query.evaluate());

            // default graph: {context1, context2}
            dataset.addDefaultGraph(context2);
            query.setDataset(dataset);
            assertTrue(query.evaluate());

            // default graph: {context2}
            dataset.removeDefaultGraph(context1);
            query.setDataset(dataset);
            assertFalse(query.evaluate());

            String queryString = """
                    PREFIX foaf: <$FOAF_NS>
                    ASK { GRAPH ?g { ?p foaf:name ?name } }
                    """.replace("$FOAF_NS", FOAF_NS);

            query = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
            query.setBinding("name", nameBob);

            // default graph: {context2}; named graph: {}
            query.setDataset(dataset);
            assertFalse(query.evaluate());

            // default graph: {context1, context2}; named graph: {context2}
            dataset.addDefaultGraph(context1);
            dataset.addNamedGraph(context2);
            query.setDataset(dataset);
            assertFalse(query.evaluate());

            // default graph: {context1, context2}; named graph: {context1, context2}
            dataset.addNamedGraph(context1);
            query.setDataset(dataset);
            assertTrue(query.evaluate());
        }

        @Test
        public void testGetStatementsCountLimit() {
            testCon.add(bob, name, nameBob);
            testCon.add(bob, name, nameAlice);
            testCon.add(alice, name, nameAlice);

            AGHttpRepoClient client = ((AGRepositoryConnection)testCon)
                .prepareHttpRepoClient();

            AGLongHandler handler = new AGLongHandler();

            client.getStatementsCountLimit(0, null, null, nameBob, "false", handler);
            assertEquals(1, handler.getResult(), "The resu lt should be 1");

            client.getStatementsCountLimit(0, null, null, nameAlice, "false", handler);
            assertEquals(2, handler.getResult(), "The result should be 2");

            client.getStatementsCountLimit(1, null, null, nameAlice, "false", handler);
            assertEquals(1, handler.getResult(), "The result should be 1");

            client.getStatementsCountLimit(2, null, name, null, "false", handler);
            assertEquals(2, handler.getResult(), "The result should be 2");

            client.getStatementsCountLimit(0, null, name, null, "false", handler);
            assertEquals(3, handler.getResult(), "The result should be 3");

            client.getStatementsCountLimit(0, alice, name, nameBob, "false", handler);
            assertEquals(0, handler.getResult(), "The result should be 0");
        }

        @Test
        public void testGetStatementsLimit() {
            testCon.add(bob, name, nameBob);
            testCon.add(bob, name, nameBob);
            testCon.add(bob, name, nameBob);

            final List<Statement> items = new ArrayList<>();
            AGHttpRepoClient client = ((AGRepositoryConnection)testCon)
                .prepareHttpRepoClient();
            StatementCollector collector = new StatementCollector() {
                public void handleStatement(Statement s) {
                    items.add(s);
                }
            };
            AGResponseHandler handler = new AGRDFHandler(
                    client.getPreferredRDFFormat(),
                    collector,
                    ((AGRepositoryConnection)testCon).getRepository()
                        .getValueFactory(),
                    client.getAllowExternalBlankNodeIds());

            client.getStatementsLimit(1, bob, name, nameBob, "false", handler);

            assertEquals(1, items.size(), "The result should contain a single statement");

            items.clear();
            client.getStatementsLimit(2, bob, name, nameBob, "false", handler);
            assertEquals(2, items.size(), "The result should contain two statements");

            items.clear();
            client.getStatementsLimit(0, bob, name, nameBob, "false", handler);
            assertEquals(3, items.size(), "The result should contain all three statements");
        }

        @Test
        public void testGetStatements() {
            testCon.add(bob, name, nameBob);

            assertTrue(testCon.hasStatement(bob, name, nameBob, false), "Repository should contain statement");

            try (RepositoryResult<Statement> result = testCon.getStatements(null, name, null, false)) {
                assertNotNull(result, "Iterator should not be null");
                assertTrue(result.hasNext(), "Iterator should not be empty");

                while (result.hasNext()) {
                    Statement st = result.next();
                    assertNull(st.getContext(), "Statement should not be in a context ");
                    assertEquals(st.getPredicate(), name, "Statement predicate should be equal to name ");
                }
            }

            List<Statement> list = Iterations.addAll(testCon.getStatements(null, name, null, false), new ArrayList<>());

            assertNotNull(list, "List should not be null");
            assertFalse(list.isEmpty(), "List should not be empty");
        }

        @Test
        public void testGetStatementsInSingleContext() {
            testCon.setAutoCommit(false);
            testCon.add(bob, name, nameBob, context1);
            testCon.add(bob, mbox, mboxBob, context1);
            testCon.add(context1, publisher, nameBob);

            testCon.add(alice, name, nameAlice, context2);
            testCon.add(alice, mbox, mboxAlice, context2);
            testCon.add(context2, publisher, nameAlice);
            testCon.setAutoCommit(true);

            assertTrue(testCon.hasStatement(bob, name, nameBob, false),
                    "Repository should contain statement");
            assertTrue(testCon.hasStatement(bob, name, nameBob, false, context1),
                    "Repository should contain statement in context1");
            assertFalse(testCon.hasStatement(bob, name, nameBob, false, context2),
                    "Repository should not contain statement in context2");

            // Check handling of getStatements without context IDs
            try (RepositoryResult<Statement> result = testCon.getStatements(bob, name, null, false)) {
                while (result.hasNext()) {
                    Statement st = result.next();
                    assertEquals(bob, st.getSubject());
                    assertEquals(name, st.getPredicate());
                    assertEquals(nameBob, st.getObject());
                    assertEquals(context1, st.getContext());
                }
            }

            // Check handling of getStatements with a known context ID
            try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, context1)) {
                while (result.hasNext()) {
                    Statement st = result.next();
                    assertEquals(context1, st.getContext());
                }
            }

            // Check handling of getStatements with an unknown context ID
            ;
            try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, unknownContext)) {
                assertNotNull(result);
                assertFalse(result.hasNext());
            }

            List<Statement> list = Iterations.addAll(testCon.getStatements(null, name, null, false, context1),
                    new ArrayList<Statement>());

            assertNotNull(list, "List should not be null");
            assertFalse(list.isEmpty(), "List should not be empty");
        }

        @Test
        public void testGetStatementsInMultipleContexts() {
            testCon.clear();

            testCon.setAutoCommit(false);
            testCon.add(alice, name, nameAlice, context2);
            testCon.add(alice, mbox, mboxAlice, context2);
            testCon.add(context2, publisher, nameAlice);
            testCon.setAutoCommit(true);

            // get statements with either no context or context2
            try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, null, context2)) {
                int count = 0;
                while (result.hasNext()) {
                    count++;
                    Statement st = result.next();
                    assertTrue(st.getContext() == null || context2.equals(st.getContext()));
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
                    Statement st = result.next();
                    // we should have _only_ statements from context2
                    assertEquals(context2, st.getContext());
                }
                assertEquals(2, count, "there should be two statements");
            }

            // get all statements with unknownContext or context2.
            try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, unknownContext, context2)) {
                int count = 0;
                while (result.hasNext()) {
                    count++;
                    Statement st = result.next();
                    // we should have _only_ statements from context2
                    assertEquals(context2, st.getContext());
                }
                assertEquals(2, count, "there should be two statements");
            }

            // add statements to context1
            testCon.setAutoCommit(false);
            testCon.add(bob, name, nameBob, context1);
            testCon.add(bob, mbox, mboxBob, context1);
            testCon.add(context1, publisher, nameBob);
            testCon.setAutoCommit(true);

            try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, context1)) {
                assertNotNull(result);
                assertTrue(result.hasNext());
            }

            // get statements with either no context or context2
            try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, null, context2)) {
                int count = 0;
                while (result.hasNext()) {
                    count++;
                    Statement st = result.next();
                    // we should have _only_ statements from context2, or without
                    // context
                    assertTrue(st.getContext() == null || context2.equals(st.getContext()));
                }
                assertEquals(4, count, "there should be four statements");
            }

            // get all statements with context1 or context2
            ;

            try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, context1, context2)) {
                int count = 0;
                while (result.hasNext()) {
                    count++;
                    Statement st = result.next();
                    assertTrue(context1.equals(st.getContext()) || context2.equals(st.getContext()));
                }
                assertEquals(4, count, "there should be four statements");
            }
        }

        @Test
        public void testDuplicateFilterWithGraphs() {
            testCon.setAutoCommit(false);
            testCon.add(bob, name, nameBob);
            testCon.add(bob, name, nameBob, context1);
            testCon.add(bob, name, nameBob, context2);
            testCon.setAutoCommit(true);

            RepositoryResult<Statement> result = testCon.getStatements(bob, name, null, true);
            result.enableDuplicateFilter();

            int count = 0;
            while (result.hasNext()) {
                result.next();
                count++;
            }
            // In Sesame 4 the duplicate filter DOES take graphs into account.
            assertEquals(3, count);
        }

        @Test
        public void testDuplicateFilter() {
            testCon.setAutoCommit(false);
            testCon.add(bob, name, nameBob);
            testCon.add(bob, name, nameBob);
            testCon.add(bob, name, nameBob);
            testCon.setAutoCommit(true);

            RepositoryResult<Statement> result = testCon.getStatements(bob, name, null, true);
            result.enableDuplicateFilter();

            int count = 0;
            while (result.hasNext()) {
                result.next();
                count++;
            }
            assertEquals(1, count);
        }

        @Test
        public void testRemoveStatements() {
            testCon.setAutoCommit(false);
            testCon.add(bob, name, nameBob);
            testCon.add(alice, name, nameAlice);
            testCon.setAutoCommit(true);

            assertTrue(testCon.hasStatement(bob, name, nameBob, false));
            assertTrue(testCon.hasStatement(alice, name, nameAlice, false));

            testCon.remove(bob, name, nameBob);

            assertFalse(testCon.hasStatement(bob, name, nameBob, false));
            assertTrue(testCon.hasStatement(alice, name, nameAlice, false));

            testCon.remove(alice, null, null);
            assertFalse(testCon.hasStatement(alice, name, nameAlice, false));
            assertTrue(testCon.isEmpty());
        }

        @Test
        public void testRemoveStatementCollection() {
            testCon.setAutoCommit(false);
            testCon.add(alice, name, nameAlice);
            testCon.add(bob, name, nameBob);
            testCon.setAutoCommit(true);

            assertTrue(testCon.hasStatement(bob, name, nameBob, false));
            assertTrue(testCon.hasStatement(alice, name, nameAlice, false));

            Collection<Statement> c = Iterations.addAll(testCon.getStatements(null, null, null, false), new ArrayList<>());

            testCon.remove(c);

            assertFalse(testCon.hasStatement(bob, name, nameBob, false));
            assertFalse(testCon.hasStatement(alice, name, nameAlice, false));
        }

        @Test
        public void testRemoveStatementIteration() {
            testCon.setAutoCommit(false);
            testCon.add(alice, name, nameAlice);
            testCon.add(bob, name, nameBob);
            testCon.setAutoCommit(true);

            assertTrue(testCon.hasStatement(bob, name, nameBob, false));
            assertTrue(testCon.hasStatement(alice, name, nameAlice, false));

            try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false)) {
                testCon.remove(result);
            }

            assertFalse(testCon.hasStatement(bob, name, nameBob, false));
            assertFalse(testCon.hasStatement(alice, name, nameAlice, false));

        }

        @Test
        public void testGetNamespaces()
                throws Exception {

            String rdfFragment = """
                    <rdf:RDF
                      xmlns:example='http://example.org/'
                      xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'
                      xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#' >
                      <rdf:Description rdf:about='http://example.org/Main'>
                        <rdfs:label>Main Node</rdfs:label>
                      </rdf:Description>
                    </rdf:RDF>
                    """;

            testCon.add(new StringReader(rdfFragment), "", RDFFormat.RDFXML);

            try (RepositoryResult<Namespace> result = testCon.getNamespaces()) {
                int nsCount = 0;
                while (result.hasNext()) {
                    nsCount++;
                    result.next();
                }
                assertEquals(0, nsCount, "Namespaces from imported RDF should not be added to repository");
            }
        }

        @Test
        public void testClear() {
            testCon.add(bob, name, nameBob);
            assertTrue(testCon.hasStatement(null, name, nameBob, false));
            testCon.clear();
            assertFalse(testCon.hasStatement(null, name, nameBob, false));
        }

        @Test
        public void testRecoverFromParseError()
                throws RepositoryException, IOException {
            String invalidData = "bad";
            String validData = "@prefix foo: <http://example.org/foo#>.\nfoo:a foo:b foo:c.";

            try {
                testCon.add(new StringReader(invalidData), "", RDFFormat.TURTLE);
                fail("Invalid data should result in an exception");
            } catch (RDFParseException e) {
                // Expected behaviour
            }

            try {
                testCon.add(new StringReader(validData), "", RDFFormat.TURTLE);
            } catch (RDFParseException e) {
                fail("Valid data should not result in an exception");
            }

            assertEquals(1, testCon.size(),"Repository contains incorrect number of statements");
        }

        @Test
        public void testStatementSerialization() throws Exception {
            testCon.add(bob, name, nameBob);

            Statement st;
            try (RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, true)) {
                st = statements.next();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(st);
            out.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bais);
            Statement deserializedStatement = (Statement) in.readObject();
            in.close();

            assertEquals(st, deserializedStatement);

            assertTrue(testCon.hasStatement(st, true));
            assertTrue(testCon.hasStatement(deserializedStatement, true));
        }

        @Test
        public void testBNodeSerialization() throws Exception {
            testCon.add(bob, name, nameBob);

            Statement st;
            try (RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, false)) {
                st = statements.next();
            }

            BNode bnode = (BNode) st.getSubject();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(bnode);
            out.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bais);
            BNode deserializedBNode = (BNode) in.readObject();
            in.close();

            assertEquals(bnode, deserializedBNode);

            assertTrue(testCon.hasStatement(bnode, name, nameBob, true));
            assertTrue(testCon.hasStatement(deserializedBNode, name, nameBob, true));
        }

        @Test
        public void testURISerialization() throws Exception {
            testCon.add(bob, name, nameBob);

            Statement st;
            try (RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, false)) {
                st = statements.next();
            }

            IRI uri = st.getPredicate();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(uri);
            out.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bais);
            IRI deserializedURI = (IRI) in.readObject();
            in.close();

            assertEquals(uri, deserializedURI);

            assertTrue(testCon.hasStatement(bob, uri, nameBob, true));
            assertTrue(testCon.hasStatement(bob, deserializedURI, nameBob, true));
        }

        @Test
        public void testLiteralSerialization() throws Exception {
            testCon.add(bob, name, nameBob);

            Statement st;
            try (RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, false)) {
                st = statements.next();
            }

            Literal literal = (Literal) st.getObject();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(literal);
            out.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bais);
            Literal deserializedLiteral = (Literal) in.readObject();
            in.close();

            assertEquals(literal, deserializedLiteral);

            assertTrue(testCon.hasStatement(bob, name, literal, true));
            assertTrue(testCon.hasStatement(bob, name, deserializedLiteral, true));
        }

        @Test
        public void testGraphSerialization() throws Exception {
            testCon.add(bob, name, nameBob);
            testCon.add(alice, name, nameAlice);

            Model graph;

            try (RepositoryResult<Statement> statements =
                         testCon.getStatements(null, null, null, true)) {
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
        public void testEmptyRollback()
                throws Exception {
            assertTrue(testCon.isEmpty());
            assertTrue(testCon2.isEmpty());
            testCon.begin();
            testCon.add(vf.createBNode(), vf.createIRI("urn:pred"), vf.createBNode());
            assertFalse(testCon.isEmpty());
            assertTrue(testCon2.isEmpty());
            testCon.rollback();
            assertTrue(testCon.isEmpty());
            assertTrue(testCon2.isEmpty());
        }

        @Test
        public void testEmptyCommit()
                throws Exception {
            assertTrue(testCon.isEmpty());
            assertTrue(testCon2.isEmpty());
            testCon.begin();
            testCon.add(vf.createBNode(), vf.createIRI("urn:pred"), vf.createBNode());
            assertFalse(testCon.isEmpty());
            assertTrue(testCon2.isEmpty());
            testCon.commit();
            assertFalse(testCon.isEmpty());
            assertFalse(testCon2.isEmpty());
        }

        @Test
        public void testOpen()
                throws Exception {
            assertTrue(testCon.isOpen());
            assertTrue(testCon2.isOpen());
            testCon.close();
            assertFalse(testCon.isOpen());
            assertTrue(testCon2.isOpen());
        }

        @Test
        public void testSizeRollback() {
            assertEquals(0, testCon.size());
            assertEquals(0, testCon2.size());
            testCon.begin();
            testCon.add(vf.createBNode(), vf.createIRI("urn:pred"), vf.createBNode());
            assertEquals(1, testCon.size());
            assertEquals(0, testCon2.size());
            testCon.add(vf.createBNode(), vf.createIRI("urn:pred"), vf.createBNode());
            assertEquals(2, testCon.size());
            assertEquals(0, testCon2.size());
            testCon.rollback();
            assertEquals(0, testCon.size());
            assertEquals(0, testCon2.size());
        }

        @Test
        public void testSizeCommit() {
            assertEquals(0, testCon.size());
            assertEquals(0, testCon2.size());
            testCon.begin();
            testCon.add(vf.createBNode(), vf.createIRI("urn:pred"), vf.createBNode());
            assertEquals(1, testCon.size());
            assertEquals(0, testCon2.size());
            testCon.add(vf.createBNode(), vf.createIRI("urn:pred"), vf.createBNode());
            assertEquals(2, testCon.size());
            assertEquals(0, testCon2.size());
            testCon.commit();
            assertEquals(2, testCon.size());
            assertEquals(2, testCon2.size());
        }

        @Test
        public void testAddRemove() {
            IRI FOAF_PERSON = vf.createIRI("http://xmlns.com/foaf/0.1/Person");
            final Statement stmt = vf.createStatement(bob, name, nameBob);

            testCon.add(bob, RDF.TYPE, FOAF_PERSON);

            testCon.begin();
            testCon.add(stmt);
            testCon.remove(stmt);
            testCon.commit();

            RDFHandler handler = new AbstractRDFHandler() {
                @Override
                public void handleStatement(Statement st)
                        throws RDFHandlerException {
                    assertNotEquals(stmt, st);
                }
            };

            testCon.exportStatements(null, null, null, false, handler);
        }

        @Test
        public void testInferredStatementCount() {
            assertTrue(testCon.isEmpty());
            int inferred = getTotalStatementCount(testCon);

            IRI root = vf.createIRI("urn:root");

            testCon.add(root, RDF.TYPE, RDF.LIST);
            testCon.remove(root, RDF.TYPE, RDF.LIST);

            assertTrue(testCon.isEmpty());
            assertEquals(inferred, getTotalStatementCount(testCon));
        }

        @Test
        public void testGetContextIDs() {
            assertEquals(0, Iterations.asList(testCon.getContextIDs()).size());

            // load data
            testCon.setAutoCommit(false);
            testCon.add(bob, name, nameBob, context1);
            assertEquals(Collections.singletonList(context1), Iterations.asList(testCon.getContextIDs()));

            testCon.remove(bob, name, nameBob, context1);
            assertEquals(0, Iterations.asList(testCon.getContextIDs()).size());
            testCon.setAutoCommit(true);

            assertEquals(0, Iterations.asList(testCon.getContextIDs()).size());

            testCon.add(bob, name, nameBob, context2);
            assertEquals(Collections.singletonList(context2), Iterations.asList(testCon.getContextIDs()));
        }

        @Disabled // was marked as Broken but it passes
        @Test
        public void testXmlCalendarZ()
                throws Exception {
            String NS = "http://example.org/rdf/";
            int OFFSET = TimeZone.getDefault().getOffset(new Date(2007, Calendar.NOVEMBER, 6).getTime()) / 1000 / 60;
            String SELECT_BY_DATE = "SELECT ?s ?d WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> ?d . FILTER (?d <= ?date) }";
            DatatypeFactory data = DatatypeFactory.newInstance();
            for (int i = 1; i < 5; i++) {
                IRI uri = vf.createIRI(NS, "date" + i);
                XMLGregorianCalendar xcal = data.newXMLGregorianCalendar();
                xcal.setYear(2000);
                xcal.setMonth(11);
                xcal.setDay(i * 2);
                testCon.add(uri, RDF.VALUE, vf.createLiteral(xcal));
                IRI uriz = vf.createIRI(NS, "dateZ" + i);
                xcal = data.newXMLGregorianCalendar();
                xcal.setYear(2007);
                xcal.setMonth(11);
                xcal.setDay(i * 2);
                xcal.setTimezone(OFFSET);
                testCon.add(uriz, RDF.VALUE, vf.createLiteral(xcal));
            }
            XMLGregorianCalendar xcal = data.newXMLGregorianCalendar();
            xcal.setYear(2007);
            xcal.setMonth(11);
            xcal.setDay(6);
            xcal.setTimezone(OFFSET);
            TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, SELECT_BY_DATE);
            query.setBinding("date", vf.createLiteral(xcal));
            List<BindingSet> list = new ArrayList<>();
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    list.add(result.next());
                }
            }
            assertEquals(7, list.size());
        }

        @Test
        public void testOptionalFilter() {
            String optional = "{ ?s :p1 ?v1 OPTIONAL {?s :p2 ?v2 FILTER(?v1<3) } }";
            IRI s = vf.createIRI("urn:test:s");
            IRI p1 = vf.createIRI("urn:test:p1");
            IRI p2 = vf.createIRI("urn:test:p2");
            Literal v1 = vf.createLiteral(1);
            Literal v2 = vf.createLiteral(2);
            Literal v3 = vf.createLiteral(3);
            testCon.add(s, p1, v1);
            testCon.add(s, p2, v2);
            testCon.add(s, p1, v3);
            String qry = "PREFIX :<urn:test:> SELECT ?s ?v1 ?v2 WHERE " + optional;
            TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, qry);
            TupleQueryResult result = query.evaluate();
            Set<List<? extends Value>> set = new HashSet<>();
            while (result.hasNext()) {
                BindingSet bindings = result.next();
                set.add(Arrays.asList(bindings.getValue("v1"), bindings.getValue("v2")));
            }
            result.close();
            assertTrue(set.contains(Arrays.asList(v1, v2)));
            assertTrue(set.contains(Arrays.asList(v3, null)));
        }

        @Test
        public void testOrPredicate() {
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
            assertTrue(list.contains(p1));
            assertTrue(list.contains(p2));
        }

        protected int getTotalStatementCount(RepositoryConnection connection) throws RepositoryException {
            try (RepositoryResult<Statement> iter = connection.getStatements(null, null, null, true)) {
                int size = 0;
                while (iter.hasNext()) {
                    iter.next();
                    ++size;
                }
                return size;
            }
        }
    }
}
