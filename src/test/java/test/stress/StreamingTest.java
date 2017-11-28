/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test.stress;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGStreamTupleQuery;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.agraph.repository.AGValueFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.junit.Before;
import org.junit.Test;
import test.AGAbstractTest;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @since v4.3
 */
public class StreamingTest extends AGAbstractTest {

    final static MemoryMXBean mem = ManagementFactory.getMemoryMXBean();

    private static final int SIZE = 300000;
    protected AGCatalog cat;
    protected AGRepository repo;
    protected AGRepositoryConnection conn;
    protected AGValueFactory vf;
    protected long repoSize = 0;
    private String limit = null;
    private long minSeconds = 20;

    @Before
    public void openLargeRepo() throws Exception {
        // do not use the super.repo because the contents are deleted in BeforeClass
        AGCatalog cat = server.getCatalog(Events.Defaults.CATALOG);
        if (cat.hasRepository(Events.Defaults.REPOSITORY)) {
            repo = closeLater(cat.openRepository(Events.Defaults.REPOSITORY));
            repo.initialize();
            conn = closeLater(repo.getConnection());
            repoSize = conn.size();
            if (repoSize < SIZE) {
                log.info("size of " + repo.getCatalogPrefixedRepositoryID() + " = " + repoSize);
                closer.close(conn);
                closer.close(repo);
                cat = null;
            } else {
                vf = repo.getValueFactory();
                conn.setAutoCommit(false);
            }
        }
        if (repo == null) {
            cat = server.getCatalog(CATALOG_ID);
            repo = closeLater(cat.createRepository("big-java-test"));
            repo.initialize();
            vf = repo.getValueFactory();
            conn = closeLater(repo.getConnection());
            conn.setAutoCommit(false);
            IRI subj = vf.createIRI("http://example.org/subj");
            IRI pred = vf.createIRI("http://example.org/pred");
            long size = conn.size();
            while (size < SIZE) {
                Literal obj = vf.createLiteral(size);
                conn.add(subj, pred, obj);
                size++;
                if (size % Math.round(SIZE / 100) == 0) {
                    conn.commit();
                    log.info("commit, size: " + conn.size());
                }
            }
            conn.commit();
        }
        repoSize = conn.size();
        log.info("size of " + repo.getCatalogPrefixedRepositoryID() + " = " + repoSize);
        System.gc();
        Thread.yield();
    }

