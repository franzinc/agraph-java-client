/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test.stress;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGBooleanQuery;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGStreamTupleQuery;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.util.Closer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import test.Util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static test.AGAbstractTest.findServerUrl;
import static test.AGAbstractTest.password;
import static test.AGAbstractTest.username;

public class Events implements Closeable {

    static final long START = System.currentTimeMillis();
    static private final test.Util.RandomLong RANDOM = new test.Util.RandomLong();
    static private final RandomDateMaker BaselineRange = new RandomDateMaker(new GregorianCalendar(2008, Calendar.JANUARY, 1),
            new GregorianCalendar(2008, Calendar.FEBRUARY, 1));
    static private final RandomDateMaker BulkRange = new RandomDateMaker(BaselineRange.end,
            new GregorianCalendar(2009, Calendar.JANUARY, 1));
    static private final RandomDateMaker SmallCommitsRange = new RandomDateMaker(BulkRange.end,
            new GregorianCalendar(2009, Calendar.FEBRUARY, 1));
    static private final RandomDateMaker DeleteRangeOne = new RandomDateMaker(BaselineRange.start,
            new GregorianCalendar(2008, Calendar.JANUARY, 31));
    static private final RandomDateMaker DeleteRangeTwo = new RandomDateMaker(DeleteRangeOne.end,
            BaselineRange.end);
    static private final RandomDateMaker FullDateRange = new RandomDateMaker(BaselineRange.start,
            SmallCommitsRange.end);
    private static AGRepository repoPool;
    private final Closer closer = new Closer();
    // Holds PhaseMeasures for each phase
    List<PhaseMeasures> phaseMeasures = new ArrayList<PhaseMeasures>();
    private int errors;

    static double logtime(double time) {
        return Defaults.LOG == Defaults.LOGT.NOTIME ? 0 : time;
    }

