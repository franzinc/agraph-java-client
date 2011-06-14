/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import org.apache.commons.pool.PoolableObjectFactory;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.util.Closer;

/**
 * Adapts the {@link AGRepositoryConnection} API
 * to the commons-pool factory interface,
 * leaving creation of the connection (and configuration)
 * to a subclass, defined by users of this library.
 * 
 * @since v4.3
 */
public abstract class AGConnFactory
extends Closer
implements PoolableObjectFactory {

	@Override
	public Object makeObject() throws Exception {
		AGRepositoryConnection conn = makeConnection();
		return conn;
	}

	protected abstract AGRepositoryConnection makeConnection() throws RepositoryException;

	@Override
	public void activateObject(Object obj) throws Exception {
		AGRepositoryConnection conn = (AGRepositoryConnection) obj;
		conn.rollback();
	}

	@Override
	public void destroyObject(Object obj) throws Exception {
		close(obj);
	}

	@Override
	public void passivateObject(Object obj) throws Exception {
		AGRepositoryConnection conn = (AGRepositoryConnection) obj;
		if (conn.isAutoCommit()) {
			conn.rollback();
		}
	}

	@Override
	public boolean validateObject(Object obj) {
		AGRepositoryConnection conn = (AGRepositoryConnection) obj;
		try {
			conn.ping();
			return true;
		} catch (RepositoryException e) {
			return false;
		}
	}
	
}
