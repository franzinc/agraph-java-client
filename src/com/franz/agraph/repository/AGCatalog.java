/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import static com.franz.agraph.http.AGProtocol.encode;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGProtocol;
import com.franz.agraph.http.exception.AGHttpException;

/**
 * Catalogs are locations on disk where AllegroGraph Server keeps its repositories.
 * 
 * <p>Catalogs are created by the
 * <a href="http://www.franz.com/agraph/support/documentation/v4/daemon-config.html#catalog">catalog
 * server configuration</a>.</p>
 * 
 * <p>A catalog is a grouping of repositories.  The root catalog is the 
 * default, unnamed catalog that is available with every server.  It
 * is also possible to create any number of named catalogs.
 * </p>
 * 
 * @see AGServer#getCatalog(String)
 */
public class AGCatalog {

	private AGServer server;
	private final String catalogName;
	private final int catalogType;
	private final String catalogURL;
	private final String repositoriesURL;

	/**
	 * @see #getCatalogType()
	 */
	public static final int ROOT_CATALOG = 0;
	
	/**
	 * @see #getCatalogType()
	 */
	public static final int NAMED_CATALOG = 2;

	/**
	 * Creates an AGCatalog instance for a named catalog having catalogName
	 * in the given server.
	 * 
	 * @param server the server housing this named catalog.
	 * @param catalogName the name for this named catalog.
	 * @see AGServer#getCatalog(String)
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
	 * such as the root catalog.
	 * 
	 * @param server the server housing the catalog.
	 * @param catalogType the type of the special catalog.
	 * @see AGServer#getCatalog()
	 */
	public AGCatalog(AGServer server, int catalogType) {
		switch (catalogType) {
		case ROOT_CATALOG:
			catalogName = "/";
			catalogURL = AGProtocol.getRootCatalogURL(server.getServerURL());
			repositoriesURL = AGProtocol.getRootCatalogRepositoriesLocation(catalogURL);
			break;
		default:
			throw new IllegalArgumentException("Invalid Catalog Type: "	+ catalogType);
		}
		this.server = server;
		this.catalogType = catalogType;
	}

	/**
	 * The AGServer instance for this catalog.
	 *  
	 * @return the AGServer instance for this catalog.
	 */
	public AGServer getServer() {
		return server;
	}

	/**
	 * The name of this catalog.  The root catalog has the name "/".
	 * 
	 * @return the name of this catalog.
	 */
	public String getCatalogName() {
		return catalogName;
	}

	/**
	 * The type of this catalog.
	 * 
	 * @return the type of this catalog, {@link #ROOT_CATALOG} or {@link #NAMED_CATALOG}
	 */
	public int getCatalogType() {
		return catalogType;
	}
	
	/**
	 * URL of this catalog.
	 * 
	 * @return the URL of this catalog.
	 */
	public String getCatalogURL() {
		return catalogURL;
	}

	/**
	 * URL for accessing the repositories of this catalog.
	 * 
	 * @return the URL for accessing the repositories of this catalog.
	 */
	public String getRepositoriesURL() {
		return repositoriesURL;
	}
	
	// TODO this should be part of AGProtocol.
	public String getRepositoryURL(String repositoryID) {
		return repositoriesURL + "/" + encode(repositoryID);
	}
	
	public AGHTTPClient getHTTPClient() {
		return getServer().getHTTPClient();
	}	

	protected String getCatalogPrefixedRepositoryID(String repositoryID) {
		String catalogPrefixedRepositoryID;
		switch (getCatalogType()) {
		case ROOT_CATALOG:
			catalogPrefixedRepositoryID = repositoryID;
			break;
		case NAMED_CATALOG:
			catalogPrefixedRepositoryID = getCatalogName() + ":" + repositoryID;
			break;
		default:
			throw new IllegalArgumentException("Invalid Catalog Type: "	+ catalogType);
		}
		return catalogPrefixedRepositoryID;
	}

	/**
	 * Returns a List of repository ids contained in this catalog.
	 * 
	 * @return a List of repository ids contained in this catalog.
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

	/**
	 * Returns true if the repository id is contained in this catalog.
	 * 
	 * @return true if the repository id is contained in this catalog.
	 */
	public boolean hasRepository(String repoId) throws OpenRDFException {
		List<String> repos = listRepositories();
		return repos.contains(repoId);
	}
	       
	/**
	 * Returns an {@link Repository#initialize() uninitialized}
	 * AGRepository instance for the given
	 * {@link #listRepositories() repository id}.
	 * 
	 * The repository is created if it does not exist.  If the
	 * repository already exists, it is simply opened. 
	 * 
	 * @param repositoryID the id (the name) of the repository. 
	 * @return an uninitialized AGRepository instance.
	 * @see #createRepository(String, boolean)
	 */
	public AGRepository createRepository(String repositoryID)
			throws RepositoryException {
		return createRepository(repositoryID, false);
	}

	/**
	 * Returns an {@link Repository#initialize() uninitialized}
	 * AGRepository instance for the given
	 * {@link #listRepositories() repository id}.
	 * 
	 * The repository is created if it does not exist.  If the
	 * repository already exists, it is simply opened, or an exception
	 * is thrown if strict=true.
	 * 
	 * @param repositoryID the id (the name) of the repository
	 * @param strict if true and the repository already exists, throw an exception
	 * @return an uninitialized AGRepository instance.
	 * @see #createRepository(String)
	 * @see #listRepositories()
	 * @see #openRepository(String)
	 */
	public AGRepository createRepository(String repositoryID, boolean strict)
			throws RepositoryException {
		String repoURL = AGProtocol.getRepositoryLocation(getCatalogURL(),
				repositoryID);
		try {
			if (strict || !hasRepository(repositoryID)) {
				getHTTPClient().putRepository(repoURL);
			}
		} catch (OpenRDFException e) {
			// TODO: modify hasRepository, shouldn't need to do this 
			throw new RepositoryException(e);
		}
		return new AGRepository(this, repositoryID);
	}

	/**
	 * Returns an uninitialized AGRepository instance for the given 
	 * {@link #listRepositories() repository id}.
	 * 
	 * If the repository already exists, it is simply opened.
	 * 
	 * @param repositoryID the id (the name) of the repository. 
	 * @return an uninitialized AGRepository instance.
	 * @throws RepositoryException if the repositoryID does not exist
	 * @see #createRepository(String, boolean)
	 */
	public AGRepository openRepository(String repositoryID)
			throws RepositoryException {
		try {
			if (!hasRepository(repositoryID)) {
				throw new RepositoryException("Repository not found with ID: " + repositoryID);
			}
		} catch (OpenRDFException e) {
			// TODO: consider having methods in this class all throw OpenRDFExceptions
			throw new RepositoryException(e);
		}
		return new AGRepository(this, repositoryID);
	}
	/**
	 * Deletes the repository with the given
	 * {@link #listRepositories() repository id}.
	 * 
	 * @param repositoryID the name of the repository to delete.
	 * @see #listRepositories()
	 */
	public void deleteRepository(String repositoryID)
			throws RepositoryException {
		String repoURL = AGProtocol.getRepositoryLocation(getCatalogURL(),
				repositoryID);
		try {
			getHTTPClient().deleteRepository(repoURL);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Returns true iff the id identifies the root catalog.
	 * 
	 * Currently null, the empty string, and "/" are all considered to identify
	 * the root catalog.
	 * 
	 * @return true iff the id identifies the root catalog.
	 */
	public static boolean isRootID(String catalogID) {
		boolean result = false;
		if (catalogID == null || catalogID.equals("") || catalogID.equals("/")) {
			result = true;
		}
		return result;
	}

}
