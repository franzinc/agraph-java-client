/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGErrorHandler;
import com.franz.agraph.http.handler.AGMethodRetryHandler;
import com.franz.agraph.http.handler.AGResponseHandler;
import com.franz.agraph.http.handler.AGStringHandler;
import com.franz.agraph.http.handler.AGTQRHandler;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.franz.agraph.http.AGProtocol.AMOUNT_PARAM_NAME;
import static com.franz.agraph.http.AGProtocol.OVERRIDE_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.ACCEPT_PARAM_NAME;

/**
 * Class responsible for handling HTTP connections.
 * <p>
 * Uses an unlimited pool of connections to allow safe, concurrent access.</p>
 * <p>
 * Also contains methods for accessing AG services that operate above
 * the repository level - such as managing repositories.</p>
 */
public class AGHTTPClient implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(AGHTTPClient.class);

    /**
     * Number of times an HTTP GET request is automatically retried,
     * not counting the initial (failed) attempt, in case of errors
     * caused by a connection timeout.
     *
     * Disabling this by setting it to 0 will expose "Connection
     * reset" and "HTTP 408 Client timeout" errors. A value of 1
     * should enough to hide these supposedly harmless errors if the
     * network is otherwise stable.
     */
    public static final String PROP_HTTP_NUM_RETRIES = "com.franz.agraph.http.numRetries";
    private static final int DEFAULT_HTTP_NUM_RETRIES = 1;

    private final String serverURL;
    private final HttpClient httpClient;

    private AuthScope authScope;
    private String masqueradeAsUser;

    private boolean isClosed = false;

    private HttpConnectionManager mManager = null;
    private final AGMethodRetryHandler retryHandler = new AGMethodRetryHandler();

    private final int httpNumRetries;

    private AGHTTPClient(String serverURL, HttpClient client) {
        this.serverURL = serverURL.replaceAll("/$", "");
        this.httpClient = client;
        this.httpNumRetries = Integer.parseInt(
            System.getProperty(PROP_HTTP_NUM_RETRIES, "" + DEFAULT_HTTP_NUM_RETRIES));
        if (logger.isDebugEnabled()) {
            logger.debug("connect: " + serverURL + " " + client);
        }
    }

    public AGHTTPClient(String serverURL) {
        this(serverURL, (MultiThreadedHttpConnectionManager) null);
    }

    public AGHTTPClient(String serverURL, HttpConnectionManager manager) {
        this(serverURL, new HttpClient(manager == null ? createManager() : manager));
        if (manager == null) {
            mManager = this.getHttpClient().getHttpConnectionManager();
        }
    }

    // This is used for clients created by the connection pool.
    public AGHTTPClient(String serverURL, HttpConnectionManagerParams params) {
        this(serverURL, createManager(params));
    }

    private static HttpConnectionManager createManager() {
        return createManager(null);
    }

    private static HttpConnectionManager createManager(HttpConnectionManagerParams params) {
        // Use MultiThreadedHttpConnectionManager to allow concurrent access
        // on HttpClient
        final MultiThreadedHttpConnectionManager manager =
                new MultiThreadedHttpConnectionManager();

        if (params == null) {
            params = new HttpConnectionManagerParams();
        }

        // Allow "unlimited" concurrent connections to the same host (default is 2)
        params.setDefaultMaxConnectionsPerHost(Integer.MAX_VALUE);
        params.setMaxTotalConnections(Integer.MAX_VALUE);
        manager.setParams(params);
        return manager;
    }

    @Override
    public String toString() {
        return "{" + super.toString()
                + " " + serverURL
                + " " + httpClient
                + "}";
    }

    public String getServerURL() {
        return serverURL;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void post(String url, Header[] headers, NameValuePair[] params,
                     RequestEntity requestEntity, AGResponseHandler handler) throws AGHttpException {
        PostMethod post = new PostMethod(url);
        setDoAuthentication(post);
        for (Header header : headers) {
            post.addRequestHeader(header);
        }

        if (System.getProperty("com.franz.agraph.http.useGzip", "true").equals("true")) {
            post.addRequestHeader("Accept-encoding", "gzip");
        }
        // bug21953. Only write params to body if content-type is appropriate.
        Header contentType = post.getRequestHeader("Content-Type");
        if (requestEntity == null && (contentType == null
                || contentType.getValue().contains(Protocol.FORM_MIME_TYPE))) {
            post.setRequestBody(params);
        } else {
            post.setQueryString(params);
            post.setRequestEntity(requestEntity);
        }
        executeMethod(url, post, handler);
    }

    /**
     * Checks whether the specified status code is in the 2xx-range, indicating a
     * successful request.
     *
     * @return <tt>true</tt> if the status code is in the 2xx range
     */
    private boolean is2xx(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    public void get(String url, Header[] headers, NameValuePair[] params,
                    AGResponseHandler handler) throws AGHttpException {
        int numTries = httpNumRetries + 1; // Always make at least one attempt
        for (int i = 0; i < numTries; i++) {
            GetMethod get = new GetMethod(url);
            setDoAuthentication(get);
            if (headers != null) {
                for (Header header : headers) {
                    get.addRequestHeader(header);
                }
            }
            if (System.getProperty("com.franz.agraph.http.useGzip", "true").equals("true")) {
                get.addRequestHeader("Accept-encoding", "gzip");
            }
            if (params != null) {
                get.setQueryString(params);
            }
            boolean mightRetry = (i < numTries - 1);
            if (executeMethod(url, get, handler, mightRetry) == ExecuteResult.SUCCESS) {
                return;
            }
        }
        // We should never get here:
        throw new AGHttpException("GET request failed (unreachable)");
    }

    public void delete(String url, Header[] headers, NameValuePair[] params, AGResponseHandler handler)
            throws AGHttpException {
        DeleteMethod delete = new DeleteMethod(url);
        setDoAuthentication(delete);
        if (headers != null) {
            for (Header header : headers) {
                delete.addRequestHeader(header);
            }
        }
        if (params != null) {
            delete.setQueryString(params);
        }
        executeMethod(url, delete, handler);
    }

    public void put(String url, Header[] headers, NameValuePair[] params, RequestEntity requestEntity, AGResponseHandler handler) throws AGHttpException {
        PutMethod put = new PutMethod(url);
        setDoAuthentication(put);
        if (headers != null) {
            for (Header header : headers) {
                put.addRequestHeader(header);
            }
        }
        if (params != null) {
            put.setQueryString(params);
        }
        if (requestEntity != null) {
            put.setRequestEntity(requestEntity);
        }
        executeMethod(url, put, handler);
    }

    private void executeMethod(String url,
                               HttpMethod method,
                               AGResponseHandler handler) {
      executeMethod(url, method, handler, false);
    }

    private enum ExecuteResult { SUCCESS, RETRY };

    /**
     * Run the HTTP request given by the method.
     *
     * @param method of the HTTP request
     * @param handler in case of 200 response status, it will be called on the method
     *        (note that it will NOT be called for other 2xx success status responses,
     *        which might be debatable)
     * @param returnRetryOn408 if true and server replied 408, then RETRY is returned
     *
     * @return either SUCCESS (for 2xx response), RETRY (for 408 response + returnRetryOn408),
     *         or throws an AGHttpException
     */
    private ExecuteResult executeMethod(String url,
                                        HttpMethod method,
                                        AGResponseHandler handler,
                                        boolean returnRetryOn408) throws AGHttpException {
        // A note about retrying requests:
        // a difficuty with HttpMethod is that once the request is attempted, you can't
        // reuse it for a second attempt. This method is unable to do the retry itself,
        // therefore returns ExecuteResult.RETRY.

        // This retry handler takes care of retrying the HTTP request in case of
        // connection problems. It does not deal with retrying in case of HTTP error codes.
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);

        try {
            int httpCode = getHttpClient().executeMethod(method);
            if (httpCode == HttpURLConnection.HTTP_OK) {
                if (handler != null) {
                    handler.handleResponse(method);
                }
                return ExecuteResult.SUCCESS;
            } else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new AGHttpException(new UnauthorizedException());
            } else if (returnRetryOn408 && (httpCode == HttpURLConnection.HTTP_CLIENT_TIMEOUT)) {
                // HTTP 408 supposedly happens if AG is busy receiving our request,
                // but before the whole request is received a read timeout occurs.
                // The problem cause is the idle time before sending the request,
                // not the request itself.
                return ExecuteResult.RETRY;
            } else if (is2xx(httpCode)) {
                return ExecuteResult.SUCCESS;
            } else {
                AGErrorHandler errHandler = new AGErrorHandler();
                errHandler.handleResponse(method);
                throw errHandler.getResult();
            }
        } catch (IOException e) {
            throw new AGHttpException(e);
        } finally {
            if (handler == null || handler.releaseConnection()) {
                releaseConnection(method);
            }
        }
    }

    /*-------------------------*
     * General utility methods *
     *-------------------------*/

    /**
     * Set the username and password for authentication with the remote server.
     *
     * @param username the username
     * @param password the password
     */
    public void setUsernameAndPassword(String username, String password) {

        if (username != null && password != null) {
            logger.debug("Setting username '{}' and password for server at {}.", username, serverURL);
            try {
                URL server = new URL(serverURL);
                authScope = new AuthScope(server.getHost(), AuthScope.ANY_PORT);
                httpClient.getState().setCredentials(authScope,
                        new UsernamePasswordCredentials(username, password));
                httpClient.getParams().setAuthenticationPreemptive(true);
            } catch (MalformedURLException e) {
                logger.warn("Unable to set username and password for malformed URL " + serverURL, e);
            }
        } else {
            authScope = null;
            httpClient.getState().clearCredentials();
            httpClient.getParams().setAuthenticationPreemptive(false);
        }
    }

    /**
     * Sets the AG user for X-Masquerade-As-User requests.
     * <p>
     * For AG superusers only.  This allows AG superusers to run requests as
     * another user in a dedicated session.</p>
     *
     * @param user the user for X-Masquerade-As-User requests
     */
    public void setMasqueradeAsUser(String user) {
        masqueradeAsUser = user;
    }

    protected final void setDoAuthentication(HttpMethod method) {
        if (authScope != null
                && httpClient.getState().getCredentials(authScope) != null) {
            method.setDoAuthentication(true);
        } else {
            //method.setDoAuthentication(false);
        }
        if (masqueradeAsUser != null) {
            method.addRequestHeader(new Header("x-masquerade-as-user", masqueradeAsUser));
        }
        // TODO probably doesn't belong here, need another method that
        // HttpMethod objects pass through.
        method.addRequestHeader(new Header("Connection", "keep-alive"));
    }

    protected final void releaseConnection(HttpMethod method) {
        try {
            // Read the entire response body to enable the reuse of the
            // connection
            InputStream responseStream = method.getResponseBodyAsStream();
            if (responseStream != null) {
                while (responseStream.read() >= 0) {
                    // do nothing
                }
            }

            method.releaseConnection();
        } catch (IOException e) {
            logger.warn("I/O error upon releasing connection", e);
        }
    }

    /*-----------*
     * Services  *
     *-----------*/

    public void putCatalog(String catalogURL) throws AGHttpException {
        if (logger.isDebugEnabled()) {
            logger.debug("putCatalog: " + catalogURL);
        }
        Header[] headers = new Header[0];
        NameValuePair[] params = new NameValuePair[0];
        put(catalogURL, headers, params, null, null);
    }

    public void deleteCatalog(String catalogURL) throws AGHttpException {
        Header[] headers = new Header[0];
        NameValuePair[] params = new NameValuePair[0];
        delete(catalogURL, headers, params, null);
    }

    public void putRepository(String repositoryURL) throws AGHttpException {
        if (logger.isDebugEnabled()) {
            logger.debug("putRepository: " + repositoryURL);
        }
        Header[] headers = new Header[0];
        NameValuePair[] params = {new NameValuePair(OVERRIDE_PARAM_NAME, "false")};
        put(repositoryURL, headers, params, null, null);
    }

    public void deleteRepository(String repositoryURL) throws AGHttpException {
        Header[] headers = new Header[0];
        NameValuePair[] params = new NameValuePair[0];
        delete(repositoryURL, headers, params, null);
    }

    public TupleQueryResult getTupleQueryResult(String url) throws AGHttpException {
        Header[] headers = {new Header(ACCEPT_PARAM_NAME, TupleQueryResultFormat.SPARQL.getDefaultMIMEType())};
        NameValuePair[] params = new NameValuePair[0];
        TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
        // TODO: avoid using AGValueFactory(null)
        AGTQRHandler handler = new AGTQRHandler(TupleQueryResultFormat.SPARQL, builder, new AGValueFactory(null), false);
        get(url, headers, params, handler);
        return builder.getQueryResult();
    }

    public String[] getBlankNodes(String repositoryURL, int amount)
            throws AGHttpException {
        String url = AGProtocol.getBlankNodesURL(repositoryURL);
        Header[] headers = new Header[0];
        NameValuePair[] data = {new NameValuePair(AMOUNT_PARAM_NAME, Integer
                .toString(amount))};

        AGStringHandler handler = new AGStringHandler();
        post(url, headers, data, null, handler);
        return handler.getResult().split("\n");
    }

    public String getString(String url) throws AGHttpException {
        Header[] headers = new Header[0];
        NameValuePair[] data = {};
        AGStringHandler handler = new AGStringHandler();
        get(url, headers, data, handler);
        return handler.getResult();
    }

    public String[] getStringArray(String url) throws AGHttpException {
        String result = getString(url);
        if (result.equals("")) {
            return new String[0];
        } else {
            return result.split("\n");
        }
    }

    public List<String> getListOfStrings(String url) throws AGHttpException {
        return Arrays.asList(getStringArray(url));
    }

    public String openSession(String spec, boolean autocommit) throws AGHttpException {
        String url = AGProtocol.getSessionURL(serverURL);
        Header[] headers = new Header[0];
        NameValuePair[] data = {new NameValuePair("store", spec),
                new NameValuePair(AGProtocol.AUTOCOMMIT_PARAM_NAME,
                        Boolean.toString(autocommit)),
                new NameValuePair(AGProtocol.LIFETIME_PARAM_NAME,
                        Long.toString(3600))}; // TODO have some kind of policy for this
        AGStringHandler handler = new AGStringHandler();
        post(url, headers, data, null, handler);
        return handler.getResult();
    }

    @Override
    public void close() {
        logger.debug("close: " + serverURL + " " + mManager);
        if (mManager instanceof MultiThreadedHttpConnectionManager) {
            ((MultiThreadedHttpConnectionManager) mManager).shutdown();
            mManager = null;
        }
        isClosed = true;
    }

    boolean isClosed() {
        return isClosed;
    }

    public String[] generateURIs(String repositoryURL, String namespace,
                                 int amount) throws AGHttpException {
        String url = repositoryURL + "/encodedIds";
        Header[] headers = new Header[0];
        NameValuePair[] data = {new NameValuePair("prefix", namespace),
                new NameValuePair(AMOUNT_PARAM_NAME, Integer.toString(amount))};
        AGStringHandler handler = new AGStringHandler();
        post(url, headers, data, null, handler);
        return handler.getResult().split("\n");
    }
}
