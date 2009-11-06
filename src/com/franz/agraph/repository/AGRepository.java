/**
 * 
 */
package com.franz.agraph.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGProtocol;

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
		initialized = true;
	}

	public AGRepositoryConnection getConnection() throws RepositoryException {
		return new AGRepositoryConnection(this);
	}

	public File getDataDir() {
		return dataDir;
	}

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

	public void setDataDir(File dataDir) {
		// TODO: consider using this for client-side logging
		this.dataDir = dataDir;
	}

	public void shutDown() throws RepositoryException {
		initialized = false;
	}

}
