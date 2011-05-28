/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import java.io.File;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.util.Closeable;

/**
 * Implements the Sesame Repository interface for AllegroGraph, representing
 * <a href="http://www.franz.com/agraph/support/documentation/v4/agraph-introduction.html#triple-store">triple-stores
 * on the server</a>.
 * In AllegroGraph, a {@link AGCatalog} contains multiple repositories.
 * With the Sesame API, most data operations on a repository are done through
 * the {@link AGRepositoryConnection} returned by {@link #getConnection()}.
 */
public class AGRepository implements AGAbstractRepository, Closeable {

	private final AGCatalog catalog;
	private final String repositoryID;
	private final String catalogPrefixedRepositoryID;
	private final String repositoryURL;
	private final AGValueFactory vf;

	/**
	 * Creates an AGRepository instance for a repository having the given
	 * repository id in the given catalog.
	 * 
	 * <p>Preferred access is from {@link AGCatalog} methods
	 * such as {@link AGCatalog#createRepository(String, boolean)}
	 * or {@link AGCatalog#openRepository(String)}.
	 * </p>
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
	 * The AllegroGraph URL of this repository.
	 * 
	 * @return the URL of this repository.
	 */
	public String getRepositoryURL() {
		return repositoryURL;
	}
	
	public AGValueFactory getValueFactory() {
		return vf;
	}

	/**
	 * The http connection to AllegroGraph server.
	 */
	public AGHTTPClient getHTTPClient() {
		return getCatalog().getHTTPClient();
	}

	/**
	 * Required by OpenRDF/Sesame, a repository must be initialized before use.
	 */
	public void initialize() throws RepositoryException {
	}

	/**
	 * Create a connection to the repository.
	 */
	public AGRepositoryConnection getConnection() throws RepositoryException {
		AGHttpRepoClient repoclient = new AGHttpRepoClient(this, getCatalog().getHTTPClient(), repositoryURL, null);
		return new AGRepositoryConnection(this, repoclient);
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

	public String getSpec() {
		String cname = getCatalog().getCatalogName(), name = getRepositoryID();
		if (AGCatalog.isRootID(cname)) return "<" + name + ">";
		else return "<" + cname + ":" + name + ">";
	}

	/**
	 * The dataDir is not currently applicable to AllegroGraph.
	 * @deprecated not applicable to AllegroGraph
	 */
	public File getDataDir() {
		return null;
	}

	/**
	 * The dataDir is not currently applicable to AllegroGraph. 
	 * @deprecated not applicable to AllegroGraph
	 */
	public void setDataDir(File dataDir) {
		// TODO: consider using this for client-side logging
	}

    @Override
	public void shutDown() throws RepositoryException {
	}

	/**
	 * Calls Sesame method {@link #shutDown()}.
	 */
    @Override
    public void close() throws RepositoryException {
        shutDown();
    }

    /**
     * Sets the repository's bulkMode (defaults to false).
     * 
     * When in bulkMode, data can be added/loaded more quickly, but 
     * there is no guarantee of durability in the event of a crash.
     * The bulkMode setting persists when the repository is closed.
     * 
     * @see #isBulkMode()
     */
	public void setBulkMode(boolean bulkMode) throws RepositoryException {
		String url = repositoryURL + "/bulkMode";
		Header[] headers = new Header[0];
		NameValuePair[] data = {};
		try {
			if (bulkMode) {
				getHTTPClient().put(url, headers, data, null);
			} else {
				getHTTPClient().delete(url, headers, data);
			}
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Returns the repository's bulkMode setting.
	 *  
	 * @return a boolean indicating the bulkMode setting.
	 * @see #setBulkMode(boolean)
	 */
	public boolean isBulkMode() throws RepositoryException {
		try {
			return Boolean.parseBoolean(getHTTPClient().getString(repositoryURL+"/bulkMode"));
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}
	
	/**
	 * Sets the repository's deleteDuplicates mode (defaults to "false").
	 * 
	 * The deleteDuplicates mode must be one of: "false" (do not delete
	 * duplicates), "true" (delete duplicate quads), or "spo" (delete
	 * duplicate triples, ignoring graph).
	 *  
	 * @param mode a String indicating the mode. 
	 * @see #getDeleteDuplicatesMode()
	 */
	public void setDeleteDuplicatesMode(String mode) throws RepositoryException {
		String url = repositoryURL + "/deleteDuplicates";
		Header[] headers = new Header[0];
		try {
			if (mode.equalsIgnoreCase("false") ||
					mode.equalsIgnoreCase("true") ||
					mode.equalsIgnoreCase("spo")) {
				NameValuePair[] data = {new NameValuePair("type",mode)};
				getHTTPClient().put(url,headers,data,null);
			} else {
				throw new IllegalArgumentException("Illegal mode: " + mode + "(must be one of: false, true, spo).");
			}
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}
	
	/**
	 * Returns the repository's deleteDuplicates mode.
	 *  
	 * @return a String indicating the deleteDuplicates mode.
	 * @see #setDeleteDuplicatesMode(String)
	 */
	public String getDeleteDuplicatesMode() throws RepositoryException {
		try {
			return getHTTPClient().getString(repositoryURL+"/deleteDuplicates");
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}

    /**
     * Forces a checkpoint for this repository. 
     * 
     * This is an internal and undocumented function.
     */
	public void forceCheckpoint() throws RepositoryException {
		String url = repositoryURL + "/force-checkpoint";
		Header[] hdr = new Header[0];
		NameValuePair[] data = {};
		getHTTPClient().post(url,hdr,data,null,null);
	}
    /**
     * Waits until background db processes have gone idle.
     * 
     * This is an internal and undocumented function.
     */
	public void ensureDBIdle() throws RepositoryException {
		String url = repositoryURL + "/ensure-db-idle";
		Header[] hdr = new Header[0];
		NameValuePair[] data = {};
		getHTTPClient().post(url,hdr,data,null,null);
	}

}
