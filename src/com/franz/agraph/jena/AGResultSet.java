/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import java.util.List;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

/**
 * Implements the Jena ResultSet interface for AllegroGraph.
 * 
 */
public class AGResultSet implements ResultSet {

	private final TupleQueryResult result;
	private final AGModel model;
	
	public AGResultSet(TupleQueryResult result, AGModel model) {
		this.result = result;
		this.model = model;
	}

	@Override
	public Model getResourceModel() {
		return model;
	}

	@Override
	public List<String> getResultVars() {
		return result.getBindingNames();
	}

	@Override
	public int getRowNumber() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public boolean hasNext() {
		boolean res;
		try {
			res = result.hasNext();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
		return res;
	}

	@Override
	public AGQuerySolution next() {
		BindingSet bs;
		try {
			bs = result.next();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
		return new AGQuerySolution(bs, model);
	}

	@Override
	/**
	 * This method is not supported.  Use next() instead and iterate
	 * over the returned QuerySolution.
	 * 
	 * @see #next()
	 * 
	 */
	public Binding nextBinding() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public AGQuerySolution nextSolution() {
		return next();
	}

	@Override
	public void remove() {
		try {
			result.remove();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
	}

}
