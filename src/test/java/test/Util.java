/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGServerVersion;
import com.franz.agraph.repository.AGTupleQuery;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

public class Util {
    public static File resource(final String path) {
        return new File(Util.class.getResource(path).toExternalForm());
    }

    public static void add(final RepositoryConnection conn,
                           final String path,
                           final String baseURI,
                           final RDFFormat dataFormat,
                           final Resource... contexts) throws RepositoryException, RDFParseException {
        try (final InputStream input = Util.resourceAsStream(path)) {
            conn.add(input, baseURI, dataFormat, contexts);
        } catch (final IOException e) {
            throw new RepositoryException("Unable to load " + path, e);
        }
    }

    public static InputStream resourceAsStream(final String path) {
        return Util.class.getResourceAsStream(path);
    }

    public static File resourceAsTempFile(final String path) {
        return resourceAsTempFile(path, false);
    }

    public static File resourceAsTempFile(final String path,
                                          final boolean executable) {
        final EnumSet<PosixFilePermission> permissions =
                EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE);
        if (executable) {
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        final FileAttribute[] attributes;
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            attributes = new FileAttribute[] {PosixFilePermissions.asFileAttribute(permissions)};
        } else {
            attributes = new FileAttribute[0];
        }
        final File result;
        try {
            result =
                    Files.createTempFile("resource", "tmp", attributes).toFile();
            result.deleteOnExit();
            FileUtils.copyInputStreamToFile(resourceAsStream(path), result);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Gets the ID of a statement with given subject, predicate and object.
     *
     * @param conn Connection object.
     * @param subj Subject.
     * @param pred Predicate.
     * @param obj  Object.
     * @return ID or {@code null}, if it couldn;t be found.
     */
    public static String getStatementId(AGRepositoryConnection conn,
                                        Resource subj, URI pred, Value obj)
            throws QueryEvaluationException {
        final String qs = "select ?id { " +
                "?id <http://franz.com/ns/allegrograph/4.0/tripleId> (?s ?p ?o) }";
        final AGTupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, qs);
        query.setBinding("s", subj);
        query.setBinding("p", pred);
        query.setBinding("o", obj);
        final TupleQueryResult result = query.evaluate();
        try {
            if (result.hasNext()) {
                final String uri = result.next().getValue("id").stringValue();
                // We get back the full URI, but other APIs want only the number
                return uri.substring(uri.indexOf('#') + 1);
            }
        } finally {
            result.close();
        }
        // Not found
        return null;
    }

    public static String get(String[] arr, int i, String defaultVal) {
        if (arr != null && arr.length > i) {
            return arr[i];
        }
        return defaultVal;
    }

    /**
     * null-safe hashCode
     */
    public static int hashCode(Object o) {
        if (o == null) {
            return 0;
        }
        return o.hashCode();
    }

