/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

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
	public static final String SPARQL_COVERAGE_PLANNER = "coverage";  // TODO add to protocol
	
	private static long prepareId = 0L;
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
	
	protected String saveName = null;
	
	protected boolean prepared = false;
	
	public AGQuery(AGRepositoryConnection con, QueryLanguage ql, String queryString, String baseURI) {
		this.httpCon = con;
		this.queryLanguage = ql;
		this.queryString = queryString;
		this.baseURI = baseURI;
	}

	/**
	 * Gets the query language for this query.
	 * 
	 * @return the query language.
	 */
	public QueryLanguage getLanguage() {
		return queryLanguage;
	}
	
	/**
	 * Gets the query string for this query.
	 * 
	 * @return the query string.
	 */
	public String getQueryString() {
		return queryString;
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
	
	/**
	 * Schedules the query to be prepared.
	 */
	synchronized public void prepare() {
		setSaveName(String.valueOf(prepareId++));
	}
	
	/**
	 * Sets the savedName for the prepared query.
	 * 
	 * @return the saved name.
	 */
	private void setSaveName(String name) {
		saveName = name;
	}
	
	/**
	 * Gets the savedName for the query.
	 * 
	 * @return the saved name.
	 */
	public String getName() {
		return saveName;
	}
	
	/**
	 * Gets the prepared flag for the query.
	 * 
	 * @return the prepared flag.
	 */
	public boolean isPrepared() {
		return prepared;
	}
	
	/**
	 * Sets the prepared flag for the query.
	 * 
	 * @param the prepared flag.
	 */
	public void setPrepared(boolean prepared) {
		this.prepared = prepared;
	}
	
	public Binding[] getBindingsArray() {
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

	
	@Override
	protected void finalize() {
		if (saveName!=null) { 
			httpCon.getHttpRepoClient().savedQueryDeleteQueue.add(saveName);
		}
	}

}
