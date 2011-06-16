/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http.handler;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpMethod;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultIO;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultParser;

import com.franz.agraph.http.AGHttpException;

public class AGTQRHandler extends AGResponseHandler {

	private final TupleQueryResultFormat format;
	private final TupleQueryResultHandler tqrhandler;
	private final ValueFactory vf;
	
	public AGTQRHandler(TupleQueryResultFormat format, TupleQueryResultHandler tqrhandler, ValueFactory vf) {
		super(format.getDefaultMIMEType());
		this.format = format;
		this.tqrhandler = tqrhandler;
		this.vf = vf;
	}

	@Override
	public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
		String mimeType = getResponseMIMEType(method);
		if (!mimeType.equals(getRequestMIMEType())) {
			throw new AGHttpException("unexpected response MIME type: " + mimeType);
		}
		InputStream response = getInputStream(method);
		try {
			TupleQueryResultParser parser = QueryResultIO.createParser(format, vf);
			parser.setTupleQueryResultHandler(tqrhandler);
			parser.parse(response);
		} catch (QueryResultParseException e) {
			throw new AGHttpException(e.getLocalizedMessage());
		} catch (TupleQueryResultHandlerException e) {
			throw new AGHttpException(e.getLocalizedMessage());
		}
	}
	
}
