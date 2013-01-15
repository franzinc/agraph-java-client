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
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;

import com.franz.agraph.http.handler.AGBQRHandler;

/**
 * Implements the Sesame Update interface for AllegroGraph.
 * 
 */
public class AGUpdate extends AGQuery implements Update {

	/**
	 * Creates an AGUpdate instance for a given connection.
	 * 
	 * @param con the connection
	 * @param ql the query language
	 * @param queryString the query
	 * @param baseURI the base URI for the query
	 */
	public AGUpdate(AGRepositoryConnection con, QueryLanguage ql,
			String queryString, String baseURI) {
		super(con, ql, queryString, baseURI);
	}

	/**
	 * Execute the update.
	 */
	@Override
	public void execute() throws UpdateExecutionException {
		AGBQRHandler handler = new AGBQRHandler();
		try {
			evaluate(handler);
		} catch (QueryEvaluationException e) {
			throw new UpdateExecutionException(e);
		}
	}

}
