/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.exception.AGCustomStoredProcException;
import com.franz.agraph.http.storedproc.AGDecoder;
import com.franz.agraph.http.storedproc.AGDeserializer;
import com.franz.agraph.http.storedproc.AGEncoder;
import com.franz.agraph.http.storedproc.AGSerializer;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import junit.framework.Assert;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static test.Stmt.statementSet;

@SuppressWarnings("deprecation")
public class ServerCodeTests extends AGAbstractTest {

    static final Object ALL_TYPES = new Object[] {
            123,
            0,
            -123,
            "abc",
            null,
            new Integer[] {9, 9, 9, 9},
            Util.arrayList(123, 0, -123, "abc"),
            new byte[] {0, 1, 2, 3, 4, 5, 6, 7}
    };
    private static final File STORED_PROC =
            Util.resourceAsTempFile("/test/ag-test-stored-proc.cl");
    private static final File RULES_FILE =
            Util.resourceAsTempFile("/tutorial/java-rules.txt");
    private static String RULES = "java-rules.prolog";
    private static AGServerCode serverCode;

    @BeforeClass
    public static void installScripts() throws Exception {
        serverCode = new AGServerCode(server);
        serverCode.putScript(SProcTest.SCRIPT, STORED_PROC);

        serverCode.deleteInitFile(); // clean
    }

    @AfterClass
    public static void cleanup() throws Exception {
        serverCode.deleteInitFile();
    }

    @Test
    public void storedProcs_encoding_rfe10189() throws Exception {
        byte[][] cases = {{1, 3, 32, 11, 13, 123},
                {},
                {33},
                {33, 44},
                {33, 44, 55},
                {33, 44, 55, 66},
                {33, 44, 55, 66, 77},
                {33, 44, 55, 66, 77, 88},
                {-1, -2, -3, -4, -5},
                {-1, -2, -3, -4}
        };

        for (int casenum = 0; casenum < cases.length; casenum++) {
            byte[] input = cases[casenum];
            String encoded = AGEncoder.encode(input);
            byte[] result = AGDecoder.decode(encoded);
            assertSetsEqual("encoding", input, result);
        }
    }

    @Test
    public void encoding_all_types_rfe10189() throws Exception {
        Object[] o = new Object[] {ALL_TYPES};
        assertEqualsDeep("all types", o,
                AGDeserializer.decodeAndDeserialize(AGSerializer.serializeAndEncode(o)));
    }

    @Test
    public void storedProcsEncoded_rfe10189() throws Exception {
        String response = (String) AGDeserializer.decodeAndDeserialize(
                conn.prepareHttpRepoClient().callStoredProcEncoded("add-two-strings", SProcTest.SCRIPT,
                        AGSerializer.serializeAndEncode(
                                new String[] {"123", "456"})));
        assertEquals(579, Integer.parseInt(response));
    }

    @Test
    public void storedProcs_rfe10189() throws Exception {
        SProcTest sp = new SProcTest(conn);
        assertEquals("supports strings", "579", sp.addTwoStrings("123", "456"));
        assertEquals("supports pos int", 579, sp.addTwoInts(123, 456));
        assertEquals("supports neg int and zero", 0, sp.addTwoInts(123, -123));
        assertEquals("supports neg int", -100, sp.addTwoInts(23, -123));
        assertEquals("supports whole arg-vec strings", "579", sp.addTwoVecStrings("123", "456"));
        assertEquals("supports whole arg-vec ints", 579, sp.addTwoVecInts(123, 456));
        assertEquals("supports null", null, sp.bestBeNull(null));
        try {
            assertEquals(null, sp.bestBeNull("abc"));
            fail("should be AGCustomStoredProcException");
        } catch (AGCustomStoredProcException e) {
            assertEquals("test null and error", "I expected a nil, but got: abc", e.getMessage());
        }
        try {
            assertEquals("579", sp.addTwoVecStringsError());
            fail("should be AGCustomStoredProcException");
        } catch (AGCustomStoredProcException e) {
            assertEquals("test error", "wrong number of args", e.getMessage());
        }
        try {
            assertEquals("579", sp.addTwoVecStrings(null, null));
            fail("should be AGCustomStoredProcException");
        } catch (AGCustomStoredProcException e) {
            assertEquals("test null and error", "There is no integer in the string nil (:start 0 :end 0)", e.getMessage());
        }
        try {
            assertEquals("579", sp.addTwoVecStrings("abc", "def"));
            fail("should be AGCustomStoredProcException");
        } catch (AGCustomStoredProcException e) {
            assertEquals("test error", "There's junk in this string: \"abc\".", e.getMessage());
        }
        assertEqualsDeep("supports all types, originating from java", ALL_TYPES, sp.checkAllTypes(ALL_TYPES));
        assertEqualsDeep("supports all types, round-trip", ALL_TYPES, sp.identity(ALL_TYPES));
        assertEqualsDeep("supports all types, originating from lisp", ALL_TYPES, sp.returnAllTypes());
    }

