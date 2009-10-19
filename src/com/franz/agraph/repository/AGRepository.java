/**
 * 
 */
package com.franz.agraph.repository;

import java.io.File;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGHTTPClient;

/**
 *
 */
public class AGRepository implements Repository {

	private final AGCatalog catalog;
	private final String repositoryID;
	private final String catalogPrefixedRepositoryID;
	private final String repositoryURL;
	private final AGValueFactory vf;

	private File dataDir;
	private boolean initialized = false;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public AGRepository(AGCatalog catalog, String repositoryID) {
		this.catalog = catalog;
		this.repositoryID = repositoryID;
		catalogPrefixedRepositoryID = catalog.getCatalogPrefixedRepositoryID(repositoryID);
		repositoryURL = catalog.getRepositoryURL(repositoryID);
		vf = new AGValueFactory(this);
	}

	public AGCatalog getCatalog() {
		return catalog;
	}
	
	public String getRepositoryID() {
		return repositoryID;
	}
	
	public String getCatalogPrefixedRepositoryID() {
		return catalogPrefixedRepositoryID;
	}
	
	public String getRepositoryURL() {
		return repositoryURL;
	}
	
	public boolean isFederation() {
		return (AGCatalog.FEDERATED_CATALOG == getCatalog().getCatalogType());
	}
	
	public AGValueFactory getValueFactory() {
		return vf;
	}

	public AGHTTPClient getHTTPClient() {
		return getCatalog().getHTTPClient();
	}
	
	public void initialize() throws RepositoryException {
		if (!isFederation()) vf.initialize();
		initialized = true;
	}

	public AGRepositoryConnection getConnection() throws RepositoryException {
		return new AGRepositoryConnection(this);
	}

	public File getDataDir() {
		return dataDir;
	}

	public boolean isWritable() throws RepositoryException {
		if (!initialized) {
			throw new IllegalStateException("AGRepository not initialized.");
		}
		// TODO need a better request to determine this than requesting 
		// a list of repositories and extracting this one's writable value
		return true;
	}

	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	public void shutDown() throws RepositoryException {
		initialized = false;
	}

}
