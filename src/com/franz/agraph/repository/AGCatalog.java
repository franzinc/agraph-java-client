/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGErrorType;
import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpException;
import com.franz.agraph.http.AGProtocol;

/**
 * Implements the notion of a catalog in an AllegroGraph server. 
 * 
 * A catalog is a grouping of repositories.  The root catalog is the 
 * default, unnamed catalog that is available with every server.  It
 * is also possible to create any number of named catalogs.  There is 
 * also a special catalog that groups federations in the server.
 */
public class AGCatalog {

	private AGServer server;
	private final String catalogName;
	private final int catalogType;
	private final String catalogURL;
	private final String repositoriesURL;

	public static final int ROOT_CATALOG = 0;
	public static final int FEDERATED_CATALOG = 1;
	public static final int NAMED_CATALOG = 2;

	/**
	 * Creates an AGCatalog instance for a named catalog having catalogName
	 * in the given server.
	 * 
	 * @param server the server housing this named catalog.
	 * @param catalogName the name for this named catalog.
	 */
	public AGCatalog(AGServer server, String catalogName) {
		this.server = server;
		this.catalogName = catalogName;
		this.catalogType = NAMED_CATALOG;
		catalogURL = AGProtocol.getNamedCatalogLocation(server.getServerURL(), catalogName);
		repositoriesURL = AGProtocol.getNamedCatalogRepositoriesLocation(catalogURL);
	}

	/**
	 * Creates an AGCatalog instance for a special catalog in the given server,
	 * either the root catalog or the federated catalog.
	 * 
	 * @param server the server housing the catalog.
	 * @param catalogType the type of the special catalog.
	 */
	public AGCatalog(AGServer server, int catalogType) {
		switch (catalogType) {
		case ROOT_CATALOG:
			catalogName = "/";
			catalogURL = AGProtocol.getRootCatalogURL(server.getServerURL());
			repositoriesURL = AGProtocol.getRootCatalogRepositoriesLocation(catalogURL);
			break;
		case FEDERATED_CATALOG:
			catalogName = "federated";
			catalogURL = AGProtocol.getFederatedCatalogURL(server.getServerURL());
			repositoriesURL = AGProtocol.getFederatedRepositoriesLocation(catalogURL);
			break;
		default:
			throw new IllegalArgumentException("Invalid Catalog Type: "	+ catalogType);
		}
		this.server = server;
		this.catalogType = catalogType;
	}

	/**
	 * Returns the AGServer instance for this catalog.
	 *  
	 * @return the AGServer instance for this catalog.
	 */
	public AGServer getServer() {
		return server;
	}

	/**
	 * Returns the name of this catalog.  The root and federated catalog
	 * also have names, "/" and "federated", respectively.
	 * 
	 * @return the name of this catalog.
	 */
	public String getCatalogName() {
		return catalogName;
	}

	/**
	 * Returns the type of this catalog.
	 * 
	 * @return the type of this catalog.
	 */
	public int getCatalogType() {
		return catalogType;
	}
	
	/**
	 * Returns the URL of this catalog.
	 * 
	 * @return the URL of this catalog.
	 */
	public String getCatalogURL() {
		return catalogURL;
	}

	/**
	 * Returns the URL for accessing the repositories of this catalog.
	 * 
	 * @return the URL for accessing the repositories of this catalog.
	 */
	public String getRepositoriesURL() {
		return repositoriesURL;
	}
	
	protected String getCatalogPrefixedRepositoryID(String repositoryID) {
		String catalogPrefixedRepositoryID;
		switch (getCatalogType()) {
		case ROOT_CATALOG:
			catalogPrefixedRepositoryID = repositoryID;
			break;
		case FEDERATED_CATALOG:
		case NAMED_CATALOG:
			catalogPrefixedRepositoryID = getCatalogName() + ":" + repositoryID;
			break;
		default:
			throw new IllegalArgumentException("Invalid Catalog Type: "	+ catalogType);
		}
		return catalogPrefixedRepositoryID;
	}

	// TODO this should be part of AGProtocol.
	public String getRepositoryURL(String repositoryID) {
		return repositoriesURL + "/" + repositoryID;
	}
	
	/**
	 * Returns a List of repository ids contained in this catalog.
	 * 
	 * @return a List of repository ids contained in this catalog.
	 * @throws OpenRDFException
	 */
	public List<String> listRepositories() throws OpenRDFException {
		String url = getRepositoriesURL();
		TupleQueryResult tqresult = getHTTPClient().getTupleQueryResult(url);
		List<String> result = new ArrayList<String>(5);
        try {
            while (tqresult.hasNext()) {
                BindingSet bindingSet = tqresult.next();
                Value id = bindingSet.getValue("id");
                result.add(id.stringValue());
            }
        } finally {
            tqresult.close();
        }
        return result;
	}

	public AGHTTPClient getHTTPClient() {
		return getServer().getHTTPClient();
	}

	/**
	 * Returns an uninitialized AGRepository instance for the given 
	 * repository id.
	 * 
	 * The repository is created if it does not exist.  If the
	 * repository already exists, it is simply opened. 
	 * 
	 * @param repositoryID the id (the name) of the repository. 
	 * @return an uninitialized AGRepository instance.
	 * @throws RepositoryException
	 */
	public AGRepository createRepository(String repositoryID)
			throws RepositoryException {
		String repoURL = AGProtocol.getRepositoryLocation(getCatalogURL(),
				repositoryID);
		try {
			getHTTPClient().putRepository(repoURL);
		} catch (IOException e) {
			throw new RepositoryException(e);
		} catch (AGHttpException e) {
			if (AGErrorType.PRECONDITION_FAILED == e.getErrorInfo()
					.getErrorType()) {
				// don't error if repo already exists
				// TODO: check if repo exists first
			} else {
				throw new RepositoryException(e);
			}
		}
		return new AGRepository(this, repositoryID);
	}

	/**
	 * Deletes the repository with the given repository id.
	 * 
	 * @param repositoryID the name of the repository to delete.
	 * @throws RepositoryException
	 */
	public void deleteRepository(String repositoryID)
			throws RepositoryException {
		String repoURL = AGProtocol.getRepositoryLocation(getCatalogURL(),
				repositoryID);
		try {
			getHTTPClient().deleteRepository(repoURL);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

}