    public static void trace(String format, Object... values) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        if (Defaults.LOG == Defaults.LOGT.ALL) {
            formatter.format("%s [%2$tF %2$tT.%2$tL]: ", Thread.currentThread().getName(), GregorianCalendar.getInstance());
        } else if (Defaults.LOG == Defaults.LOGT.ELAPSED) {
            formatter.format("%s [%d]: ", Thread.currentThread().getName(), (System.currentTimeMillis() - START));
        } else if (Defaults.LOG == Defaults.LOGT.NOTIME) {
            formatter.format("%s: ", Thread.currentThread().getName());
        }
        formatter.format(format, values);
        System.out.println(sb.toString());
        formatter.close();
    }

    private static void MaybeInitializePool() {
        if (Defaults.POOL_SIZE == 0) {
            return;
        }

        try {
            String myServerUrl = Defaults.URL; // "http://localhost:10035";
            String myUsername = Defaults.USERNAME; // "test";
            String myPassword = Defaults.PASSWORD; // "xyzzy";
            String myCatalog = Defaults.CATALOG; // "java-catalog";
            String myRepo = Defaults.REPOSITORY; // "javatest";

            AGConnPool pool = AGConnPool.create(
                    AGConnProp.serverUrl, myServerUrl,
                    AGConnProp.username, myUsername,
                    AGConnProp.password, myPassword,
                    AGConnProp.catalog, myCatalog,
                    AGConnProp.repository, myRepo,
                    // The above values must match the repo defined above;
                    // that redundancy should go away in a future release,
                    // as part of rfe11963.
                    AGConnProp.session,
                    (Defaults.SHARED ? AGConnProp.Session.SHARED : AGConnProp.Session.DEDICATED),
                    AGConnProp.sessionLifetime, TimeUnit.MINUTES.toSeconds(10),
                    AGPoolProp.testOnBorrow, true,
                    AGPoolProp.timeBetweenEvictionRunsMillis, TimeUnit.SECONDS.toMillis(120),
                    AGPoolProp.minEvictableIdleTimeMillis, TimeUnit.SECONDS.toMillis(120),
                    AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(60),
                    AGPoolProp.initialSize, Defaults.POOL_SIZE,
                    AGPoolProp.maxActive, Defaults.POOL_SIZE);

            AGServer server = new AGServer(myServerUrl, myUsername, myPassword);
            try {
                AGCatalog catalog = server.getCatalog(myCatalog);
                repoPool = catalog.createRepository(myRepo);
                repoPool.setConnPool(pool);
                repoPool.initialize();
            } finally {
                server.close();
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    private static Value CalendarToValue(GregorianCalendar calendar) {
        return ThreadVars.valueFactory.get().createLiteral(ThreadVars.datatypeFactory.get().newXMLGregorianCalendar(calendar));
    }

    static AGTupleQuery streamQuery(AGTupleQuery tupleQuery) {
        if (Defaults.stream == Defaults.STREAM.PULL || Defaults.stream == Defaults.STREAM.PULH) {
            tupleQuery = new AGStreamTupleQuery(tupleQuery);
        }
        return tupleQuery;
    }

    /**
     * @param args Run with --help
     */
    public static void main(String[] args) throws Exception {
        Events events = new Events();
        try (Events ignored = events) {
            events.run(args);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(-1);
        }
        if (events.errors > 0) {
            // exit with error
            throw new Exception("Errors during execution: " + events.errors);
        }
        System.exit(0);
    }

    @Override
    public void close() {
        closer.close();
    }

    private AGRepositoryConnection connect() throws RepositoryException {
        AGServer server = closer.closeLater(new AGServer(Defaults.URL, Defaults.USERNAME, Defaults.PASSWORD));
        AGCatalog cat = server.getCatalog(Defaults.CATALOG);
        AGRepository repo = closer.closeLater(cat.createRepository(Defaults.REPOSITORY));
        repo.initialize();
        AGRepositoryConnection conn = closer.closeLater(repo.getConnection());
        if (!Defaults.SHARED) {
            // Force an auto-committing non-shared backend
            conn.setAutoCommit(false);
            conn.setAutoCommit(true);
            trace("Dedicated backend: " + conn.prepareHttpRepoClient().getRoot());
        } else {
            trace("Shared backend: " + conn.prepareHttpRepoClient().getRoot());
        }
        return conn;
    }

    // Prepares a new PhaseMeasures object for 'phase', adds it to
    // phaseMeasures, and returns it.
    PhaseMeasures addPhaseMeasures(String phase) {
        PhaseMeasures pm = new PhaseMeasures(phase);

        phaseMeasures.add(pm);
        return pm;
    }

    String phaseMeasuresToString() {
        StringBuilder res = new StringBuilder();
        boolean first = true;

        res.append("(");
        for (PhaseMeasures pm : phaseMeasures) {
            if (first) {
                first = false;
            } else {
                res.append(" ");
            }
            res.append(pm.toString());
        }
        res.append(")");

        return res.toString();
    }

    public void run(String[] args) throws Exception {
        Defaults.init(args);

        Thread.currentThread().setName("./events");

        if (Defaults.hasOption("open")) {
            trace("OPENING %s %s:%s.", Defaults.URL, Defaults.CATALOG, Defaults.REPOSITORY);
        } else {
            trace("SUPERSEDING %s %s:%s.", Defaults.URL, Defaults.CATALOG, Defaults.REPOSITORY);
        }

        long initStart = System.currentTimeMillis(), initEnd;
        double initSeconds;
        trace("Phase 0 Begin: " + (Defaults.hasOption("open") ? "opening " : "renewing ") +
                Defaults.CATALOG + ":" + Defaults.REPOSITORY);
        AGServer server = new AGServer(Defaults.URL, Defaults.USERNAME, Defaults.PASSWORD);
        AGCatalog catalog = server.getCatalog(Defaults.CATALOG);
        if (!Defaults.hasOption("open")) {
            catalog.deleteRepository(Defaults.REPOSITORY);
        }
        server.close();

        MaybeInitializePool();

        AGRepositoryConnection conn = connect();
        initEnd = System.currentTimeMillis();
        initSeconds = (initEnd - initStart) / 1000;
        trace("Phase 0 End: Initial " + (Defaults.hasOption("open") ? "opening" : "renewing") +
                " took " + initSeconds + " seconds.");

        // thread needed to send pings to conn in case any phase exceeds the session lifetime.
        Thread ping = new Thread(new Pinger(conn));
        ping.start();

        ThreadVars.valueFactory.set(conn.getValueFactory());
        conn.getRepository().getValueFactory().getRepository().setBulkMode(Defaults.BULKMODE);

        AllEvents.initialize();

        final long triplesAtStart = conn.size();

        trace("Testing with %d loading, %d querying processes. Repository contains %d triples.",
                Defaults.LOAD_WORKERS, Defaults.QUERY_WORKERS, triplesAtStart);
        final long startTime = System.currentTimeMillis();

        if (Defaults.LOAD_WORKERS > 0) {
            long triplesStart = triplesAtStart;
            long triplesEnd = triplesStart;
            long start, end, triples;
            double seconds;
            ExecutorService executor = Executors.newFixedThreadPool(Defaults.LOAD_WORKERS);

            /////////////////////////////////////////////////////////////////////// PHASE 1
            if (Defaults.START_PHASE <= 1) {
                start = System.currentTimeMillis();
                trace("Phase 0 Begin: Launching child load workers.");
                List<Loader> tasks = new ArrayList<>(Defaults.LOAD_WORKERS);
                for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
                    tasks.add(new Loader(task, Defaults.SIZE / 10, 1, BaselineRange));
                }
                end = System.currentTimeMillis();
                seconds = (end - start) / 1000;
                trace("Phase 0 End: Initial load_workers took " + seconds + " seconds.");

                trace("Phase 1 Begin: Baseline %d triple commits.", Defaults.EVENT_SIZE);
                Monitor.start(1);
                start = System.currentTimeMillis();
                invokeAndGetAll(executor, tasks);
                end = System.currentTimeMillis();
                triplesEnd = conn.size();
                triples = triplesEnd - triplesStart;
                seconds = (end - start) / 1000.0;
                addPhaseMeasures("Phase 1 (small commits)").add(new Measure("timestamp", end / 1000L)).add(new Measure("seconds", seconds));
                trace("Phase 1 End: %d total triples added in %.3f seconds " +
                                "(%.2f triples/second, %.2f commits/second). " +
                                "Store contains %d triples.", triples, logtime(seconds),
                        logtime(triples / seconds),
                        logtime(triples / Defaults.EVENT_SIZE / seconds), triplesEnd);

                closer.close(tasks);
                Monitor.stop(1, conn);

            }

            /////////////////////////////////////////////////////////////////////// PHASE 2
            if (Defaults.START_PHASE <= 2) {
                triplesStart = triplesEnd;
                List<Loader> tasks = new ArrayList<>(Defaults.LOAD_WORKERS);
                for (int task = 0; task < (Defaults.LOAD_WORKERS); task++) {
                    tasks.add(new Loader(task, (Defaults.SIZE / 10) * 9, Defaults.BULK_EVENTS, BulkRange));
                }
                trace("Phase 2 Begin: Grow store by about %d triples.", (Defaults.SIZE * 9 / 10));
                Monitor.start(2);
                start = System.currentTimeMillis();
                invokeAndGetAll(executor, tasks);
                end = System.currentTimeMillis();
                triplesEnd = conn.size();
                triples = triplesEnd - triplesStart;
                seconds = (end - start) / 1000.0;
                addPhaseMeasures("Phase 2 (big commits)").add(new Measure("timestamp", end / 1000L)).add(new Measure("seconds", seconds));
                trace("Phase 2 End: %d total triples bulk-loaded in %.3f seconds " +
                                "(%.2f triples/second, %.2f commits/second). " +
                                "Store contains %d triples.", triples, seconds, triples / seconds,
                        triples / Defaults.BULK_EVENTS / Defaults.EVENT_SIZE / seconds, triplesEnd);

                closer.close(tasks);
                Monitor.stop(2, conn);

            }

            /////////////////////////////////////////////////////////////////////// PHASE 3
            if (Defaults.START_PHASE <= 3) {
                triplesStart = triplesEnd;
                List<Loader> tasks = new ArrayList<>(Defaults.LOAD_WORKERS);
                for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
                    tasks.add(task, new Loader(task, Defaults.SIZE / 10, 1, SmallCommitsRange));
                }
                trace("Phase 3 Begin: Perform %d triple commits.", Defaults.EVENT_SIZE);
                Monitor.start(3);
                start = System.currentTimeMillis();
                invokeAndGetAll(executor, tasks);
                end = System.currentTimeMillis();
                trace("p3: trying to get repo size on conn " + conn.prepareHttpRepoClient().getRoot());
                triplesEnd = conn.size();
                triples = triplesEnd - triplesStart;
                seconds = (end - start) / 1000.0;
                addPhaseMeasures("Phase 3 (small commits)").add(new Measure("timestamp", end / 1000L)).add(new Measure("seconds", seconds));
                trace("Phase 3 End: %d total triples added in %.3f seconds " +
                                "(%.2f triples/second, %.2f commits/second). " +
                                "Store contains %d triples.", triples, seconds, triples / seconds,
                        triples / Defaults.EVENT_SIZE / seconds, triplesEnd);

                executor.shutdown();
                closer.close(tasks);
                Monitor.stop(3, conn);

            }
        }

        /////////////////////////////////////////////////////////////////////// PHASE 4 & 5
        if (Defaults.START_PHASE <= 4) {
            if (Defaults.QUERY_WORKERS > 0 && Defaults.START_PHASE > 0) {
                long start = System.currentTimeMillis(), end;
                double seconds;

                trace("Phase 0 Begin: Launching child query workers.");
                ExecutorService executor = Executors.newFixedThreadPool(Defaults.QUERY_WORKERS);
                List<Querier> sparqlQueriers = new ArrayList<>(Defaults.QUERY_WORKERS);
                List<Querier> prologQueriers = new ArrayList<>(Defaults.QUERY_WORKERS);
                for (int task = 0; task < Defaults.QUERY_WORKERS; task++) {
                    sparqlQueriers.add(new Querier(task, Defaults.QUERY_TIME * 60, FullDateRange,
                            AGQueryLanguage.SPARQL));
                    prologQueriers.add(new Querier(task, Defaults.QUERY_TIME * 60, FullDateRange,
                            AGQueryLanguage.PROLOG));
                }
                end = System.currentTimeMillis();
                seconds = (end - start) / 1000.0;
                trace("Phase 0 End: Initial query_workers took " + seconds + " seconds.");

                trace("Phase 4 Begin: Perform SPARQL queries with %d processes for %d %s.",
                        Defaults.QUERY_WORKERS,
                        Defaults.QUERY_SIZE == 0 ? Defaults.QUERY_TIME : Defaults.QUERY_SIZE,
                        Defaults.QUERY_SIZE == 0 ? "minutes" : "queries");
                Monitor.start(4);
                int queries = 0;
                long triples = 0;
                start = System.currentTimeMillis();
                try {
                    List<Future<Object>> fs = executor.invokeAll(sparqlQueriers);
                    for (Future<Object> f : fs) {
                        QueryResult queryResult = (QueryResult) f.get();
                        queries += queryResult.queries;
                        triples += queryResult.triples;
                    }
                } catch (InterruptedException e) {
                    errors++;
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    errors++;
                    e.printStackTrace();
                }
                end = System.currentTimeMillis();
                seconds = (end - start) / 1000.0;
                addPhaseMeasures("Phase 4 (SPARQL queries)").add(new Measure("timestamp", end / 1000L)).add(new Measure("seconds", seconds));
                trace("Phase 4 End: %d total triples returned over %d queries in " +
                                "%.3f seconds (%.2f triples/second, %.2f queries/second, " +
                                "%d triples/query) MemUsed %d.", triples, queries, logtime(seconds),
                        logtime(triples / seconds), logtime(queries / seconds), triples / queries,
                        ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                Monitor.stop(4, conn);

                closer.close(sparqlQueriers);

                trace("Phase 5 Begin: Perform Prolog queries with %d processes for %d %s.",
                        Defaults.QUERY_WORKERS,
                        Defaults.QUERY_SIZE == 0 ? Defaults.QUERY_TIME : Defaults.QUERY_SIZE,
                        Defaults.QUERY_SIZE == 0 ? "minutes" : "queries");
                Monitor.start(5);
                queries = 0;
                triples = 0;
                start = System.currentTimeMillis();
                try {
                    List<Future<Object>> fs = executor.invokeAll(prologQueriers);
                    for (Future<Object> f : fs) {
                        QueryResult queryResult = (QueryResult) f.get();
                        queries += queryResult.queries;
                        triples += queryResult.triples;
                    }
                } catch (InterruptedException e) {
                    errors++;
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    errors++;
                    e.printStackTrace();
                }
                end = System.currentTimeMillis();
                seconds = (end - start) / 1000.0;
                addPhaseMeasures("Phase 5 (Prolog queries)").add(new Measure("timestamp", end / 1000L)).add(new Measure("seconds", seconds));
                trace("Phase 5 End: %d total triples returned over %d queries in " +
                                "%.3f seconds (%.2f triples/second, %.2f queries/second, " +
                                "%d triples/query).", triples, queries, logtime(seconds),
                        logtime(triples / seconds), logtime(queries / seconds), triples / queries);
                Monitor.stop(5, conn);

                closer.close(prologQueriers);

                executor.shutdown();
            }
        }

        /////////////////////////////////////////////////////////////////////// PHASE 6
        if (Defaults.START_PHASE <= 6 && Defaults.DELETE_WORKERS > 0) {
            long triplesStart = conn.size();
            long start = System.currentTimeMillis(), end;
            double seconds;

            trace("Phase 0 Begin: Launching child delete workers.");

            ExecutorService executor = Executors.newFixedThreadPool(2);
            List<Deleter> tasks = new ArrayList<>(2);
            tasks.add(new Deleter(DeleteRangeOne));
            if (Defaults.DELETE_WORKERS > 1) {
                tasks.add(new Deleter(DeleteRangeTwo));
            }
            end = System.currentTimeMillis();
            seconds = (end - start) / 1000.0;
            trace("Phase 0 End: Initial delete_workers took " + seconds + " seconds.");
            trace("Phase 6 Begin: Delete events.");
            Monitor.start(6);
            start = System.currentTimeMillis();
            invokeAndGetAll(executor, tasks);
            end = System.currentTimeMillis();
            closer.close(tasks);
            executor.shutdown();
            long triplesEnd = conn.size();
            long triples = triplesStart - triplesEnd;
            seconds = (end - start) / 1000.0;
            addPhaseMeasures("Phase 6 (deletes)").add(new Measure("timestamp", end / 1000L)).add(new Measure("seconds", seconds));
            trace("Phase 6 End: %d total triples deleted in %.3f seconds " +
                            "(%.2f triples/second). Store contains %d triples.",
                    triples, logtime(seconds), logtime(triples / seconds), triplesEnd);
            Monitor.stop(6, conn);
        }

        /////////////////////////////////////////////////////////////////////// PHASE 7
        if (Defaults.START_PHASE <= 7 && Defaults.MIXED_RUNS != 0) {
            Monitor.start(7);
            RandomDateMaker smallCommitsRange = SmallCommitsRange;
            RandomDateMaker fullDateRange = FullDateRange;
            RandomDateMaker deleteRangeOne = DeleteRangeOne;
            RandomDateMaker deleteRangeTwo = DeleteRangeTwo;

            ExecutorService executor = Executors.newFixedThreadPool(Defaults.LOAD_WORKERS + Defaults.QUERY_WORKERS + 2);
            for (int run = 0; run < Defaults.MIXED_RUNS || Defaults.MIXED_RUNS == -1; run++) {
                int queries = 0;
                long triples = 0;
                long added = 0;
                long deleted = 0;
                smallCommitsRange = smallCommitsRange.next(Calendar.DAY_OF_YEAR, 30);
                fullDateRange = new RandomDateMaker(deleteRangeTwo.end, smallCommitsRange.end);
                deleteRangeOne = deleteRangeTwo.next(Calendar.DAY_OF_YEAR, 15);
                deleteRangeTwo = deleteRangeOne.next(Calendar.DAY_OF_YEAR, 15);

                trace("Phase 7 Begin: Mixed workload - adds, queries, deletes. %s", run);
                List<Task> tasks = new ArrayList<>(Defaults.LOAD_WORKERS + Defaults.QUERY_WORKERS + 2);

                for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
                    tasks.add(new Loader(task, Defaults.SIZE / 10, 1, smallCommitsRange));
                }
                for (int task = 0; task < Defaults.QUERY_WORKERS; task++) {
                    tasks.add(new Querier(task, Defaults.QUERY_TIME * 60, fullDateRange, AGQueryLanguage.SPARQL));
                }
                if (Defaults.DELETE_WORKERS > 0) {
                    tasks.add(new Deleter(deleteRangeOne));
                    if (Defaults.DELETE_WORKERS > 1) {
                        tasks.add(new Deleter(deleteRangeTwo));
                    }
                }

                long start = System.currentTimeMillis();
                try {
                    List<Future<Object>> fs = executor.invokeAll(tasks);
                    for (Future<Object> f : fs) {
                        Object o = f.get();
                        if (o instanceof QueryResult) {
                            QueryResult queryResult = (QueryResult) o;
                            queries += queryResult.queries;
                            triples += queryResult.triples;
                        } else {
                            int i = (Integer) o;
                            if (i < 0) {
                                deleted += i;
                            } else {
                                added += i;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    errors++;
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    errors++;
                    e.printStackTrace();
                }
                long end = System.currentTimeMillis();
                double seconds = (end - start) / 1000.0;
                trace("Phase 7 End: %d total triples returned over %d queries in " +
                                "%.3f seconds (%.2f triples/second, %.2f queries/second, " +
                                "%d triples/query, %d triples added, %d deletes).", triples, queries,
                        logtime(seconds), logtime(triples / seconds), logtime(queries / seconds),
                        (queries == 0 ? 0 : triples / queries), added, deleted);
                closer.close(tasks);
            }
            executor.shutdown();
            Monitor.stop(7, conn);
        }

        if (repoPool != null) {
            repoPool.shutDown();
        }

        /////////////////////////////////////////////////////////////////////// END
        long triplesEnd = conn.size();
        double totalSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        long triples = triplesEnd - triplesAtStart;

        trace("Test completed in %.1f total seconds - store contains %d triples (%d triples added/removed).",
                logtime(totalSeconds), triplesEnd, triples);
        trace("MEASURES: " + phaseMeasuresToString());
    }

    private <Type> void invokeAndGetAll(ExecutorService executor,
                                        List<? extends Callable<Type>> tasks) {
        try {
            List<Future<Type>> fs = executor.invokeAll(tasks);
            for (Future<Type> f : fs) {
                f.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            errors++;
            e.printStackTrace();
        }
    }

    private static interface RandomCallback {
        public Value makeValue();
    }

    // This exists because Java can't handle stuff like
    // List<? extends A & B>. #$%#$%^!!! generics....
    private interface Task extends Callable<Object>, AutoCloseable {
    }

    static class Defaults {

        // The namespace
        static private final String NS = "http://franz.com/events#";
        // Number of worker processes
        static int LOAD_WORKERS = 20;
        static int QUERY_WORKERS = 8;
        static int DELETE_WORKERS = 2;
        static int MIXED_RUNS = 0;
        // Goal store size
        static long SIZE = (long) Math.pow(10, 9);
        // The catalog name
        static String CATALOG = "tests";
        // The repository name
        static String REPOSITORY = "events_test";
        static int START_PHASE = 1;
        static int STATUS = 500;
        static int VERBOSE = 0;
        static String URL = findServerUrl();
        static String USERNAME = username();
        static String PASSWORD = password();
        static boolean MONITOR = false;
        static boolean BULKMODE = false;
        static boolean SHARED = false;
        static LOGT LOG = LOGT.ALL;
        static STREAM stream = STREAM.NONE;
        private static CommandLine cmd;
        // Time to run queries in minutes
        static private int QUERY_TIME = 10;
        // When non-zero perform QUERY_SIZE queries in query phases
        static private int QUERY_SIZE = 0;
        // Size of the Events
        static private int EVENT_SIZE = 50;
        // Events per commit in bulk load phase
        static private int BULK_EVENTS = 250;
        // When non-zero use a connection pool of the specified size
        static private int POOL_SIZE = 0;

        static String cmdVal(String opt, String defaultVal) {
            String val = cmd.getOptionValue(opt);
            if (val == null) {
                return defaultVal;
            } else {
                return val;
            }
        }

        static int cmdVal(String opt, int defaultVal) {
            String val = cmd.getOptionValue(opt);
            if (val == null) {
                return defaultVal;
            } else {
                return Integer.parseInt(val);
            }
        }

        static int cmdVal(String opt, int defaultVal, int defaultVal2) {
            if (cmd.hasOption(opt)) {
                String val = cmd.getOptionValue(opt);
                if (val == null) {
                    return defaultVal2;
                } else {
                    return Integer.parseInt(val);
                }
            } else {
                return defaultVal;
            }
        }

        static boolean cmdVal(String opt, boolean defaultVal) {
            String val = cmd.getOptionValue(opt);
            if (val == null) {
                return defaultVal;
            } else {
                return Boolean.parseBoolean(val);
            }
        }

        @SuppressWarnings("static-access")
        static <EnumType extends Enum> EnumType cmdVal(String opt, EnumType defaultVal) {
            String val = cmd.getOptionValue(opt);
            if (val == null) {
                return defaultVal;
            } else {
                return (EnumType) defaultVal.valueOf(defaultVal.getClass(), val);
            }
        }

        @SuppressWarnings("static-access")
        public static void init(String[] args) throws Exception {
            Options options = new Options();
            options.addOption(new Option("h", "help", false, "print this message"));
            options.addOption(OptionBuilder.withLongOpt("delete-using-prolog")
                    .withDescription("Use prolog to delete triples in phase 6.  Default is to use SPARQL")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("open")
                    .withDescription("OPEN the repository and add to it [default is to SUPERSEDE]")
                    .create("o"));
            options.addOption(OptionBuilder.withLongOpt("status")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("use STATUS size [default=" + STATUS + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("load")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("use number of loading processes [default=" + LOAD_WORKERS + "]")
                    .create("l"));
            options.addOption(OptionBuilder.withLongOpt("query")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("use number of querying processes [default=" + QUERY_WORKERS + "]")
                    .create("q"));
            options.addOption(OptionBuilder.withLongOpt("delete")
                    .withArgName("0-2").hasArg()
                    .withDescription("use 0-2 deleting processes [default=" + DELETE_WORKERS + "]")
                    .create("d"));
            options.addOption(OptionBuilder.withLongOpt("size")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("triple limit for bulk load (e.g. 10,000, 100m, 2b, 1.5t) (minimum=1000) [default=" + SIZE + "]")
                    .create("s"));
            options.addOption(OptionBuilder.withLongOpt("time")
                    .withArgName("MINUTES").hasArg()
                    .withDescription("time limit for query phase [default=" + QUERY_TIME + "]")
                    .create("t"));
            options.addOption(OptionBuilder.withLongOpt("query-size")
                    .withArgName("QUERY_SIZE").hasArg()
                    .withDescription("Total QUERY_EVENTS to perform in each query phase [default=" + QUERY_SIZE + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("event")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("each event will contain number of triples [default=" + EVENT_SIZE + "]")
                    .create("e"));
            options.addOption(OptionBuilder.withLongOpt("bulk")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("commit number of events per commit during bulk load [default=" + BULK_EVENTS + "]")
                    .create("b"));
            options.addOption(OptionBuilder.withLongOpt("mixed")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("Add MIXED workload phases after the normal run (-1, runs until java process is killed) [default=" + MIXED_RUNS + "]")
                    .create("m"));
            options.addOption(OptionBuilder.withLongOpt("verbose")
                    .withArgName("INTEGER").hasOptionalArg()
                    .withDescription("Verbosity level for extra log messages if VERBOSE > 0. [default=" + VERBOSE + ", no value=1]")
                    .create("v"));
            options.addOption(OptionBuilder.withLongOpt("catalog")
                    .withArgName("NAME").hasArg()
                    .withDescription("AGraph catalog [default=" + CATALOG + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("repository")
                    .withArgName("NAME").hasArg()
                    .withDescription("AGraph repository [default=" + REPOSITORY + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("url")
                    .withArgName("URL").hasArg()
                    .withDescription("AGraph URL [default=" + URL +
                            " from env var AGRAPH_HOST, java property AGRAPH_HOST, or \"localhost\";" +
                            " and env var AGRAPH_PORT, java property AGRAPH_PORT, ../agraph/lisp/agraph.port, or 10035")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("username")
                    .withArgName("NAME").hasArg()
                    .withDescription("AGraph username [default=" + USERNAME +
                            " from env var AGRAPH_USER, or \"test\"]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("password")
                    .withArgName("PASSWORD").hasArg()
                    .withDescription("AGraph password [default=" + PASSWORD +
                            " from env var AGRAPH_PASSWORD, or \"xyzzy\"]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("seed")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("seed to the random number generator so that one can do repeated runs with" +
                            " the exact same data generated. Default: random.")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("phase")
                    .withArgName("1-6").hasArg()
                    .withDescription("Run the test starting at phase 1-6 [default=" + START_PHASE + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("monitor")
                    .withArgName("true|false").hasArg()
                    .withDescription("Try to start monitor.sh or not. [default=" + MONITOR + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("log")
                    .withArgName("LOG").hasArg()
                    .withDescription("One of: " + Arrays.asList(LOGT.values()) + ". [default=" + LOG + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("stream")
                    .withArgName("STREAM").hasArg()
                    .withDescription("Stream results. One of: " + Arrays.asList(STREAM.values()) + ". [default=" + stream + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("bulkmode")
                    .withArgName("true|false").hasArg()
                    .withDescription("Use repository bulk mode. [default=" + BULKMODE + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("pool-size")
                    .withArgName("POOL_SIZE").hasArg()
                    .withDescription("When non-zero, sets the connection pool size [default=" + POOL_SIZE + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("shared")
                    .withArgName("true|false").hasArg()
                    .withDescription("Use shared backends instead of dedicated sessions. [default=" + SHARED + "]")
                    .create());

            cmd = new PosixParser().parse(options, args);
            if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("./events [options]",
                        "To performance test AllegroGraph v4, connects as" +
                                " a client, uses multiple threads, and adds data, makes" +
                                " queries, deletes data, and runs mixed-load threads.",
                        options,
                        "Example usage:\n" +
                                "./events.sh --help\n" +
                                "# Prints help text.\n" +
                                "./events.sh \n" +
                                "# Runs default options: supersedes repo, runs test.\n" +
                                "./events.sh --open --status 500 -v 1 -l 1 -d 0 -q 1 -m 1 --size 100000 --sparql\n" +
                                "# Runs less verbose status, single-threaded, adding to an existing repo, no deletes, sparql queries.\n" +
                                "./events.sh -l 10 -q 2 -m 2 --size 1m\n" +
                                "# Runs with a superseded repo, prolog queries, multi-threaded.\n",
                        false);
                System.exit(0);
            }

            LOAD_WORKERS = cmdVal("load", LOAD_WORKERS);
            QUERY_WORKERS = cmdVal("query", QUERY_WORKERS);
            DELETE_WORKERS = cmdVal("delete", DELETE_WORKERS);
            MIXED_RUNS = cmdVal("mixed", MIXED_RUNS);
            STATUS = cmdVal("status", STATUS);
            SIZE = Math.max(1000, test.Util.fromHumanInt(cmdVal("size", "" + SIZE)));
            QUERY_TIME = cmdVal("time", QUERY_TIME);
            QUERY_SIZE = cmdVal("query-size", QUERY_SIZE);
            EVENT_SIZE = cmdVal("event", EVENT_SIZE);
            BULK_EVENTS = cmdVal("bulk", BULK_EVENTS);
            VERBOSE = cmdVal("verbose", VERBOSE, 1);
            CATALOG = cmdVal("catalog", CATALOG);
            REPOSITORY = cmdVal("repository", REPOSITORY);
            URL = cmdVal("url", URL);
            USERNAME = cmdVal("username", USERNAME);
            PASSWORD = cmdVal("password", PASSWORD);
            START_PHASE = cmdVal("phase", START_PHASE);
            MONITOR = cmdVal("monitor", MONITOR);
            LOG = cmdVal("log", LOG);
            stream = cmdVal("stream", stream);
            BULKMODE = cmdVal("bulkmode", BULKMODE);
            POOL_SIZE = cmdVal("pool-size", POOL_SIZE);
            SHARED = cmdVal("shared", SHARED);

            if (cmd.hasOption("seed")) {
                long seed = Long.parseLong(cmd.getOptionValue("seed"));
                RANDOM.setSeed(seed);
                trace("Set random seed to %s.", seed);
            }
            trace("Parameters:"
                    + " catalog=" + CATALOG
                    + " repository=" + REPOSITORY
                    + " url=" + URL
                    + " size=" + SIZE
                    + " stream=" + stream);
        }

        public static boolean hasOption(String opt) {
            return cmd.hasOption(opt);
        }

        public enum LOGT {
            ALL, ELAPSED, NOTIME;
        }

        /* FIXME: What do each of these mean? */
        public enum STREAM {
            NONE, PULL, HAND, PULH;
        }
    }

    private static class ThreadVars {
        private static ThreadLocal<ValueFactory> valueFactory = new ThreadLocal<ValueFactory>();
        private static ThreadLocal<RandomDateMaker> dateMaker = new ThreadLocal<RandomDateMaker>();
        private static ThreadLocal<DatatypeFactory> datatypeFactory = new ThreadLocal<DatatypeFactory>() {
            protected DatatypeFactory initialValue() {
                DatatypeFactory factory = null;
                try {
                    factory = DatatypeFactory.newInstance();
                } catch (DatatypeConfigurationException e) {
                    System.err.println("Couldn't create DatatypeFactory!");
                    e.printStackTrace();
                }
                return factory;
            }
        };
    }

    /* This is a class which will return random (by way of getRandom)
       GregorianCalendar's between a given start and end
       GregorianCalendar */
    private static class RandomDateMaker {
        public GregorianCalendar start, end;
        int seconds; // The number of seconds in the range

        public RandomDateMaker(GregorianCalendar theStart, GregorianCalendar theEnd) {
            assert theStart.before(theEnd);
            start = theStart;
            end = theEnd;
            seconds = (int) ((end.getTimeInMillis() - start.getTimeInMillis()) / 1000L);
        }

        /* Return a random GregorianCalendar between the start and end GregorianCalendar's */
        public GregorianCalendar getRandom() {
            GregorianCalendar rand = (GregorianCalendar) start.clone();
            rand.setTimeInMillis(rand.getTimeInMillis() + RANDOM.nextInt(seconds) * 1000L);
            return rand;
        }

        /* Returns a new RandomDateMaker object representing a range
           of 30 days beyond the end of "this" date range.

           WARNING: field and val are not actually used!! I don't know
           what their intended use was.  */
        public RandomDateMaker next(int field, int val) {
            GregorianCalendar c = (GregorianCalendar) end.clone();
            c.add(Calendar.DAY_OF_YEAR, 30);
            return new RandomDateMaker(end, c);
        }

        @Override
        public String toString() {
            DateFormat f = SimpleDateFormat.getDateInstance();
            return "[" + f.format(start.getTime()) + " - " + f.format(end.getTime()) + "]";
        }

    }

    private static class RandomCalendar implements RandomCallback {
        public Value makeValue() {
            return CalendarToValue(ThreadVars.dateMaker.get().getRandom());
        }
    }

    private static class RandomInteger implements RandomCallback {
        public Value makeValue() {
            return ThreadVars.valueFactory.get().createLiteral(RANDOM.nextInt());
        }
    }

    private static class RandomName implements RandomCallback {
        static String[] firsts = new String[] {"Sam", "Bruce", "Mandy", "Stacy", "Marcus", "Susan",
                "Jason", "Chris", "Becky", "Britney", "David", "Paul", "Daniel", "James",
                "Bradley", "Amy", "Tina", "Brandy", "Jessica", "Mary", "George", "Jane"};
        static String[] lasts = new String[] {"Smith", "Jones", "Flintstones", "Rubble", "Jetson",
                "Wayne", "McFly", "Stadtham", "Lee", "Chan", "Brown", "Quinn",
                "Henderson", "Anderson", "Roland"};

        public Value makeValue() {
            int first = RANDOM.nextInt(firsts.length);
            int last = RANDOM.nextInt(lasts.length);
            return ThreadVars.valueFactory.get().createLiteral(firsts[first] + " " + lasts[last]);
        }
    }

    private static class RandomDirection implements RandomCallback {
        static String[] directions = new String[] {"Inbound", "Outbound"};

        public Value makeValue() {
            int direction = RANDOM.nextInt(directions.length);
            return ThreadVars.valueFactory.get().createLiteral(directions[direction]);
        }
    }

    private static class RandomBoolean implements RandomCallback {
        public Value makeValue() {
            boolean isOdd = RANDOM.nextInt(2) == 1;
            return ThreadVars.valueFactory.get().createLiteral(isOdd);
        }
    }

    private static class RandomOrigin implements RandomCallback {
        static String[] origins = new String[] {"Call Center", "Sales", "Front Desk"};

        public Value makeValue() {
            int origin = RANDOM.nextInt(origins.length);
            return ThreadVars.valueFactory.get().createLiteral(origins[origin]);
        }
    }

    private static class RandomPayOption implements RandomCallback {
        static String[] payoptions = new String[] {"Cash", "Credit", "Money Order"};

        public Value makeValue() {
            int payoption = RANDOM.nextInt(payoptions.length);
            return ThreadVars.valueFactory.get().createLiteral(payoptions[payoption]);
        }
    }

    private static class RandomDelivery implements RandomCallback {
        static String[] deliverytypes = new String[] {"Mail", "FedEx", "UPS", "Truck Freight"};

        public Value makeValue() {
            int deliverytype = RANDOM.nextInt(deliverytypes.length);
            return ThreadVars.valueFactory.get().createLiteral(deliverytypes[deliverytype]);
        }
    }

    private static class RandomMoney implements RandomCallback {
        public Value makeValue() {
            return ThreadVars.valueFactory.get().createLiteral(RANDOM.nextFloat() * 10000.0);
        }
    }

    private static class RandomAction implements RandomCallback {
        static String[] actions = new String[] {"Add", "Modify"};

        public Value makeValue() {
            int action = RANDOM.nextInt(actions.length);
            return ThreadVars.valueFactory.get().createLiteral(actions[action]);
        }
    }

    private static class RandomHandling implements RandomCallback {
        static String[] handlings = new String[] {"Nothing", "Past Due Notice", "Collections"};

        public Value makeValue() {
            int handling = RANDOM.nextInt(handlings.length);
            return ThreadVars.valueFactory.get().createLiteral(handlings[handling]);
        }
    }

    private static class RandomCustomer implements RandomCallback {
        public Value makeValue() {
            return ThreadVars.valueFactory.get().createIRI(Defaults.NS, "Customer-" + RANDOM.nextLong(Defaults.SIZE / 1000));
        }
    }

    private static class RandomAccount implements RandomCallback {
        public Value makeValue() {
            return ThreadVars.valueFactory.get().createIRI(Defaults.NS, "Account-" + RANDOM.nextLong(Defaults.SIZE / 1000));
        }
    }

    private static class RandomProductID implements RandomCallback {
        public Value makeValue() {
            return ThreadVars.valueFactory.get().createIRI(Defaults.NS, "ProductID-" + RANDOM.nextLong(Defaults.SIZE / 1000));
        }
    }

    private static class RandomPaymentTarget implements RandomCallback {
        public Value makeValue() {
            return ThreadVars.valueFactory.get().createIRI(Defaults.NS, "PaymentTarget-" + RANDOM.nextInt(100));
        }
    }

    private static class RandomBusinessEntity implements RandomCallback {
        public Value makeValue() {
            return ThreadVars.valueFactory.get().createIRI(Defaults.NS, "DepositDesignation-" + RANDOM.nextInt(100));
        }
    }

    private static class RandomDepositDesignation implements RandomCallback {
        public Value makeValue() {
            return ThreadVars.valueFactory.get().createIRI(Defaults.NS, "BusinessEntity-" + RANDOM.nextInt(100));
        }
    }

    private static class TypeURI implements RandomCallback {
        private String typeLabel;

        public TypeURI(String label) {
            typeLabel = label;
        }

        public Value makeValue() {
            return ThreadVars.valueFactory.get().createIRI(Defaults.NS, typeLabel);
        }
    }

    private static class StringValue implements RandomCallback {
        private String value;

        public StringValue(String theValue) {
            value = theValue;
        }

        public Value makeValue() {
            return ThreadVars.valueFactory.get().createLiteral(value);
        }
    }

    private static class PredicateInfo {
        private IRI predicate;
        private RandomCallback objectMaker;

        public PredicateInfo(IRI thePredicate, RandomCallback theMaker) {
            predicate = thePredicate;
            objectMaker = theMaker;
        }

        public PredicateInfo(String label, RandomCallback theMaker) {
            predicate = ThreadVars.valueFactory.get().createIRI(Defaults.NS, label);
            objectMaker = theMaker;
        }

        public Statement makeStatement(Resource subject, Resource context) {
            return ThreadVars.valueFactory.get().createStatement(subject, predicate, objectMaker.makeValue(), context);
        }
    }

    private static class AllEvents {
        private static PredicateInfo[] interaction = new PredicateInfo[Defaults.EVENT_SIZE];
        private static PredicateInfo[] invoice = new PredicateInfo[Defaults.EVENT_SIZE];
        private static PredicateInfo[] payment = new PredicateInfo[Defaults.EVENT_SIZE];
        private static PredicateInfo[] purchase = new PredicateInfo[Defaults.EVENT_SIZE];

        private static void PadEvent(String padName, PredicateInfo[] to, PredicateInfo[] from) {
            System.arraycopy(from, 0, to, 0, from.length);

            for (int i = from.length; i < Defaults.EVENT_SIZE; i++) {
                to[i] = new PredicateInfo(padName + i, new RandomInteger());
            }
        }

        public static void initialize() {
            PadEvent("Interaction", interaction, new PredicateInfo[] {
                    new PredicateInfo(RDF.TYPE, new TypeURI("CustomerInteraction")),
                    new PredicateInfo("EventTimeStamp", new RandomCalendar()),
                    new PredicateInfo("EntityId", new RandomInteger()),
                    new PredicateInfo("OriginatingSystem", new StringValue("CRM")),
                    new PredicateInfo("Agent", new RandomName()),
                    new PredicateInfo("Direction", new RandomDirection()),
                    new PredicateInfo("DoneInOne", new RandomBoolean()),
                    new PredicateInfo("EndDate", new RandomCalendar()),
                    new PredicateInfo("FeeBased", new RandomBoolean()),
                    new PredicateInfo("InteractionId", new RandomInteger()),
                    new PredicateInfo("Origin", new RandomOrigin()),
                    new PredicateInfo("PayOption", new RandomPayOption()),
                    new PredicateInfo("ReasonLevel1", new RandomInteger()),
                    new PredicateInfo("ReasonLevel2", new RandomInteger()),
                    new PredicateInfo("OriginatingSystem", new RandomInteger()),
                    new PredicateInfo("Result", new RandomInteger())
            });

            PadEvent("Invoice", invoice, new PredicateInfo[] {
                    new PredicateInfo(RDF.TYPE, new TypeURI("Invoice")),
                    new PredicateInfo("EventTimeStamp", new RandomCalendar()),
                    new PredicateInfo("AccountId", new RandomAccount()),
                    new PredicateInfo("OriginatingSystem", new StringValue("Invoicing")),
                    new PredicateInfo("DueDate", new RandomCalendar()),
                    new PredicateInfo("Action", new RandomAction()),
                    new PredicateInfo("TotalAmountDue", new RandomMoney()),
                    new PredicateInfo("AmountDueHandling", new RandomHandling()),
                    new PredicateInfo("LegalInvoiceNumber", new RandomInteger()),
                    new PredicateInfo("PreviousBalanceAmount", new RandomMoney()),
                    new PredicateInfo("TotalFinanceActivites", new RandomMoney()),
                    new PredicateInfo("BillDate", new RandomCalendar()),
                    new PredicateInfo("TotalUsageCharges", new RandomMoney()),
                    new PredicateInfo("TotalRecurringCharges", new RandomMoney()),
                    new PredicateInfo("TotalOneTimeCharges", new RandomMoney())
            });

            PadEvent("Payment", payment, new PredicateInfo[] {
                    new PredicateInfo(RDF.TYPE, new TypeURI("AccountPayment")),
                    new PredicateInfo("EventTimeStamp", new RandomCalendar()),
                    new PredicateInfo("AccountId", new RandomAccount()),
                    new PredicateInfo("OriginatingSystem", new StringValue("Ordering")),
                    new PredicateInfo("SubscriberId", new RandomCustomer()),
                    new PredicateInfo("InvoiceId", new RandomInteger()),
                    new PredicateInfo("PaymentAmount", new RandomMoney()),
                    new PredicateInfo("OriginalAmount", new RandomMoney()),
                    new PredicateInfo("AmountDue", new RandomMoney()),
                    new PredicateInfo("DepositDate", new RandomCalendar()),
                    new PredicateInfo("PaymentType", new RandomPayOption()),
                    new PredicateInfo("OriginalPostedAmount", new RandomMoney()),
                    new PredicateInfo("PaymentTarget", new RandomPaymentTarget()),
                    new PredicateInfo("DepositDesignation", new RandomDepositDesignation()),
                    new PredicateInfo("BusinessEntity", new RandomBusinessEntity())
            });

            PadEvent("Purchase", purchase, new PredicateInfo[] {
                    new PredicateInfo(RDF.TYPE, new TypeURI("Purchase")),
                    new PredicateInfo("EventTimeStamp", new RandomCalendar()),
                    new PredicateInfo("AccountId", new RandomAccount()),
                    new PredicateInfo("OriginatingSystem", new StringValue("Sales")),
                    new PredicateInfo("PurchaseDate", new RandomCalendar()),
                    new PredicateInfo("PurchaseAmount", new RandomMoney()),
                    new PredicateInfo("InvoiceID", new RandomInteger()),
                    new PredicateInfo("ProductID", new RandomProductID()),
                    new PredicateInfo("LegalInvoiceNumber", new RandomInteger()),
                    new PredicateInfo("PreviousBalanceAmount", new RandomMoney()),
                    new PredicateInfo("DeliveredVia", new RandomDelivery()),
                    new PredicateInfo("PaidVia", new RandomPayOption()),
                    new PredicateInfo("CCApprovalNumber", new RandomInteger()),
                    new PredicateInfo("TotalRecurringCharges", new RandomMoney()),
                    new PredicateInfo("TotalOneTimeCharges", new RandomMoney())
            });
        }

        ;

        public static int makeEvent(Vector<Statement> statements, int index) {
            PredicateInfo[] event = null;

            switch (RANDOM.nextInt(4)) {
                case 0:
                    event = interaction;
                    break;
                case 1:
                    event = invoice;
                    break;
                case 2:
                    event = payment;
                    break;
                case 3:
                    event = purchase;
                    break;
            }

            BNode bnode = ThreadVars.valueFactory.get().createBNode();
            Resource context = (Resource) new RandomCustomer().makeValue();

            for (int i = 0; i < Defaults.EVENT_SIZE; index++, i++) {
                statements.set(index, event[i].makeStatement(bnode, context));
            }

            return index;
        }
    }

    private static class QueryResult {
        public long queries;
        public long triples;

        public QueryResult(long theQueries, long theTriples) {
            queries = theQueries;
            triples = theTriples;
        }
    }

    public static class Monitor {

        private static void printOutput(Process p) throws IOException {
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                System.out.println(line);
                System.out.flush();
            }
            input.close();
        }

        static public void start(int phase) {
            if (Defaults.MONITOR) {
                try {
                    final File monitor = Util.resourceAsTempFile(
                            "/test/stress/monitor.sh", true);
                    String[] commands =
                            new String[] {monitor.getAbsolutePath(), "start", "phase-" + phase,
                                    Defaults.CATALOG, Defaults.REPOSITORY};
                    Process p = Runtime.getRuntime().exec(commands);
                    printOutput(p);
                    trace("./monitor.sh was started.");
                } catch (IOException e) {
                    trace("./monitor.sh was not started.");
                }
            }
        }

        static public void stop(int phase, AGRepositoryConnection c) {
            if (Defaults.MONITOR) {
                try {
                    String[] commands = new String[] {"src/test/stress/monitor.sh", "end",
                            "phase-" + phase, Defaults.CATALOG,
                            Defaults.REPOSITORY};
                    Process p = Runtime.getRuntime().exec(commands);
                    printOutput(p);
                    trace("./monitor.sh was stopped.");
                    // use http api for starting checkpoint and merge.
                    if (phase < 4) {
                        AGRepository repo = c.getRepository().getValueFactory().getRepository();

                        trace("Phase 0 Begin: Forced Merge");
                        c.optimizeIndices(true, 1);
                        repo.ensureDBIdle();
                        trace("Phase 0 End: Forced Merge");
                        trace("Phase 0 Begin: Forced Checkpoint");
                        repo.forceCheckpoint();
                        trace("Phase 0 End: Forced Checkpoint");
                    }
                } catch (IOException e) {
                    trace("./monitor.sh was not stopped.");
                } catch (RepositoryException e) {
                    trace("error in Monitor.stop()");
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }

    }

    // This class holds a single named measurement
    private class Measure {
        String name;
        Object value;

        Measure(String name, double value) {
            this.name = name;
            this.value = value;
        }

        Measure(String name, long value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return "(" + this.name + " . " + this.value + ")";
        }

    }

    // This class holds a list of measures for a phase
    private class PhaseMeasures {
        String phase;
        List<Measure> measures = new ArrayList<Measure>();

        PhaseMeasures(String phase) {
            this.phase = phase;
        }

        PhaseMeasures add(Measure m) {
            measures.add(m);
            // Return 'this' so that the builder pattern can be used
            return this;
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder();

            res.append("(\"" + phase + "\" . (");
            for (Measure m : measures) {
                res.append(m.toString());
            }
            res.append("))");

            return res.toString();
        }
    }

    public class ConnectionHolder {
        private AGRepositoryConnection conn;

        public ConnectionHolder() throws RepositoryException {
            if (repoPool == null) {
                conn = connect();
            }
        }

        public void begin() {
            try {
                if (repoPool != null) {
                    conn = repoPool.getConnection();
                }
                ThreadVars.valueFactory.set(conn.getValueFactory());
            } catch (RepositoryException e) {
                errors++;
                e.printStackTrace();
            }
        }

        public AGRepositoryConnection use() {
            return conn;
        }

        public void end() {
            try {
                ThreadVars.valueFactory.set(null);
                if (repoPool != null) {
                    conn.close();
                    conn = null;
                }
            } catch (RepositoryException e) {
                errors++;
                e.printStackTrace();
            }
        }

        void close() {
            if (repoPool == null) {
                ThreadVars.valueFactory.set(null);
                closer.close(conn);
            }
        }
    }

    class Loader implements Task {
        private final RandomDateMaker dateMaker;
        private final ConnectionHolder connHolder;
        private int id;
        private long loopCount;
        private int eventsPerCommit;
        private int triplesPerCommit;

        public Loader(int theId, long theTripleGoal, int theEventsPerCommit, RandomDateMaker dateMaker) throws Exception {
            id = theId;
            this.dateMaker = dateMaker;
            triplesPerCommit = theEventsPerCommit * Defaults.EVENT_SIZE;
            loopCount = theTripleGoal / triplesPerCommit / Defaults.LOAD_WORKERS;
            eventsPerCommit = theEventsPerCommit;
            connHolder = new ConnectionHolder();
        }

        public Integer call() {
            Thread.currentThread().setName("loader(" + id + ")");
            ThreadVars.dateMaker.set(dateMaker);

            final int statusSize = Defaults.STATUS;
            int count = 0, errors = 0;
            Vector<Statement> statements = new Vector<Statement>(triplesPerCommit);
            Calendar start = GregorianCalendar.getInstance(), end;

            statements.setSize(triplesPerCommit);

            for (int loop = 0; loop < loopCount; loop++) {
                connHolder.begin();
                // Fill in the statements to commit
                for (int event = 0, index = 0; event < eventsPerCommit; event++) {
                    index = AllEvents.makeEvent(statements, index);
                }

                if (loop > 0 && (loop % statusSize) == 0) {
                    end = GregorianCalendar.getInstance();
                    double seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
                    trace("Loading Status - %d triples loaded so " +
                                    "far at %d triples per commit (%.2f commits/sec, %.2f triples/sec" +
                                    " over last %d commits), %d errors.", count,
                            triplesPerCommit, logtime(statusSize / seconds),
                            logtime(statusSize * triplesPerCommit / seconds),
                            statusSize, errors);
                    start = end;
                }

                try {
                    connHolder.use().add(statements);
                    count += triplesPerCommit;
                } catch (Exception e) {
                    errors++;
                    trace("Error adding statements...");
                    e.printStackTrace();
                }
                connHolder.end();
            }

            trace("Loading Done - %d triples at %d triples " +
                    "per commit, %d errors.", count, triplesPerCommit, errors);

            return 0;
        }

        @Override
        public void close() {
            connHolder.close();
        }
    }

    class Querier implements Task {
        private final RandomDateMaker dateMaker;
        private final QueryLanguage language;
        private int secondsToRun;
        private int id;
        private String timestamp;
        private ConnectionHolder connHolder;

        public Querier(int theId, int theSecondsToRun, RandomDateMaker dateMaker, QueryLanguage lang) throws Exception {
            id = theId;
            secondsToRun = theSecondsToRun;
            this.dateMaker = dateMaker;
            language = lang;
            connHolder = new ConnectionHolder();
        }

        private long sparqlQuery(AGRepositoryConnection conn, boolean trace) {
            // Pick a random customer
            String customerNT = NTriplesUtil.toNTriplesString(new RandomCustomer().makeValue());

            // Pick a random date range
            GregorianCalendar start, end;
            start = FullDateRange.getRandom();
            end = FullDateRange.getRandom();

            if (start.after(end)) {
                GregorianCalendar swap = end;
                end = start;
                start = swap;
            }

            String startNT, endNT;
            startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
            endNT = NTriplesUtil.toNTriplesString(CalendarToValue(end));

            String queryString;
            AGTupleQuery tupleQuery;
            queryString = String.format(
                    "select ?s ?p ?o " +
                            "from %s " +
                            "where { " +
                            "  ?s %s ?date . " +
                            "  filter ( ( ?date >= %s ) && ( ?date <= %s ) ) " +
                            "  ?s ?p ?o " +
                            "}",
                    customerNT, timestamp, startNT, endNT);
            tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.SPARQL, queryString);
            tupleQuery = streamQuery(tupleQuery);

            // Actually pull the full results to the client, then just count them
            TupleQueryResult result = null;
            long count = 0;
            try {
                if (Defaults.VERBOSE > 0 && trace) {
                    trace("query: %s", queryString);
                }
                if (Defaults.stream == Defaults.STREAM.HAND || Defaults.stream == Defaults.STREAM.PULH) {
                    CountingHandler handler = new CountingHandler();
                    tupleQuery.evaluate(handler);
                    count = handler.count;
                } else {
                    result = tupleQuery.evaluate();
                    count = count(result);
                }
                // test sparql and prolog return same results:
                // Set<Stmt> stmts = Stmt.statementSet(result);
                // count = stmts.size();
                // AGAbstractTest.assertSetsEqual(queryString, stmts,
                // Stmt.statementSet(tupleQuery2.evaluate()));
            } catch (Exception e) {
                errors++;
                trace("Error executing query:\n%s\n", queryString);
                e.printStackTrace();
                count = -1;
            } finally {
                closer.close(result);
            }

            return count;
        }

        private long prologQuery(AGRepositoryConnection conn, boolean trace) {
            // Pick a random customer
            String customerNT = NTriplesUtil.toNTriplesString(new RandomCustomer().makeValue());

            // Pick a random date range
            GregorianCalendar start, end;
            start = FullDateRange.getRandom();
            end = FullDateRange.getRandom();

            if (start.after(end)) {
                GregorianCalendar swap = end;
                end = start;
                start = swap;
            }

            String startNT, endNT;
            startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
            endNT = NTriplesUtil.toNTriplesString(CalendarToValue(end));

            String queryString;
            AGTupleQuery tupleQuery;

            queryString = String.format(
                    "(select (?s ?p ?o)" +
                            "(:use-planner nil)" +
                            "(q- ?s !%s (? !%s !%s) !%s)" +
                            "(q- ?s ?p ?o !%s))",
                    timestamp, startNT, endNT, customerNT, customerNT);
            tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
            tupleQuery = streamQuery(tupleQuery);

            // Actually pull the full results to the client, then just count them
            TupleQueryResult result = null;
            long count = 0;
            try {
                if (Defaults.VERBOSE > 0 && trace) {
                    trace("query: %s", queryString);
                }
                if (Defaults.stream == Defaults.STREAM.HAND || Defaults.stream == Defaults.STREAM.PULH) {
                    CountingHandler handler = new CountingHandler();
                    tupleQuery.evaluate(handler);
                    count = handler.count;
                } else {
                    result = tupleQuery.evaluate();
                    count = count(result);
                }
                // test sparql and prolog return same results:
                // Set<Stmt> stmts = Stmt.statementSet(result);
                // count = stmts.size();
                // AGAbstractTest.assertSetsEqual(queryString, stmts,
                // Stmt.statementSet(tupleQuery2.evaluate()));
            } catch (Exception e) {
                errors++;
                trace("Error executing query:\n%s\n", queryString);
                e.printStackTrace();
                count = -1;
            } finally {
                closer.close(result);
            }

            return count;
        }

        private long randomQuery(AGRepositoryConnection conn, boolean trace) {

            if (this.language == AGQueryLanguage.PROLOG) {
                return prologQuery(conn, trace);
            } else {
                return sparqlQuery(conn, trace);
            }
        }

        private int count(TupleQueryResult result) throws Exception {
            int count = 0;
            while (result.hasNext()) {
                result.next();
                count++;
            }
            return count;
        }

        public QueryResult call() {
            Thread.currentThread().setName("query(" + id + ")");
            ThreadVars.dateMaker.set(dateMaker);

            connHolder.begin();
            timestamp = NTriplesUtil.toNTriplesString(ThreadVars.valueFactory.get().createIRI(Defaults.NS, "EventTimeStamp"));
            connHolder.end();

            final int statusSize = Math.max(1, Defaults.STATUS / 5);
            long count = 0, subcount = 0, queries = 0, restarts = 0;
            Calendar startTime, start, end;
            startTime = start = GregorianCalendar.getInstance();

            while (true) {
                // Do the query
                connHolder.begin();
                long result = randomQuery(connHolder.use(), (queries % statusSize == 0));
                connHolder.end();
                if (result < 0) {
                    restarts++;
                } else {
                    queries++;
                    count += result;
                }

                if (queries % statusSize == 0) {
                    //if (queries % statusSize == 0 || restarts % statusSize == 0) {
                    end = GregorianCalendar.getInstance();
                    double seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
                    subcount = count - subcount;
                    trace("Querying status - %d triples returned for %d queries in %.2f sec (%.2f queries/sec, " +
                                    "%d triples per query), %d queries aborted.", subcount, statusSize,
                            logtime(seconds), logtime(statusSize / seconds), subcount / statusSize, restarts);
                    start = end;
                    subcount = count;

                    seconds = (end.getTimeInMillis() - startTime.getTimeInMillis()) / 1000.0;
                    // stop after secondsToRun only if Defaults.QUERY_SIZE == 0
                    // or if we've performed Defaults.QUERY_SIZE / Defaults.QUERY_WORKERS
                    // queries.
                    if ((Defaults.QUERY_SIZE == 0 && seconds > secondsToRun) ||
                            (Defaults.QUERY_SIZE > 0 &&
                                    queries >= Defaults.QUERY_SIZE / Defaults.QUERY_WORKERS)) {
                        break;
                    }
                }
            }

            double seconds = (GregorianCalendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis()) / 1000.0;
            trace("Querying done - %d triple results returned for %d queries in %f seconds " +
                            "(%f queries/second, %d triples per query), %d queries aborted.",
                    count, queries, logtime(seconds), logtime(queries / seconds), count / queries, restarts);

            return new QueryResult(queries, count);
        }

        @Override
        public void close() {
            connHolder.close();
        }

        class CountingHandler implements TupleQueryResultHandler {
            long count = 0;

            public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
            }

            public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
                count++;
            }

            public void endQueryResult() throws TupleQueryResultHandlerException {
            }

            public void handleBoolean(boolean arg0)
                    throws QueryResultHandlerException {
            }

            public void handleLinks(List<String> arg0)
                    throws QueryResultHandlerException {
            }
        }
    }

    class Deleter implements Task {

        private final RandomDateMaker range;
        private ConnectionHolder connHolder;

        public Deleter(RandomDateMaker range) throws Exception {
            this.range = range;
            connHolder = new ConnectionHolder();
        }

        public Integer call() {
            Thread.currentThread().setName("deleter(" + range + ")");
            connHolder.begin();
            String timestamp = NTriplesUtil.toNTriplesString(ThreadVars.valueFactory.get().createIRI(Defaults.NS, "EventTimeStamp"));

            // interval is 1 day's worth of milliseconds.
            long interval = 24 * 60 * 60 * 1000;

            GregorianCalendar start, eod;
            start = range.start;
        /* eod: eod of day */
            eod = (GregorianCalendar) range.start.clone();
            eod.setTimeInMillis(eod.getTimeInMillis() + interval);

            String startNT, endNT, queryString;
            startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
            endNT = NTriplesUtil.toNTriplesString(CalendarToValue(eod));
            connHolder.end();

            DateFormat f = SimpleDateFormat.getDateInstance();

            if (Defaults.hasOption("delete-using-prolog")) {
                trace("Deleting using Prolog");
            } else {
                trace("Deleting using SPARQL");
            }

        /* Issue delete requests covering one day at a time */
            while (start.before(range.end)) {
                if (Defaults.VERBOSE > 0) {
                    trace("deleting " + f.format(start.getTime()) + " to " + f.format(eod.getTime()));
                }

                if (!Defaults.hasOption("delete-using-prolog")) {
                    queryString = String.format(
                            "delete { ?s ?p ?o } " +
                                    "where { " +
                                    "    ?s %s ?date . " +
                                    "    filter ( ( ?date >= %s ) && ( ?date <= %s ) ) . " +
                                    "    ?s ?p ?o . " +
                                    "}",
                            timestamp, startNT, endNT);
                    if (Defaults.VERBOSE > 0) {
                        trace(queryString);
                    }
                    connHolder.begin();
                    AGBooleanQuery query = connHolder.use().prepareBooleanQuery(AGQueryLanguage.SPARQL, queryString);
                    try {
            /* The result always appears to be true.  We don't get back useful information
               such as the number of triples that have been deleted */
                        boolean result = query.evaluate();

                        if (!result) {
                            trace("Got non-true response");
                        }

                    } catch (Exception e) {
                        errors++;
                        trace("Error executing query:\n%s\n", queryString);
                        e.printStackTrace();
                    }
                    connHolder.end();
                } else {
            /* FIXME: What is this doing? */
                    int limit = (9 * (int) Math.pow(10, 5)) / Defaults.EVENT_SIZE;

                    queryString = String.format("(select0 (?event)" +
                            "(:limit %d) (:count-only t)" +
                            "(q- ?event !%s (? !%s !%s))" +
                            "(lisp (delete-triples :s ?event)))", limit, timestamp, startNT, endNT);
                    if (Defaults.VERBOSE > 0) {
                        trace(queryString);
                    }
                    connHolder.begin();
                    AGTupleQuery tupleQuery = connHolder.use().prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
                    try {
                        tupleQuery.count();
                    } catch (Exception e) {
                        errors++;
                        trace("Error executing query:\n%s\n", queryString);
                        e.printStackTrace();
                    }
                    connHolder.end();
                }

                // Advance to the next day
                start = eod;
                connHolder.begin();
                startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
                eod = (GregorianCalendar) start.clone();
                eod.setTimeInMillis(eod.getTimeInMillis() + interval);
                endNT = NTriplesUtil.toNTriplesString(CalendarToValue(eod));
                connHolder.end();
            }

            return -1;
        }

        @Override
        public void close() {
            connHolder.close();
        }
    }

    class DeletingHandler implements TupleQueryResultHandler {
        private final AGRepositoryConnection conn;
        long count = 0;

        DeletingHandler(AGRepositoryConnection conn) {
            this.conn = conn;
        }

        public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
        }

        public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
            try {
                conn.remove(ThreadVars.valueFactory.get().createStatement(
                        (BNode) bs.getValue("s"),
                        (IRI) bs.getValue("p"),
                        bs.getValue("o")));
            } catch (RepositoryException e) {
                throw new TupleQueryResultHandlerException("failed to remove " + bs, e);
            }
            count++;
        }

        public void endQueryResult() throws TupleQueryResultHandlerException {
        }

        @Override
        public void handleBoolean(boolean arg0)
                throws QueryResultHandlerException {
        }

        @Override
        public void handleLinks(List<String> arg0)
                throws QueryResultHandlerException {
        }
    }

    class Pinger implements Runnable {

        private AGRepositoryConnection conn;

        public Pinger(AGRepositoryConnection c) {
            conn = c;
        }

        public void run() {
            while (true) {
                try {
                    trace("pinger running for " + conn.prepareHttpRepoClient().getRoot());
                    conn.ping();
                    Thread.sleep(Math.round(conn.getSessionLifetime() * 1000 / 2));
                } catch (InterruptedException e) {
                    trace("Exception encountered in pinger: %s", e.toString());
                } catch (RepositoryException e) {
                    trace("Exception encountered in pinger: %s", e.toString());
                }
            }
        }
    }

}
