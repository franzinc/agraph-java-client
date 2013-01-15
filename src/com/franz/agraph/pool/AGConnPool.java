/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.ServletContextListener;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.util.Closeable;
import com.franz.util.Closer;

/**
 * Pooling for {@link AGRepositoryConnection}s.
 * The recommended way to create a pool is by {@link #create(Object...)}
 * or by configuring a {@link AGConnPoolJndiFactory} with your appserver.
 * 
 * <p><code><pre>
 * 	AGConnPool pool = AGConnPool.create(
 * 		AGConnProp.serverUrl, "http://localhost:10035",
 * 		AGConnProp.username, "test",
 * 		AGConnProp.password, "xyzzy",
 * 		AGConnProp.catalog, "/",
 * 		AGConnProp.repository, "my_repo",
 * 		AGConnProp.session, AGConnProp.Session.DEDICATED,
 * 		AGPoolProp.shutdownHook, true,
 * 		AGPoolProp.maxActive, 10,
 * 		AGPoolProp.initialSize, 2);
 * 	AGRepositoryConnection conn = pool.borrowConnection();
 * 	try {
 * 		...
 * 		conn.commit();
 * 	} finally {
 * 		conn.close();
 * 		// or equivalently
 * 		pool.returnObject(conn);
 * 	}
 * </pre></code></p>
 * 
 * <p>This pool delegates the pooling implementation to another
 * pool (a {@link GenericObjectPool}).
 * When {@link AGRepositoryConnection Connections} are {@link #borrowObject() borrowed},
 * they are wrapped so that {@link Closeable#close()}
 * will call {@link #returnObject(Object)} instead of actually closing.
 * </p>
 * 
 * <p>Closing the connection pool is important because server sessions will
 * stay active until {@link AGConnProp#sessionLifetime}.
 * The option to use a Runtime shutdownHook is built-in with {@link AGPoolProp#shutdownHook}.
 * Another option is to use {@link ServletContextListener} - this is appropriate if the
 * agraph jar is deployed within your webapp and not with the webserver.
 * With tomcat, a <a href="http://tomcat.apache.org/tomcat-6.0-doc/config/context.html#Lifecycle_Listeners"
 * >Lifecycle Listener</a> can be configured, but the implementation to do this
 * is not included in this library.
 * </p>
 * 
 * <p>Note, when {@link AGConnPool#close()} is called
 * on a {@link AGConnPool},
 * connections will be closed whether they are
 * idle (have been returned) or not.
 * This is different from {@link GenericObjectPool#close()}.
 * </p>
 * 
 * @since v4.3.3
 */
