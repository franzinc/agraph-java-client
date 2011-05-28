/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;

import com.franz.agraph.repository.AGBooleanQuery;
import com.franz.agraph.repository.AGGraphQuery;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.util.Closeable;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.util.FileManager;

/**
 * Implements the Jena QueryExecution interface for AllegroGraph.
 * 
 */
public class AGQueryExecution implements QueryExecution, Closeable {

	private final AGQuery query;
	private final AGModel model;
	
	public AGQueryExecution(AGQuery query, AGModel model) {
		this.query = query;
		this.model = model;
	}

	
	@Override
	public void abort() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public void close() {
	}

	@Override
	public boolean execAsk() {
		if (query.getLanguage()!=QueryLanguage.SPARQL) {
			throw new UnsupportedOperationException(query.getLanguage().getName() + " language does not support ASK queries.");
		}
		AGBooleanQuery bq = model.getGraph().getConnection().prepareBooleanQuery(query.getLanguage(), query.getQueryString());
		bq.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
		bq.setEntailmentRegime(model.getGraph().getEntailmentRegime());
		bq.setCheckVariables(query.isCheckVariables());
		boolean result;
		try {
			bq.setDataset(model.getGraph().getDataset());
			result = bq.evaluate();
		} catch (QueryEvaluationException e) {
			throw new QueryException(e);
		}
		return result;
	}

	@Override
	public Model execConstruct() {
		return execConstruct(null);
	}

	@Override
	public Model execConstruct(Model m) {
		if (query.getLanguage()!=QueryLanguage.SPARQL) {
			throw new UnsupportedOperationException(query.getLanguage().getName() + " language does not support CONSTRUCT queries.");
		}
		AGGraphQuery gq = model.getGraph().getConnection().prepareGraphQuery(query.getLanguage(), query.getQueryString());
		gq.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
		gq.setEntailmentRegime(model.getGraph().getEntailmentRegime());
		gq.setCheckVariables(query.isCheckVariables());
		gq.setLimit(query.getLimit());
		gq.setOffset(query.getOffset());
		GraphQueryResult result;
		try {
			gq.setDataset(model.getGraph().getDataset());
			result = gq.evaluate();
		} catch (QueryEvaluationException e) {
			throw new QueryException(e);
		}
		if (m==null) {
			m = ModelFactory.createDefaultModel();
		}
		try {
			m.setNsPrefixes(result.getNamespaces());
			while (result.hasNext()) {
				m.add(model.asStatement(AGNodeFactory.asTriple(result.next())));
			}
		} catch (QueryEvaluationException e) {
			throw new QueryException(e);
		}
		return m;
	}

	@Override
	public Model execDescribe() {
		return execDescribe(null);
	}

	@Override
	public Model execDescribe(Model m) {
		return execConstruct(m);
	}

	@Override
	public ResultSet execSelect() {
		AGTupleQuery tq = model.getGraph().getConnection().prepareTupleQuery(query.getLanguage(), query.getQueryString());
		tq.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
		tq.setEntailmentRegime(model.getGraph().getEntailmentRegime());
		tq.setCheckVariables(query.isCheckVariables());
		tq.setLimit(query.getLimit());
		tq.setOffset(query.getOffset());
		TupleQueryResult result;
		try {
			tq.setDataset(model.getGraph().getDataset());
			result = tq.evaluate();
		} catch (QueryEvaluationException e) {
			throw new QueryException(e);
		}
		return new AGResultSet(result, model);
	}

	public long countSelect() {
		AGTupleQuery tq = model.getGraph().getConnection().prepareTupleQuery(query.getLanguage(), query.getQueryString());
		tq.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
		tq.setEntailmentRegime(model.getGraph().getEntailmentRegime());
		tq.setCheckVariables(query.isCheckVariables());
		tq.setLimit(query.getLimit());
		tq.setOffset(query.getOffset());
		tq.setDataset(model.getGraph().getDataset());
		long count;
		try {
			count = tq.count();
		} catch (QueryEvaluationException e) {
			throw new QueryException(e);
		}
		return count;
	}
	
	public long countConstruct() {
		if (query.getLanguage()!=QueryLanguage.SPARQL) {
			throw new UnsupportedOperationException(query.getLanguage().getName() + " language does not support CONSTRUCT queries.");
		}
		AGGraphQuery gq = model.getGraph().getConnection().prepareGraphQuery(query.getLanguage(), query.getQueryString());
		gq.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
		gq.setEntailmentRegime(model.getGraph().getEntailmentRegime());
		gq.setCheckVariables(query.isCheckVariables());
		gq.setLimit(query.getLimit());
		gq.setOffset(query.getOffset());
		gq.setDataset(model.getGraph().getDataset());
		long count;
		try {
			count = gq.count();
		} catch (QueryEvaluationException e) {
			throw new QueryException(e);
		}
		return count;
	}
	
	@Override
	public Context getContext() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public Dataset getDataset() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public void setFileManager(FileManager fm) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public void setInitialBinding(QuerySolution binding) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

}