    /**
     * Returns the first argument that is not null.
     * <p>
     * Returns null if there are no such arguments.
     *
     * @param values List of arguments.
     * @param <Type> Type of arguments.
     * @return The first non null argument if it exists, null otherwise.
     */
    @SafeVarargs
    public static <Type> Type coalesce(final Type... values) {
        for (final Type e : values) {
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    public static String ifBlank(String str, String defaultValue) {
        if (str == null || str.trim().isEmpty()) {
            return defaultValue;
        } else {
            return str;
        }
    }

    public static void gzip(File in, File out) throws IOException {
        try (final OutputStream os = new GZIPOutputStream(new FileOutputStream(out))) {
            FileUtils.copyFile(in, os);
            os.flush();
        }
    }

    public static void zip(File in, File out) throws IOException {
        try (final OutputStream os = new ZipOutputStream(new FileOutputStream(out))) {
            FileUtils.copyFile(in, os);
            os.flush();
        }
    }

    public static List arrayList(Object... elements) {
        List list = new ArrayList();
        for (int i = 0; i < elements.length; i++) {
            list.add(elements[i]);
        }
        return list;
    }

    /**
     * List Arrays.asList, but is not varargs,
     * also allows null (returns null), and will
     * convert primitive arrays to List of wrapper objects.
     *
     * @return list or null
     */
    public static List toList(Object arr) {
        if (arr == null) {
            return null;
        }
        if (arr instanceof List) {
            return (List) arr;
        }
        if (arr instanceof Object[]) {
            return Arrays.asList((Object[]) arr);
        }
        List list = new ArrayList();
        if (arr instanceof byte[]) {
            byte[] a = ((byte[]) arr);
            for (int i = 0; i < a.length; i++) {
                list.add(a[i]);
            }
        } else if (arr instanceof char[]) {
            char[] a = ((char[]) arr);
            for (int i = 0; i < a.length; i++) {
                list.add(a[i]);
            }
        } else if (arr instanceof int[]) {
            int[] a = ((int[]) arr);
            for (int i = 0; i < a.length; i++) {
                list.add(a[i]);
            }
        } else if (arr instanceof long[]) {
            long[] a = ((long[]) arr);
            for (int i = 0; i < a.length; i++) {
                list.add(a[i]);
            }
        } else if (arr instanceof float[]) {
            float[] a = ((float[]) arr);
            for (int i = 0; i < a.length; i++) {
                list.add(a[i]);
            }
        } else if (arr instanceof double[]) {
            double[] a = ((double[]) arr);
            for (int i = 0; i < a.length; i++) {
                list.add(a[i]);
            }
        } else {
            throw new IllegalArgumentException("type not handled: " + arr.getClass());
        }
        return list;
    }

    public static List toListDeep(Object obj) {
        List in = toList(obj);
        if (in == null) {
            return null;
        }
        List out = new ArrayList(in.size());
        for (Object o : in) {
            if (o == null) {
                out.add(null);
            } else if (o instanceof List || o.getClass().isArray()) {
                out.add(toListDeep(o));
            } else {
                out.add(o);
            }
        }
        return out;
    }

    public static long fromHumanInt(String value) {
        int len = value.length();
        if (len > 1) {
            char c = value.charAt(len - 1);
            if (!Character.isDigit(c)) {
                int n = Integer.parseInt(value.substring(0, len - 1));
                if (c == 'm') {
                    return n * (long) Math.pow(10, 6);
                } else if (c == 'b') {
                    return n * (long) Math.pow(10, 9);
                } else if (c == 't') {
                    return n * (long) Math.pow(10, 12);
                }
            }
        }
        return Long.parseLong(value);
    }

    public static String toHumanInt(long num, int type) {
        long[] mult;
        if (type == 2) {
            mult = new long[] {1024, 1024, 1024, 1024, 1024};
        } else if (type == 10) {
            mult = new long[] {1000, 1000, 1000, 1000, 1000};
        } else if (type == 60) // milliseconds
        {
            mult = new long[] {1000, 60, 60, 24, 30};
        } else {
            throw new IllegalArgumentException("unknown type: " + type);
        }
        String[] abbrevs;
        if (type == 2) {
            abbrevs = new String[] {"b", "k", "m", "g", "t"};
        } else if (type == 10) {
            abbrevs = new String[] {"", "k", "m", "b", "t"};
        } else if (type == 60) // time
        {
            abbrevs = new String[] {"ms", "s", "m", "h", "d"};
        } else {
            throw new IllegalArgumentException("unknown type: " + type);
        }
        for (int i = 0; i < abbrevs.length; i++) {
            if (num < (mult[i] * 10)) {
                return num + abbrevs[i];
            }
            num = num / mult[i];
        }
        return "" + num;
    }

    public static List reverse(List list) {
        list = new ArrayList(list);
        Collections.reverse(list);
        return list;
    }

    /**
     * Call fn until it returns null or false, or until maxWait time units have elapsed.
     * Sleep for 'sleep' time units between calls.
     *
     * @return the last value from fn
     */
    public static <ReturnType> ReturnType waitFor(TimeUnit unit, long sleep, long maxWait, Callable<ReturnType> fn) throws Exception {
        return waitFor(unit.toMillis(sleep), unit.toNanos(maxWait), fn);
    }

    /**
     * Call fn until it returns null or false, or until maxWaitNanos nanoseconds have elapsed.
     * Sleep for sleepMillis milliseconds between calls.
     *
     * @return the last value from fn
     */
    public static <ReturnType> ReturnType waitFor(long sleepMillis, long maxWaitNanos, Callable<ReturnType> fn) throws Exception {
        long start = System.nanoTime();
        while (true) {
            ReturnType ret = fn.call();
            if (ret == null || Boolean.FALSE.equals(ret)) {
                return ret;
            }
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                continue;
            }
            if ((System.nanoTime() - start) >= maxWaitNanos) {
                return ret;
            }
        }
    }

    /**
     * Exec 'netstat -ntap' and extract the lines which pertain to this java process.
     *
     * @return output lines from netstat
     */
    public static List<String> netstat() throws IOException {
        String[] cmd = {"bash", "-c", "netstat -ntap 2>/dev/null | egrep '\\<'$PPID/java'\\>'"};
        Process p = Runtime.getRuntime().exec(cmd);
        String string = IOUtil.readString(p.getInputStream());
        List<String> list = new ArrayList(Arrays.asList(string.split("\n")));
        list.remove("");
        return list;
    }

    public static List<String> closeWait(List<String> netstat) throws Exception {
        return netstat.stream().filter(line -> line.contains("CLOSE_WAIT")).collect(Collectors.toList());
    }

    /* Warning: Returns null if the filtered netstat output results in 0 lines */
    public static List<String> waitForNetStat(int maxWaitSeconds, final List<String> excluding) throws Exception {
        return Util.waitFor(TimeUnit.SECONDS, 1, maxWaitSeconds, new Callable<List<String>>() {
            public List<String> call() throws Exception {
                List<String> netstat = netstat().stream().filter(line -> {
                    for (String exclude : excluding) {
                        if (line.matches(exclude)) {
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.toList());
                return netstat.isEmpty() ? null : netstat;
            }
        });
    }

    /**
     * Waits up to 30 seconds for all sessions with repoName in the description to go away, polling once per
     * second.
     *
     * @param server
     * @param repoName
     * @return null if all sessions with repoName in the description have gone away within 30 seconds, otherwise returns
     * the last seen map of live sessions (uri/description pairs).
     * @throws Exception
     */
    public static Map<String, String> waitForSessionsToGoAway(final AGServer server, final String repoName) throws Exception {
        Map<String, String> sessions = Util.waitFor(TimeUnit.SECONDS, 1, 30, new Callable<Map<String, String>>() {
            public Map<String, String> call() throws Exception {
                /* Ask server for a map of session uri/description entries */
                Map<String, String> sessions = AGAbstractTest.sessions(server);
                /* Search for a session with repoName in its description */
                for (Entry<String, String> entry : sessions.entrySet()) {
                    if (entry.getValue().contains(repoName)) {
                        /* Found a session with repoName in its description.  Return the sessions map. */
                        return sessions;
                    }
                }
                /* Did not find a session with repoName in its description */
                return null;
            }
        });
        return sessions;
    }

    public static void logTimeStamped(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        System.out.println(sdf.format(Calendar.getInstance().getTime()) + " " + message);
    }

    public static String GetLogDirFromAgraphCFG(String filename)
            throws java.io.FileNotFoundException,
            java.io.IOException {
        String res = null;

        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
        /* Confusingly, this String.match() has implicit ^ and $
           anchors so the regexp needs to be constructed such that
           it matches the entire string */
            if (line.matches("(?i)LogDir\\s+.*")) {
                res = line.split("\\s+", 2)[1];
                break;
            }
        }
        br.close();

        return res;
    }

    public static String GetAgraphCfgFilenameFromAgraphRoot(String filename)
            throws java.io.FileNotFoundException,
            java.io.IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));

        String res = br.readLine();

        br.close();

        return res;
    }

