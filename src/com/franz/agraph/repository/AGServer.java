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
import org.openrdf.query.TupleQueryResult;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpException;
import com.franz.agraph.http.AGProtocol;
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
	 * Returns the root catalog if the id is a root id.
	 *    
	 * @param catalogID a catalog id.
	 * @return the corresponding catalog instance.
	 */
	public AGCatalog getCatalog(String catalogID) {
		AGCatalog catalog;
		if (AGCatalog.isRootID(catalogID)) {
			catalog = getRootCatalog();
		} else {
			catalog = new AGCatalog(this, catalogID);
		}
		return catalog;
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
	 * Used by AllegroGraph Jena implementation.
	 */
	public AGVirtualRepository virtualRepository(String spec) {
		return new AGVirtualRepository(this, spec, null);
	}

	/**
	 * Create a federated view of multiple repositories.
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
