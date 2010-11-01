/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.httpclient.HttpException;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.impl.AbstractQuery;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;

import com.franz.agraph.http.AGResponseHandler;

/**
 * An abstract query class common to Boolean, Graph and Tuple Queries.
 * 
 */
public abstract class AGQuery extends AbstractQuery {

	/**
	 * The default query planner for SPARQL.
	 */
	public static final String SPARQL_COVERAGE_PLANNER = "coverage";  // TODO add to protocol
	
	/**
	 * A query planner for SPARQL that processes queries without doing
	 * any reordering of clauses or optimization, useful if the user
	 * knows the best order for processing the query.
	 */
	public static final String SPARQL_IDENTITY_PLANNER = "identity";

	/**
	 * The default entailment regime to use when inferences are included.
	 */
	public static final String RDFS_PLUS_PLUS = "rdfs++";

	/**
	 * An entailment regime that includes hasValue, someValuesFrom and
	 * allValuesFrom reasoning in addition to RDFS++ entailment.
	 */
	public static final String RESTRICTION = "restriction";

	//private static long prepareId = 0L;
	
	protected AGRepositoryConnection httpCon;

	protected QueryLanguage queryLanguage;

	protected String queryString;

	protected String baseURI;
	
	protected String entailmentRegime = RDFS_PLUS_PLUS; 
	
	protected String planner;
	
	protected String saveName = null;
	
	protected boolean prepared = false;
	
	public AGQuery(AGRepositoryConnection con, QueryLanguage ql, String queryString, String baseURI) {
		super.setIncludeInferred(false); // set default
		this.httpCon = con;
		this.queryLanguage = ql;
		this.queryString = queryString;
		this.baseURI = baseURI;
		// AG queries exclude inferences by default
		super.includeInferred = false; 
	}

	/**
	 * Determine whether evaluation results of this query should include inferred
	 * statements (if any inferred statements are present in the repository). The
	 * default setting is 'false'.
	 * 
	 * @param includeInferred
	 *        indicates whether inferred statements should included in the
	 *        result.
	 * @see #setEntailmentRegime(String)
	 */
	@Override
	public void setIncludeInferred(boolean includeInferred) {
		super.setIncludeInferred(includeInferred);
	}
	
	/**
	 * Sets the entailment regime to use when including inferences with this
	 * query.  Default is 'rdfs++'.  
	 * 
	 * @param entailmentRegime
	 *        indicates the entailment regime to use when reasoning.
	 * @see #RDFS_PLUS_PLUS       
	 * @see #RESTRICTION       
	 * @see #setIncludeInferred(boolean)
	 */
	public void setEntailmentRegime(String entailmentRegime) {
		this.entailmentRegime = entailmentRegime; 
	}
	
	/**
	 * Gets the entailment regime being used when including inferences
	 * with this query.
	 * 
	 */
	public String getEntailmentRegime() {
		return entailmentRegime; 
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
	 * 
	 * Note: this is a no-op pending further cost-benefit analysis of
	 * the server's saved query service.
	 */
	synchronized void prepare() {
		//setSaveName(String.valueOf(prepareId++));
	}
	
	/**
	 * Sets the name to use when saving this query with the
	 * server's saved query service.
	 * 
	 * @param name the saved name.
	 */
	public void setSaveName(String name) {
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
	 * @param prepared the prepared flag.
	 */
	public void setPrepared(boolean prepared) {
		this.prepared = prepared;
	}
	
	/**
	 * Evaluates the query and processes the result in handler.
	 * 
	 * @param handler processes or stores the result
	 * @throws QueryEvaluationException
	 */
	protected void evaluate(AGResponseHandler handler)
			throws QueryEvaluationException {
		evaluate(false, handler);
	}
	
	/**
	 * Evaluates the query and processes the result in handler.
	 * 
	 * When the analyzeOnly flag is true, only a query analysis is
	 * performed; when false, the query is executed.
	 * 
	 * @param analyzeOnly flags for analyzing or executing
	 * @param handler processes or stores the result
	 * @throws QueryEvaluationException
	 */
	protected void evaluate(boolean analyzeOnly, AGResponseHandler handler)
			throws QueryEvaluationException {
		try {
			httpCon.getHttpRepoClient().query(this, analyzeOnly, handler);
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

	/**
	 * Returns the query analysis for the query.
	 * 
	 * The query is not evaluated.
	 * 
	 * @return the query analysis as a string.
	 * @throws QueryEvaluationException
	 */
	public String analyze() throws QueryEvaluationException {
		AGResponseHandler handler = new AGResponseHandler("");
		evaluate(true, handler);
		return handler.getString();
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
	public String toString() {
		return queryString;
	}

	
	@Override
	protected void finalize() {
		if (saveName!=null) { 
			httpCon.getHttpRepoClient().savedQueryDeleteQueue.add(saveName);
		}
	}

}
