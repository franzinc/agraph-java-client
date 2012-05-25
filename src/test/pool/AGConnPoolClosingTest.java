/******************************************************************************
** Copyright (c) 2008-2012 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.pool;

import static test.Util.closeWait;
import static test.Util.netstat;
import static test.Util.waitFor;
import static test.util.Clj.applyStr;
import static test.util.Clj.filter;
import static test.util.Clj.interpose;

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
import clojure.lang.AFn;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.util.Closer;

public class AGConnPoolClosingTest extends Closer {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private static final int NUM = 10;

    @After
    public void closeAfter() throws Exception {
    	close();
    	Map<String, String> sessions = AGAbstractTest.sessions(AGAbstractTest.newAGServer());
    	if (!sessions.isEmpty()) {
        	log.warn("sessions after close: " + sessions);
    	}
    }

    @Test
    @Category(TestSuites.Stress.class)
    public void openAG() throws Exception {
    	final String repoName = "pool.openAG";
		final AGConnPool pool = closeLater( AGConnPool.create(
				AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
				AGConnProp.username, AGAbstractTest.username(),
				AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, AGAbstractTest.CATALOG_ID,
				AGConnProp.repository, repoName,
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
        Map<String, String> sessions = AGAbstractTest.waitForSessions(server, repoName);
        Assert.assertNull("Sessions alive " + TimeUnit.NANOSECONDS.toSeconds((System.nanoTime() - start)) + " seconds after closing: " + sessions, sessions);
        close(server);
    }

    @Test
    @Category(TestSuites.Stress.class)
    public void openSockets_bug21099() throws Exception {
        final List<String> netstatBefore = netstat();
        {
        	log.debug("openSockets_bug21099 netstatBefore: " + applyStr(interpose("\n", netstatBefore)));
            List<String> closeWait = closeWait(netstatBefore);
            if (!closeWait.isEmpty()) {
            	log.warn("openSockets_bug21099 netstat close_wait Before: " + applyStr(interpose("\n", closeWait)));
            }
        }
    	final String repoName = "pool.bug21099";
    	AGConnPool pool = closeLater( AGConnPool.create(
    			AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
    			AGConnProp.username, AGAbstractTest.username(),
    			AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, AGAbstractTest.CATALOG_ID,
				AGConnProp.repository, repoName,
                AGConnProp.session, AGConnProp.Session.TX,
                AGPoolProp.maxActive, 40,
                AGPoolProp.maxWait, 40000,
                AGPoolProp.shutdownHook, true,
                AGPoolProp.testOnBorrow, false,
                AGPoolProp.minIdle, 10,
                AGPoolProp.maxIdle, 20,
                AGPoolProp.timeBetweenEvictionRunsMillis, 1000,
                AGPoolProp.minEvictableIdleTimeMillis, 2000,
                AGConnProp.sessionLifetime, 30,
                AGPoolProp.testWhileIdle, true,
                AGPoolProp.numTestsPerEvictionRun, 5,
                AGPoolProp.initialSize, 5
    	));
        Thread.sleep(30000);
        List<String> netstat = filter(new AFn() {
        	public Object invoke(Object line) {
        		return ! netstatBefore.contains(line);
        	}
        }, netstat());
        List<String> closeWait = closeWait(netstat);
        Assert.assertTrue("sockets in CLOSE_WAIT: " + applyStr(interpose("\n", closeWait)), closeWait.isEmpty());
        Assert.assertTrue("too many sockets open: " + applyStr(interpose("\n", netstat)), 10 >= netstat.size());
        
        close(pool);
        
        netstat = AGAbstractTest.waitForNetStat(netstatBefore);
        Assert.assertNull("sockets open after closing pool: " + netstat, netstat);
        
    	final AGServer server = closeLater(AGAbstractTest.newAGServer());
    	long start = System.nanoTime();
        Map<String, String> sessions = AGAbstractTest.waitForSessions(server, repoName);
        Assert.assertNull("Sessions alive " + TimeUnit.NANOSECONDS.toSeconds((System.nanoTime() - start)) + " seconds after closing.", sessions);
        close(server);
    }

    @Test
    @Category(TestSuites.Broken.class)
    /**
     * Test without the pool.
     */
    public void openSockets_bug21109_direct() throws Exception {
        final List<String> netstatBefore = netstat();
    	log.debug("openSockets_bug21109_direct netstatBefore: " + applyStr(interpose("\n", netstatBefore)));
        
    	final String repoName = "direct.bug21109";
    	AGServer server = closeLater(AGAbstractTest.newAGServer());
    	AGCatalog cat = server.getCatalog(AGAbstractTest.CATALOG_ID);
    	AGRepository repo = cat.createRepository(repoName);
    	for (int i = 0; i < 30; i++) {
        	AGRepositoryConnection conn1 = closeLater(repo.getConnection());
        	conn1.setAutoCommit(false);
        	AGRepositoryConnection conn2 = closeLater(repo.getConnection());
        	conn2.setAutoCommit(false);
        	AGRepositoryConnection conn3 = closeLater(repo.getConnection());
        	conn3.setAutoCommit(false);
        	close(conn1);
        	close(conn2);
        	close(conn3);
        	if (i==5) {
            	log.debug("netstat " + i + ": " + applyStr(interpose("\n", netstat())));
        	}
		}
        List<String> netstat = filter(new AFn() {
        	public Object invoke(Object line) {
        		return ! netstatBefore.contains(line);
        	}
        }, netstat());
        List<String> closeWait = closeWait(netstat);
        Assert.assertTrue("sockets in CLOSE_WAIT: " + applyStr(interpose("\n", closeWait)), closeWait.isEmpty());
        Assert.assertTrue("too many sockets open: " + applyStr(interpose("\n", netstat)), 10 >= netstat.size());
        
        repo = close(repo);
        server = close(server);
        
        netstat = AGAbstractTest.waitForNetStat(netstatBefore);
        Assert.assertNull("sockets open after closing pool: " + applyStr(interpose("\n", netstat)), netstat);
        
    	server = closeLater(AGAbstractTest.newAGServer());
    	long start = System.nanoTime();
        Map<String, String> sessions = AGAbstractTest.waitForSessions(server, repoName);
        Assert.assertNull("Sessions alive " + TimeUnit.NANOSECONDS.toSeconds((System.nanoTime() - start)) + " seconds after closing: " + sessions, sessions);
        close(server);
    }

}
