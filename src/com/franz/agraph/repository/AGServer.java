/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGProtocol;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.util.Closeable;
import com.franz.util.Closer;

/**
 * The starting point for interacting with an
 * <a target="_top"
 *    href="http://www.franz.com/agraph/support/documentation/v4/agraph-introduction.html"
 *    >AllegroGraph server</a>.
 * 
 * An AGServer {@link #listCatalogs() references} {@link AGCatalog}s,
 * which {@link AGCatalog#listRepositories() reference}
 * {@link AGRepository AGRepositories}, from which
 * {@link AGRepositoryConnection connections} may be obtained,
 * on which data is manipulated and queried.
 * AGServer also provides {@link #federate(AGAbstractRepository...) federated} repositories.
 */
public class AGServer implements Closeable {

	private final String serverURL;
	private final AGHTTPClient httpClient;
	private final AGCatalog rootCatalog;
	
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
	}
	
	public AGServer(String username, String password, AGHTTPClient httpClient) {
		this.serverURL = httpClient.getServerURL();
		this.httpClient = httpClient; 
		this.httpClient.setUsernameAndPassword(username, password);
		rootCatalog = new AGCatalog(this,AGCatalog.ROOT_CATALOG);
	}
	
	/**
	 * Returns the URL of this AllegroGraph server.
	 * 
	 * @return the URL of this AllegroGraph server. 
	 */
	public String getServerURL() {
		return serverURL;
	}
	
	public AGHTTPClient getHTTPClient() {
		return httpClient;
	}
	
	/**
	 * Returns the server version.
	 */
	public String getVersion() throws AGHttpException {
		return getHTTPClient().getString(serverURL + "/version");
	}
	
	/**
	 * Returns the server's build date.
	 */
	public String getBuildDate() throws AGHttpException {
		return getHTTPClient().getString(serverURL + "/version/date");
	}
	
	/**
	 * Returns the server's revision info.
	 */
	public String getRevision() throws AGHttpException {
		return getHTTPClient().getString(serverURL + "/version/revision");
	}
	
	/**
	 * Returns the unnamed root catalog for this AllegroGraph server.
	 * Note: this method may be deprecated in an upcoming release.
	 * 
	 * @return the root catalog.
	 * @see #getCatalog() 
	 */
	public AGCatalog getRootCatalog() {
		return rootCatalog;
	}
	
	/**
	 * Returns a List of catalog ids known to this AllegroGraph server.
	 * 
	 * @return List of catalog ids.
	 * @throws OpenRDFException
	 */
	public List<String> listCatalogs() throws AGHttpException {
		String url = AGProtocol.getNamedCatalogsURL(serverURL);
		TupleQueryResult tqresult = getHTTPClient().getTupleQueryResult(url);
		List<String> result = new ArrayList<String>(5);
        try {
            while (tqresult.hasNext()) {
                BindingSet bindingSet = tqresult.next();
                Value id = bindingSet.getValue("id");
                result.add(id.stringValue());
            }
        } catch (QueryEvaluationException e) {
        	throw new AGHttpException(e);
        } 
        return result;
	}
	
	/**
	 * Gets the catalog instance for a given catalog id.
	 * 
	 * Returns the root catalog if the id is a root id.  If
	 * the catalog Id is not found on the server,  returns
	 * null.
	 *    
	 * @param catalogID a catalog id.
	 * @return the corresponding catalog instance.
	 * @throws AGHttpException
	 */
	public AGCatalog getCatalog(String catalogID) throws AGHttpException {
		if (AGCatalog.isRootID(catalogID)) {
			return rootCatalog;
		} else if (listCatalogs().contains(catalogID)) {
			return new AGCatalog(this, catalogID);
		} else {
			return null;
		}
	}

	/**
	 * Returns the unnamed root catalog for this AllegroGraph server.
	 * 
	 * @return the root catalog. 
	 */
	public AGCatalog getCatalog() {
		return rootCatalog;
	}

	/**
	 * Returns an AGCatalog instance for the given catalogID.
	 * 
	 * If the catalog already exists on the server, an AGCatalog
	 * instance is simply returned.  If the catalog does not exist,
	 * it is created if the server has been configured to allow 
	 * <a href="http://www.franz.com/agraph/support/documentation/current/daemon-config.html#DynamicCatalogs">
	 * dynamic catalogs</a>; otherwise, an exception is thrown.
	 * 
	 * @param catalogID the id (the name) of the catalog
	 * @return an AGCatalog instance.
	 * @throws AGHttpException
	 */
	public AGCatalog createCatalog(String catalogID) throws AGHttpException {
		AGCatalog catalog = getCatalog(catalogID);
		if (catalog==null) {
			String catalogURL = AGProtocol.getNamedCatalogLocation(getServerURL(), catalogID);
			getHTTPClient().putCatalog(catalogURL);
			catalog = new AGCatalog(this, catalogID);
		}
		return catalog;
	}
	
	/**
	 * Deletes any catalog with the given repository id.
	 * 
	 * This method only applies to dynamically created catalogs.
	 * 
	 * @param catalogID the name of the catalog to delete.
	 * @throws AGHttpException 
	 */
	public void deleteCatalog(String catalogID) throws AGHttpException {
		String catalogURL = AGProtocol.getNamedCatalogLocation(getServerURL(),
				catalogID);
		getHTTPClient().deleteCatalog(catalogURL);
	}

	/**
	 * Creates a virtual repository with the given store specification. 
	 * <p>
	 * The storeSpec parameter is a string using the  
	 * <a href="http://www.franz.com/agraph/support/documentation/v4/http-protocol.html#post-session"
	 * target="_top">minilanguage for store specification</a> described in the HTTP protocol document 
	 * (see the store parameter there).
	 * <p>
	 * This syntax can be used to create federations, graph-filtered stores, 
	 * reasoning stores, and compositions thereof. 
	 * 
	 * @param storeSpec the store specification 
	 */
	public AGVirtualRepository virtualRepository(String storeSpec) {
		return new AGVirtualRepository(this, storeSpec, null);
	}

	/**
	 * Creates a federated view of multiple repositories.
	 * 
	 * See <a href="http://www.franz.com/agraph/support/documentation/v4/agraph-introduction.html#intro-federation">Managing
	 * Massive Data - Federation</a>.
	 * @return a virtual repository that federates queries across multiple physical repositories
	 */
	public AGVirtualRepository federate(AGAbstractRepository... repositories) {
		String[] specstrings = new String[repositories.length];
		for (int i = 0; i < repositories.length; i++)
			specstrings[i] = repositories[i].getSpec();
		String spec = AGVirtualRepository.federatedSpec(specstrings);

		return new AGVirtualRepository(this, spec, null);
	}

	/**
	 * Close the HTTP connection resources to the AllegroGraph server.
	 */
    @Override
    public void close() {
        Closer.Close(httpClient);
    }
	
}
