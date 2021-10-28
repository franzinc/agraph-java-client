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
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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

    private String masqueradeAsUser;

    private boolean isClosed = false;

    private final HttpClientConnectionManager mManager;
    private final AGMethodRetryHandler retryHandler = new AGMethodRetryHandler();

    private final int httpNumRetries;

    private CredentialsProvider credsProvider;
    private AuthCache authCache;

    public AGHTTPClient(String serverURL, HttpClientConnectionManager manager,
                        SocketConfig socketConfig) {
        this.serverURL = serverURL.replaceAll("/$", "");
        this.mManager = manager != null ? manager : createManager();
        HttpClientBuilder httpClientBuilder = HttpClients.custom()
            .setConnectionManager(mManager)
            .setRetryHandler(retryHandler);
        if (socketConfig != null) {
            httpClientBuilder.setDefaultSocketConfig(socketConfig);
        }
        this.httpClient = httpClientBuilder.build();
        this.httpNumRetries = Integer.parseInt(
            System.getProperty(PROP_HTTP_NUM_RETRIES, "" + DEFAULT_HTTP_NUM_RETRIES));
        if (logger.isDebugEnabled()) {
            logger.debug("connect: {} {}", serverURL, httpClient);
        }
    }

    public AGHTTPClient(String serverURL, HttpClientConnectionManager manager) {
        this(serverURL, manager, null);
    }

    public AGHTTPClient(String serverURL) {
        this(serverURL, null, null);
    }

    private static HttpClientConnectionManager createManager() {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        // Allow "unlimited" concurrent connections to the same host (default is 2)
        manager.setDefaultMaxPerRoute(Integer.MAX_VALUE);
        manager.setMaxTotal(Integer.MAX_VALUE);
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
                     HttpEntity requestEntity, AGResponseHandler handler) throws AGHttpException {
        HttpPost post = new HttpPost(url);
        setDoAuthentication(post);
        for (Header header : headers) {
            post.addHeader(header);
        }

        if (System.getProperty("com.franz.agraph.http.useGzip", "true").equals("true")) {
            post.addHeader("Accept-encoding", "gzip");
        }
        // bug21953. Only write params to body if content-type is appropriate.
        Header contentType = post.getFirstHeader("Content-Type");
        if (requestEntity == null && (contentType == null
                || contentType.getValue().contains(Protocol.FORM_MIME_TYPE))) {
            post.setEntity(new UrlEncodedFormEntity(Arrays.asList(params), StandardCharsets.UTF_8));
        } else {
            post.setURI(getUri(url, params));
            post.setEntity(requestEntity);
        }
        executeMethod(url, post, handler);
    }

    /**
     * Checks whether the specified status code is in the 2xx-range, indicating a
     * successful request.
     *
     * @return <code>true</code> if the status code is in the 2xx range
     */
    private boolean is2xx(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    public void get(String url, Header[] headers, NameValuePair[] params,
                    AGResponseHandler handler) throws AGHttpException {
        URI uri = getUri(url, params);
        int numTries = httpNumRetries + 1; // Always make at least one attempt
        for (int i = 0; i < numTries; i++) {
            HttpGet get = new HttpGet(uri);
            setDoAuthentication(get);
            if (headers != null) {
                for (Header header : headers) {
                    get.setHeader(header);
                }
            }
            if (System.getProperty("com.franz.agraph.http.useGzip", "true").equals("true")) {
                get.setHeader("Accept-encoding", "gzip");
            }

            boolean mightRetry = (i < numTries - 1);
            if (executeMethod(url, get, handler, mightRetry) == ExecuteResult.SUCCESS) {
                return;
            }
        }
        // We should never get here:
        throw new AGHttpException("GET request failed (unreachable)");
    }

    private URI getUri(String url, NameValuePair[] params) {
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            if (params != null) {
                uriBuilder = uriBuilder.addParameters(Arrays.asList(params));
            }
            return uriBuilder.build();
        } catch (URISyntaxException exc) {
            throw new AGHttpException(exc);
        }
    }

    public void delete(String url, Header[] headers, NameValuePair[] params, AGResponseHandler handler)
            throws AGHttpException {
        HttpDelete delete = new HttpDelete(getUri(url, params));
        setDoAuthentication(delete);
        if (headers != null) {
            for (Header header : headers) {
                delete.addHeader(header);
            }
        }
        executeMethod(url, delete, handler);
    }

    public void put(String url, Header[] headers, NameValuePair[] params, HttpEntity requestEntity, AGResponseHandler handler)
            throws AGHttpException {
        HttpPut put = new HttpPut(getUri(url, params));
        setDoAuthentication(put);
        if (headers != null) {
            for (Header header : headers) {
                put.addHeader(header);
            }
        }
        if (requestEntity != null) {
            put.setEntity(requestEntity);
        }
        executeMethod(url, put, handler);
    }

    private void executeMethod(String url,
                               HttpUriRequest method,
                               AGResponseHandler handler) {
      executeMethod(url, method, handler, false);
    }

    private enum ExecuteResult { SUCCESS, RETRY };

    /**
     * Run the HTTP request given by the method.
     *
     * @param httpUriRequest of the HTTP request
     * @param handler in case of 200 response status, it will be called on the method
     *        (note that it will NOT be called for other 2xx success status responses,
     *        which might be debatable)
     * @param returnRetryOn408 if true and server replied 408, then RETRY is returned
     *
     * @return either SUCCESS (for 2xx response), RETRY (for 408 response + returnRetryOn408),
     *         or throws an AGHttpException
     */
    private ExecuteResult executeMethod(String url,
                                        HttpUriRequest httpUriRequest,
                                        AGResponseHandler handler,
                                        boolean returnRetryOn408) throws AGHttpException {
        // A note about retrying requests:
        // a difficuty with HttpMethod is that once the request is attempted, you can't
        // reuse it for a second attempt. This method is unable to do the retry itself,
        // therefore returns ExecuteResult.RETRY.

        // Will be set to false if the handler takes ownership of the method object.
        // Otherwise we must close the method by the end of this procedure.
        boolean release = true;
        HttpResponse httpResponse = null;
        try {
            HttpClientContext context = HttpClientContext.create();
            if (credsProvider != null && authCache != null) {
                context.setCredentialsProvider(credsProvider);

                // Reuse authCache if this is a request to client's serverUri.
                URI serverUri = getUri(serverURL, null);
                HttpHost serverHost = new HttpHost(serverUri.getHost(), serverUri.getPort(), serverUri.getScheme());
                URI targetUri = getUri(url, null);
                HttpHost targetHost = new HttpHost(targetUri.getHost(), targetUri.getPort(), targetUri.getScheme());
                if (!serverHost.equals(targetHost)) {
                    AuthCache newac = new BasicAuthCache();
                    newac.put(targetHost, new BasicScheme());
                    context.setAuthCache(newac);
                } else {
                    context.setAuthCache(authCache);
                }
            }
            httpUriRequest.addHeader(new BasicHeader("Connection", "keep-alive"));

            httpResponse = getHttpClient().execute(httpUriRequest, context);
            int httpCode = httpResponse.getStatusLine().getStatusCode();
            if (httpCode == HttpURLConnection.HTTP_OK
                || httpCode == HttpURLConnection.HTTP_NO_CONTENT) {
                if (handler != null) {
                    release = handler.releaseConnection();
                    handler.handleResponse(httpResponse, httpUriRequest);
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
                release = errHandler.releaseConnection();
                errHandler.handleResponse(httpResponse, httpUriRequest);
                throw errHandler.getResult();
            }
        } catch (IOException e) {
            throw new AGHttpException(e);
        } finally {
            if (release && httpResponse != null) {
                // Note: this will read the response body if necessary
                // to allow connection reuse.
                EntityUtils.consumeQuietly(httpResponse.getEntity());
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
                URI serverUri = new URI(serverURL);
                HttpHost targetHost = new HttpHost(serverUri.getHost(), serverUri.getPort(), serverUri.getScheme());
                credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(AuthScope.ANY,
                                             new UsernamePasswordCredentials(username, password));

                authCache = new BasicAuthCache();
                authCache.put(targetHost, new BasicScheme());

            } catch (URISyntaxException e) {
                logger.warn("Unable to set username and password for malformed URL " + serverURL, e);
            }
        } else {
            credsProvider = null;
            authCache = null;
        }
    }

    /**
     * Retrieve the username and password previously set by setUsernameAndPassword.
     *
     * @return An array of two Strings.  The first element is the username (or null)
     *         and the second element is the password (or null).
     */
    public String[] getUsernameAndPassword() {
        String[] res = new String[2];

        // WARNING: The call to getCredentials will throw an IllegalArgumentException of credentials have not previously
        // been set with a call to setUsernameAndPassword

        res[0] = credsProvider.getCredentials(AuthScope.ANY).getUserPrincipal().getName();
        res[1] = credsProvider.getCredentials(AuthScope.ANY).getPassword();

        return res;
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

    protected final void setDoAuthentication(HttpRequest method) {
        if (masqueradeAsUser != null) {
            method.addHeader(new BasicHeader("x-masquerade-as-user", masqueradeAsUser));
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
        NameValuePair[] params = {new BasicNameValuePair(OVERRIDE_PARAM_NAME, "false")};
        put(repositoryURL, headers, params, null, null);
    }

    public void deleteRepository(String repositoryURL) throws AGHttpException {
        Header[] headers = new Header[0];
        NameValuePair[] params = new NameValuePair[0];
        delete(repositoryURL, headers, params, null);
    }

    public TupleQueryResult getTupleQueryResult(String url) throws AGHttpException {
        Header[] headers = {new BasicHeader(ACCEPT_PARAM_NAME, TupleQueryResultFormat.SPARQL.getDefaultMIMEType())};
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
        NameValuePair[] data = {new BasicNameValuePair(AMOUNT_PARAM_NAME, Integer
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
        NameValuePair[] data = {
            new BasicNameValuePair("store", spec),
            new BasicNameValuePair(AGProtocol.AUTOCOMMIT_PARAM_NAME,
                                   Boolean.toString(autocommit)),
            new BasicNameValuePair(AGProtocol.LIFETIME_PARAM_NAME,
                                   Long.toString(3600))}; // TODO have some kind of policy for this
        AGStringHandler handler = new AGStringHandler();
        post(url, headers, data, null, handler);
        return handler.getResult();
    }

    @Override
    public void close() {
        logger.debug("close: " + serverURL + " " + mManager);
        if (mManager instanceof PoolingHttpClientConnectionManager) {
            mManager.shutdown();
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
        NameValuePair[] data = {
            new BasicNameValuePair("prefix", namespace),
            new BasicNameValuePair(AMOUNT_PARAM_NAME, Integer.toString(amount))};
        AGStringHandler handler = new AGStringHandler();
        post(url, headers, data, null, handler);
        return handler.getResult().split("\n");
    }
}
