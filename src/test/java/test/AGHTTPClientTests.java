package test;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGResponseHandler;
import org.apache.commons.httpclient.ConnectionPoolTimeoutException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Low-level HTTP client tests.
 */
public class AGHTTPClientTests {
    private AGHTTPClient client;
    private AuditingConnectionManager manager;
    private String serverUrl;

    @Before
    public void setUp() {
        serverUrl = AGAbstractTest.findServerUrl();
        manager = new AuditingConnectionManager(new MultiThreadedHttpConnectionManager());
        client = new AGHTTPClient(serverUrl, manager);
        client.setUsernameAndPassword(
                AGAbstractTest.username(),
                AGAbstractTest.password());
    }

    @Test
    public void testConnectionReleasedOnErrorWhenStreaming() {
        final AGResponseHandler handler = new NullStreamer();
        final int connectionsBefore = manager.getConnectionCount();
        try {
            client.get(serverUrl + "/PleaseRespondWith404", null, null, null);
        } catch (AGHttpException e) {
            // ignore
        }
        manager.closeIdleConnections(0);
        Assert.assertEquals(connectionsBefore, manager.getConnectionCount());
    }

    private static final class NullStreamer extends AGResponseHandler {
        public NullStreamer() {
            super("who/cares/and/why/is/this/required");
        }
        @Override
        public void handleResponse(HttpMethod method) {
            // This streamer takes ownership of the connection,
            // so we must release it.
            method.releaseConnection();
        }
        @Override
        public boolean releaseConnection() {
            // The handler takes ownership of the connection.
            return false;
        }
    }

    // A connection manager that tracks the number of unreleased connections.
    private static final class AuditingConnectionManager implements HttpConnectionManager {
        private final HttpConnectionManager wrapped;
        private int connectionCount = 0;

        public AuditingConnectionManager(final HttpConnectionManager wrapped) {
            this.wrapped = wrapped;
        }

        public int getConnectionCount() {
            return connectionCount;
        }

        // We need to ensure that connections will be returned here,
        // not directly to the wrapped manager.
        private HttpConnection fixConnection(HttpConnection conn) {
            conn.setHttpConnectionManager(this);
            return conn;
        }

        @Override
        public HttpConnection getConnection(HostConfiguration hostConfiguration) {
            connectionCount++;
            return fixConnection(wrapped.getConnection(hostConfiguration));
        }

        @Override
        public HttpConnection getConnection(HostConfiguration hostConfiguration,
                                            long timeout) throws HttpException {
            connectionCount++;
            return fixConnection(wrapped.getConnection(hostConfiguration, timeout));
        }

        @Override
        public HttpConnection getConnectionWithTimeout(
                HostConfiguration hostConfiguration,
                long timeout) throws ConnectionPoolTimeoutException {
            connectionCount++;
            return fixConnection(wrapped.getConnectionWithTimeout(hostConfiguration, timeout));
        }

        @Override
        public void releaseConnection(HttpConnection conn) {
            connectionCount--;
            wrapped.releaseConnection(conn);
        }

        @Override
        public void closeIdleConnections(long idleTimeout) {
            wrapped.closeIdleConnections(idleTimeout);
        }

        @Override
        public HttpConnectionManagerParams getParams() {
            return wrapped.getParams();
        }

        @Override
        public void setParams(HttpConnectionManagerParams params) {
            wrapped.setParams(params);
        }
    }
}
