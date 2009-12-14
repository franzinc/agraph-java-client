/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;

import com.franz.agraph.http.AGResponseHandler;

/**
 * Implements the Sesame BooleanQuery interface for AllegroGraph.
 * 
 */
public class AGBooleanQuery extends AGQuery implements BooleanQuery {

	/**
	 * Creates an AGBooleanQuery instance for a given connection.
	 * 
	 * @param con the connection
	 * @param ql the query language
	 * @param queryString the query
	 * @param baseURI the base URI for the query
	 */
	public AGBooleanQuery(AGRepositoryConnection con, QueryLanguage ql,
			String queryString, String baseURI) {
		super(con, ql, queryString, baseURI);
	}

	/**
	 * Evaluates the query and returns a boolean result.
	 */
	public boolean evaluate() throws QueryEvaluationException {
		AGResponseHandler handler = new AGResponseHandler(true);
		try {
			httpCon.getHttpRepoClient().query(handler,
					queryLanguage, queryString, dataset, includeInferred, planner,
					getBindingsArray());
		} catch (HttpException e) {
			throw new QueryEvaluationException(e);
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		} catch (RDFParseException e) {
			throw new QueryEvaluationException(e);
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
		return handler.getBoolean();
	}

}
