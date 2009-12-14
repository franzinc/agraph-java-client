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
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.helpers.StatementCollector;

import com.franz.agraph.http.AGResponseHandler;

/**
 * Implements the Sesame GraphQuery interface for AllegroGraph.
 * 
 */
public class AGGraphQuery extends AGQuery implements GraphQuery {

	/**
	 * Creates an AGGraphQuery instance for the given connection.
	 * 
	 * @param con the connection.
	 * @param ql the query language.
	 * @param queryString the query.
	 * @param baseURI the base URI for the query.
	 */
	public AGGraphQuery(AGRepositoryConnection con, QueryLanguage ql,
			String queryString, String baseURI) {
		super(con, ql, queryString, baseURI);
	}

	/**
	 * Evaluates the query and returns a GraphQueryResult.
	 */
	public GraphQueryResult evaluate() throws QueryEvaluationException {
		try {
			// TODO: make this efficient for large result sets
			StatementCollector collector = new StatementCollector();
			evaluate(collector);
			return new GraphQueryResultImpl(collector.getNamespaces(),
					collector.getStatements());
		} catch (RDFHandlerException e) {
			// Found a bug in StatementCollector?
			throw new RuntimeException(e);
		}
	}

	/**
	 * Evaluates the query and uses handler to process the result. 
	 */
	public void evaluate(RDFHandler handler) throws QueryEvaluationException,
			RDFHandlerException {
		try {
			// TODO: deal with the hard coded return format
			httpCon.getHttpRepoClient().query(
					new AGResponseHandler(httpCon.getRepository(), handler,
							RDFFormat.NTRIPLES), queryLanguage, queryString,
					dataset, includeInferred, planner, getBindingsArray());
		} catch (HttpException e) {
			throw new QueryEvaluationException(e);
		} catch (RepositoryException e) {
		    throw new QueryEvaluationException(e);
		} catch (RDFParseException e) {
		    throw new QueryEvaluationException(e);
		} catch (IOException e) {
		    throw new QueryEvaluationException(e);
		}
	}

}
