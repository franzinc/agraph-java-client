package com.franz.agraph.jena;

import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class AGBulkUpdateHandler implements BulkUpdateHandler {

	private final AGGraph graph;
	
	public AGBulkUpdateHandler(AGGraph graph) {
		this.graph = graph;
	}

	@Override
	public void add(Triple[] triples) {
		// TODO Auto-generated method stub

	}

	@Override
	public void add(List<Triple> triples) {
		graph.getConnection(); //TODO: .add(triples.asStatements(), graph.getGraphContext());
	}

	@Override
	public void add(Iterator<Triple> it) {
		// TODO Auto-generated method stub

	}

	@Override
	public void add(Graph g) {
		// TODO Auto-generated method stub

	}

	@Override
	public void add(Graph g, boolean withReifications) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Triple[] triples) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(List<Triple> triples) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Iterator<Triple> it) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Graph g) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Graph g, boolean withReifications) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(Node s, Node p, Node o) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAll() {
		// TODO Auto-generated method stub

	}

}
