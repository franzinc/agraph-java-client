/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.stress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.AGAbstractTest;

import com.franz.agraph.pool.AGConnPool;
import com.franz.agraph.pool.AGConnProp;
import com.franz.agraph.pool.AGPoolProp;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.util.Util;

public class AGConnPoolTest {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private static final int NUM = 10;

	private AGConnPool pool;

	@Before
	public void connect() throws RepositoryException {
		Assert.assertNull(pool);
		pool = AGConnPool.create(
				AGConnProp.serverUrl, AGAbstractTest.findServerUrl(),
				AGConnProp.username, AGAbstractTest.username(),
				AGConnProp.password, AGAbstractTest.password(),
				AGConnProp.catalog, AGAbstractTest.CATALOG_ID,
				AGConnProp.repository, "javatest",
				AGConnProp.session, AGConnProp.Session.DEDICATED,
				AGPoolProp.shutdownHook, true,
				AGPoolProp.initialSize, 2
				// AGPoolProp.maxActive, 5,
				// AGPoolProp.maxWait, TimeUnit.MINUTES.toMillis(5),
				// AGPoolProp.maxIdle, 2,
				// AGPoolProp.minIdle, 2,
				// AGPoolProp.timeBetweenEvictionRunsMillis, TimeUnit.MINUTES.toMillis(5),
				// AGPoolProp.testWhileIdle, true
		);
	}
		
    @After
    public void closePool() {
    	log.info("closing " + pool);
    	AGConnPool closed = Util.close(pool);
    	log.info("closed " + pool);
    	pool = closed;
    }

    @Test
    public void openAG() throws Exception {
        int activeConnections = pool.getNumActive();
        ExecutorService exec = Executors.newFixedThreadPool(NUM);
        for (int i = 0; i < NUM; i++) {
			exec.execute(new Runnable() {
				public void run() {
					try {
						AGRepositoryConnection conn = pool.borrowConnection();
						try {
							conn.ping();
						} finally {
							conn.close();
						}
					} catch (Throwable e) {
						log.error(this.toString(), e);
					}
				}
			});
		}
        exec.awaitTermination(30, TimeUnit.SECONDS);
        Assert.assertEquals(pool.toString(), activeConnections, pool.getNumActive());
    }

}