public class AGConnPool
extends Closer
implements ObjectPool, Closeable {

	private static final Logger log = LoggerFactory.getLogger(AGConnPool.class);

	private final ObjectPool delegate;
	
	private final AGConnFactory factory;

	/**
	 * Delegates all methods to the wrapped conn except for close.
	 * 
	 * <p>Note, these objects are never closed for real, but they don't hold any resources.</p>
	 */
	class AGRepositoryConnectionPooled extends AGRepositoryConnection {
		
		final AGRepositoryConnection conn;
		
		public AGRepositoryConnectionPooled(AGRepositoryConnection conn) {
			super((AGRepository) conn.getRepository(), conn.getHttpRepoClient());
			this.conn = conn;
		}
		
		@Override
		public void close() throws RepositoryException {
			try {
				returnObject(this);
			} catch (Exception e) {
				throw new RepositoryException(e);
			}
		}
	}
	
	/**
	 * 
	 */
	class ClosingFactory implements PoolableObjectFactory {

		private final PoolableObjectFactory delegate;
		
		ClosingFactory(PoolableObjectFactory delegate) {
			this.delegate = delegate;
		}
		
		@Override
		public void activateObject(Object object) throws Exception {
			delegate.activateObject(unwrap(object));
		}

		@Override
		public void destroyObject(Object object) throws Exception {
			delegate.destroyObject(unwrap(object));
		}

		@Override
		public Object makeObject() throws Exception {
			return new AGRepositoryConnectionPooled((AGRepositoryConnection) delegate.makeObject());
		}

		@Override
		public void passivateObject(Object object) throws Exception {
			delegate.passivateObject(unwrap(object));
		}

		@Override
		public boolean validateObject(Object object) {
			return delegate.validateObject(unwrap(object));
		}
		
	}
	
	private static class ShutdownHookCloser extends Thread implements Closeable {
		
		private static final Logger log = LoggerFactory.getLogger(ShutdownHookCloser.class);
		
		private final Closer closer;
		
		public ShutdownHookCloser(Closer closer) {
			this.closer = closer;
		}

		@Override
		public void run() {
			log.info("closing " + closer);
			// remove this before closing, because removeShutdownHook will throw if inside of run
			closer.remove(this);
			Closer.Close(closer);
			log.debug("closed " + closer);
		}

		/**
		 * Removing is useful for pools that get closed manually
		 * so the Runtime doesn't hold on to this and everything.
		 */
		@Override
		public void close() {
			Runtime.getRuntime().removeShutdownHook(this);
		}
	}
	
	/**
	 * @see #create(Object...)
	 */
	private AGConnPool(AGConnFactory factory, AGPoolConfig poolConfig) {
		this.delegate = new GenericObjectPool(new ClosingFactory(factory), poolConfig);
		closeLater(delegate);
		this.factory = factory;

		if (poolConfig.initialSize > 0) {
			List<AGRepositoryConnection> conns = new ArrayList<AGRepositoryConnection>(poolConfig.initialSize);
			try {
				for (int i = 0; i < poolConfig.initialSize; i++) {
					conns.add(borrowConnection());
				}
			} catch (RepositoryException e) {
				throw new RuntimeException(e);
			}
			// return them to the pool
			closeAll(conns);
		}
		if (poolConfig.shutdownHook) {
			Runtime.getRuntime().addShutdownHook( closeLater( new ShutdownHookCloser(this)));
		}
	}

	/**
	 * A {@link GenericObjectPool} is used.
	 */
	public static AGConnPool create(AGConnFactory factory, AGPoolConfig poolConfig) {
		return new AGConnPool(factory, poolConfig);
	}

	/**
	 * Create a pool from configuration properties.
	 * @param connProps keys are {@link AGConnProp}
	 * @param poolProps keys are {@link AGPoolProp}
	 */
	public static AGConnPool create(Map<AGConnProp, String> connProps, Map<AGPoolProp, String> poolProps) throws RepositoryException {
		AGPoolConfig poolConfig = new AGPoolConfig(poolProps);
		AGConnFactory fact = new AGConnFactory(new AGConnConfig(connProps), poolConfig);
		final AGConnPool pool = AGConnPool.create(fact, poolConfig);
		pool.closeLater(fact);
		return pool;
	}
		
	/**
	 * Create a pool from configuration properties.
	 * @param keyValuePairs alternating key/value pairs where keys are {@link AGConnProp} and {@link AGPoolProp}
	 */
	public static AGConnPool create(Object...keyValuePairs) throws RepositoryException {
		Map<AGConnProp, String> connProps = (Map<AGConnProp, String>) toMap(keyValuePairs, EnumSet.allOf(AGConnProp.class));
		Map<AGPoolProp, String> poolProps = (Map<AGPoolProp, String>) toMap(keyValuePairs, EnumSet.allOf(AGPoolProp.class));
		return create(connProps, poolProps);
	}

    protected static Map<? extends Enum, String> toMap(Object[] keyValuePairs, EnumSet<? extends Enum> enumSet) {
    	Map<Enum, String> map = new HashMap<Enum, String>();
    	for (int i = 0; i < keyValuePairs.length; i=i+2) {
    		Enum key = (Enum) keyValuePairs[i];
    		if (enumSet.contains(key)) {
        		Object val = keyValuePairs[i+1];
    			map.put(key, val==null ? null : val.toString());
    		}
		}
    	return map;
    }
    
	@Override
	public void addObject() throws Exception, IllegalStateException, UnsupportedOperationException {
		delegate.addObject();
	}
	
	@Override
	public Object borrowObject() throws Exception, NoSuchElementException, IllegalStateException {
		return delegate.borrowObject();
	}
	
	public AGRepositoryConnection borrowConnection() throws RepositoryException {
		try {
			return (AGRepositoryConnection) delegate.borrowObject();
		} catch (Exception e) {
			throw new RepositoryException(e);
		}
	}
	
	@Override
	public void clear() throws Exception, UnsupportedOperationException {
		delegate.clear();
	}
	
	@Override
	public int getNumActive() throws UnsupportedOperationException {
		return delegate.getNumActive();
	}
	
	@Override
	public int getNumIdle() throws UnsupportedOperationException {
		return delegate.getNumIdle();
	}
	
	@Override
	public void invalidateObject(Object obj) throws Exception {
		delegate.invalidateObject(obj);
	}
	
	@Override
	public void returnObject(Object obj) throws Exception {
		delegate.returnObject(obj);
	}
	
	public static Object unwrap(Object obj) {
		if (obj instanceof AGConnPool.AGRepositoryConnectionPooled) {
			return ((AGConnPool.AGRepositoryConnectionPooled)obj).conn;
		} else {
			return obj;
		}
	}
	
	@Override
	@Deprecated
	public void setFactory(PoolableObjectFactory obj) throws IllegalStateException, UnsupportedOperationException {
		delegate.setFactory(obj);
	}
	
	public void ensureIdle(int n) throws Exception {
		if (delegate instanceof GenericObjectPool) {
			GenericObjectPool gop = (GenericObjectPool) delegate;
			gop.setMinIdle(n);
			if (gop.getMaxIdle() < n) {
				gop.setMaxIdle(n);
			}
		}
		for (int i = 0; i < n; i++) {
			addObject();
		}
	}

	@Override
	public void close() {
		if (log.isDebugEnabled()) log.debug("close " + this);
		close(delegate);
		super.close();
	}

	protected void finalize() throws Throwable {
		if (getNumActive() > 0) {
			close();
			log.warn("Finalizing with open connections, please close the pool properly. " + this);
		}
	}
	
	@Override
	public String toString() {
		return "{AGConnPool"
		+ " active=" + getNumActive()
		+ " idle=" + getNumIdle()
		+ " delegate=" + delegate
		+ " factory=" + factory
		+ " this=" + super.toString()
		+ "}";
	}

}
