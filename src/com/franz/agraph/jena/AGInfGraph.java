/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import static com.franz.agraph.repository.AGVirtualRepository.filteredSpec;
import static com.franz.agraph.repository.AGVirtualRepository.reasoningSpec;

import java.util.Iterator;

import org.openrdf.model.Resource;
import org.openrdf.query.Dataset;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.repository.AGAbstractRepository;
import com.franz.agraph.repository.AGVirtualRepository;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Implements the Jena InfGraph interface for AllegroGraph.
 * 
 */
public class AGInfGraph extends AGGraph implements InfGraph {

	private final AGReasoner reasoner;
	private final AGGraph rawGraph;
	
	AGVirtualRepository infRepo;
	
	AGInfGraph(AGReasoner reasoner, AGGraph rawGraph) {
		super(rawGraph.getGraphMaker(), rawGraph.getGraphContext(), new Resource[0]);
		this.reasoner = reasoner;
		this.rawGraph = rawGraph;
		entailmentRegime = reasoner.getEntailmentRegime();
		if (rawGraph.getGraphContexts().length>0) {
			// create a reasoning, graph-filtered store over rawGraph's contexts
			AGAbstractRepository repo = rawGraph.getConnection().getRepository();
			String infSpec = reasoningSpec(filteredSpec(repo,rawGraph.getGraphContexts()),entailmentRegime);
			infRepo = repo.getCatalog().getServer().virtualRepository(infSpec);
			try {
				conn = infRepo.getConnection();
			} catch (RepositoryException e) {
				throw new RuntimeException(e);
			}
			vf = conn.getValueFactory();
		} else {
			// for this common case of reasoning over the whole store,
			// no need to create a reasoning, graph-filtered store
			infRepo = null;
		}
	}

	@Override 
	public void close() {
		if (infRepo!=null) {
			try {
				conn.close();
				infRepo.close();
			} catch (RepositoryException e) {
				throw new RuntimeException(e);
			}
		}
		super.close();
	}
	
	@Override
	Dataset getDataset() {
		// use the whole underlying repository, it is designed to
		// contain just the right set of graphs.
		DatasetImpl dataset = new DatasetImpl();
		return dataset;
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
