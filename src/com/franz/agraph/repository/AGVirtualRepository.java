package com.franz.agraph.repository;

import java.io.File;

import org.openrdf.model.BNode;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.util.Closeable;

public class AGVirtualRepository implements AGAbstractRepository, Closeable {
	private AGServer server;
	private AGRepository wrapped;
	private String spec;
	private AGValueFactory vf;

	private class AGFederatedValueFactory extends AGValueFactory {
		public AGFederatedValueFactory() {
			super(null);
		}

		public BNode createBNode(String nodeID) {
			throw new RuntimeException("Can not create a blank node for a federated store.");
		}
	}

	public AGVirtualRepository(AGServer server, String spec, AGRepository wrapped) {
		this.server = server;
		this.spec = spec;
		this.wrapped = wrapped;
		if (wrapped == null)
			vf = new AGFederatedValueFactory();
		else
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
	public static String federatedSpec(String[] repos) {
		String spec = "";
		for (int i = 0; i < repos.length; i++) {
			if (spec.length() > 0) spec += " + ";
			spec += repos[i];
		}
		return spec;
	}
	public static String reasoningSpec(String repo, String reasoner) {
		return repo + "[" + reasoner + "]";
	}
	public static String filteredSpec(String repo, String[] graphs) {
		repo += "{";
		for (String graph : graphs) repo += " " + graph;
		return repo + "}";
	}
}
