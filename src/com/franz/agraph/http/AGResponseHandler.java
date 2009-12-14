/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpMethod;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultParser;
import org.openrdf.query.resultio.QueryResultIO;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultParser;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;

// TODO: make this class abstract and subclass the distinct handlers
public class AGResponseHandler {

	private final Repository repository;
	private final RDFHandler rdfhandler;
	private final TupleQueryResultHandler tqrhandler;
	private final boolean bqrHandler;
	private final boolean longHandler;
	private final boolean stringHandler;

	private boolean bqrresult;
	private long longresult;
	private String stringresult = null;

	private String requestMIMEType;
	
	// TODO: pass in the parser instead of the handler
	public AGResponseHandler(Repository repository, RDFHandler rdfhandler, RDFFormat rdfformat) {
		this.repository = repository;
		this.rdfhandler = rdfhandler;
		this.tqrhandler = null;
		this.bqrHandler = false;
		this.longHandler = false;
		this.stringHandler = false;
		requestMIMEType = rdfformat.getDefaultMIMEType();
	}

	// TODO: pass in the parser instead
	public AGResponseHandler(Repository repository,
			TupleQueryResultHandler tqrhandler) {
		this.repository = repository;
		this.rdfhandler = null;
		this.tqrhandler = tqrhandler;
		this.bqrHandler = false;
		this.longHandler = false;
		this.stringHandler = false;
		requestMIMEType = TupleQueryResultFormat.SPARQL.getDefaultMIMEType();
	}

	public AGResponseHandler(boolean bqrresponse) {
		this.repository = null;
		this.rdfhandler = null;
		this.tqrhandler = null;
		this.bqrHandler = true;
		this.longHandler = false;
		this.stringHandler = false;
		requestMIMEType = BooleanQueryResultFormat.TEXT.getDefaultMIMEType();
	}

	public AGResponseHandler(long l) {
		this.repository = null;
		this.rdfhandler = null;
		this.tqrhandler = null;
		this.bqrHandler = false;
		this.longHandler = true;
		this.stringHandler = false;
		requestMIMEType = "text/integer"; // TODO: add to AGProtocol
	}

	public AGResponseHandler(String s) {
		this.repository = null;
		this.rdfhandler = null;
		this.tqrhandler = null;
		this.bqrHandler = false;
		this.longHandler = false;
		this.stringHandler = true;
		requestMIMEType = null;
	}
	
	public String getRequestMIMEType() {
		return requestMIMEType;
	}
	
	public boolean getBoolean() {
		return bqrresult;
	}

	public long getLong() {
		return longresult;
	}

	public String getString() {
		return stringresult;
	}
	
	public void handleResponse(HttpMethod method) throws IOException, RepositoryException {

		String mimeType = getResponseMIMEType(method);
		try {
			if (rdfhandler != null) {
				RDFFormat format = RDFFormat.forMIMEType(mimeType, RDFFormat.TRIX); 
				// TODO:
				// .matchMIMEType(mimeType,
				// rdfFormats);
				RDFParser parser = Rio.createParser(format, repository
						.getValueFactory());
				parser.setPreserveBNodeIDs(true);
				parser.setRDFHandler(rdfhandler);
				parser.parse(method.getResponseBodyAsStream(), method.getURI()
						.getURI());
			} else if (tqrhandler != null) {
				TupleQueryResultFormat format = TupleQueryResultFormat.SPARQL; // TODO:
				// .matchMIMEType(mimeType,
				// tqrFormats);
				TupleQueryResultParser parser = QueryResultIO.createParser(
						format, repository.getValueFactory());
				parser.setTupleQueryResultHandler(tqrhandler);
				parser.parse(method.getResponseBodyAsStream());

			} else if (true == bqrHandler) {
				BooleanQueryResultFormat format = BooleanQueryResultFormat.TEXT; // TODO:
																					// .matchMIMEType(mimeType,
																					// booleanFormats);
				BooleanQueryResultParser parser = QueryResultIO
						.createParser(format);
				bqrresult = parser.parse(method.getResponseBodyAsStream());
			} else if (true == longHandler) {
				try {
					InputStream is = method.getResponseBodyAsStream();
					String str = streamToString(is);
					longresult = Long.parseLong(str);
				} catch (NumberFormatException e) {
					throw new RepositoryException(
							"Server responded with invalid long value", e);
				}
			} else if (true == stringHandler) {
				InputStream is = method.getResponseBodyAsStream();
				stringresult = streamToString(is);
			} else {
				throw new RuntimeException(
						"Cannot handle response, unexpected type.");
			}
		} catch (UnsupportedRDFormatException e) {
			throw new RepositoryException(
					"Server responded with an unsupported file format: "
							+ mimeType);
		} catch (RDFParseException e) {
			throw new RepositoryException("Malformed query result from server",
					e);
		} catch (RDFHandlerException e) {
			throw new RepositoryException(e);
		} catch (QueryResultParseException e) {
			throw new RepositoryException(e);
		} catch (TupleQueryResultHandlerException e) {
			throw new RepositoryException(e);
		}
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
					// TODO: logger.debug("reponse MIME type is {}", mimeType);
					return mimeType;
				}
			}
		}

		return null;
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
