/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import java.io.File;

import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.util.Closeable;

public class AGVirtualRepository implements AGAbstractRepository, Closeable {
	private final AGServer server;
	final AGRepository wrapped;
	private final String spec;
	private final AGValueFactory vf;

	public AGVirtualRepository(AGServer server, String spec, AGRepository wrapped) {
		this.server = server;
		this.spec = spec;
		this.wrapped = wrapped;
		vf = new AGValueFactory(wrapped);
	}

	public AGServer getServer() {
		return server;
	}
	public AGCatalog getCatalog() {
		return null;
	}
	public String getSpec() {
		return spec;
	}

	// interface
	public boolean isWritable() {
		return wrapped != null;
	}
	public AGValueFactory getValueFactory() {
		return vf;
	}
	public AGRepositoryConnection getConnection() throws RepositoryException {
		AGHTTPClient client = server.getHTTPClient();
		AGHttpRepoClient repoclient = new AGHttpRepoClient(this, client, null, client.openSession(spec, true));
		return new AGRepositoryConnection(this, repoclient);
	}
    public void close() throws RepositoryException {
        shutDown();
    }

	// stubs
	public void initialize() {}
	public void shutDown() throws RepositoryException {}
	public void setDataDir(File dataDir) {
		throw new RuntimeException("setDataDir is inapplicable for AG repositories");
	}
	public File getDataDir() {
		throw new RuntimeException("getDataDir is inapplicable for AG repositories");
	}

	// string-mangling utilities for creating sessions
	public static String federatedSpec(String[] repoSpecs) {
		String spec = "";
		for (int i = 0; i < repoSpecs.length; i++) {
			if (spec.length() > 0) spec += " + ";
			spec += repoSpecs[i];
		}
		return spec;
	}
	
	public static String reasoningSpec(String repoSpec, String reasoner) {
		return reasoningSpec(repoSpec,reasoner,null);
	}
	
	public static String reasoningSpec(String repoSpec, String reasoner, Resource inferredGraph) {
		String reasoningSpec = repoSpec + "[" + reasoner;
		if (null!=inferredGraph && !inferredGraph.equals("")) {
			reasoningSpec += ("#<" + inferredGraph.stringValue() + ">");
		}
		return reasoningSpec + "]";
	}

	public static String filteredSpec(AGAbstractRepository repo, Resource[] contexts) {
		String[] graphs = new String[contexts.length];
		for (int i=0;i<contexts.length;i++) {
			if (null==contexts[i]) {
				graphs[i] = null;
			} else {
				graphs[i] = "<"+contexts[i].stringValue()+">";
			}
		}
		return filteredSpec(repo.getSpec(),graphs);
	}
	
	public static String filteredSpec(String repoSpec, String[] graphs) {
		repoSpec += "{";
		for (String graph : graphs) repoSpec += " " + graph;
		return repoSpec + "}";
	}
}
