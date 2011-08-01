/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import java.util.NoSuchElementException;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.util.Closeable;
import com.franz.util.Closer;

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
 * @since v4.3.1
 */
public class AGConnPool <ObjectPoolType extends ObjectPool>
extends Closer
implements ObjectPool, Closeable {

	/**
	 * Delegates all methods to the wrapped conn execpt for close.
	 * 
	 */
	 // TODO: note, these objects are never closed for real, but they don't hold any resources
	class PoolConn extends AGRepositoryConnection {
		
		final AGRepositoryConnection conn;
		
		public PoolConn(AGRepositoryConnection conn) {
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

	public AGConnPool(ObjectPoolType delegate) {
		this.delegate = delegate;
		closeLater(delegate);
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
			return new PoolConn((AGRepositoryConnection) delegate.borrowObject());
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
		if (obj instanceof AGConnPool.PoolConn) {
			return ((AGConnPool.PoolConn)obj).conn;
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
	
}
