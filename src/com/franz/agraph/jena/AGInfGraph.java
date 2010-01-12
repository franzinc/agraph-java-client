/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
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
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public Graph getDeductionsGraph() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public Iterator<Derivation> getDerivation(Triple triple) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public Node getGlobalProperty(Node property) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
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
	}

	@Override
	public void rebind() {
	}

	@Override
	public void rebind(Graph data) {
	}

	@Override
	public void reset() {
	}

	@Override
	public void setDerivationLogging(boolean logOn) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public boolean testGlobalProperty(Node property) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public ValidityReport validate() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}


}
