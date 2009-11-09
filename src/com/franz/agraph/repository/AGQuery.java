/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package com.franz.agraph.repository;

import java.util.Iterator;

import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.impl.AbstractQuery;


public abstract class AGQuery extends AbstractQuery {

	public static final String SPARQL_COVERAGE_PLANNER = "coverage";  // TODO confirm and add to protocol
	public static final String SPARQL_IDENTITY_PLANNER = "identity";
	
	protected AGRepositoryConnection httpCon;

	protected QueryLanguage queryLanguage;

	protected String queryString;

	protected String baseURI;

	protected String planner;
	
	public AGQuery(AGRepositoryConnection con, QueryLanguage ql, String queryString, String baseURI) {
		this.httpCon = con;
		this.queryLanguage = ql;
		this.queryString = queryString;
		this.baseURI = baseURI;
	}

	public String getPlanner() {
		return planner;
	}
	
	public void setPlanner(String planner) {
		this.planner = planner;
	}
	
	protected Binding[] getBindingsArray() {
		BindingSet bindings = this.getBindings();

		Binding[] bindingsArray = new Binding[bindings.size()];

		Iterator<Binding> iter = bindings.iterator();
		for (int i = 0; i < bindings.size(); i++) {
			bindingsArray[i] = iter.next();
		}

		return bindingsArray;
	}

	@Override
	public String toString()
	{
		return queryString;
	}
}
