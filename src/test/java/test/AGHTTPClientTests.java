package test;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGResponseHandler;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Low-level HTTP client tests.
 */
public class AGHTTPClientTests {
    private AGHTTPClient client;
    private AuditingConnectionManager manager;
    private String serverUrl;

    @BeforeEach
    public void setUp() {
        serverUrl = AGAbstractTest.findServerUrl();
        manager = new AuditingConnectionManager(new PoolingHttpClientConnectionManager());
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
        manager.closeIdleConnections(0, SECONDS);
        Assert.assertEquals(connectionsBefore, manager.getConnectionCount());
    }

    private static final class NullStreamer extends AGResponseHandler {

        public NullStreamer() {
            super("who/cares/and/why/is/this/required");
        }

        @Override
        public void handleResponse(HttpResponse httpResponse, HttpUriRequest httpUriRequest) {
            // This streamer takes ownership of the connection,
            // so we must release it.
            EntityUtils.consumeQuietly(httpResponse.getEntity());
        }

        @Override
        public boolean releaseConnection() {
            // The handler takes ownership of the connection.
            return false;
        }
    }

    // A connection manager that tracks the number of unreleased connections.
    private static final class AuditingConnectionManager implements HttpClientConnectionManager {

        private final HttpClientConnectionManager wrapped;
        private int connectionCount = 0;

        public AuditingConnectionManager(final HttpClientConnectionManager wrapped) {
            this.wrapped = wrapped;
        }

        public int getConnectionCount() {
            return connectionCount;
        }

        @Override
        public ConnectionRequest requestConnection(HttpRoute route, Object state) {
            connectionCount++;
            return wrapped.requestConnection(route, state);
        }

        @Override
        public void releaseConnection(HttpClientConnection conn, Object newState, long validDuration, TimeUnit timeUnit) {
            wrapped.releaseConnection(conn, newState, validDuration, timeUnit);
            connectionCount--;
        }

        @Override
        public void connect(HttpClientConnection conn, HttpRoute route, int connectTimeout, HttpContext context) throws IOException {
            wrapped.connect(conn, route, connectTimeout, context);
        }

        @Override
        public void upgrade(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
            wrapped.upgrade(conn, route, context);
        }

        @Override
        public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
            wrapped.routeComplete(conn, route, context);
        }

        @Override
        public void closeIdleConnections(long idletime, TimeUnit timeUnit) {
            wrapped.closeIdleConnections(idletime, timeUnit);
            connectionCount = 0;
        }

        @Override
        public void closeExpiredConnections() {
            wrapped.closeExpiredConnections();
        }

        @Override
        public void shutdown() {
            wrapped.shutdown();
        }
    }
}
