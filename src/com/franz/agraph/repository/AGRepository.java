/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

/**
 * 
 */
package com.franz.agraph.repository;

import java.io.File;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.util.Closeable;

/**
 * Implements the Sesame Repository interface for AllegroGraph.
 * 
 */
public class AGRepository implements Repository, Closeable {

	private final AGCatalog catalog;
	private final String repositoryID;
	private final String catalogPrefixedRepositoryID;
	private final String repositoryURL;
	private final AGValueFactory vf;

	private File dataDir;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates an AGRepository instance for a repository having the given
	 * repository id in the given catalog.  
	 */
	public AGRepository(AGCatalog catalog, String repositoryID) {
		this.catalog = catalog;
		this.repositoryID = repositoryID;
		catalogPrefixedRepositoryID = catalog.getCatalogPrefixedRepositoryID(repositoryID);
		repositoryURL = catalog.getRepositoryURL(repositoryID);
		vf = new AGValueFactory(this);
	}

	/**
	 * Gets the catalog to which this repository belongs.
	 * 
	 * @return the catalog.
	 */
	public AGCatalog getCatalog() {
		return catalog;
	}
	
	/**
	 * Gets the repository id for this repository.
	 * 
	 * @return the repository id.
	 */
	public String getRepositoryID() {
		return repositoryID;
	}
	
	public String getCatalogPrefixedRepositoryID() {
		return catalogPrefixedRepositoryID;
	}
	
	/**
	 * Gets the URL of this repository.
	 * 
	 * @return the URL of this repository.
	 */
	public String getRepositoryURL() {
		return repositoryURL;
	}
	
	/**
	 * Returns true iff this repository is a federation.
	 * 
	 * @return true iff this repository is a federation.
	 */
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
	}

	public AGRepositoryConnection getConnection() throws RepositoryException {
		return new AGRepositoryConnection(this);
	}

	/**
	 * The dataDir is not currently applicable to AllegroGraph.
	 */
	public File getDataDir() {
		return dataDir;
	}

	/**
	 * Returns true iff this repository is writable.
	 */
	public boolean isWritable() throws RepositoryException {
		String url = getCatalog().getRepositoriesURL();
		TupleQueryResult tqresult = getHTTPClient().getTupleQueryResult(url);
		Value writable = null;
		boolean result;
        try {
            while (null==writable && tqresult.hasNext()) {
                BindingSet bindingSet = tqresult.next();
                Value uri = bindingSet.getValue("uri");
                if (uri.stringValue().equals(getRepositoryURL())) {
                	writable = bindingSet.getValue("writable");
                }
            }
        } catch (QueryEvaluationException e) {
        	throw new RepositoryException(e);
        } finally {
            try {
				tqresult.close();
			} catch (QueryEvaluationException e) {
				throw new RepositoryException(e);
			}
        }
        if (null==writable) {
        	throw new IllegalStateException("Repository not found in catalog's list of repositories: " + getRepositoryURL());
        }
        result = Boolean.parseBoolean(writable.stringValue());
        return result;
	}

	/**
	 * The dataDir is not currently applicable to AllegroGraph. 
	 */
	public void setDataDir(File dataDir) {
		// TODO: consider using this for client-side logging
		this.dataDir = dataDir;
	}

	public void shutDown() throws RepositoryException {
	}

    @Override
    public void close() throws RepositoryException {
        shutDown();
    }

}
