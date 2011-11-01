/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.pool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.AGAbstractTest;
import test.TestSuites;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.util.Closer;

public class AGConnPoolSessionTest extends Closer {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private static final String ns = "http://example.com/#";

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
        AGServer server = closeLater( new AGServer(AGAbstractTest.findServerUrl(), AGAbstractTest.username(), AGAbstractTest.password()));
        AGCatalog catalog = server.getCatalog("/");
        AGRepository repo = closeLater( catalog.createRepository("pool.testPlain"));
        AGRepositoryConnection conn = closeLater( repo.getConnection());
        Assert.assertTrue(conn.toString(), conn.getHttpRepoClient().getRoot().contains(AGAbstractTest.findServerUrl()));
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
    	AGConnPool pool = closeLater( AGConnPool.create(
    			AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
    			AGConnProp.username, AGAbstractTest.username(),
    			AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, "/",
				AGConnProp.repository, "pool.testPoolDedicated",
				AGConnProp.session, AGConnProp.Session.DEDICATED,
                AGConnProp.sessionLifetime, TimeUnit.MINUTES.toSeconds(1),
                AGPoolProp.shutdownHook, true,
                AGPoolProp.testOnBorrow, true,
                AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(30)
		));
        AGRepositoryConnection conn = closeLater( pool.borrowConnection());
        Assert.assertFalse(conn.toString(), conn.getHttpRepoClient().getRoot().contains(AGAbstractTest.findServerUrl()));
        Assert.assertEquals(0, conn.size());
        conn.deleteDatatypeMapping(XMLSchema.DOUBLE);
        conn.deleteDatatypeMapping(XMLSchema.FLOAT);
        Assert.assertEquals(0, conn.size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void testPoolTx() throws Exception {
    	AGConnPool pool = closeLater( AGConnPool.create(
    			AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
    			AGConnProp.username, AGAbstractTest.username(),
    			AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, "/",
				AGConnProp.repository, "pool.testPoolTx",
				AGConnProp.session, AGConnProp.Session.TX,
                AGConnProp.sessionLifetime, TimeUnit.MINUTES.toSeconds(1),
                AGPoolProp.testOnBorrow, true,
                AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(30)
		));
        AGRepositoryConnection conn = closeLater( pool.borrowConnection());
        Assert.assertFalse(conn.toString(), conn.getHttpRepoClient().getRoot().contains(AGAbstractTest.findServerUrl()));
        Assert.assertEquals(0, conn.size());
        conn.deleteDatatypeMapping(XMLSchema.DOUBLE);
        conn.deleteDatatypeMapping(XMLSchema.FLOAT);
        Assert.assertEquals(0, conn.size());
    }

    /**
     * Tests there is no problem calling deleteDatatypeMapping
     * in many concurrent threads.
     */
    @Test
    @Category(TestSuites.Stress.class)
    public void deleteDatatypeMapping() throws Exception {
        // TODO refactor to not use the pool, reconnect each time
    	final int NUM = 20;
    	final int MINUTES = 1;
    	final AGConnPool pool = closeLater( AGConnPool.create(
    			AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
    			AGConnProp.username, AGAbstractTest.username(),
    			AGConnProp.password, AGAbstractTest.password(),
    			AGConnProp.catalog, "/",
    			AGConnProp.repository, "pool.deleteDatatypeMapping",
    			AGConnProp.session, AGConnProp.Session.SHARED,
    			AGConnProp.sessionLifetime, TimeUnit.MINUTES.toSeconds(1),
                AGPoolProp.shutdownHook, true,
    			AGPoolProp.testOnBorrow, true,
    			AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(30)
    	));
    	{
    		AGRepositoryConnection conn = closeLater( pool.borrowConnection());
            Assert.assertTrue(conn.toString(), conn.getHttpRepoClient().getRoot().contains(AGAbstractTest.findServerUrl()));
    		AGValueFactory vf = conn.getValueFactory();
    		for (int i = 0; i < NUM * 10; i++) {
    			conn.add(new File("src/tutorial/java-kennedy.ntriples"), ns, RDFFormat.NTRIPLES, vf.createURI(ns + i));
    		}
    		log.debug("size=" + conn.size());
    		close(conn);
    	}
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
    						if (now-start > TimeUnit.MINUTES.toNanos((int)(MINUTES * 0.9))) {
    							break;
    						}
    						AGRepositoryConnection conn = pool.borrowConnection();
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
    	Assert.assertEquals(pool.toString(), 0, pool.getNumActive());
    	if (!errors.isEmpty()) {
    		for (Throwable e : errors) {
    			log.error("error", e);
    		}
    		Assert.fail("see log for details: " + errors.toString());
    	}
    }
    
    @Test
    @Category(TestSuites.Stress.class)
    public void maxActive() throws Exception {
    	final int seconds = 5;
    	final int clients = 4;
    	final AGConnPool pool = closeLater( AGConnPool.create(
    			AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
    			AGConnProp.username, AGAbstractTest.username(),
    			AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, "/",
				AGConnProp.repository, "pool.maxActive",
				AGConnProp.session, AGConnProp.Session.DEDICATED,
				AGConnProp.sessionLifetime, seconds * clients * 2,
				AGConnProp.httpSocketTimeout, TimeUnit.SECONDS.toMillis(seconds * clients),
                AGPoolProp.shutdownHook, true,
                AGPoolProp.testOnBorrow, true,
				AGPoolProp.maxActive, 2,
				AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis((seconds * clients) + 10),
				AGPoolProp.maxIdle, 8
		));
        ExecutorService exec = Executors.newFixedThreadPool(clients);
    	List<Future<Boolean>> errors = new ArrayList<Future<Boolean>>(clients);
        final AtomicLong idx = new AtomicLong(0);
        for (int i = 0; i < clients; i++) {
        	errors.add( exec.submit( new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					try {
						long id = idx.incrementAndGet();
						log.debug(id + " start");
						AGRepositoryConnection conn = pool.borrowConnection();
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
        assertSuccess(errors,  seconds * clients * 2, TimeUnit.SECONDS);
    }

    @Test
    @Category(TestSuites.Stress.class)
    public void fast() throws Exception {
    	final int seconds = 35;
    	final int clients = 8;
    	AGAbstractTest.deleteRepository("/", "pool.fast");
    	final AGConnPool pool = closeLater( AGConnPool.create(
    			AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
    			AGConnProp.username, AGAbstractTest.username(),
    			AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, "/",
				AGConnProp.repository, "pool.fast",
				AGConnProp.session, AGConnProp.Session.DEDICATED,
				AGConnProp.sessionLifetime, seconds * clients * 2,
				AGConnProp.httpSocketTimeout, TimeUnit.SECONDS.toMillis(seconds * clients),
                AGPoolProp.shutdownHook, true,
                AGPoolProp.testOnBorrow, true,
				AGPoolProp.maxActive, 2,
				AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(seconds + 10),
				AGPoolProp.maxIdle, 8
		));
        final AtomicLong idx = new AtomicLong(0);
        final AtomicLong count = new AtomicLong(0);
        ExecutorService exec = Executors.newFixedThreadPool(clients);
    	List<Future<Boolean>> errors = new ArrayList<Future<Boolean>>(clients);
        for (int i = 0; i < clients; i++) {
        	errors.add( exec.submit(new Callable<Boolean>() {
        		@Override
				public Boolean call() throws Exception {
					try {
						long id = idx.incrementAndGet();
						int myCount = 0;
						log.debug(id + " start");
						long duration = TimeUnit.SECONDS.toNanos(seconds);
						long start = System.nanoTime();
						while (System.nanoTime()-start < duration) {
							AGRepositoryConnection conn = pool.borrowConnection();
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
        assertSuccess(errors,  seconds * clients * 2, TimeUnit.SECONDS);
        log.info("count=" + count);
    }

}