    public Stats measurePerf1(STREAM stream) throws Exception {
        AGTupleQuery qu = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?s ?p ?o WHERE {?s ?p ?o . }" + limit);
        if (stream == STREAM.PULL || stream == STREAM.PULH) {
            qu = new AGStreamTupleQuery(qu);
        }

        final Stats stats = new Stats();
        stats.memUsed = memUsed(mem); // array so the inner class can modify
        stats.nano = System.nanoTime();

        try {
            if (stream == STREAM.HAND || stream == STREAM.PULH) {
                qu.evaluate(new TupleQueryResultHandler() {
                    public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
                        log.debug("bindings: " + bindingNames);
                    }

                    public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
                        stats.count++;
                        if (stats.count % 100 == 0) {
                            stats.memUsed = Math.max(stats.memUsed, memUsed(mem));
                        }
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
                });
            } else {
                try (TupleQueryResult results = qu.evaluate()) {
                    log.debug("bindings: " + results.getBindingNames());
                    while (results.hasNext()) {
                        results.next();
                        stats.count++;
                        if (stats.count % 100 == 0) {
                            stats.memUsed = Math.max(stats.memUsed, memUsed(mem));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("count=" + stats.count, e);
        }
        stats.nano = (System.nanoTime() - stats.nano);
        stats.memUsed = Math.max(stats.memUsed, memUsed(mem));
        return stats;
    }

    public Stats measurePerf(STREAM stream) throws Exception {
        System.gc();
        long maxNano = minSeconds * 1000 * 1000 * 1000;
        Stats stats = new Stats();
        try {
            do {
                Stats tmp = measurePerf1(stream);
                stats.add(tmp);
            } while (stats.nano < maxNano);
        } catch (Error e) {
            throw new Error(stream + "\t" + stats, e);
        } catch (Throwable e) {
            throw new RuntimeException(stream + "\t" + stats, e);
        }
        log.info(stream + "\t" + stats);
        return stats;
    }

    private long memUsed(MemoryMXBean mem) {
        MemoryUsage heap = mem.getHeapMemoryUsage();
        return heap.getUsed();
    }

    @Test
    public void n10() throws Exception {
        minSeconds = 30;
        limit = " limit 10";
        measurePerf(STREAM.NONE);
        measurePerf(STREAM.HAND);
        measurePerf(STREAM.PULL);
        measurePerf(STREAM.PULH);
    }

    @Test
    public void n500() throws Exception {
        minSeconds = 60;
        limit = " limit 500";
        measurePerf(STREAM.NONE);
        measurePerf(STREAM.HAND);
        measurePerf(STREAM.PULL);
        measurePerf(STREAM.PULH);
    }

    @Test
    public void n100000() throws Exception {
        minSeconds = 300;
        limit = " limit 100000";
        measurePerf(STREAM.NONE);
        measurePerf(STREAM.HAND);
        measurePerf(STREAM.PULL);
        measurePerf(STREAM.PULH);
    }

    @Test
    public void none() throws Exception {
        minSeconds = 300;
        limit = " limit 1000000";
        measurePerf(STREAM.NONE);
    }

    @Test
    public void pull() throws Exception {
        minSeconds = 300;
        limit = " limit 1000000";
        measurePerf(STREAM.PULL);
    }

    @Test
    public void hand() throws Exception {
        minSeconds = 300;
        limit = " limit 1000000";
        measurePerf(STREAM.HAND);
    }

    @Test
    public void pulh() throws Exception {
        minSeconds = 300;
        limit = " limit 1000000";
        measurePerf(STREAM.PULH);
    }

    @Test
    public void concurrentNone() throws Exception {
        concurrent(STREAM.NONE, STREAM.HAND);
    }

    @Test
    public void concurrentPull() throws Exception {
        concurrent(STREAM.PULL, STREAM.PULH);
    }

    public void concurrent(STREAM... sts) throws Exception {
        minSeconds = 300;
        limit = " limit 15000";
        int proc = Runtime.getRuntime().availableProcessors();
        ExecutorService exec = Executors.newFixedThreadPool(proc);
        List<Callable<Stats>> tasks = new LinkedList<Callable<Stats>>();
        for (int i = 0; i < (proc / (sts.length)); i++) {
            for (int j = 0; j < sts.length; j++) {
                final STREAM st = sts[j];
                tasks.add(new Callable<Stats>() {
                    public Stats call() throws Exception {
                        return measurePerf(st);
                    }
                });
            }
        }
        Stats stats = new Stats();
        List<Future<Stats>> futures = exec.invokeAll(tasks);
        for (Future<Stats> future : futures) {
            stats.add(future.get());
        }
        log.info("concurrent " + Arrays.asList(sts) + "\t" + stats);
    }

    public enum STREAM {
        NONE, PULL, HAND, PULH;
    }

    static class Stats {
        long count = 0;
        long memUsed = 0;
        long nano = 0;
        long iterations = 0;

        public String toString() {
            return "Count=" + test.Util.toHumanInt(count, 10)
                    + "\tMaxMemUsed=" + test.Util.toHumanInt(memUsed, 2)
                    + "\tTime=" + test.Util.toHumanInt((nano / (1000 * 1000)), 60)
                    + "\tIterations=" + iterations;
        }

        public void add(Stats tmp) {
            count += tmp.count;
            memUsed = Math.max(memUsed, tmp.memUsed);
            nano += tmp.nano;
            iterations++;
        }
    }

}
