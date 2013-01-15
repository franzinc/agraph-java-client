/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.TupleQueryResultBuilder;

import com.franz.agraph.http.handler.AGLongHandler;
import com.franz.agraph.http.handler.AGTQRHandler;

/**
 * Implements the Sesame TupleQuery interface for AllegroGraph.
 * 
 */
public class AGTupleQuery extends AGQuery implements TupleQuery {

	public AGTupleQuery(AGRepositoryConnection con, QueryLanguage ql,
			String queryString, String baseURI) {
		super(con, ql, queryString, baseURI);
	}

	public TupleQueryResult evaluate() throws QueryEvaluationException {
		try {
			// TODO: make this efficient for large result sets
			TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
			evaluate(builder);
			return builder.getQueryResult();
		} catch (TupleQueryResultHandlerException e) {
			// Found a bug in TupleQueryResultBuilder?
			throw new RuntimeException(e);
		}
	}

	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {
		evaluate(new AGTQRHandler(httpCon.getHttpRepoClient().getPreferredTQRFormat(), handler, httpCon.getValueFactory(),httpCon.getHttpRepoClient().getAllowExternalBlankNodeIds()));
	}

	/**
	 * Evaluates the query and returns only the number of results
	 * to the client (counting is done on the server, the results
	 * are not returned).
	 * 
	 * @return the number of results
	 * @throws QueryEvaluationException
	 */
	public long count() throws QueryEvaluationException {
		AGLongHandler handler = new AGLongHandler();
		evaluate(handler);
		return handler.getResult();
	}
}