    public static void DumpFile(String filename)
            throws java.io.FileNotFoundException,
            java.io.IOException {
        System.out.println("Dump of " + filename);
        System.out.println("--------------------------------------------------------------");

        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        br.close();

        System.out.println("--------------------------------------------------------------");
    }

    public static void DumpAgraphLog()
            throws java.io.FileNotFoundException,
            java.io.IOException {
        String AgraphRootFilename = "../agraph/lisp/agraph.root";
        File AgraphRootFile = new File(AgraphRootFilename);

        if (AgraphRootFile.exists()) {
            String AgraphCfgFilename = GetAgraphCfgFilenameFromAgraphRoot(AgraphRootFilename);
            String LogDir = GetLogDirFromAgraphCFG(AgraphCfgFilename);

            DumpFile(LogDir + "/agraph.log");
        }
    }

    /**
     * Adds a method nextLong with a max value similar to {@link Random#nextInt(int)}.
     */
    public static class RandomLong extends Random {
        private static final long serialVersionUID = 4874437974204550876L;

        public long nextLong(long max) {
            if (max <= 0) {
                throw new IllegalArgumentException("max must be positive");
            }
            if (max <= Integer.MAX_VALUE) {
                return nextInt((int) max);
            } else {
                int x = (int) (max >> 31);
                if (x == 0) {
                    return nextInt();
                } else {
                    return ((long) nextInt((int) (max >> 31)) << 32) + nextInt();
                }
            }
        }
    }

    public enum LogSearchResult {
        FOUND, NOT_FOUND, TOO_OLD;
    }

    /**
     * Returns the length of the log file.
     *
     * Used to check if a log message was produced as a result
     * of recent operations.
     *
     * @param server Server to connect to.
     * @return Number of characters in the log file.
     */
    public static int getLogSize(AGServer server) {
        return server.getLogFile().length();
    }

