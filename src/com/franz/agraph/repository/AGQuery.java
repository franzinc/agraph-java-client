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

/**
 * An abstract query class common to Boolean, Graph and Tuple Queries.
 * 
 */
public abstract class AGQuery extends AbstractQuery {

	/**
	 * The default query planner for SPARQL.
	 */
	public static final String SPARQL_COVERAGE_PLANNER = "coverage";  // TODO add these to protocol
	
	/**
	 * A query planner for SPARQL that processes queries without doing
	 * and reordering of clauses or optimization, useful if the user
	 * knows the best order for processing the query.
	 */
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

	/**
	 * Gets the query planner that processes the query.
	 * 
	 * @return the planner name.
	 */
	public String getPlanner() {
		return planner;
	}
	
	/**
	 * Sets the query planner to use when processing the query.
	 * 
	 * @param planner the planner name.
	 */
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
