/******************************************************************************
** Copyright (c) 2008-2012 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.AGAbstractTest;
import test.TestSuites;
import test.Util;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.util.Closer;

public class AGConnPoolClosingTest extends Closer {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private static final int NUM = 10;

    @After
    public void closeAfter() throws Exception {
    	close();
    	log.debug("sessions after close: " + AGAbstractTest.sessions(AGAbstractTest.newAGServer()));
    }

    @Test
    @Category(TestSuites.Stress.class)
    public void openAG() throws Exception {
		final AGConnPool pool = closeLater( AGConnPool.create(
				AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
				AGConnProp.username, AGAbstractTest.username(),
				AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, "/",
				AGConnProp.repository, "test.pool.AGConnPoolClosingTest",
				AGConnProp.session, AGConnProp.Session.DEDICATED,
				AGConnProp.sessionLifetime, TimeUnit.MINUTES.toSeconds(5),
				AGPoolProp.shutdownHook, true,
				AGPoolProp.initialSize, 2,
				AGPoolProp.maxActive, 6,
				AGPoolProp.maxWait, TimeUnit.SECONDS.toMillis(45),
				AGPoolProp.maxIdle, 8
				// AGPoolProp.minIdle, 2,
				// AGPoolProp.timeBetweenEvictionRunsMillis, TimeUnit.MINUTES.toMillis(5),
				// AGPoolProp.testWhileIdle, true
		));
		
        int activeConnections = pool.getNumActive();
        ExecutorService exec = Executors.newFixedThreadPool(NUM);
        final List<Throwable> errors = new ArrayList<Throwable>();
        for (int i = 0; i < NUM; i++) {
			exec.execute(new Runnable() {
				public void run() {
					try {
						AGRepositoryConnection conn = pool.borrowConnection();
						try {
							conn.size();
							Thread.sleep(TimeUnit.SECONDS.toMillis(20));
							conn.size();
						} finally {
							conn.close();
						}
					} catch (Throwable e) {
						errors.add(e);
					}
				}
			});
		}
        exec.awaitTermination(120, TimeUnit.SECONDS);
        Assert.assertEquals(pool.toString(), activeConnections, pool.getNumActive());
        if (!errors.isEmpty()) {
        	for (Throwable e : errors) {
				log.error("error", e);
			}
        	Assert.fail("see log for details: " + errors.toString());
        }

        close();
    	long start = System.nanoTime();
    	final AGServer server = closeLater(AGAbstractTest.newAGServer());
        Map<String, String> procs = Util.waitFor(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(30),
        		new Callable<Map<String, String>>() {
        	public Map<String, String> call() throws Exception {
        		Map<String, String> procs = AGAbstractTest.processes(server);
		        for (Entry<String, String> entry : procs.entrySet()) {
					if (entry.getValue().contains("test.pool.AGConnPoolClosingTest")
							&& entry.getValue().contains("session")) {
						return procs;
					}
				}
		        return null;
			}
		});
        Assert.assertNull("Session process " + TimeUnit.NANOSECONDS.toSeconds((System.nanoTime() - start)) + " seconds after closing.", procs);
    }

    @Test
    @Category(TestSuites.Stress.class)
    public void test_openSockets_spr39342() throws Exception {
    	AGConnPool pool = closeLater( AGConnPool.create(
    			AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
    			AGConnProp.username, AGAbstractTest.username(),
    			AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, "/",
				AGConnProp.repository, "pool.spr39342",
                AGConnProp.session, AGConnProp.Session.TX,
                AGPoolProp.maxActive, 12,
                AGPoolProp.maxWait, 40000,
                AGPoolProp.shutdownHook, true,
                AGPoolProp.testOnBorrow, false,
                AGPoolProp.minIdle, 6,
                AGPoolProp.maxIdle, 7,
                AGPoolProp.timeBetweenEvictionRunsMillis, 1000,
                AGPoolProp.minEvictableIdleTimeMillis, 1000,
                AGPoolProp.initialSize, 10,
                AGConnProp.sessionLifetime, 20,
                AGPoolProp.testWhileIdle, true,
                AGPoolProp.numTestsPerEvictionRun, 1));
    	final AGServer server = closeLater(AGAbstractTest.newAGServer());
    	AGRepositoryConnection conn = closeLater( pool.borrowConnection() );
        Thread.sleep(20000);
        pool.returnObject(conn);
        log.debug("After return");
        Thread.sleep(10000);
    }

}
