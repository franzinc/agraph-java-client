/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import java.util.Iterator;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;

/**
 * Implements the Jena InfModel interface for AllegroGraph.
 * 
 */
public class AGInfModel extends AGModel implements InfModel {
	
	private final AGReasoner reasoner;
	private final AGModel baseModel;
	
	public AGInfModel(AGReasoner reasoner, AGModel baseModel) {
		super(reasoner.bind(baseModel.getGraph()));
		this.reasoner = reasoner;
		this.baseModel = baseModel;
	}

	@Override
	public Model getDeductionsModel() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public Iterator<Derivation> getDerivation(Statement statement) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public Model getRawModel() {
		return baseModel;
	}

	@Override
	public Reasoner getReasoner() {
		return reasoner;
	}

	@Override
	public StmtIterator listStatements(Resource subject, Property predicate,
			RDFNode object, Model posit) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public void prepare() {
	}

	@Override
	public void rebind() {
	}

	@Override
	public void reset() {
	}

	@Override
	public void setDerivationLogging(boolean logOn) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public ValidityReport validate() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}


}
