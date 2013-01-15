/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import org.openrdf.model.Statement;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.StatementCollector;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGRDFHandler;

public class AGFreetextQuery {

	private final AGRepositoryConnection conn;
	
	protected String pattern;
	protected String expression;
	protected String index;
	protected boolean sorted = false;
	protected int limit = 0;
	protected int offset = 0;
	
	public AGFreetextQuery(AGRepositoryConnection conn) {
		this.conn = conn;
	}
	
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	
	public void setExpression(String expression) {
		this.expression = expression;
	}
	
	public void setIndex(String index) {
		this.index = index;
	}
	
	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public void setOffset(int offset) {
		this.offset = offset;
	}
	
	public RepositoryResult<Statement> evaluate() throws QueryEvaluationException {
		try {
			// TODO: make this efficient for large result sets
			StatementCollector collector = new StatementCollector();
			evaluate(collector);
			return conn.createRepositoryResult(collector.getStatements());
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
			conn.getHttpRepoClient().evalFreetextQuery(pattern, expression, index, sorted, limit, offset, 
						new AGRDFHandler(conn.getHttpRepoClient().getPreferredRDFFormat(), handler, conn.getValueFactory(),conn.getHttpRepoClient().getAllowExternalBlankNodeIds()));
		} catch (AGHttpException e) {
			// TODO: distinguish RDFHandlerException
			throw new QueryEvaluationException(e);
		}
	}

}
