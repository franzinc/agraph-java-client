/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpMethod;

import com.franz.agraph.http.exception.AGHttpException;

public abstract class AGResponseHandler {

	private final String requestMIMEType;
	
	public AGResponseHandler(String mimeType) {
		requestMIMEType = mimeType;
	}
	
	public String getRequestMIMEType() {
		return requestMIMEType;
	}
	
	public abstract void handleResponse(HttpMethod method) throws IOException, AGHttpException;
	
	/**
	 * For most responses, AGHTTPClient releases resources after
	 * calling {@link #handleResponse(HttpMethod)}; this can be
	 * overridden in subclasses that stream results. 
	 */
	public boolean releaseConnection() {
		return true;
	}
	
	/**
	 * Gets the MIME type specified in the response headers of the supplied
	 * method, if any. For example, if the response headers contain
	 * <tt>Content-Type: application/xml;charset=UTF-8</tt>, this method will
	 * return <tt>application/xml</tt> as the MIME type.
	 * 
	 * @param method
	 *            The method to get the reponse MIME type from.
	 * @return The response MIME type, or <tt>null</tt> if not available.
	 */
	protected String getResponseMIMEType(HttpMethod method) throws IOException {
		Header[] headers = method.getResponseHeaders("Content-Type");

		for (Header header : headers) {
			HeaderElement[] headerElements = header.getElements();

			for (HeaderElement headerEl : headerElements) {
				String mimeType = headerEl.getName();
				if (mimeType != null) {
					// TODO: logger.debug("response MIME type is {}", mimeType);
					return mimeType;
				}
			}
		}

		return null;
	}

	protected static InputStream getInputStream(HttpMethod method) throws IOException {
		InputStream is = method.getResponseBodyAsStream();
		Header h = method.getResponseHeader("Content-Encoding");
		if (h!=null && h.getValue().equals("gzip")) {
			is = new GZIPInputStream(is);
		}
		return is;
	}
	
	protected static String streamToString(InputStream in) throws IOException {
		// TODO: protect against buffering very large streams
	    StringBuffer out = new StringBuffer();
	    byte[] b = new byte[4096];
	    for (int n; (n = in.read(b)) != -1;) {
	        out.append(new String(b, 0, n));
	    }
	    return out.toString();
	}
	
}
