package com.franz.agraph.jena;

import com.franz.agraph.repository.AGRepositoryConnection;
import com.hp.hpl.jena.graph.GraphMaker;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class AGGraphMaker implements GraphMaker {

	private AGRepositoryConnection conn;
	private AGGraph defaultGraph;
	
	public AGGraphMaker(AGRepositoryConnection conn) {
		this.conn = conn;
	}

	public AGRepositoryConnection getRepositoryConnection() {
		return conn;
	}

	@Override
	public void close() {
	}

	@Override
	public AGGraph createGraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AGGraph createGraph(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AGGraph createGraph(String arg0, boolean arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AGGraph getGraph() {
		if (defaultGraph==null) {
			defaultGraph = new AGGraph(this, null);
		}
		return defaultGraph;
	}

	@Override
	public ReificationStyle getReificationStyle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasGraph(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ExtendedIterator<String> listGraphs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AGGraph openGraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AGGraph openGraph(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AGGraph openGraph(String arg0, boolean arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeGraph(String arg0) {
		// TODO Auto-generated method stub

	}

}
