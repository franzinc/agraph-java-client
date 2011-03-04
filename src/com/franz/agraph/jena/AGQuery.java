/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
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
	
}
