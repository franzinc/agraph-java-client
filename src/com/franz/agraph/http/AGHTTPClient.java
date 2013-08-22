/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http;

import static com.franz.agraph.http.AGProtocol.AMOUNT_PARAM_NAME;
import static com.franz.agraph.http.AGProtocol.OVERRIDE_PARAM_NAME;
import static org.openrdf.http.protocol.Protocol.ACCEPT_PARAM_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
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
import org.openrdf.http.protocol.UnauthorizedException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGErrorHandler;
import com.franz.agraph.http.handler.AGResponseHandler;
import com.franz.agraph.http.handler.AGStringHandler;
import com.franz.agraph.http.handler.AGTQRHandler;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.util.Closeable;
import com.franz.util.Closer;

/**
 * TODO: another pass over this class for response and error handling
 * replace RepositoryExceptions, this class shouldn't know about them.
 */
public class AGHTTPClient
implements Closeable {
    
	private static final Logger logger = LoggerFactory.getLogger(AGHTTPClient.class);
	
	private final String serverURL;
	private final HttpClient httpClient;

	private AuthScope authScope;
	private String masqueradeAsUser;
	
	private boolean isClosed = false;

	private MultiThreadedHttpConnectionManager mManager = null;
	
	public AGHTTPClient(String serverURL) {
		this(serverURL, null);
	}

	public AGHTTPClient(String serverURL, HttpConnectionManager manager) {
		this.serverURL = serverURL.replaceAll("/$","");
		if (manager == null) {
			// Use MultiThreadedHttpConnectionManager to allow concurrent access
			// on HttpClient
		    mManager = new MultiThreadedHttpConnectionManager();
		    manager = mManager;
		    
			// Allow "unlimited" concurrent connections to the same host (default is 2)
			HttpConnectionManagerParams params = new HttpConnectionManagerParams();
			params.setDefaultMaxConnectionsPerHost(Integer.MAX_VALUE);
			params.setMaxTotalConnections(Integer.MAX_VALUE);
			manager.setParams(params);
		}
		httpClient = new HttpClient(manager);
		if (logger.isDebugEnabled()) {
			logger.debug("connect: " + serverURL + " " + httpClient + " " + manager);
		}
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
		
		if (System.getProperty("com.franz.agraph.http.useGzip","true").equals("true")) {
		    post.addRequestHeader("Accept-encoding", "gzip");
		}
		// bug21953. Only write params to body if content-type is appropriate.
		Header contentType = post.getRequestHeader("Content-Type");
		if (requestEntity == null && ( contentType == null ||
					       "application/x-www-form-urlencoded".contains(contentType.getValue()) )) {
			post.setRequestBody(params);
		} else {
			post.setQueryString(params);
			post.setRequestEntity(requestEntity);
		}
		try {
			int httpCode = getHttpClient().executeMethod(post);
			if (httpCode == HttpURLConnection.HTTP_OK) {
				if (handler!=null) handler.handleResponse(post);
			} else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new AGHttpException(new UnauthorizedException());
			} else if (!is2xx(httpCode)) {
				AGErrorHandler errHandler = new AGErrorHandler();
				errHandler.handleResponse(post);
				throw errHandler.getResult();
			}
		} catch (HttpException e) {
			throw new AGHttpException(e);
		} catch (IOException e) {
			handleSessionConnectionError(e,url);
		} finally {
			if (handler == null || handler.releaseConnection()) {
				releaseConnection(post);
			}
		}
	}

	/**
	* Checks whether the specified status code is in the 2xx-range, indicating a
	* successfull request.
	*
	* @return <tt>true</tt> if the status code is in the 2xx range.
	*/
	private boolean is2xx(int statusCode) {
		return statusCode >= 200 && statusCode < 300;
	}
	
	public void get(String url, Header[] headers, NameValuePair[] params,
			AGResponseHandler handler) throws AGHttpException {
		GetMethod get = new GetMethod(url);
		setDoAuthentication(get);
		if (headers != null) {
			for (Header header : headers) {
				get.addRequestHeader(header);
			}
		}
		if (System.getProperty("com.franz.agraph.http.useGzip","true").equals("true")) {
		    get.addRequestHeader("Accept-encoding", "gzip");
		}
		if (params != null) {
			get.setQueryString(params);
		}
		try {
			int httpCode = getHttpClient().executeMethod(get);
			if (httpCode == HttpURLConnection.HTTP_OK) {
				if (handler!=null) handler.handleResponse(get);
			} else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new AGHttpException(new UnauthorizedException());
			} else if (!is2xx(httpCode)) {
				AGErrorHandler errHandler = new AGErrorHandler();
				errHandler.handleResponse(get);
				throw errHandler.getResult();
			}
		} catch (HttpException e) {
			throw new AGHttpException(e);
		} catch (IOException e) {
			handleSessionConnectionError(e,url);
		} finally {
			if (handler == null || handler.releaseConnection()) {
				releaseConnection(get);
			}
		}
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
		try {
			int httpCode = getHttpClient().executeMethod(delete);
			if (httpCode == HttpURLConnection.HTTP_OK) {
				if (handler!=null) handler.handleResponse(delete);
			} else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new AGHttpException(new UnauthorizedException());
			} else if (!is2xx(httpCode)) {
				AGErrorHandler errHandler = new AGErrorHandler();
				errHandler.handleResponse(delete);
				throw errHandler.getResult();
			}
		} catch (HttpException e) {
			throw new AGHttpException(e);
		} catch (IOException e) {
			handleSessionConnectionError(e,url);
		} finally {
			if (handler == null || handler.releaseConnection()) {
				releaseConnection(delete);
			}
		}
	}

	public void put(String url, Header[] headers, NameValuePair[] params, RequestEntity requestEntity,AGResponseHandler handler) throws AGHttpException {
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
		try {
			int httpCode = getHttpClient().executeMethod(put);
			if (httpCode == HttpURLConnection.HTTP_OK) {
				if (handler!=null) handler.handleResponse(put);
			} else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new AGHttpException(new UnauthorizedException());
			} else if (!is2xx(httpCode)) {
				AGErrorHandler errHandler = new AGErrorHandler();
				errHandler.handleResponse(put);
				throw errHandler.getResult();
			}
		} catch (HttpException e) {
			throw new AGHttpException(e);
		} catch (IOException e) {
			handleSessionConnectionError(e,url);
		} finally {
			if (handler == null || handler.releaseConnection()) {
				releaseConnection(put);
			}
		}
	}

	/*-------------------------*
	 * General utility methods *
	 *-------------------------*/

	/**
	 * Set the username and password for authentication with the remote server.
	 * 
	 * @param username
	 *            the username
	 * @param password
	 *            the password
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
	 * 
	 * For AG superusers only.  This allows AG superusers to run requests as
	 * another user in a dedicated session.
	 *  
	 * @param user the user for X-Masquerade-As-User requests.
	 */
	public void setMasqueradeAsUser(String user) throws AGHttpException {
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
		if (logger.isDebugEnabled()) logger.debug("putCatalog: " + catalogURL);
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		put(catalogURL,headers,params,null,null);
	}
	
	public void deleteCatalog(String catalogURL) throws AGHttpException {
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		delete(catalogURL, headers, params, null);
	}
	
	public void putRepository(String repositoryURL) throws AGHttpException {
		if (logger.isDebugEnabled()) logger.debug("putRepository: " + repositoryURL);
		Header[] headers = new Header[0];
		NameValuePair[] params = { new NameValuePair(OVERRIDE_PARAM_NAME, "false") };
		put(repositoryURL,headers,params,null,null);
	}

	public void deleteRepository(String repositoryURL) throws AGHttpException {
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		delete(repositoryURL, headers, params, null);
	}

	public TupleQueryResult getTupleQueryResult(String url) throws AGHttpException {
		Header[] headers = { new Header(ACCEPT_PARAM_NAME, TupleQueryResultFormat.SPARQL.getDefaultMIMEType()) };
		NameValuePair[] params = new NameValuePair[0];
		TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
		// TODO: avoid using AGValueFactory(null)
		AGTQRHandler handler = new AGTQRHandler(TupleQueryResultFormat.SPARQL,builder,new AGValueFactory(null),false);
		get(url, headers, params, handler);
		return builder.getQueryResult();
	}
	
	public String[] getBlankNodes(String repositoryURL, int amount)
			throws AGHttpException {
		String url = AGProtocol.getBlankNodesURL(repositoryURL);
		Header[] headers = new Header[0];
		NameValuePair[] data = { new NameValuePair(AMOUNT_PARAM_NAME, Integer
				.toString(amount)) };

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
		NameValuePair[] data = { new NameValuePair("store", spec),
								 new NameValuePair(AGProtocol.AUTOCOMMIT_PARAM_NAME,
												   Boolean.toString(autocommit)),
								 new NameValuePair(AGProtocol.LIFETIME_PARAM_NAME,
												   Long.toString(3600)) }; // TODO have some kind of policy for this
		AGStringHandler handler = new AGStringHandler();
		post(url, headers, data, null, handler);
		return handler.getResult();
	}

    @Override
    public void close() {
        logger.debug("close: " + serverURL + " " + mManager);
        mManager = Closer.Close(mManager);
        isClosed = true;
    }
    
    boolean isClosed() {
    	return isClosed;
    }

	public String[] generateURIs(String repositoryURL, String namespace,
			int amount) throws AGHttpException {
		String url = repositoryURL + "/encodedIds";
		Header[] headers = new Header[0];
		NameValuePair[] data = { new NameValuePair("prefix", namespace),
				new NameValuePair(AMOUNT_PARAM_NAME, Integer.toString(amount)) };
		AGStringHandler handler = new AGStringHandler();
		post(url, headers, data, null, handler);
		return handler.getResult().split("\n");
	}

	private void handleSessionConnectionError(IOException e, String url) throws AGHttpException {
		if (e instanceof java.net.ConnectException) {
			// To test this exception, setup remote server and only open port to
			// the main port, not the SessionPorts and run TutorialTest.example6()
			throw new AGHttpException("Session port connection failure. Consult the Server Installation document for correct settings for SessionPorts. Url: " + url
					+ ". Documentation: http://www.franz.com/agraph/support/documentation/v4/server-installation.html#sessionport", e);
		} else {
			throw new AGHttpException("Possible session port connection failure. Consult the Server Installation document for correct settings for SessionPorts. Url: " + url
					+ ". Documentation: http://www.franz.com/agraph/support/documentation/v4/server-installation.html#sessionport", e);
		}
	}

}
