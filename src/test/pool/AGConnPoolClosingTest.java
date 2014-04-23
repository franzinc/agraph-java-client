/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.pool;

import static test.Util.closeWait;
import static test.Util.netstat;
import static test.util.Clj.applyStr;
import static test.util.Clj.interpose;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.AGAbstractTest;
import test.TestSuites;
import test.Util;
import test.util.Clj;

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
		
	Assert.assertEquals(pool.toString(), 0, pool.getNumActive());

	final int NUM = 10; // Number of workers
	final int HOLD_TIME = 20; // seconds

	final AGServer server = AGAbstractTest.newAGServer();
	AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
	final AGRepository repo = closeLater(catalog.createRepository(repoName));
	repo.setConnPool(pool);

        ExecutorService exec = Executors.newFixedThreadPool(NUM);
        final List<Throwable> errors = new ArrayList<Throwable>();
        for (int i = 0; i < NUM; i++) {
			exec.execute(new Runnable() {
				public void run() {
					try {
						AGRepositoryConnection conn = repo.getConnection();
						try {
							conn.size();
							// Hold the connection for a while 
							Thread.sleep(TimeUnit.SECONDS.toMillis(HOLD_TIME));
							// Make sure it still works.
							conn.size();
						} finally {
						    // Now return it to the pool.
							conn.close();
						}
					} catch (Throwable e) {
						errors.add(e);
					}
				}
			});
		}
	/* Given the current configuration, 6 workers will be able to acquire a connection
	   object.  The remaining 4 will block.  The original 6 will (in parallel)
	   delay for HOLD_TIME seconds, return their connection to the pool, then terminate.
	   The remaining 4 will them able to proceed.  So ultimately we will have two sets
	   of delays, so that's how long we need to wait (plus some fudge to compensate for unexpected
	   delays) */
	final int WAIT_FUDGE = 5;
        exec.awaitTermination(HOLD_TIME * 2 + WAIT_FUDGE, TimeUnit.SECONDS);
        Assert.assertEquals(pool.toString(), 0, pool.getNumActive());
        if (!errors.isEmpty()) {
        	for (Throwable e : errors) {
				log.error("error", e);
			}
        	Assert.fail("see log for details: " + errors.toString());
        }

        close();
    	long start = System.nanoTime();
        Map<String, String> sessions = Util.waitForSessions(server, repoName);
        Assert.assertNull("Sessions alive " + TimeUnit.NANOSECONDS.toSeconds((System.nanoTime() - start)) + " seconds after closing: " + sessions, sessions);
        close(server);
    }
    
	private static List<String> netstatLinesToRegex(List<String> netstatBefore) throws Exception {
		return Clj.map(new AFn() {
        	public Object invoke(Object netstatLine) {
        		String[] netstatFields = ((String)netstatLine).split(" +");
        		return ".*" + netstatFields[3] + " +" + netstatFields[4] + ".*";
        	}
		}, netstatBefore);
	}
	
    @Test
    // @Category(TestSuites.Stress.class)
    @Category(TestSuites.Broken.class) // This test is unreliable and has been disabled. -- dancy
    public void openSockets_bug21099() throws Exception {
    	final String repoName = "pool.bug21099";
    	
        Thread.sleep(1000);
        List<String> netstatBefore = netstat();
        {
        	log.info("openSockets_bug21099 netstatBefore:\n" + applyStr(interpose("\n", netstatBefore)));
		/*
                List<String> closeWait = closeWait(netstatBefore);
                if (!closeWait.isEmpty()) {
                        log.warn("openSockets_bug21099 netstat close_wait Before:\n" + applyStr(interpose("\n", closeWait)));
                }
		*/
        }
        // use a regex to get the ports from netstat, but allow for the state to change (when filtering in waitForNetStat below)
        netstatBefore = netstatLinesToRegex(netstatBefore);
        //log.info("openSockets_bug21099 netstatBefore regexes:\n" + applyStr(interpose("\n", netstatBefore)));
	final int minIdle = 10;
        final int maxIdle = 20;
    	AGConnPool pool = closeLater( AGConnPool.create(
					      // Connection Properties
                                              AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
                                              AGConnProp.username, AGAbstractTest.username(),
                                              AGConnProp.password, AGAbstractTest.password(),
                                              AGConnProp.catalog, AGAbstractTest.CATALOG_ID,
                                              AGConnProp.repository, repoName,
                                              AGConnProp.session, AGConnProp.Session.TX,
					      AGConnProp.sessionLifetime, 30, // seconds

					      // Pool properties.
					      AGPoolProp.maxActive, 40, 
					      AGPoolProp.maxWait, 40000, // Wait up to 40 seconds for borrowObject() to succeed
					      AGPoolProp.shutdownHook, true, // Register a hook to close the pool when the JVM shuts down.
					      AGPoolProp.testOnBorrow, false, // Don't attempt to validate each object before borrowObject() returns it
					      AGPoolProp.minIdle, minIdle, // Ensure that the eviction thread maintains at least this many idle objects in the pool
					      /* Max number of idle (unused) objects in the pool.  If returnObject() is called and the number of
						 idle objects has reached this value, the returned object is immediately destroyed */
					      AGPoolProp.maxIdle, maxIdle, 
					      AGPoolProp.timeBetweenEvictionRunsMillis, 1000, // eviction thread polling interval
					      AGPoolProp.minEvictableIdleTimeMillis, 2000, // An unused object must be idle at least this long before being eligible for eviction
					      AGPoolProp.testWhileIdle, true, // validate objects in the idle object eviction thread
					      AGPoolProp.numTestsPerEvictionRun, minIdle, // max number of objects to examine during each run of the idle object evictor thread
					      AGPoolProp.initialSize, 5 // When the pool is created, this many connections will be initialized, then returned to the pool.
    	));

	log.info("openSockets_bug21099: Pool created");

	/* The AGConnPool configuration above will start off with 5
	   (initialSize) session connections, then will immediately
	   jump up to 10 (minIdle).  Then, every second, connections
	   that have lived more than two seconds will be closed and
	   new session connections will be created to replace them.
           
	   We let this cycle operate for a while (30 seconds).
	   Afterward, we verify that we haven't leaked any session
	   connections.  We use the presence of sockets in the
	   CLOSE_WAIT state to determine if we've had such a leak.  As
	   a reminder, a socket in CLOSE_WAIT state means that the
	   remote end of the socket has closed but the local end
	   hasn't yet.  So, the leak detection depends on the
	   dedication session exceeding its sessionLifetime and
	   closing down. 
	*/

	log.info("openSockets_bug21099: Sleeping for 30 seconds while pool oscillates");
        Thread.sleep(30000);
	log.info("openSockets_bug21099: Sleep completed.  Checking for sockets in CLOSE_WAIT state");

	/* Warning: netstat will be null if the filtered output resulted in 0 lines */
        List<String> netstat = Util.waitForNetStat(0, netstatBefore);
        List<String> closeWait = closeWait(netstat);
        Assert.assertTrue("sockets in CLOSE_WAIT:\n" + applyStr(interpose("\n", closeWait)), closeWait.isEmpty());
        // there may be maxIdle sockets "ESTABLISHED" and some in TIME_WAIT, so check for (maxIdle*2)
	if (netstat != null) {
	    Assert.assertTrue("too many sockets open:\n" + applyStr(interpose("\n", netstat)), (maxIdle*2) >= netstat.size());
	}

        close(pool);

        netstat = Util.waitForNetStat(120, netstatBefore);
        Assert.assertNull("sockets open after closing pool:\n" + applyStr(interpose("\n", netstat)), netstat);

        final AGServer server = closeLater(AGAbstractTest.newAGServer());
        long start = System.nanoTime();
        Map<String, String> sessions = Util.waitForSessions(server, repoName);
        Assert.assertNull("Sessions alive " + TimeUnit.NANOSECONDS.toSeconds((System.nanoTime() - start)) + " seconds after closing:\n" + sessions, sessions);
        close(server);
    }

    @Test
    @Category(TestSuites.Broken.class)
    /**
     * Test without the pool.
     */
    public void openSockets_bug21109_direct() throws Exception {
	log.info("openSockets_bug21109_direct: Test started. Sleeping for 1 second");
        Thread.sleep(1000);
        List<String> netstatBefore = netstat();
    	log.info("openSockets_bug21109_direct netstatBefore:\n" + applyStr(interpose("\n", netstatBefore)));
        // use a regex to get the ports from netstat, but allow for the state to change (when filtering in waitForNetStat below)
        netstatBefore = netstatLinesToRegex(netstatBefore);
        
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
                        log.debug("netstat " + i + ":\n" + applyStr(interpose("\n", netstat())));
        	}
        }
        List<String> netstat = Util.waitForNetStat(0, netstatBefore);
        List<String> closeWait = closeWait(netstat);
        Assert.assertTrue("sockets in CLOSE_WAIT:\n" + applyStr(interpose("\n", closeWait)), closeWait.isEmpty());
        Assert.assertTrue("too many sockets open:\n" + applyStr(interpose("\n", netstat)), 10 >= netstat.size());
        
        repo = close(repo);
        server = close(server);
        
        netstat = Util.waitForNetStat(120, netstatBefore);
        Assert.assertNull("sockets open after closing pool:\n" + applyStr(interpose("\n", netstat)), netstat);
        
    	server = closeLater(AGAbstractTest.newAGServer());
    	long start = System.nanoTime();
        Map<String, String> sessions = Util.waitForSessions(server, repoName);
        Assert.assertNull("Sessions alive " + TimeUnit.NANOSECONDS.toSeconds((System.nanoTime() - start)) + " seconds after closing: " + sessions, sessions);
        close(server);
    }
    
    
    @Test
    @Category(TestSuites.Broken.class)
    /**
     * Test for invalid configuration of HttpSessionManager
     * Test fails if the AGConnFactory does not properly initialize
     * the http session manager with MultiThreadedHttpSessionManager object.
     * See: spr30491
     */
    public void invalidSessionPortsConfiguration_spr30491() throws Exception {
    	final String repoName = "pool.spr30491-test";
    	
    	AGConnPool pool = closeLater( AGConnPool.create(
					                AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
					                AGConnProp.username, AGAbstractTest.username(),
					                AGConnProp.password, AGAbstractTest.password(),
					                AGConnProp.catalog, AGAbstractTest.CATALOG_ID,
					                AGConnProp.repository, repoName,
					                AGConnProp.session, AGConnProp.Session.DEDICATED,
									AGPoolProp.shutdownHook, true,
									AGPoolProp.maxActive, 1,
									AGPoolProp.initialSize, 1 ));
    	
    	AGRepositoryConnection conn = pool.borrowConnection();

    	Assert.assertEquals(pool.getNumActive(), 1);
    	
    	ValueFactory vf = conn.getValueFactory();
    	conn.add( vf.createStatement(vf.createURI("http://ag/test-spr30491-test1"),
					    				vf.createURI("http://ag/spr-name"),
					    				vf.createLiteral("spr30491")) );
    	
    	TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,"SELECT ?s ?p ?o WHERE { ?s ?p ?o . }");
    	
    	final ValueFactory fvf = vf;
    	final RepositoryConnection fconn = conn;
    	
    	//The handler executes on a separate thread and requires a thread safe Http Session Manager
    	TupleQueryResultHandler handler = new TupleQueryResultHandler() {

			@Override
			public void startQueryResult(List<String> bindingNames)
					throws TupleQueryResultHandlerException {
			}

			@Override
			public void endQueryResult()
					throws TupleQueryResultHandlerException {
			}

			@Override
			public void handleSolution(BindingSet bindingSet)
					throws TupleQueryResultHandlerException {
				
				try {
					RepositoryResult<Statement> statements = fconn.getStatements(fvf.createURI(bindingSet.getValue("s").stringValue()), null, null, false);
					Assert.assertNotNull(statements);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new TupleQueryResultHandlerException(e);
                }
			}

			@Override
			public void handleBoolean(boolean arg0)
					throws QueryResultHandlerException {
			}

			@Override
			public void handleLinks(List<String> arg0)
					throws QueryResultHandlerException {
			}
    		
    	};

    	try {
        	query.evaluate(handler);
    	}
    	catch (QueryEvaluationException e) {
    		//In failure situation the error message is expected to be similar in content to:
    		//  org.openrdf.query.QueryEvaluationException: com.franz.agraph.http.exception.AGHttpException: Possible session port connection failure. Consult the Server Installation document for correct settings for SessionPorts. Url: http://localhost:29005/sessions/9475d245-3c15-d054-9e7b-000c293de094. Documentation: http://www.franz.com/agraph/support/documentation/v4/server-installation.html#sessionport
    		//   at com.franz.agraph.repository.AGQuery.evaluate(AGQuery.java:287)
    		
    		Assert.fail("HttpSessionManager no configured properly in com.franz.agraph.pool.AGConnFactory");
    	}
    	
    	pool.returnObject(conn);
    	
    	Assert.assertEquals(pool.getNumActive(), 0);
    	
    	close(pool);
    }

}
