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
import com.franz.util.Closeable;
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
	
	protected AGRepositoryConnection makeConnection() throws Exception {
		AGServer server = closeLater( new AGServer(props.serverUrl, props.username, props.password) );
		AGCatalog cat;
		if (props.catalog != null) {
			cat = server.getCatalog(props.catalog);
		} else {
			cat = server.getRootCatalog();
		}
		
		final AGRepository repo;
		if (!cat.hasRepository(props.repository)) {
			repo = closeLater( createRepo(cat));
		} else {
			repo = closeLater( new AGRepository(cat, props.repository));
		}
		repo.initialize();
		
		AGRepositoryConnection conn = closeLater( new AGRepositoryConnectionCloseup(this, closeLater( repo.getConnection())));
		if (props.sessionLifetime != null) {
			conn.setSessionLifetime(props.sessionLifetime);
		}
		activateConnection(conn);
		return conn;
	}
	
	/**
	 * Synchronized and re-checks hasRepository so multiple
	 * do not try to create the repo at the same time.
	 */
	private synchronized AGRepository createRepo(AGCatalog cat) throws Exception {
		if (!cat.hasRepository(props.repository)) {
			return cat.createRepository(props.repository, true);
		} else {
			return new AGRepository(cat, props.repository);
		}
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
	
	/**
	 * Calls {@link AGRepositoryConnection#size(org.openrdf.model.Resource...)}.
	 */
	@Override
	public boolean validateObject(Object obj) {
		AGRepositoryConnection conn = (AGRepositoryConnection) obj;
		try {
			// ping only checks the network is up
			// size also ensures the repo exists
			conn.size();
			return true;
		} catch (Exception e) {
			log.debug("validateObject " + obj, e);
			return false;
		}
	}

	@Override
	public <Obj extends Object> Obj handleCloseException(Obj o, Throwable e) {
		if (e.getCause() instanceof java.net.ConnectException && e.getCause().getMessage().equals("Connection refused")) {
			// squelch this (debug instead of warn) because it's common that the session has timed out
			log.debug("ignoring error with close (probably session timeout): " + o, e);
		} else {
			log.warn("ignoring error with close: " + o, e);
		}
		return o;
	}
	
	/**
	 * Delegates all methods to the wrapped conn except for close.
	 */
	class AGRepositoryConnectionCloseup extends AGRepositoryConnection {
		
		private final AGRepositoryConnection conn;
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
			closer.close(new Closeable() {
				public void close() throws Exception {
					AGRepositoryConnectionCloseup.super.close();
				}
			});
			closer.close(conn);
			closer.close(conn.getRepository());
			closer.close(conn.getRepository().getCatalog().getServer());
		}
		
	}

}
