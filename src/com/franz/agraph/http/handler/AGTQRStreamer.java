/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.httpclient.HttpMethod;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.util.Closer;

/**
 * Similar to {@link org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLParser}
 * but uses {@link XMLStreamReader} instead of SAXParser so the results
 * streaming in the http response can be processed in a
 * {@link TupleQueryResult} pulling from the stream.
 * 
 * @since v4.3
 */
public class AGTQRStreamer extends AGResponseHandler {

	private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
	
	final Logger log = LoggerFactory.getLogger(this.getClass());

	private final AGValueFactory vf;
	private XMLStreamReader xml;
	private HttpMethod method;

	public AGTQRStreamer(TupleQueryResultFormat format, AGValueFactory vf) {
		super(format.getDefaultMIMEType());
		this.vf = vf;
	}

	@Override
	public String getRequestMIMEType() {
		return TupleQueryResultFormat.SPARQL.getDefaultMIMEType();
	}
	
	/**
	 * False because the Result will release the HTTP resources.
	 * For most responses, AGHTTPClient releases resources after
	 * calling {@link #handleResponse(HttpMethod)},
	 * but here the work is pulled later from the {@link Result}.
	 */
	@Override
	public boolean releaseConnection() {
		return false;
	}

	@Override
	public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
		this.method = method;
		try {
			xml = xmlInputFactory.createXMLStreamReader(AGResponseHandler.getInputStream(method));
		} catch (XMLStreamException e) {
			throw new AGHttpException(e);
		} 
	}
	
	public TupleQueryResult getResult() {
		return new Result();
	}

	class Result implements TupleQueryResult {

		private List<String> bindingNames;
		
		private MapBindingSet next = null;
		
		@Override
		public List<String> getBindingNames() {
			if (bindingNames == null) {
				try {
					while (xml.hasNext()) {
						switch (xml.next()) {
						case XMLStreamConstants.START_ELEMENT:
							if ("head".equals(xml.getLocalName())) {
								bindingNames = new ArrayList<String>();
							}
							else if ("variable".equals(xml.getLocalName())) {
								for (int i = 0; i < xml.getAttributeCount(); i++) {
									if ("name".equals(xml.getAttributeLocalName(i))) {
										bindingNames.add(xml.getAttributeValue(i));
									}
								}
							}
							else if ("sparql".equals(xml.getLocalName())) {
								// continue
							} else {
								throw new RuntimeException("Unexpected tag: " + xml.getLocalName());
								//return null;
							}
							break;
						case XMLStreamConstants.END_ELEMENT:
							if ("head".equals(xml.getLocalName())) {
								return bindingNames;
							}
							break;
						}
					}
				} catch (XMLStreamException e) {
					throw new RuntimeException(e);
				}
			}
			return bindingNames;
		}
		
		@Override
		public boolean hasNext() throws QueryEvaluationException {
			if (next == null) {
				if (closed) {
					return false;
				}
				getBindingNames(); // must be done first
				try {
					String bindingName = null;
					while (xml.hasNext()) {
						final int event = xml.next();
						if (xml.isStartElement()) {
							String name = xml.getLocalName();
							if ("result".equals(name)) {
								next = new MapBindingSet(bindingNames.size());
							}
							else if ("results".equals(xml.getLocalName())) {
								// continue
							}
							else if ("binding".equals(name)) {
								for (int i = 0; i < xml.getAttributeCount(); i++) {
									if ("name".equals(xml.getAttributeLocalName(i))) {
										bindingName = xml.getAttributeValue(i);
									}
								}
							}
							else if ("literal".equals(name)) {
								String lang = null;
								String datatype = null;
								for (int i = 0; i < xml.getAttributeCount(); i++) {
									if ("lang".equals(xml.getAttributeLocalName(i))
											&& "http://www.w3.org/XML/1998/namespace".equals(xml.getAttributeNamespace(i))) {
										lang = xml.getAttributeValue(i);
									}
									else if ("datatype".equals(xml.getAttributeLocalName(i))) {
										datatype = xml.getAttributeValue(i);
									}
								}
								String text = xml.getElementText();
								Value value = null;
								if (datatype != null) {
									try {
										value = vf.createLiteral(text, vf.createURI(datatype));
									} catch (IllegalArgumentException e) {
										// Illegal datatype URI
										throw new QueryEvaluationException(e.getMessage(), e);
									}
								}
								else if (lang != null) {
									value = vf.createLiteral(text, lang);
								}
								else {
									value = vf.createLiteral(text);
								}
								next.addBinding(bindingName, value);
							}
							else if ("uri".equals(name)) {
								next.addBinding(bindingName, AGHttpRepoClient.getApplicationResource(vf.createURI(xml.getElementText()),vf));
							}
							else if ("bnode".equals(name)) {
								next.addBinding(bindingName, vf.createBNode(xml.getElementText()));
							} else {
								log.warn("unknown elem: " + name + " attrs=" + xml.getAttributeCount());
							}
						}
						else if (xml.isEndElement()) {
							if ("result".equals(xml.getLocalName())) {
								return next != null;
							}
							else if ("results".equals(xml.getLocalName())) {
								// in normal use, closed will be called here
								close();
								return false;
							}
							else if ("sparql".equals(xml.getLocalName())) {
								close();
								return false;
							}
							else if ("binding".equals(xml.getLocalName())) {
							}
							else {
								log.warn("unknown elem end: " + xml.getLocalName());
							}
						} else if (event == XMLStreamConstants.END_DOCUMENT) {
							close();
							return false;
						}
					}
				} catch (XMLStreamException e) {
					throw new QueryEvaluationException(e);
				}
			}
			return next != null;
		}
		
		@Override
		public BindingSet next() throws QueryEvaluationException {
			if (hasNext()) {
				BindingSet curr = next;
				next = null;
				return curr;
			} else if (closed) {
				throw new RuntimeException("closed");
			} else {
				throw new NoSuchElementException();
			}
		}
		
		private boolean closed = false;
		
		@Override
		public void close() throws QueryEvaluationException {
			if ( ! closed) {
				closed = true;
				try {
					Closer.Close(xml);
					Closer.Close(method.getResponseBodyAsStream());
					method.releaseConnection();
				} catch (Exception e) {
					log.warn("I/O error closing resources", e);
				}
			}
		}
		
		@Override
		public void remove() throws QueryEvaluationException {
			throw new UnsupportedOperationException();
		}
	}
	
}
