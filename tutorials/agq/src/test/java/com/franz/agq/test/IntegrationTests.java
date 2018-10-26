package com.franz.agq.test;

import com.franz.agq.Main;
import com.franz.agq.test.utils.DatasetBuilder;
import com.franz.agq.test.utils.RedirectedStdout;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 'Integration' tests - i.e. tests that call 'main'.
 */
public class IntegrationTests {
    // TODO: All the config and running utilities below should be extracted
    //       to a separate project that will be shared by all AG test suites.
    private static String AGRAPH_HOST;
    private static String AGRAPH_PORT;
    private static String AGRAPH_SCHEME;
    private static String AGRAPH_USER;
    private static String AGRAPH_PASSWORD;
    private static String AGRAPH_REPO;

    private AGRepositoryConnection conn;
    private DatasetBuilder stmts;

    /**
     * Tries to retrieve a value from the environment.
     *
     * The following sources are checked to find the value:
     *   - an environment variable named {@code envVar}.
     *   - a property named {@code property}
     *   - the {@code defaultValue}.
     *
     * @param envVar Name of an environment variable.
     * @param property Name of a system property.
     * @param defaultValue Value to return if neither the variable
     *                     nor the property exists.
     * @return Retrieved value.
     */
    private static String getEnvValue(final String envVar,
                                      final String property,
                                      final String defaultValue) {
        String result = null;
        if (envVar != null) {
            result = System.getenv(envVar);
        }
        if (result == null && property != null) {
            result = System.getProperty(property, defaultValue);
        }
        return result;
    }

    /**
     * Compares a CSV file with a given sequence of rows.
     * Causes a test failure if there is a difference.
     *
     * @param message Message to be used for test failures.
     * @param actual CSV file content (*not* path).
     * @param expected Expected values.
     */
    private static void assertCSVEquals(final String message,
                                        final String actual,
                                        final String[]... expected) {
        final Iterable<CSVRecord> records;
        try {
            records = CSVFormat.RFC4180.parse(new StringReader(actual));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int idx = 0;
        for (final CSVRecord record : records) {
            final String[] expectedRecord = expected[idx];
            assertEquals(
                    expectedRecord.length, record.size(),
                    message + " : record lengths differ");
            for (int column = 0; column < record.size(); column++) {
                assertEquals(
                        expectedRecord[column], record.get(column),
                        message + " : difference at " + idx + ":" + column);
            }
            idx++;
        }
    }

    @BeforeAll
    public static void beforeAll() {
        AGRAPH_HOST = getEnvValue("AGRAPH_HOST", "agraph.host", "127.0.0.1");
        AGRAPH_PORT = getEnvValue("AGRAPH_PORT", "agraph.port", "10035");
        AGRAPH_SCHEME = getEnvValue("AGRAPH_SCHEME", "agraph.scheme", "http");
        AGRAPH_REPO = getEnvValue("AGRAPH_REPO", "agraph.repo", "scratchpad");
        AGRAPH_USER = getEnvValue("AGRAPH_USER", "agraph.user", "test");
        AGRAPH_PASSWORD =
                getEnvValue("AGRAPH_PASSWORD", "agraph.password", "xyzzy");
    }

    @BeforeEach
    public void setUp() throws Exception {
        conn = AGServer.createRepositoryConnection(
                AGRAPH_REPO, "/",
                AGRAPH_SCHEME + "://" + AGRAPH_HOST + ":" + AGRAPH_PORT,
                AGRAPH_USER, AGRAPH_PASSWORD);
        conn.clear();
        // Commit after each add, so we do not have to worry about
        // closing the builder in each test.
        stmts = new DatasetBuilder(conn, 1).base("ex://");
    }

    @AfterEach
    public void tearDown() throws Exception  {
        stmts.close();
        conn.close();
    }

    /**
     * Runs the program, interprets its output as UTF-8 and returns it as a string.
     *
     * @param args Arguments to be passed to the program.
     *
     * @return Program's output.
     */
    private String runMain(final String... args) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (final RedirectedStdout unused = new RedirectedStdout(output)) {
            Main.main(args);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        try {
            return output.toString("utf-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs the program, interprets its output as UTF-8 and returns it as a string.
     *
     * Also appends host and credentials to the list of arguments.
     *
     * @param args Arguments to be passed to the program.
     *
     * @return Program's output.
     */
    private String run(final String... args) {
        final String[] defaultArgs = new String[] {
                "--host", AGRAPH_HOST, "--port", AGRAPH_PORT, "--scheme", AGRAPH_SCHEME,
                "--user", AGRAPH_USER, "--password", AGRAPH_PASSWORD, AGRAPH_REPO
        };
        final String[] finalArgs = Arrays.copyOf(args, args.length + defaultArgs.length);
        System.arraycopy(
                defaultArgs, 0, finalArgs, args.length, defaultArgs.length);
        return runMain(finalArgs);
    }

    @Test
    @DisplayName("Run a basic SELECT query")
    public void testSelect() {
        stmts.s("s").p("p").o("o");
        final String result = run(
                "--query", "select ?s ?p ?o { ?s ?p ?o }",
                "--format", "csv");
        assertCSVEquals("Incorrect query result", result,
                new String[] {"s", "p", "o"},
                new String[] {"ex://s", "ex://p", "o"});
    }

    @Test
    @DisplayName("Run a basic ASK query")
    public void testAsk() {
        stmts.s("s").p("p").o("o");
        final String result = run(
                "--query", "ask { ?s ?p \"o\" }",
                "--format", "csv");
        assertEquals("true", result.trim());
    }

    @Test
    @DisplayName("Run a basic CONSTRUCT query")
    public void testConstruct() {
        stmts.s("s").p("p").o("o");
        final String result = run(
                "--query",
                "construct { ?s <ex://x> ?o } where { ?s <ex://p> ?o }",
                "--format", "nt");
        assertEquals("<ex://s> <ex://x> \"o\" .", result.trim());
    }
}
