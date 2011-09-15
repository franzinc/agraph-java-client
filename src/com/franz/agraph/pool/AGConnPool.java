/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.pool.AGConnPoolJndiFactory.AGConfig;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.util.Closeable;
import com.franz.util.Closer;
import com.franz.util.Util;

/**
 * Connection pool that delegates pooling to another instance,
 * adding only {@link #borrowConnection()}, which also wraps
 * the {@link AGRepositoryConnection} so that {@link Closeable#close()}
 * will call {@link #returnObject(Object)} instead of actually closing.
 * 
 * <p>Warning: Since the objects {@link #borrowConnection() borrowed}
 * from this class are wrapped, you can not use them directly with
 * the delegate pool.
 * The delegate pool should only be used for its pooling
 * implementation and configuration.
 * </p>
 * 
 * <p>Note, when {@link AGConnPool#close()} is called
 * on a {@link AGConnPool},
 * connections will be closed whether they are
 * idle (have been returned) or not.
 * This is different from {@link GenericObjectPool#close()}.
 * </p>
 * 
 * @param <ObjectPoolType>
 * 
 * @since v4.3.3
 */
public class AGConnPool <ObjectPoolType extends ObjectPool>
extends Closer
implements ObjectPool, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(AGConnPool.class);

	/**
	 * Delegates all methods to the wrapped conn except for close.
	 */
	// TODO: note, these objects are never closed for real, but they don't hold any resources
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
	
	public final ObjectPoolType delegate;
	
	public final AGConnFactory factory;
	
	public AGConnPool(ObjectPoolType delegate, AGConnFactory factory, AGConfig config) {
		this.delegate = delegate;
		closeLater(delegate);
		this.factory = factory;

		if (config.initialSize > 0) {
			List<AGRepositoryConnection> conns = new ArrayList<AGRepositoryConnection>(config.initialSize);
			try {
				for (int i = 0; i < config.initialSize; i++) {
					conns.add(borrowConnection());
				}
			} catch (RepositoryException e) {
				throw new RuntimeException(e);
			}
			// return them to the pool
			closeAll(conns);
		}
		if (config.shutdownHook) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					logger.debug("closing " + AGConnPool.this);
					Util.close(AGConnPool.this);
					logger.debug("closed " + AGConnPool.this);
				}
			});
		}
	}
	
	public static AGConnPool create(AGConnFactory fact, AGConfig config) {
		return new AGConnPool<GenericObjectPool>(new GenericObjectPool(fact, config), fact, config);
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
			return new AGRepositoryConnectionPooled((AGRepositoryConnection) delegate.borrowObject());
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
		delegate.invalidateObject(unwrap(obj));
	}
	
	@Override
	public void returnObject(Object obj) throws Exception {
		delegate.returnObject(unwrap(obj));
	}
	
	public Object unwrap(Object obj) {
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
	public String toString() {
		return "{" + super.toString()
		+ " active=" + getNumActive()
		+ " idle=" + getNumIdle()
		+ " delegate=" + delegate
		+ " factory=" + factory
		+ "}";
	}

}
