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
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;

import com.franz.agraph.http.AGHttpException;

public class AGRDFHandler extends AGResponseHandler {

	private final RDFFormat format;
	private final RDFHandler rdfhandler;
	private final ValueFactory vf;
	
	public AGRDFHandler(RDFFormat format, RDFHandler rdfhandler, ValueFactory vf) {
		super(format.getDefaultMIMEType());
		this.format = format;
		this.rdfhandler = rdfhandler;
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
			throw new AGHttpException(e.getLocalizedMessage());
		} catch (RDFHandlerException e) {
			throw new AGHttpException(e.getLocalizedMessage());
		}
	}
	
}
