/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

/**
 * 
 */
package com.franz.agraph.http;

import static com.franz.agraph.http.AGProtocol.AMOUNT_PARAM_NAME;
import static org.openrdf.http.protocol.Protocol.ACCEPT_PARAM_NAME;
import info.aduna.net.http.HttpClientUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.util.Util;
import com.franz.util.Closeable;

/**
 * TODO: another pass over this class for response and error handling
 * replace RepositoryExceptions, this class shouldn't know about them.
 */
public class AGHTTPClient
implements Closeable {
    
	private final String serverURL;
	private final HttpClient httpClient;

	private AuthScope authScope;
	final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private MultiThreadedHttpConnectionManager mManager = null;

	public AGHTTPClient(String serverURL) {
		this(serverURL, null);
	}

	public AGHTTPClient(String serverURL, HttpConnectionManager manager) {
		this.serverURL = serverURL;
		if (manager == null) {
			// Use MultiThreadedHttpConnectionManager to allow concurrent access
			// on HttpClient
		    mManager = new MultiThreadedHttpConnectionManager();
		    manager = mManager;
		    
			// Allow 20 concurrent connections to the same host (default is 2)
			HttpConnectionManagerParams params = new HttpConnectionManagerParams();
			params.setDefaultMaxConnectionsPerHost(20);
			manager.setParams(params);
		}
		httpClient = new HttpClient(manager);
	}

	public String getServerURL() {
		return serverURL;
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	public void post(String url, Header[] headers, NameValuePair[] params,
			RequestEntity requestEntity, AGResponseHandler handler) throws HttpException, IOException,
			RepositoryException, RDFParseException {
		PostMethod post = new PostMethod(url);
		setDoAuthentication(post);
		for (Header header : headers) {
			post.addRequestHeader(header);
		}
		post.setQueryString(params);
		if (requestEntity != null) {
			post.setRequestEntity(requestEntity);
		}
		try {
			int httpCode = getHttpClient().executeMethod(post);
			if (httpCode == HttpURLConnection.HTTP_OK) {
				if (handler!=null) handler.handleResponse(post);
			} else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new UnauthorizedException();
			} else if (!HttpClientUtil.is2xx(httpCode)) {
				AGErrorInfo errInfo = getErrorInfo(post);
				if (errInfo.getErrorType() == AGErrorType.MALFORMED_DATA) {
					throw new RDFParseException(errInfo.getErrorMessage());
				} else if (errInfo.getErrorType() == AGErrorType.UNSUPPORTED_FILE_FORMAT) {
					throw new UnsupportedRDFormatException(errInfo
							.getErrorMessage());
				} else {
					throw new RepositoryException("POST failed " + url + ":"
							+ errInfo + " (" + httpCode + ")");
				}
			}
		} finally {
			releaseConnection(post);
		}
	}

	public void get(String url, Header[] headers, NameValuePair[] params,
			AGResponseHandler handler) throws IOException, RepositoryException,
			AGHttpException {
		GetMethod get = new GetMethod(url);
		setDoAuthentication(get);
		for (Header header : headers) {
			get.addRequestHeader(header);
		}
		get.setQueryString(params);
		try {
			int httpCode = getHttpClient().executeMethod(get);
			if (httpCode == HttpURLConnection.HTTP_OK) {
				if (handler!=null) handler.handleResponse(get);
			} else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new UnauthorizedException();
			} else if (!HttpClientUtil.is2xx(httpCode)) {
				AGErrorInfo errInfo = getErrorInfo(get);
				throw new AGHttpException(errInfo);
			}
		} finally {
			releaseConnection(get);
		}
	}

	public void delete(String url, Header[] headers, NameValuePair[] params)
			throws HttpException, IOException, RepositoryException {
		DeleteMethod delete = new DeleteMethod(url);
		setDoAuthentication(delete);
		for (Header header : headers) {
			delete.addRequestHeader(header);
		}
		delete.setQueryString(params);
		try {
			int httpCode = getHttpClient().executeMethod(delete);
			if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new UnauthorizedException();
			} else if (!HttpClientUtil.is2xx(httpCode)) {
				AGErrorInfo errInfo = getErrorInfo(delete);
				throw new RepositoryException("DELETE failed " + url + ":"
						+ errInfo + " (" + httpCode + ")");
			}
		} finally {
			releaseConnection(delete);
		}
	}

	public void put(String url, Header[] headers, NameValuePair[] params, RequestEntity requestEntity) throws IOException, AGHttpException, UnauthorizedException {
		PutMethod put = new PutMethod(url);
		setDoAuthentication(put);
		for (Header header : headers) {
			put.addRequestHeader(header);
		}
		put.setQueryString(params);
		if (requestEntity != null)
			put.setRequestEntity(requestEntity);
		try {
			int httpCode = getHttpClient().executeMethod(put);
			if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new UnauthorizedException();
			} else if (!HttpClientUtil.is2xx(httpCode)) {
				AGErrorInfo errInfo = getErrorInfo(put);
				throw new AGHttpException(errInfo);
			}
		} finally {
			releaseConnection(put);
		}
	}

	/*-------------------------*
	 * General utility methods *
	 *-------------------------*/

	protected AGErrorInfo getErrorInfo(HttpMethod method) {
		AGErrorInfo errorInfo;
		try {
			// TODO: check the case where the server supplies
			// no error message
			AGResponseHandler handler = new AGResponseHandler("");
			handler.handleResponse(method);
			errorInfo = AGErrorInfo.parse(handler.getString());
			logger.warn("Server reports problem: {}", errorInfo.getErrorMessage());
		} catch (Exception e) {
			logger.warn("Unable to retrieve error info from server");
			errorInfo = new AGErrorInfo("Unable to retrieve error info from server");
		}
		return errorInfo;
	}

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
			logger.debug(
					"Setting username '{}' and password for server at {}.",
					username, serverURL);
			try {
				URL server = new URL(serverURL);
				authScope = new AuthScope(server.getHost(), AuthScope.ANY_PORT);
				httpClient.getState().setCredentials(authScope,
						new UsernamePasswordCredentials(username, password));
				httpClient.getParams().setAuthenticationPreemptive(true);
			} catch (MalformedURLException e) {
				logger
						.warn(
								"Unable to set username and password for malformed URL {}",
								serverURL);
			}
		} else {
			authScope = null;
			httpClient.getState().clearCredentials();
			httpClient.getParams().setAuthenticationPreemptive(false);
		}
	}

	protected final void setDoAuthentication(HttpMethod method) {
		if (authScope != null
				&& httpClient.getState().getCredentials(authScope) != null) {
			method.setDoAuthentication(true);
		} else {
			method.setDoAuthentication(false);
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

	public void putRepository(String repositoryURL) throws IOException,
			RepositoryException, UnauthorizedException, AGHttpException {
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		put(repositoryURL,headers,params,null);
	}

	public void deleteRepository(String repositoryURL) throws IOException,
			RepositoryException, UnauthorizedException {
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		delete(repositoryURL, headers, params);
	}

	public TupleQueryResult getTupleQueryResult(String url) throws RepositoryException {
		Header[] headers = { new Header(ACCEPT_PARAM_NAME, TupleQueryResultFormat.SPARQL.getDefaultMIMEType()) };
		NameValuePair[] params = new NameValuePair[0];
		Repository repo = new SailRepository( new MemoryStore() );
		repo.initialize();
		TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
		AGResponseHandler handler = new AGResponseHandler(repo,builder);
		try {
			get(url, headers, params, handler);
		} catch (IOException e) {
			throw new RepositoryException(e);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
		return builder.getQueryResult();
	}
	
	public String[] getBlankNodes(String repositoryURL, int amount)
			throws IOException, RepositoryException, UnauthorizedException {
		String url = AGProtocol.getBlankNodesURL(repositoryURL);
		Header[] headers = new Header[0];
		NameValuePair[] data = { new NameValuePair(AMOUNT_PARAM_NAME, Integer
				.toString(amount)) };

		AGResponseHandler handler = new AGResponseHandler("");
		try {
			post(url, headers, data, null, handler);
		} catch (RDFParseException e) {
			// bug.
			throw new RuntimeException(e);
		}
		return handler.getString().split("\n");
	}

    @Override
    public void close() {
        Util.close(this.mManager);
    }

}
