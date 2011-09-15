/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import java.util.Map;

import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.util.Closer;

/**
 * A connection factory that connects based on properties
 * where the keys/names supported are specified by {@link Prop}.
 * 
 * @since v4.3.3
 */
public class AGConnPropFactory extends AGConnFactory {
	
	private final static Logger log = LoggerFactory.getLogger(AGConnPropFactory.class);
	
	private final Map<Prop, String> props;
	
	/**
	 * Property names to open a {@link AGRepositoryConnection}.
	 * 
	 * <p>TODO: {@link AGRepositoryConnection#setSessionLoadInitFile(boolean)}</p>
	 * <p>TODO: {@link AGRepositoryConnection#addSessionLoadScript(String)}</p>
	 */
	public enum Prop {
		
		/**
		 * @see AGServer#AGServer(String, String, String)
		 */
		url,
		
		/**
		 * @see AGServer#AGServer(String, String, String)
		 */
		username,
		
		/**
		 * @see AGServer#AGServer(String, String, String)
		 */
		password,
		
		/**
		 * Catalog name or no value for {@link AGServer#getRootCatalog()}
		 * @see AGServer#getCatalog(String)
		 */
		catalog,
		
		/**
		 * @see AGCatalog#openRepository(String)
		 */
		repository,
		
		/**
		 * Value must be one of {@link Session}.
		 */
		session,
		
		/**
		 * @see AGRepositoryConnection#setSessionLifetime(int)
		 */
		sessionLifetime
	}
	
	/**
	 * Property values for {@link Prop#session}.
	 */
	public enum Session {
		
		/**
		 * No dedicated session, and autoCommit is true
		 * (that is, {@link AGRepositoryConnection#setAutoCommit(boolean)} is not called).
		 */
		SHARED,
		
		/**
		 * Calls {@link AGRepositoryConnection#setAutoCommit(boolean)} with true.
		 */
		DEDICATED,
		
		/**
		 * Calls {@link AGRepositoryConnection#setAutoCommit(boolean)} with false.
		 */
		TX;
		
		static Session valueOfCaseInsensitive(String name) {
			Session s = Session.valueOf(name);
			if (s == null) {
				s = Session.valueOf(name.toUpperCase());
			}
			return s;
		}
	}
	
	public AGConnPropFactory(Map<Prop, String> props) throws RepositoryException {
		this.props = props;
	}
	
	private String prop(Prop p) {
		String value = props.get(p);
		if (value == null) {
			value = props.get(p);
		}
		return value;
	}
	
	@Override
	protected AGRepositoryConnection makeConnection() throws RepositoryException {
		AGServer server = closeLater( new AGServer(prop(Prop.url), prop(Prop.username), prop(Prop.password)) );
		AGCatalog cat;
		if (prop(Prop.catalog) != null) {
			cat = server.getCatalog(prop(Prop.catalog));
		} else {
			cat = server.getRootCatalog();
		}
		
		AGRepository repo = closeLater( cat.createRepository(prop(Prop.repository)) );
		repo.initialize();
		
		AGRepositoryConnection conn = closeLater( new AGRepositoryConnectionCloseup(this, closeLater( repo.getConnection())));
		activateConnection(conn);
		return conn;
	}
	
	@Override
	protected void activateConnection(AGRepositoryConnection conn) throws RepositoryException {
		if (props.containsKey(Prop.session.name())) {
			switch (Session.valueOfCaseInsensitive(prop(Prop.session))) {
			case SHARED:
				if (!conn.isAutoCommit()) {
					// it must have been set by the user, but restore it anyway
					// it is no longer actually SHARED but DEDICATED
					conn.setAutoCommit(true);
					log.debug("Dedicated (not shared) backend: " + conn.getHttpRepoClient().getRoot());
				}
				break;
			case DEDICATED:
				conn.setAutoCommit(true);
				log.debug("Dedicated backend: " + conn.getHttpRepoClient().getRoot());
				break;
			case TX:
				conn.setAutoCommit(false);
				log.debug("TX dedicated backend: " + conn.getHttpRepoClient().getRoot());
				break;
			default:
				throw new IllegalArgumentException("Unrecognized value for " + Prop.session + ": " + prop(Prop.session));
			}
		}
		
		if (prop(Prop.sessionLifetime) != null) {
			conn.setSessionLifetime(Integer.parseInt(prop(Prop.sessionLifetime)));
		}
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
			if (logger.isWarnEnabled())
				logger.warn("ignoring error with close: ", e);
		}
		closer.close(conn);
		closer.close(conn.getRepository());
		closer.close(conn.getRepository().getCatalog().getServer());
	}
	
}
