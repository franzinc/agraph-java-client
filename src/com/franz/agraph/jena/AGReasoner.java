/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerException;

public class AGReasoner implements Reasoner {

	@Override
	public void addDescription(Model configSpec, Resource base) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AGInfGraph bind(Graph data) throws ReasonerException {
		if (!(data instanceof AGGraph)) {
			throw new IllegalArgumentException("Only AGGraphs are supported.");
		}
		return new AGInfGraph(this,(AGGraph)data);
	}

	@Override
	public Reasoner bindSchema(Graph tbox) throws ReasonerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reasoner bindSchema(Model tbox) throws ReasonerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Capabilities getGraphCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model getReasonerCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDerivationLogging(boolean logOn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setParameter(Property parameterUri, Object value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean supportsProperty(Property property) {
		// TODO Auto-generated method stub
		return false;
	}


}
