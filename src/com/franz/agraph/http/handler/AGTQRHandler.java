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
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HttpMethod;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.query.resultio.QueryResultIO;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultParser;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.agraph.http.exception.AGHttpException;

public class AGTQRHandler extends AGResponseHandler {

	private final TupleQueryResultFormat format;
	private final TupleQueryResultHandler tqrhandler;
	private final AGValueFactory vf;
	
	public AGTQRHandler(TupleQueryResultFormat format, TupleQueryResultHandler tqrhandler, AGValueFactory vf, boolean recoverExternalBNodes) {
		super(format.getDefaultMIMEType());
		this.format = format;
		if (recoverExternalBNodes) {
			this.tqrhandler = recoverBNodesTQRHandler(tqrhandler);
		} else {
			this.tqrhandler = tqrhandler;
		}
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
			parser.setTupleQueryResultHandler(recoverBNodesTQRHandler(tqrhandler));
			parser.parse(response);
		} catch (QueryResultParseException e) {
			throw new AGHttpException(e);
		} catch (TupleQueryResultHandlerException e) {
			throw new AGHttpException(e);
		}
	}
	
	private TupleQueryResultHandler recoverBNodesTQRHandler(final TupleQueryResultHandler handler) {
		return new TupleQueryResultHandler(){

			@Override
			public void startQueryResult(List<String> arg0)
					throws TupleQueryResultHandlerException {
				handler.startQueryResult(arg0);
			}

			@Override
			public void endQueryResult()
					throws TupleQueryResultHandlerException {
				handler.endQueryResult();
			}

			@Override
			public void handleSolution(BindingSet arg0)
					throws TupleQueryResultHandlerException {
				Set<String> names = arg0.getBindingNames();
				MapBindingSet sol = new MapBindingSet(names.size());
				for (String n: names) {
					Value v = AGHttpRepoClient.getApplicationValue(arg0.getValue(n),vf);
					sol.addBinding(n,v);
				}
				handler.handleSolution(sol);
			}
		};
	}
}
