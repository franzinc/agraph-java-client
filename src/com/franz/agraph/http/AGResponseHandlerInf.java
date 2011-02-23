/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http;

import java.io.IOException;

import org.apache.commons.httpclient.HttpMethod;
import org.openrdf.repository.RepositoryException;

/**
 * @since v4.2.1
 */
public interface AGResponseHandlerInf {
	
	public String getRequestMIMEType();

	public void handleResponse(HttpMethod method) throws IOException, RepositoryException;

	/**
	 * Return true if {@link AGHTTPClient} should release resources after
	 * calling {@link #handleResponse(HttpMethod)}.
	 * @since v4.2.1
	 */
	public boolean releaseConnection();
	
}
