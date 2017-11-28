/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test.pool;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.util.Closer;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.AGAbstractTest;
import test.TestSuites;
import test.Util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

;

public class AGConnPoolSessionTest extends Closer {

    private static final String ns = "http://example.com/#";
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @After
    public void closeAfter() {
        close();
    }

    private void assertSuccess(List<Future<Boolean>> errors, long timeout, TimeUnit unit) throws Exception {
        boolean fail = false;
        for (Future<Boolean> f : errors) {
            Boolean e = f.get(timeout, unit);
            if (!e) {
                fail = true;
            }
        }
        if (fail) {
            throw new RuntimeException("See log for details.");
        }
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testPlain() throws Exception {
        AGServer server = closeLater(new AGServer(AGAbstractTest.findServerUrl(), AGAbstractTest.username(), AGAbstractTest.password()));
        AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
        AGRepository repo = closeLater(catalog.createRepository("pool.testPlain"));
        AGRepositoryConnection conn = closeLater(repo.getConnection());
        Assert.assertTrue(conn.toString(), conn.prepareHttpRepoClient().getRoot().contains(AGAbstractTest.findServerUrl()));
        Assert.assertEquals(0, conn.size());
        conn.deleteDatatypeMapping(XMLSchema.DOUBLE);
        conn.setSessionLifetime(60);
        conn.setAutoCommit(true);
        conn.deleteDatatypeMapping(XMLSchema.FLOAT);
        Assert.assertEquals(0, conn.size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testPoolDedicated() throws Exception {
        String oldOverride = System.setProperty("com.franz.agraph.http.overrideServerUseMainPortForSessions", "true");
        AGConnPool pool = closeLater(AGConnPool.create(
                AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
                AGConnProp.username, AGAbstractTest.username(),
                AGConnProp.password, AGAbstractTest.password(),
                AGConnProp.catalog, AGAbstractTest.CATALOG_ID,
                AGConnProp.repository, "pool.testPoolDedicated",
                AGConnProp.session, AGConnProp.Session.DEDICATED,
                AGConnProp.sessionLifetime, TimeUnit.MINUTES.toSeconds(1),
                AGPoolProp.shutdownHook, true,
                AGPoolProp.testOnBorrow, true,
                AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(30)
        ));
        AGServer server = closeLater(AGAbstractTest.newAGServer());
        AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
        AGRepository repo = closeLater(catalog.createRepository("pool.testPoolDedicated"));
        repo.setConnPool(pool);
        AGRepositoryConnection conn = closeLater(repo.getConnection());
        Assert.assertFalse(conn.toString(), conn.prepareHttpRepoClient().getRoot().contains(AGAbstractTest.findServerUrl()));
        Assert.assertEquals(0, conn.size());
        conn.deleteDatatypeMapping(XMLSchema.DOUBLE);
        conn.deleteDatatypeMapping(XMLSchema.FLOAT);
        Assert.assertEquals(0, conn.size());
        if (oldOverride != null) {
            System.setProperty("com.franz.agraph.http.overrideServerUseMainPortForSessions", oldOverride);
        } else {
            System.clearProperty("com.franz.agraph.http.overrideServerUseMainPortForSessions");
        }
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testPoolTx() throws Exception {
        String oldOverride = System.setProperty("com.franz.agraph.http.overrideServerUseMainPortForSessions", "true");
        AGConnPool pool = closeLater(AGConnPool.create(
                AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
                AGConnProp.username, AGAbstractTest.username(),
                AGConnProp.password, AGAbstractTest.password(),
                AGConnProp.catalog, AGAbstractTest.CATALOG_ID,
                AGConnProp.repository, "pool.testPoolTx",
                AGConnProp.session, AGConnProp.Session.TX,
                AGConnProp.sessionLifetime, TimeUnit.MINUTES.toSeconds(1),
                AGPoolProp.testOnBorrow, true,
                AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(30)
        ));
        AGServer server = closeLater(AGAbstractTest.newAGServer());
        AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
        AGRepository repo = closeLater(catalog.createRepository("pool.testPoolTx"));
        repo.setConnPool(pool);
        AGRepositoryConnection conn = closeLater(repo.getConnection());
        Assert.assertFalse(conn.toString(), conn.prepareHttpRepoClient().getRoot().contains(AGAbstractTest.findServerUrl()));
        Assert.assertEquals(0, conn.size());
        conn.deleteDatatypeMapping(XMLSchema.DOUBLE);
        conn.deleteDatatypeMapping(XMLSchema.FLOAT);
        Assert.assertEquals(0, conn.size());
        if (oldOverride != null) {
            System.setProperty("com.franz.agraph.http.overrideServerUseMainPortForSessions", oldOverride);
        } else {
            System.clearProperty("com.franz.agraph.http.overrideServerUseMainPortForSessions");
        }
    }

    /**
     * Tests there is no problem calling deleteDatatypeMapping
     * in many concurrent threads.
     */
    @Test
    @Category(TestSuites.Stress.class)
    public void deleteDatatypeMapping() throws Exception {
        final int NUM = 20;
        final int MINUTES = 1;
        AGServer server = closeLater(AGAbstractTest.newAGServer());
        AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
        final AGRepository repo = closeLater(catalog
                .createRepository("pool.deleteDatatypeMapping"));
        AGRepositoryConnection conn = closeLater(repo.getConnection());
        Assert.assertTrue(conn.toString(), conn.prepareHttpRepoClient().getRoot().contains(AGAbstractTest.findServerUrl()));

        AGValueFactory vf = conn.getValueFactory();
        for (int i = 0; i < NUM * 10; i++) {
            Util.add(conn, "/tutorial/java-kennedy.ntriples", ns,
                    RDFFormat.NTRIPLES, vf.createIRI(ns + i));
        }
        log.debug("size=" + conn.size());
        close(conn);
        ExecutorService exec = Executors.newFixedThreadPool(NUM);
        final List<Throwable> errors = new ArrayList<Throwable>();
        final AtomicLong count = new AtomicLong(0);
        for (int i = 0; i < NUM; i++) {
            exec.execute(new Runnable() {
                public void run() {
                    try {
                        long start = System.nanoTime();
                        while (true) {
                            long now = System.nanoTime();
                            if (now - start > TimeUnit.MINUTES.toNanos((int) (MINUTES * 0.9))) {
                                break;
                            }
                            AGRepositoryConnection conn = repo.getConnection();
                            try {
                                conn.deleteDatatypeMapping(XMLSchema.DOUBLE);
                                count.incrementAndGet();
                                conn.deleteDatatypeMapping(XMLSchema.FLOAT);
                                count.incrementAndGet();
                            } finally {
                                conn.close();
                            }
                        }
                    } catch (Throwable e) {
                        errors.add(e);
                    }
                }
            });
        }
        exec.awaitTermination(MINUTES, TimeUnit.MINUTES);
        log.debug("count=" + count);
        if (!errors.isEmpty()) {
            for (Throwable e : errors) {
                log.error("error", e);
            }
            Assert.fail("see log for details: " + errors.toString());
        }
    }

    @Test
    //@Category(TestSuites.Stress.class)
    @Category(TestSuites.Broken.class) // This test is unreliable and has been disabled. -- dancy
    public void maxActive() throws Exception {
        final int seconds = 5;
        final int clients = 4;
        final int wait = seconds * clients * 2;
        final AGConnPool pool = closeLater(AGConnPool.create(
                AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
                AGConnProp.username, AGAbstractTest.username(),
                AGConnProp.password, AGAbstractTest.password(),
                AGConnProp.catalog, AGAbstractTest.CATALOG_ID,
                AGConnProp.repository, "pool.maxActive",
                AGConnProp.session, AGConnProp.Session.DEDICATED,
                AGConnProp.sessionLifetime, wait,
                AGConnProp.httpSocketTimeout, TimeUnit.SECONDS.toMillis(wait),
                AGPoolProp.shutdownHook, true,
                AGPoolProp.testOnBorrow, true,
                AGPoolProp.maxActive, 2,
                AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(wait),
                AGPoolProp.maxIdle, 8
        ));
        AGServer server = closeLater(AGAbstractTest.newAGServer());
        AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
        final AGRepository repo = closeLater(catalog.createRepository("pool.maxActive"));
        repo.setConnPool(pool);
        ExecutorService exec = Executors.newFixedThreadPool(clients);
        List<Future<Boolean>> errors = new ArrayList<Future<Boolean>>(clients);
        final AtomicLong idx = new AtomicLong(0);
        for (int i = 0; i < clients; i++) {
            errors.add(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        long id = idx.incrementAndGet();
                        log.debug(id + " start");
                        AGRepositoryConnection conn = repo.getConnection();

                        try {
                            log.debug(id + " open");
                            conn.size();
                            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
                            conn.size();
                        } finally {
                            conn.close();
                            log.debug(id + " close");
                        }
                        return true;
                    } catch (Throwable e) {
                        log.error("error " + this, e);
                        return false;
                    }
                }
            }));
        }
        assertSuccess(errors, wait, TimeUnit.SECONDS);
    }

