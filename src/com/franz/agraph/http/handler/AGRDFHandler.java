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
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.agraph.http.exception.AGHttpException;

public class AGRDFHandler extends AGResponseHandler {

	private final RDFFormat format;
	private final RDFHandler rdfhandler;
	private final AGValueFactory vf;
	
	public AGRDFHandler(RDFFormat format, RDFHandler rdfhandler, AGValueFactory vf, boolean recoverExternalBNodes) {
		super(format.getDefaultMIMEType());
		this.format = format;
		if (recoverExternalBNodes) {
			this.rdfhandler = recoverBNodesRDFHandler(rdfhandler);
		} else {
			this.rdfhandler = rdfhandler;
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
			RDFParser parser = Rio.createParser(format, vf);
			parser.setPreserveBNodeIDs(true);
			parser.setRDFHandler(rdfhandler);
			parser.parse(response, method.getURI().getURI());
		} catch (RDFParseException e) {
			throw new AGHttpException(e);
		} catch (RDFHandlerException e) {
			throw new AGHttpException(e);
		}
	}
	
	private RDFHandler recoverBNodesRDFHandler(final RDFHandler handler) {
		return new RDFHandler(){

			@Override
			public void startRDF() throws RDFHandlerException {
				handler.startRDF();
			}

			@Override
			public void endRDF() throws RDFHandlerException {
				handler.endRDF();
			}

			@Override
			public void handleNamespace(String prefix, String uri)
					throws RDFHandlerException {
				handler.handleNamespace(prefix, uri);
			}

			@Override
			public void handleStatement(Statement st)
					throws RDFHandlerException {
				Resource s = AGHttpRepoClient.getApplicationResource(st.getSubject(),vf);
				Value o = AGHttpRepoClient.getApplicationValue(st.getObject(),vf);
				Resource c = AGHttpRepoClient.getApplicationResource(st.getContext(),vf);
				st = vf.createStatement(s,st.getPredicate(),o,c);
				handler.handleStatement(st);
			}

			@Override
			public void handleComment(String comment)
					throws RDFHandlerException {
				handler.handleComment(comment);
			}};
	}
}
