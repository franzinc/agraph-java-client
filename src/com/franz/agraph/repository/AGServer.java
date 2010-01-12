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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGErrorType;
import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpException;
import com.franz.agraph.http.AGProtocol;
import com.franz.util.Util;
import com.franz.util.Closeable;

/**
 * The top-level class for interacting with an AllegroGraph server.
 */
public class AGServer implements Closeable {

	private final String serverURL;
	private final AGHTTPClient httpClient;
	private final AGCatalog rootCatalog;
	private final AGCatalog federatedCatalog;
	
	
	/**
	 * Creates an AGServer instance for interacting with an AllegroGraph
	 * server at serverURL.
	 *     
	 * @param serverURL the URL of the server.
	 * @param username a user id for authenticating with the server 
	 * @param password a password for authenticating with the server  
	 */
	public AGServer(String serverURL, String username, String password) {
		this.serverURL = serverURL;
		httpClient = new AGHTTPClient(serverURL); 
		httpClient.setUsernameAndPassword(username, password);
		rootCatalog = new AGCatalog(this,AGCatalog.ROOT_CATALOG);
		federatedCatalog = new AGCatalog(this,AGCatalog.FEDERATED_CATALOG);
	}
	
	/**
	 * Returns the URL of this AllegroGraph server.
	 * 
	 * @return the URL of this AllegroGraph server. 
	 */
	public String getServerURL() {
		return serverURL;
	}
	
	AGHTTPClient getHTTPClient() {
		return httpClient;
	}
	
	/**
	 * Returns the root catalog for this AllegroGraph server.
	 * 
	 * @return the root catalog. 
	 */
	public AGCatalog getRootCatalog() {
		return rootCatalog;
	}
	
	/**
	 * Returns the catalog housing all federations for this AllegroGraph server.
	 * 
	 * @return the federated catalog. 
	 */
	public AGCatalog getFederatedCatalog() {
		return federatedCatalog;
	}
	
	/**
	 * Returns a List of catalog ids known to this AllegroGraph server.
	 * 
	 * @return List of catalog ids.
	 * @throws OpenRDFException
	 */
	public List<String> listCatalogs() throws OpenRDFException {
		String url = AGProtocol.getNamedCatalogsURL(serverURL);
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
	 * Gets the catalog instance for a given catalog id.
	 * 
	 * @param catalogID a catalog id.
	 * @return the corresponding catalog instance.
	 */
	public AGCatalog getCatalog(String catalogID) {
		return new AGCatalog(this, catalogID);
	}

	/**
	 * Creates a federation over the specified component repositories,
	 * naming it by federationName.
	 *  
	 * @param federationName the name of the federation.
	 * @param repositories the component repositories.
	 * @return the federation instance.
	 * @throws RepositoryException
	 */
	public AGRepository createFederation(String federationName, AGRepository... repositories) 
	throws RepositoryException {
		String url = AGProtocol.getFederationLocation(getServerURL(), federationName); 
		Header[] headers = {};
		List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		for (AGRepository repo : repositories) {
			params.add(new NameValuePair(AGProtocol.REPO_PARAM_NAME, repo.getCatalogPrefixedRepositoryID()));
			// TODO: deal with remote repositories
			// params.add(new NameValuePair(AGProtocol.URL_PARAM_NAME, repo.getRepositoryURL()));
		}
		try {
			getHTTPClient().put(url,headers,params.toArray(new NameValuePair[params.size()]),null);
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
		return new AGRepository(getFederatedCatalog(),federationName);
	}
	
	/**
	 * Deletes a federation by name.
	 * 
	 * @param federationName the name of the federation to delete.
	 * @throws RepositoryException
	 */
	public void deleteFederation(String federationName)	throws RepositoryException {
		String url = AGProtocol.getFederationLocation(getServerURL(), federationName); 
		Header[] headers = new Header[0];
		try {
			getHTTPClient().delete(url,headers,null);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

    @Override
    public void close() {
        Util.close(httpClient);
    }
	
}