    @Test
    @Category(TestSuites.Stress.class)
    public void fast() throws Exception {
        final int seconds = 35;
        final int clients = 8;
        final int wait = seconds * clients * 2;
        AGAbstractTest.deleteRepository(AGAbstractTest.CATALOG_ID, "pool.fast");
        final AGConnPool pool = closeLater(AGConnPool.create(
                AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
                AGConnProp.username, AGAbstractTest.username(),
                AGConnProp.password, AGAbstractTest.password(),
                AGConnProp.catalog, "/",
                AGConnProp.repository, "pool.fast",
                AGConnProp.session, AGConnProp.Session.DEDICATED,
                AGConnProp.sessionLifetime, wait,
                AGConnProp.httpSocketTimeout, TimeUnit.SECONDS.toMillis(wait),
                AGPoolProp.shutdownHook, true,
                AGPoolProp.testOnBorrow, true,
                AGPoolProp.maxActive, 2,
                AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(wait),
                AGPoolProp.maxIdle, 8
        ));
        AGServer server = closeLater(AGAbstractTest.newAGServer());
        AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
        final AGRepository repo = closeLater(catalog.createRepository("pool.fast"));
        repo.setConnPool(pool);
        final AtomicLong idx = new AtomicLong(0);
        final AtomicLong count = new AtomicLong(0);
        ExecutorService exec = Executors.newFixedThreadPool(clients);
        List<Future<Boolean>> errors = new ArrayList<Future<Boolean>>(clients);
        for (int i = 0; i < clients; i++) {
            errors.add(exec.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        long id = idx.incrementAndGet();
                        int myCount = 0;
                        log.debug(id + " start");
                        long duration = TimeUnit.SECONDS.toNanos(seconds);
                        long start = System.nanoTime();
                        while (System.nanoTime() - start < duration) {
                            AGRepositoryConnection conn = repo.getConnection();
                            try {
                                //log.debug(id + " open");
                                myCount++;
                                count.incrementAndGet();
                                conn.size();
                            } finally {
                                conn.close();
                                //log.debug(id + " close");
                            }
                        }
                        log.debug(id + " finished " + myCount);
                        return true;
                    } catch (Throwable e) {
                        log.error("error " + this, e);
                        return false;
                    }
                }
            }));
        }
        assertSuccess(errors, wait, TimeUnit.SECONDS);
        log.info("count=" + count);
    }

    // This is a PTEST, comparing performance of connection pools vs
    // fetching a non-pool connection for every use. Not intended to be run as
    // part of the test suite.
    @Test
    @Category(TestSuites.NonPrepushTest.class)
    public void connectionTimeTest() throws Exception {
        System.out.println("Testing AGConnProp.Session.SHARED");
        doTestConnection(AGConnProp.Session.SHARED, true, 1000);
        doTestConnection(AGConnProp.Session.SHARED, false, 1000);
        System.out.println();

        System.out.println("Testing AGConnProp.Session.TX");
        doTestConnection(AGConnProp.Session.TX, true, 1000);
        doTestConnection(AGConnProp.Session.TX, false, 1000);
        System.out.println();

        System.out.println("Testing AGConnProp.Session.DEDICATED");
        doTestConnection(AGConnProp.Session.DEDICATED, true, 1000);
        doTestConnection(AGConnProp.Session.DEDICATED, false, 1000);
    }

    private void doTestConnection(AGConnProp.Session style, boolean usePooling, int iterations) throws Exception {
        AGServer server = closeLater(AGAbstractTest.newAGServer());
        AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
        AGRepository repository = catalog.createRepository("spr43583");
        AGConnPool pool = null;


        if (usePooling) {
            pool = AGConnPool.create(
                    AGConnProp.serverUrl, server.getServerURL(),
                    AGConnProp.username, AGAbstractTest.username(),
                    AGConnProp.password, AGAbstractTest.password(),
                    AGConnProp.catalog, catalog.getCatalogName(),
                    AGConnProp.repository, repository.getRepositoryID(),
                    AGConnProp.session, style
            );
            repository.setConnPool(pool);
        }

        Instant start = Instant.now();

        for (int i = 0; i < iterations; i++) {
            AGRepositoryConnection connection = repository.getConnection();
            if (connection == null) {
                Assert.fail("Connection is null");
            }

            // for apple to apple comparison, the non-pool case must create a dedicated session if
            // the style argument is TX or DEDICATED.
            if (usePooling == false && (style == AGConnProp.Session.TX ||
                    style == AGConnProp.Session.DEDICATED)) {
                // autoCommit is true for DEDICATED, false for TX
                // on return from setAutoCommit() CONNECTION is guaranteed to be
                // a dedicated session.
                connection.setAutoCommit(style == AGConnProp.Session.DEDICATED);
            }

            // Used it to check that my repository has it's ~500 statements:
            //System.out.println("Connection size = " + connection.size());
            // connection.size();
            connection.close();
        }

        System.out.println("TIMER: usePooling=" + usePooling +
                ". It took " + Duration.between(start, Instant.now()) +
                " to make " + iterations + " iterations of " +
                "get-close connection.");

        if (pool != null) {
            pool.close();
        }
        repository.close();
        server.close();
    }
}
