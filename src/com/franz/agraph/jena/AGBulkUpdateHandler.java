/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.repository.AGValueFactory;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.SimpleBulkUpdateHandler;

/**
 * Implements the Jena BulkUpdateHandler interface for AllegroGraph.
 * 
 */
public class AGBulkUpdateHandler extends SimpleBulkUpdateHandler implements
		BulkUpdateHandler {

	private final AGGraph graph;

	public AGBulkUpdateHandler(AGGraph graph) {
		super(graph);
		this.graph = graph;
	}

	@Override
	protected void add(List<Triple> triples, boolean notify) {
		// TODO speed this up
		AGValueFactory vf = graph.getConnection().getValueFactory();
		List<Statement> statements = new ArrayList<Statement>(triples.size());
		for (Triple tr: triples) {
			statements.add(new StatementImpl(vf.asResource(tr.getSubject()),vf.asURI(tr.getPredicate()),vf.asValue(tr.getObject())));
		}
		try {
			graph.getConnection().add(statements, graph.getGraphContext());
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
		if (notify)
			manager.notifyAddList(graph, triples);
	}

	@Override
	protected void delete(List<Triple> triples, boolean notify) {
		// TODO speed this up
		AGValueFactory vf = graph.getConnection().getValueFactory();
		List<Statement> statements = new ArrayList<Statement>(triples.size());
		for (Triple tr: triples) {
			statements.add(new StatementImpl(vf.asResource(tr.getSubject()),vf.asURI(tr.getPredicate()),vf.asValue(tr.getObject())));
		}
		try {
			graph.getConnection().remove(statements, graph.getGraphContext());
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
		if (notify)
			manager.notifyDeleteList(graph, triples);
	}
	
	@Override
	public void removeAll() {
		try {
			graph.getConnection().clear(graph.getGraphContexts());
			notifyRemoveAll(); 
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}
}
