/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import org.openrdf.query.QueryLanguage;

import com.franz.agraph.repository.AGQueryLanguage;

/**
 * The class of queries that can be posed to AllegroGraph via Jena.  
 *
 */
public class AGQuery {

	private final QueryLanguage language;
	private final String queryString;
	
	private boolean checkVariables = false;
	private int limit = -1;
	private int offset = -1;
	
	AGQuery(String queryString) {
		this.language = AGQueryLanguage.SPARQL;
		this.queryString = queryString;
	}
	
	public AGQuery(QueryLanguage language, String queryString) {
		this.language = language;
		this.queryString = queryString;
	}

	public QueryLanguage getLanguage() {
		return language;
	}

	public String getQueryString() {
		return queryString;
	}
	
	/**
	 * Gets the flag for checkVariables.
	 * 
	 * @return the checkVariables flag.
	 */
	public boolean isCheckVariables() {
		return checkVariables;
	}
	
	/**
	 * A boolean that defaults to false, indicating whether an error
	 * should be raised when a SPARQL query selects variables that
	 * are not mentioned in the query body.
	 * 
	 * @param checkVariables the checkVariables flag.
	 */
	public void setCheckVariables(boolean checkVariables) {
		this.checkVariables = checkVariables;
	}
	
	/**
	 * Gets the limit on the number of solutions for this query.
	 * 
	 * @return limit
	 */
	public int getLimit() {
		return limit;
	}

	/**
	 * Sets the limit on the number of solutions for this query.
	 * 
	 * By default, the value is -1, meaning no constraint is imposed.
	 */
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	/**
	 * Gets the offset, the number of solutions to skip for this query.
	 * 
	 * @return offset
	 */
	public int getOffset() {
		return offset;
	}
	
	/**
	 * Sets the offset, the number of solutions to skip for this query.
	 * 
	 * By default, the value is -1, meaning no constraint is imposed.
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}
	
}
