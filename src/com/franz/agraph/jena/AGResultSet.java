/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
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

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

public class AGResultSet implements ResultSet {

	private final TupleQueryResult result;
	private final AGModel model;
	
	public AGResultSet(TupleQueryResult result, AGModel model) {
		this.result = result;
		this.model = model;
	}

	@Override
	public Model getResourceModel() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public List<String> getResultVars() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
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
	public QuerySolution next() {
		BindingSet bs;
		try {
			bs = result.next();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
		return new AGQuerySolution(bs, model);
	}

	@Override
	public Binding nextBinding() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public QuerySolution nextSolution() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);

	}

}
