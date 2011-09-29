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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.util.Closer;

/**
 * Adapts the {@link AGRepositoryConnection} API
 * to the commons-pool factory interface,
 * leaving creation of the connection (and configuration)
 * to a subclass, defined by users of this library.
 * 
 * @since v4.3.3
 */
public class AGConnFactory
extends Closer
implements PoolableObjectFactory {

	private final static Logger log = LoggerFactory.getLogger(AGConnFactory.class);
	
	private final AGConnConfig props;
	
	public AGConnFactory(AGConnConfig props) throws RepositoryException {
		this.props = props;
	}
	
	@Override
	public Object makeObject() throws Exception {
		return makeConnection();
	}
	
	protected AGRepositoryConnection makeConnection() throws RepositoryException {
		AGServer server = closeLater( new AGServer(props.serverUrl, props.username, props.password) );
		AGCatalog cat;
		if (props.catalog != null) {
			cat = server.getCatalog(props.catalog);
		} else {
			cat = server.getRootCatalog();
		}
		
		AGRepository repo = closeLater( cat.createRepository(props.repository) );
		repo.initialize();
		
		AGRepositoryConnection conn = closeLater( new AGRepositoryConnectionCloseup(this, closeLater( repo.getConnection())));
		if (props.sessionLifetime != null) {
			conn.setSessionLifetime(props.sessionLifetime);
		}
		activateConnection(conn);
		return conn;
	}
	
	protected void activateConnection(AGRepositoryConnection conn) throws RepositoryException {
		switch (props.session) {
		case SHARED:
			if (!conn.isAutoCommit()) {
				// it must have been set by the user, but restore it anyway
				// it is no longer actually SHARED but DEDICATED
				conn.setAutoCommit(true);
				if (log.isDebugEnabled())
					log.debug("Dedicated (not shared) backend: " + conn.getHttpRepoClient().getRoot());
			}
			break;
		case DEDICATED:
			if (!conn.isAutoCommit()) {
				conn.setAutoCommit(true);
				if (log.isDebugEnabled())
					log.debug("Dedicated backend: " + conn.getHttpRepoClient().getRoot());
			}
			break;
		case TX:
			if (conn.isAutoCommit()) {
				conn.setAutoCommit(false);
				if (log.isDebugEnabled())
					log.debug("TX dedicated backend: " + conn.getHttpRepoClient().getRoot());
			}
			break;
		}
	}
	
	@Override
	public void activateObject(Object obj) throws Exception {
		activateConnection( (AGRepositoryConnection) obj);
	}
	
	@Override
	public void destroyObject(Object obj) throws Exception {
		close(obj);
	}
	
	@Override
	public void passivateObject(Object obj) throws Exception {
		AGRepositoryConnection conn = (AGRepositoryConnection) obj;
		if (!conn.isAutoCommit() && conn.getRepository().isWritable()) {
			conn.rollback();
			// TODO MH: any reason to do this?
			//conn.setAutoCommit(true);
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
	
	/**
	 * Delegates all methods to the wrapped conn except for close.
	 */
	class AGRepositoryConnectionCloseup extends AGRepositoryConnection {
		
		final AGRepositoryConnection conn;
		private final Closer closer;
		
		public AGRepositoryConnectionCloseup(Closer closer, AGRepositoryConnection conn) {
			super((AGRepository) conn.getRepository(), conn.getHttpRepoClient());
			this.closer = closer;
			this.conn = conn;
		}
		
		/**
		 * Closes the {@link AGRepositoryConnection}, {@link AGRepository}, and {@link AGServer}.
		 */
		@Override
		public void close() throws RepositoryException {
			try {
				super.close();
			} catch (Exception e) {
				// it is likely the session has timed out, so only log to debug
				logger.debug("ignoring error with close", e);
			}
			closer.close(conn);
			closer.close(conn.getRepository());
			closer.close(conn.getRepository().getCatalog().getServer());
		}
		
	}

}
