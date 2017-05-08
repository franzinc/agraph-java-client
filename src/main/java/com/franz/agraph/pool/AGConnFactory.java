/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.agraph.pool;

import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.pool.PoolableObjectFactory;
import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.util.Closer;

import java.io.Closeable;
import java.io.IOException;

/**
 * Adapts the {@link AGRepositoryConnection} API
 * to the commons-pool factory interface,
 * leaving creation of the connection (and configuration)
 * to a subclass, defined by users of this library.
 * 
 * @since v4.3.3
 */
public class AGConnFactory implements PoolableObjectFactory, Closeable {

	private final static Logger log = LoggerFactory.getLogger(AGConnFactory.class);
	
	private final AGConnConfig props;

	@SuppressWarnings("unused")
	private final AGPoolConfig poolProps;

	private final Closer closer = new Closer() {
		@Override
		protected void handleCloseException(Object o, Throwable e) {
			if (e.getCause() instanceof java.net.ConnectException && e.getCause().getMessage().equals("Connection refused")) {
				// squelch this (debug instead of warn) because it's common that the session has timed out
				log.debug("ignoring error with close (probably session timeout): " + o, e);
			} else {
				log.warn("ignoring error with close: " + o, e);
			}
		}
	};

	public AGConnFactory(AGConnConfig props) {
		this.props = props;
		this.poolProps = null;
	}

	public AGConnFactory(AGConnConfig props, AGPoolConfig poolProps) {
		this.props = props;
		this.poolProps = poolProps;
	}

	@Override
	public Object makeObject() throws Exception {
		return makeConnection();
	}
	
	private AGRepositoryConnection makeConnection() throws Exception {
		HttpConnectionManagerParams params = new HttpConnectionManagerParams();
		//params.setDefaultMaxConnectionsPerHost(Integer.MAX_VALUE);
		//params.setMaxTotalConnections(Integer.MAX_VALUE);
		//params.setConnectionTimeout((int) TimeUnit.SECONDS.toMillis(10));
		if (props.httpSocketTimeout != null) {
			params.setSoTimeout(props.httpSocketTimeout);
		}

		MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
		manager.setParams(params);
		AGHTTPClient httpClient = new AGHTTPClient(props.serverUrl, manager);
		AGServer server = new AGServer(props.username, props.password, httpClient);
		AGCatalog cat;
		if (props.catalog != null) {
			cat = server.getCatalog(props.catalog);
		} else {
			cat = server.getRootCatalog();
		}
		
		final AGRepository repo;
		if (!cat.hasRepository(props.repository)) {
			repo = createRepo(cat);
		} else {
			repo = new AGRepository(cat, props.repository);
		}
		repo.initialize();

		AGRepositoryConnection conn = closer.closeLater(
				new AGRepositoryConnectionCloseup(closer, closer.closeLater(repo.getConnection()), manager));
		if (props.sessionLifetime != null) {
			conn.setSessionLifetime(props.sessionLifetime);
		}
		activateObject(conn);
		return conn;
	}
	
	/**
	 * Synchronized and re-checks hasRepository so multiple
	 * do not try to create the repo at the same time.
	 */
	private synchronized AGRepository createRepo(AGCatalog cat) throws OpenRDFException {
		if (!cat.hasRepository(props.repository)) {
			return cat.createRepository(props.repository, true);
		} else {
			return new AGRepository(cat, props.repository);
		}
	}

	protected void activateConnection(AGRepositoryConnection conn) throws RepositoryException {
		// if autoCommit is false, then rollback to refresh this connection's view of the repository.
		if (!conn.isAutoCommit()) {
			conn.rollback();
		}
		
		switch (props.session) {
		case SHARED:
			// Typically a shared connection (url through the frontend port) but may be a dedicated
			// session if setAutoCommit() has been called on this connection.
			if (!conn.isAutoCommit()) {
				// it must have been set by the user, but restore it anyway
				// it is no longer actually SHARED but DEDICATED
				conn.setAutoCommit(true);
				log.debug("Dedicated (not shared) backend: " + conn.getHttpRepoClient().getRoot());
			}
			break;
		case DEDICATED:
			// Dedicated Session in autoCommit mode.
			// Ensure conn is a dedicated session, with autoCommit set to true.
			if (!conn.getHttpRepoClient().isDedicatedSession() || !conn.isAutoCommit()) {
				// forces conn to a dedicated session if not already.
				conn.setAutoCommit(true);
				log.debug("Dedicated backend: " + conn.getHttpRepoClient().getRoot());
			}
			break;
		case TX:
			// Dedicated Session not in autoCommit mode.
			if (conn.isAutoCommit()) {
				// forces conn to a dedicated session if not already.
				conn.setAutoCommit(false);
				log.debug("TX dedicated backend: " + conn.getHttpRepoClient().getRoot());
			}
			break;
		}
	}

	/**
	 * Calls {@link AGRepositoryConnection#rollback()} and
	 * resets {@link AGRepositoryConnection#setAutoCommit(boolean)}
	 * if it has changed.
	 */
	@Override
	public void activateObject(Object obj) throws Exception {
		activateConnection( (AGRepositoryConnection) obj);
	}
	
	@Override
	public void destroyObject(Object obj) throws Exception {
		closer.close((AGRepositoryConnection)obj);
	}
	
	@Override
	public void passivateObject(Object obj) throws Exception {
		// Do nothing when returning a connection to the pool.
		// Users can set TestOnReturn to true to force a rollback on
		// a connection when it is returned to the pool.
		//
		// Setting TestWhileIdle to true (and associated PoolProps) will
		// trigger a rollback when the connection is not being used.
		//
		// rollback() will always be called when a connection is borrowed
		// from the pool.
	}
	
	/**
	 * Calls {@link AGRepositoryConnection#size(org.eclipse.rdf4j.model.Resource...)}.
	 */
	@Override
	public boolean validateObject(Object obj) {
		AGRepositoryConnection conn = (AGRepositoryConnection) obj;
		try {
			// ping() only checks that the network is up, so we call size(),
			// which ensures the repo exists.
			AGHttpRepoClient client = conn.getHttpRepoClient();
			// Have the server perform a rollback as part of the size request.
			client.setSendRollbackHeader(true);
			conn.size();
			client.setSendRollbackHeader(false);
			return true;
		} catch (Exception e) {
			log.debug("validateObject " + obj, e);
			return false;
		}
	}

	/** Release resources. */
	@Override
	public void close() throws IOException {
		closer.close();
	}

	/**
	 * Delegates all methods to the wrapped conn except for close.
	 */
	class AGRepositoryConnectionCloseup extends AGRepositoryConnection {
		
		private final AGRepositoryConnection conn;
		private final Closer closer;
		private final MultiThreadedHttpConnectionManager manager;
		
		public AGRepositoryConnectionCloseup(Closer closer, AGRepositoryConnection conn,
											 MultiThreadedHttpConnectionManager manager) {
			super((AGRepository) conn.getRepository(), conn.getHttpRepoClient());
			this.closer = closer;
			this.conn = conn;
			this.manager = manager;
		}
		
		/**
		 * Closes the {@link AGRepositoryConnection}, {@link AGRepository}, {@link AGServer}, and {@link HttpConnectionManager}.
		 */
		@Override
		public void close() throws RepositoryException {
			AGRepositoryConnectionCloseup.super.close();
			closer.close(conn);
			closer.close(conn.getRepository());
			closer.close(conn.getRepository().getCatalog().getServer());
			closer.close(manager::shutdown);
		}

	}

}
