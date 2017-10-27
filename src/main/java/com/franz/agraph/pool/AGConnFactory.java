/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.agraph.pool;

import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
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

import java.net.ConnectException;

/**
 * Adapts the {@link AGRepositoryConnection} API
 * to the commons-pool factory interface,
 * leaving creation of the connection (and configuration)
 * to a subclass, defined by users of this library.
 * 
 * @since v4.3.3
 */
public class AGConnFactory extends BasePooledObjectFactory<AGRepositoryConnection>
		implements PooledObjectFactory<AGRepositoryConnection> {

	private final static Logger log = LoggerFactory.getLogger(AGConnFactory.class);
	
	private final AGConnConfig props;

	@SuppressWarnings("unused")
	private final AGPoolConfig poolProps;

	public AGConnFactory(AGConnConfig props) {
		this.props = props;
		this.poolProps = null;
	}

	public AGConnFactory(AGConnConfig props, AGPoolConfig poolProps) {
		this.props = props;
		this.poolProps = poolProps;
	}

	@Override
	public AGRepositoryConnection create() throws Exception {
		HttpClientParams params = new HttpClientParams();
		if (props.httpSocketTimeout != null) {
			params.setSoTimeout(props.httpSocketTimeout);
		}
		AGHTTPClient httpClient = new AGHTTPClient(props.serverUrl, params);
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
		AGHttpRepoClient repoClient = new AGHttpRepoClient(
				repo, httpClient, repo.getRepositoryURL(), null);
		AGRepositoryConnection conn = new AGRepositoryConnection(repo, repoClient);
		if (props.sessionLifetime != null) {
			conn.setSessionLifetime(props.sessionLifetime);
		}
		return conn;
	}

	@Override
	public PooledObject<AGRepositoryConnection> wrap(AGRepositoryConnection conn) {
		return new DefaultPooledObject<>(conn);
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

	@Override
	public void activateObject(PooledObject<AGRepositoryConnection> pooled)
			throws RepositoryException {
		final AGRepositoryConnection conn = pooled.getObject();
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

	// Do nothing when returning a connection to the pool.
	// Users can set TestOnReturn to true to force a rollback on
	// a connection when it is returned to the pool.
	//
	// Setting TestWhileIdle to true (and associated PoolProps) will
	// trigger a rollback when the connection is not being used.
	//
	// rollback() will always be called when a connection is borrowed
	// from the pool.

	/**
	 * Calls {@link AGRepositoryConnection#size(org.eclipse.rdf4j.model.Resource...)}.
	 */
	@Override
	public boolean validateObject(PooledObject<AGRepositoryConnection> pooled) {
		final AGRepositoryConnection conn = pooled.getObject();
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
			log.debug("validateObject " + conn, e);
			return false;
		}
	}

	@Override
	public void destroyObject(PooledObject<AGRepositoryConnection> pooled) {
		final AGRepositoryConnection conn = pooled.getObject();
		// Make 'close' really shutdown the connection.
		conn.setPool(null);
		try {
			conn.close();
		} catch (Exception e) {
			final Throwable cause = e.getCause();
			if (cause instanceof ConnectException &&
					cause.getMessage().equals("Connection refused")) {
				// squelch this because it's common that the session has timed out
				log.debug("ignoring close error (probably session timeout): " + conn, e);
			} else {
				throw e;
			}
		}
		conn.getRepository().shutDown();
		conn.getHttpRepoClient().getHTTPClient().close();
		conn.getRepository().getCatalog().getServer().close();
	}
}
