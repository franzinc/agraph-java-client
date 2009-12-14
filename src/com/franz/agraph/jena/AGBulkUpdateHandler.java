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
}
