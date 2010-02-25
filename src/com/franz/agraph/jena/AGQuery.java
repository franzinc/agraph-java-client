/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import org.openrdf.query.QueryLanguage;

import com.franz.agraph.repository.AGQueryLanguage;

public class AGQuery {

	private final QueryLanguage language;
	private final String queryString;
	
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
	
}
