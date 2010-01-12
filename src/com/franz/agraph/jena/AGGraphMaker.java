/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.util.Closeable;
import com.hp.hpl.jena.graph.GraphMaker;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class AGGraphMaker implements GraphMaker, Closeable {

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
	public AGGraph getGraph() {
		if (defaultGraph==null) {
			defaultGraph = new AGGraph(this, null);
		}
		return defaultGraph;
	}

	@Override
	public AGGraph createGraph() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public AGGraph createGraph(String uri) {
		return createGraph(uri, false);
	}

	@Override
	public AGGraph createGraph(String uri, boolean strict) {
		// TODO: strictness
		return new AGGraph(this, Node.createURI(uri));
	}

	@Override
	public ReificationStyle getReificationStyle() {
		return ReificationStyle.Minimal;
	}

	@Override
	public boolean hasGraph(String name) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public ExtendedIterator<String> listGraphs() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public AGGraph openGraph() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public AGGraph openGraph(String name) {
		return openGraph(name, false);
	}

	@Override
	public AGGraph openGraph(String uri, boolean strict) {
		// TODO deal with strictness
		return new AGGraph(this, Node.createURI(uri));
	}

	@Override
	public void removeGraph(String name) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

}
