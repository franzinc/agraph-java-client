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
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultParser;
import org.openrdf.query.resultio.QueryResultIO;
import org.openrdf.query.resultio.QueryResultParseException;

import com.franz.agraph.http.exception.AGHttpException;

public class AGBQRHandler extends AGResponseHandler {

	private boolean result;

	public AGBQRHandler() {
		super(BooleanQueryResultFormat.TEXT.getDefaultMIMEType());
	}

	@Override
	public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
		String mimeType = getResponseMIMEType(method);
		if (!mimeType.equals(getRequestMIMEType())) {
			throw new AGHttpException("unexpected response MIME type: " + mimeType);
		}
		InputStream response = getInputStream(method);
		try {
			BooleanQueryResultFormat format = BooleanQueryResultFormat.TEXT;
			BooleanQueryResultParser parser = QueryResultIO.createParser(format);
			result = parser.parse(response);
		} catch (QueryResultParseException e) {
			throw new AGHttpException(e);
		}
	}
	
	public boolean getResult() {
		return result;
	}
	
}