    @Test
    public void storedProcs_triples_rfe10189() throws Exception {
        SProcTest sp = new SProcTest(conn);
        Assert.assertNotNull("add-a-triple-int", sp.addATripleInt(1));
        assertEqualsDeep("get-a-triple-int", Stmt.stmts(new Stmt(vf.createIRI("http://test.com/add-a-triple-int"),
                        vf.createIRI("http://test.com/p"), vf.createLiteral(1))),
                Stmt.stmts(new Stmt(sp.getATripleInt(1))));
        // TODO: transferring triples does not work yet
        // TODO: change the expected return value when that is known
        //Assert.assertNotNull("add-a-triple", sp.addATriple(vf.createIRI("http://test.com/s"), vf.createIRI("http://test.com/p"), vf.createIRI("http://test.com/p")));
    }

    @Test
    public void storedProcs_rollbackAfterException_spr38315() throws Exception {
        SProcTest sp = new SProcTest(conn);
        conn.setAutoCommit(false);
        long count = conn.size();
        try {
            // add a triple
            Assert.assertNotNull("add-a-triple-int", sp.addATripleInt(1));
            Assert.assertEquals("expected a change in triple count", count + 1, conn.size());
            assertEqualsDeep("get-a-triple-int", Stmt.stmts(new Stmt(vf.createIRI("http://test.com/add-a-triple-int"),
                            vf.createIRI("http://test.com/p"), vf.createLiteral(1))),
                    Stmt.stmts(new Stmt(sp.getATripleInt(1))));
            // this should throw an AGCustomStoredProcException
            sp.bestBeNull("foo");
            Assert.fail("expected an exception.");
        } catch (AGCustomStoredProcException e) {
            conn.rollback();
        }
        Assert.assertEquals("expected no change in triple count", count, conn.size());
    }

    /**
     * @see TutorialExamples#example18()
     * @see TutorialTests#example18()
     */
    public TupleQuery rfe10256_setup() throws Exception {
        TutorialTests.example6_setup(conn, repo);
        conn.setNamespace("kdy", "http://www.franz.com/simple#");
        conn.setNamespace("rltv", "http://www.franz.com/simple#");
        // The rules are already loaded in initFile, so this line is commented:
        // conn.addRules(Util.resourceAsStream("/tutorial/java-rules.txt"));
        String queryString =
                "(select (?ufirst ?ulast ?cfirst ?clast)" +
                        "(uncle ?uncle ?child)" +
                        "(name ?uncle ?ufirst ?ulast)" +
                        "(name ?child ?cfirst ?clast))";
        return conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
    }

    private void rfe10256_fail(TupleQuery tupleQuery) throws QueryEvaluationException {
        try {
            tupleQuery.evaluate();
            fail("expected QueryEvaluationException");
        } catch (QueryEvaluationException e) {
            if (e.getMessage().contains("Undefined functor in Prolog query")) {
                // good
            } else {
                throw e;
            }
        }
    }

    // rfe10256: support loadInitFile param

    @Test
    public void rfe10256_loadInitFile() throws Exception {
        serverCode.deleteInitFile(); // clean before test

        serverCode.putInitFile(RULES_FILE);
        // note, setSessionLoadInitFile must be done before setAutoCommit in example6_setup
        conn.setSessionLoadInitFile(true);
        TupleQuery tupleQuery = rfe10256_setup();
        assertEquals(52, statementSet(tupleQuery.evaluate()).size());
    }

    @Test
    public void rfe10256_doNotPut_InitFile() throws Exception {
        serverCode.deleteInitFile(); // clean before test

        TupleQuery tupleQuery = rfe10256_setup();
        rfe10256_fail(tupleQuery);
    }

    @Test
    public void rfe10256_doNotUse_InitFile() throws Exception {
        serverCode.deleteInitFile(); // clean before test

        serverCode.putInitFile(RULES_FILE);
        TupleQuery tupleQuery = rfe10256_setup();
        rfe10256_fail(tupleQuery);
    }

