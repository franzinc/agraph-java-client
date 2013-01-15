/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import javax.xml.stream.XMLStreamReader;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;

import com.franz.agraph.http.handler.AGTQRStreamer;

/**
 * Wraps an AGTupleQuery to provide streaming results.
 * The default, TupleQueryResultParser and TupleQueryResultBuilder use
 * SAX to parse and an ArrayList to collect results,
 * so {@link AGTupleQuery#evaluate()} does not return until the
 * entire stream is parsed.
 * 
 * <p>AGStreamTupleQuery uses {@link XMLStreamReader}, so the result is
 * pulled from the http response stream as methods such as
 * {@link TupleQueryResult}.{@link TupleQueryResult#hasNext() hasNext()}
 * are called.
 * </p>
 * 
 * <p>Usage:
 * <code><pre>
 * AGTupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ...");
 * query = new AGStreamTupleQuery(query);
 * TupleQueryResult results = query.evaluate();
 * ...
 * </pre></code></p>
 * 
 * @see AGRepositoryConnection#prepareTupleQuery(org.openrdf.query.QueryLanguage, String)
 * @since v4.3
 */
public class AGStreamTupleQuery extends AGTupleQuery implements TupleQuery {

	/**
	 * Wraps a query with this object that will stream the response.
	 * 
	 * @param query to wrap
	 */
	public AGStreamTupleQuery(AGTupleQuery query) {
		super(query.httpCon, query.queryLanguage, query.queryString, query.baseURI);
	}
	
	/**
	 * Returns a result object that will read from the http response as
	 * results are requested, by
	 * {@link TupleQueryResult}.{@link TupleQueryResult#hasNext() hasNext()}.
	 * (Note that {@link TupleQueryResult}.{@link TupleQueryResult#next() next()}
	 * does not actually do the work if hasNext() is called first.)
	 */
	@Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		AGTQRStreamer handler = new AGTQRStreamer(httpCon.getHttpRepoClient().getPreferredTQRFormat(),httpCon.getRepository().getValueFactory());
		try {
			httpCon.getHttpRepoClient().query(this, false, handler);
		} catch (Exception e) {
			throw new QueryEvaluationException(e);
		}
		return handler.getResult();
	}

	/**
	 * 
	 */
	@Override
	public void evaluate(TupleQueryResultHandler handler) throws QueryEvaluationException, TupleQueryResultHandlerException {
		TupleQueryResult result = evaluate();
		handler.startQueryResult(result.getBindingNames());
		while (result.hasNext()) {
			handler.handleSolution(result.next());
		}
		handler.endQueryResult();
	}

}
