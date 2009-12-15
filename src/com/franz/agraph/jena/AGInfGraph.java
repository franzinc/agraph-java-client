/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class AGInfGraph extends AGGraph implements InfGraph {

	private final AGReasoner reasoner;
	private final AGGraph rawGraph;
	
	AGInfGraph(AGReasoner reasoner, AGGraph rawGraph) {
		super(rawGraph.getGraphMaker(), rawGraph.getGraphNode());
		this.reasoner = reasoner;
		this.rawGraph = rawGraph;
		inferred = true;
	}

	@Override
	public ExtendedIterator<Triple> find(Node subject, Node property,
			Node object, Graph param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Graph getDeductionsGraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Derivation> getDerivation(Triple triple) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node getGlobalProperty(Node property) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AGGraph getRawGraph() {
		return rawGraph;
	}

	@Override
	public Reasoner getReasoner() {
		return reasoner;
	}

	@Override
	public void prepare() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rebind() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rebind(Graph data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDerivationLogging(boolean logOn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean testGlobalProperty(Node property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ValidityReport validate() {
		// TODO Auto-generated method stub
		return null;
	}


}
