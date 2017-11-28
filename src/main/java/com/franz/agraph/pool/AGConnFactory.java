/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.pool;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public AGConnFactory(AGConnConfig props) {
        this.props = props;
    }

    @Override
    public AGRepositoryConnection create() throws Exception {
        final HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        if (props.httpSocketTimeout != null) {
            params.setSoTimeout(props.httpSocketTimeout);
        }
        AGHTTPClient httpClient = new AGHTTPClient(props.serverUrl, params);
        final AGServer server = new AGServer(props.username, props.password, httpClient);
        final AGCatalog catalog;
        if (props.catalog != null) {
            catalog = server.getCatalog(props.catalog);
        } else {
            catalog = server.getRootCatalog();
        }

        final AGRepository repo;
        if (!catalog.hasRepository(props.repository)) {
            // This avoids multiple connections sending create requests
            // at the same time.
            synchronized (this) {
                repo = catalog.createRepository(props.repository, false);
            }
        } else {
            // Create directly to skip a redundant check
            repo = new AGRepository(catalog, props.repository);
            repo.initialize();
        }
        AGRepositoryConnection conn = repo.getConnection();
        if (props.sessionLifetime != null) {
            conn.setSessionLifetime(props.sessionLifetime);
        }
        return conn;
    }

    @Override
    public PooledObject<AGRepositoryConnection> wrap(AGRepositoryConnection conn) {
        return new DefaultPooledObject<>(conn);
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
                    log.debug("Dedicated (not shared) backend: " + conn.prepareHttpRepoClient().getRoot());
                }
                break;
            case DEDICATED:
                // Dedicated Session in autoCommit mode.
                // Ensure conn is a dedicated session, with autoCommit set to true.
                if (!conn.prepareHttpRepoClient().isDedicatedSession() || !conn.isAutoCommit()) {
                    // forces conn to a dedicated session if not already.
                    conn.setAutoCommit(true);
                    log.debug("Dedicated backend: " + conn.prepareHttpRepoClient().getRoot());
                }
                break;
            case TX:
                // Dedicated Session not in autoCommit mode.
                if (conn.isAutoCommit()) {
                    // forces conn to a dedicated session if not already.
                    conn.setAutoCommit(false);
                    log.debug("TX dedicated backend: " + conn.prepareHttpRepoClient().getRoot());
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
            AGHttpRepoClient client = conn.prepareHttpRepoClient();
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
            if (cause instanceof ConnectException
                    && cause.getMessage().equals("Connection refused")) {
                // squelch this because it's common that the session has timed out
                log.debug("ignoring close error (probably session timeout): " + conn, e);
            } else {
                throw e;
            }
        }
        // Each pooled connection has dedicated server and repository objects.
        conn.getRepository().shutDown();
        conn.getServer().close();
    }
}
