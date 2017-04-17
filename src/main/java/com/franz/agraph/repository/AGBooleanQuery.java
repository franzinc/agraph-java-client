/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.agraph.repository;

import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;

import com.franz.agraph.http.handler.AGBQRHandler;

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
		AGBQRHandler handler = new AGBQRHandler();
		evaluate(handler);
		return handler.getResult();
	}

}
