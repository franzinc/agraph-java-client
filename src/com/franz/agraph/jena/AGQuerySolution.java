/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import java.util.Iterator;

import org.openrdf.query.BindingSet;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class AGQuerySolution implements QuerySolution {

	private final BindingSet bs;
	private final AGModel model;
	
	public AGQuerySolution(BindingSet bs, AGModel model) {
		this.bs = bs;
		this.model = model;
	}

	@Override
	public boolean contains(String varName) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public RDFNode get(String varName) {
		return model.asRDFNode(AGNodeFactory.asNode(bs.getValue(varName)));
	}

	@Override
	public Literal getLiteral(String varName) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public Resource getResource(String varName) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public Iterator<String> varNames() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public String toString() {
		return bs.toString();
	}
}