    /**
     * Gets the log file and looks for a given string.
     *
     * @param server Server to connect to.
     * @param text Text to look for.
     * @param after Number of log file characters to skip.
     *
     * @return A result object, can be used to tell if the string was
     *         found and if it is present in the specified part of the file.
     */
    public static LogSearchResult searchLog(AGServer server, String text, int after) {
        String logs = server.getLogFile();
        int index = logs.lastIndexOf(text);
        if (index == -1) {
            return LogSearchResult.NOT_FOUND;
        } else if (index < after) {
            return LogSearchResult.TOO_OLD;
        } else {
            return LogSearchResult.FOUND;
        }
    }

    /**
     * Checks the server version to determine if the warmup operation
     * leaves a trace in the log file.
     */
    private static boolean isWarmupLogged(AGServer server) {
        AGServerVersion minVersion = new AGServerVersion("v6.3.0");
        return server.getComparableVersion().compareTo(minVersion) >= 0;
    }

    /**
     * Searches the log file for traces of a string table warmup.
     *
     * @param server Server to connect to.
     * @param after Number of log file characters to skip.
     * @param repo Repository to check.
     * @return Search result.
     */
    public static LogSearchResult searchLogForStringsWarmup(AGServer server, int after, String repo) {
        String strings = String.format("%s\", Warmup of strings", repo);
        return searchLog(server, strings, after);
    }

    /**
     * Searches the log file for traces of a triple indices warmup.
     *
     * @param server Server to connect to.
     * @param after Number of log file characters to skip.
     * @param repo Repository to check.
     * @return Search result.
     */
    public static LogSearchResult searchLogForTriplesWarmup(AGServer server, int after, String repo) {
        String strings = String.format("%s\", Warmup of triples", repo);
        return searchLog(server, strings, after);
    }

    /**
     * Looks at the log file and checks if the string table has been warmed up recently.
     *
     * Raises an assertion failure if there is no trace of a recent string table warmup.
     * Always succeeds if the server is too old to produce log messages during warmup.
     *
     * @param server Server to connect to.
     * @param after Number of log file characters to skip.
     * @param repo Repository to check.
     */
    public static void assertStringsWarmedUpRecently(AGServer server, int after, String repo) {
        if (isWarmupLogged(server)) {
            switch (searchLogForStringsWarmup(server, after, repo)) {
                case NOT_FOUND:
                    Assert.fail("Warmup message not found in logs");
                case TOO_OLD:
                    Assert.fail("Last occurrence of the warmup message is too old.");
                default:
                    break;
            }
        }
    }

    /**
     * Looks at the log file and checks if triple indices have been warmed up recently.
     *
     * Raises an assertion failure if there is no trace of a recent index warmup.
     * Always succeeds if the server is too old to produce log messages during warmup.
     *
     * @param server Server to connect to.
     * @param after Number of log file characters to skip.
     * @param repo Repository to check.
     */
    public static void assertTriplesWarmedUpRecently(AGServer server, int after, String repo) {
        if (isWarmupLogged(server)) {
            switch (searchLogForTriplesWarmup(server, after, repo)) {
                case NOT_FOUND:
                    Assert.fail("Warmup message not found in logs");
                case TOO_OLD:
                    Assert.fail("Last occurrence of the warmup message is too old.");
                default:
                    break;
            }
        }
    }

    /**
     * Looks at the log file and fails if the string table has been warmed up recently.
     * Always succeeds if the server is too old to produce log messages during warmup.
     *
     * @param server Server to connect to.
     * @param after Number of log file characters to skip.
     * @param repo Repository to check.
     */
    public static void assertStringsNotWarmedUpRecently(AGServer server, int after, String repo) {
        if (isWarmupLogged(server)) {
            Assert.assertNotEquals(
                    "The string table has been warmed up recently",
                    LogSearchResult.FOUND,
                    searchLogForStringsWarmup(server, after, repo));
        }
    }

    /**
     * Looks at the log file and fails if triple indices have been warmed up recently.
     * Always succeeds if the server is too old to produce log messages during warmup.
     *
     * @param server Server to connect to.
     * @param after Number of log file characters to skip.
     * @param repo Repository to check.
     */
    public static void assertTriplesNotWarmedUpRecently(AGServer server, int after, String repo) {
        if (isWarmupLogged(server)) {
            Assert.assertNotEquals(
                    "Triple indices have been warmed up recently",
                    LogSearchResult.FOUND,
                    searchLogForTriplesWarmup(server, after, repo));
        }
    }
}
