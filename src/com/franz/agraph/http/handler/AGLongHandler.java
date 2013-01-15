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

import org.apache.commons.httpclient.HttpMethod;

import com.franz.agraph.http.exception.AGHttpException;

public class AGLongHandler extends AGResponseHandler {

	private long result;
	
	public AGLongHandler() {
		super("text/integer");
	}

	@Override
	public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
		/* TODO: server responds with text/plain here, not text/integer
		String mimeType = getResponseMIMEType(method);
		if (!mimeType.equals(getRequestMIMEType())) {
			throw new AGHttpException("unexpected response MIME type: " + mimeType);
		}*/
		InputStream response = getInputStream(method);
		try {
			String str = streamToString(response);
			result = Long.parseLong(str);
		} catch (NumberFormatException e) {
			throw new AGHttpException(
					"Server responded with invalid long value: " + e.getLocalizedMessage(), e);
		}
	}
	
	public long getResult() {
		return result;
	}
	
}