    @Test
    public void rfe10257_script() throws Exception {
        serverCode.deleteInitFile(); // clean before test
        serverCode.deleteScript(RULES); // clean before test

        serverCode.putScript(RULES, RULES_FILE);
        // note, addSessionLoadScript must be done before setAutoCommit in example6_setup
        conn.addSessionLoadScript(RULES);
        TupleQuery tupleQuery = rfe10256_setup();
        assertEquals(52, statementSet(tupleQuery.evaluate()).size());
    }

    @Test
    public void rfe10257_doNotPut_script() throws Exception {
        serverCode.deleteInitFile(); // clean before test
        serverCode.deleteScript(RULES); // clean before test

        TupleQuery tupleQuery = rfe10256_setup();
        rfe10256_fail(tupleQuery);
    }

    // rfe10257: support script param

    @Test
    public void rfe10257_doNotUse_script() throws Exception {
        serverCode.deleteInitFile(); // clean before test
        serverCode.deleteScript(RULES); // clean before test

        serverCode.putScript(RULES, RULES_FILE);
        TupleQuery tupleQuery = rfe10256_setup();
        rfe10256_fail(tupleQuery);
    }

    static class AGServerCode {
        private final AGServer server;

        AGServerCode(AGServer server) {
            this.server = server;
        }

        private AGHTTPClient http() {
            return this.server.getHTTPClient();
        }

        public TupleQueryResult scripts() throws Exception {
            return http().getTupleQueryResult(server.getServerURL() + "/scripts");
        }

        public void putScript(String path, File script) throws Exception {
            http().put(server.getServerURL() + "/scripts/" + path, null, null,
                    new FileRequestEntity(script, "text/plain"), null);
        }

        public void deleteScript(String path) throws Exception {
            http().delete(server.getServerURL() + "/scripts/" + path, null, null, null);
        }

        public TupleQueryResult initFile() throws Exception {
            return http().getTupleQueryResult(server.getServerURL() + "/initfile");
        }

        public void putInitFile(File script) throws Exception {
            http().put(server.getServerURL() + "/initfile", null, null,
                    new FileRequestEntity(script, "text/plain"), null);
        }

        public void deleteInitFile() throws Exception {
            http().delete(server.getServerURL() + "/initfile", null, null, null);
        }

    }

    /**
     * Example class of how a user might wrap a stored-proc for convenience.
     */
    class SProcTest {
        static final String SCRIPT = "ag-test-stored-proc.cl";

        private final AGRepositoryConnection conn;

        SProcTest(AGRepositoryConnection conn) {
            this.conn = conn;
        }

        String addTwoStrings(String a, String b) throws Exception {
            return (String) conn.callStoredProc("add-two-strings", SCRIPT, a, b);
        }

        int addTwoInts(int a, int b) throws Exception {
            return (Integer) conn.callStoredProc("add-two-ints", SCRIPT, a, b);
        }

        String addTwoVecStrings(String a, String b) throws Exception {
            return (String) conn.callStoredProc("add-two-vec-strings", SCRIPT, a, b);
        }

        String addTwoVecStringsError() throws Exception {
            return (String) conn.callStoredProc("add-two-vec-strings", SCRIPT);
        }

        int addTwoVecInts(int a, int b) throws Exception {
            return (Integer) conn.callStoredProc("add-two-vec-ints", SCRIPT, a, b);
        }

        Object bestBeNull(String a) throws Exception {
            return conn.callStoredProc("best-be-nil", SCRIPT, a);
        }

        Object returnAllTypes() throws Exception {
            return conn.callStoredProc("return-all-types", SCRIPT);
        }

        Object identity(Object input) throws Exception {
            return conn.callStoredProc("identity", SCRIPT, input);
        }

        Object checkAllTypes(Object input) throws Exception {
            return conn.callStoredProc("check-all-types", SCRIPT, input);
        }

        Object addATripleInt(int i) throws Exception {
            return conn.callStoredProc("add-a-triple-int", SCRIPT, i);
        }

        Statement getATripleInt(int i) throws Exception {
            String r = (String) conn.callStoredProc("get-a-triple-int", SCRIPT, i);
            Statement st = parseNtriples(r);
            return st;
        }

        private Statement parseNtriples(String ntriples) throws IOException,
                RDFParseException, RDFHandlerException {
            RDFParser parser = Rio.createParser(RDFFormat.NTRIPLES, vf);
            parser.setPreserveBNodeIDs(true);
            StatementCollector collector = new StatementCollector();
            parser.setRDFHandler(collector);
            parser.parse(new StringReader(ntriples), "http://example.com/");
            Statement st = collector.getStatements().iterator().next();
            return st;
        }

//        Object addATriple(Object s, Object p, Object o) throws Exception {
//            return conn.callStoredProc("add-a-triple", FASL, s, p, o);
//        }

    }

}
