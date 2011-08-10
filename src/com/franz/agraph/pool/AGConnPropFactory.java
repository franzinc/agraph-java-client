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
import com.franz.util.Util;

/**
 * A connection factory that connects based on properties
 * where the keys/names supported are specified by {@link Prop}.
 * 
 * @since v4.3.1
 */
public class AGConnPropFactory extends AGConnFactory {

    private final static Logger log = LoggerFactory.getLogger(AGConnPropFactory.class);

	private final Map<String, String> props;
	
	/**
	 * Property names.
	 * TODO: {@link AGRepositoryConnection#setSessionLoadInitFile(boolean)}
	 * TODO: {@link AGRepositoryConnection#addSessionLoadScript(String)}
	 */
	public enum Prop {
		
		/**
		 * @see AGServer#AGServer(String, String, String)
		 */
		URL,
		
		/**
		 * @see AGServer#AGServer(String, String, String)
		 */
		USERNAME,
		
		/**
		 * @see AGServer#AGServer(String, String, String)
		 */
		PASSWORD,
		
		/**
		 * Catalog name or, no value for {@link AGServer#getRootCatalog()}
		 * @see AGServer#getCatalog(String)
		 */
		CATALOG,
		
		/**
		 * @see AGCatalog#openRepository(String)
		 */
		REPOSITORY,
		
		/**
		 * Value must be one of {@link Session}.
		 */
		SESSION,
		
		/**
		 * @see AGRepositoryConnection#setSessionLifetime(int)
		 */
		SESSION_LIFETIME
	}

	/**
	 * Property values for {@link Prop#SESSION}.
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
		TX
	}
	
	public AGConnPropFactory(Map<String, String> props) throws RepositoryException {
		this.props = props;
	}
	
	private String prop(Prop p) {
		return props.get(p.name());
	}

	@Override
	protected AGRepositoryConnection makeConnection() throws RepositoryException {
	    AGServer server = closeLater( new AGServer(prop(Prop.URL), prop(Prop.USERNAME), prop(Prop.PASSWORD)) );
	    AGCatalog cat;
	    if (prop(Prop.CATALOG) != null) {
	        cat = server.getCatalog(prop(Prop.CATALOG));
	    } else {
	        cat = server.getRootCatalog();
	    }
	    
	    AGRepository repo = closeLater( cat.createRepository(prop(Prop.REPOSITORY)) );
	    repo.initialize();
		
		AGRepositoryConnection conn = closeLater( new AGRepositoryConnectionCloseup( repo.getConnection()));
		if (props.containsKey(Prop.SESSION.name())) {
			switch (Session.valueOf(prop(Prop.SESSION))) {
			case SHARED:
			    break;
			case DEDICATED:
				conn.setAutoCommit(true);
				log.debug("Dedicated backend: " + conn.getHttpRepoClient().getRoot());
				break;
			case TX:
				conn.setAutoCommit(true);
                log.debug("TX dedicated backend: " + conn.getHttpRepoClient().getRoot());
				break;
			default:
			    throw new IllegalArgumentException("Unrecognized value for " + Prop.SESSION + ": " + prop(Prop.SESSION));
			}
		}

		if (prop(Prop.SESSION_LIFETIME) != null) {
			conn.setSessionLifetime(Integer.parseInt(prop(Prop.SESSION_LIFETIME)));
		}
		return conn;
	}

}

/**
 * Delegates all methods to the wrapped conn except for close.
 */
class AGRepositoryConnectionCloseup extends AGRepositoryConnection {
    
    final AGRepositoryConnection conn;
    
    public AGRepositoryConnectionCloseup(AGRepositoryConnection conn) {
        super((AGRepository) conn.getRepository(), conn.getHttpRepoClient());
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
                logger.warn("ignoring error with close:" + e);
        }
        Util.close(conn.getRepository());
        Util.close(conn.getRepository().getCatalog().getServer());
    }

}
