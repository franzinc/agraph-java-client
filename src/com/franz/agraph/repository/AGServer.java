/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
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
import org.openrdf.http.protocol.UnauthorizedException;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpException;
import com.franz.agraph.http.AGProtocol;
import com.franz.util.Closeable;
import com.franz.util.Util;

/**
 * The top-level class for interacting with an AllegroGraph server.
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
	 * Returns an AGCatalog instance for the given catalogID.
	 * 
	 * The catalog is dynamically created if it does not exist.
	 * 
	 * @param catalogID the id (the name) of the catalog
	 * @return an AGCatalog instance.
	 * @throws AGHttpException
	 */
	public AGCatalog createCatalog(String catalogID)
			throws AGHttpException, IOException, UnauthorizedException {
		return createCatalog(catalogID, false);
	}
	
	/**
	 * Returns an AGCatalog instance for the given catalogID.
	 * 
	 * The catalog is dynamically created if it does not exist,
	 * or an exception is thrown if strict=true.
	 * 
	 * @param catalogID the id (the name) of the catalog
	 * @param strict if true and the catalog already exists, throw an exception
	 * @return an AGCatalog instance.
	 * @throws AGHttpException
	 */
	public AGCatalog createCatalog(String catalogID, boolean strict)
	throws AGHttpException, IOException, UnauthorizedException {
		String catalogURL = AGProtocol.getNamedCatalogLocation(getServerURL(),
				catalogID);
		if (strict || !hasCatalog(catalogID)) {
			getHTTPClient().putCatalog(catalogURL);
		}
		return new AGCatalog(this, catalogID);
	}
	
	/**
	 * Returns true if the catalog id is known to the server.
	 * 
	 * @return true if the catalog id is known to the server.
	 * @throws AGHttpException
	 */
	public boolean hasCatalog(String catalogId) throws AGHttpException {
		List<String> catalogs;
		try {
			catalogs = listCatalogs();
		} catch (OpenRDFException e) {
			throw new AGHttpException(e.getMessage());
		}
		return catalogs.contains(catalogId);
	}

	/**
	 * Deletes the catalog with the given repository id.
	 * 
	 * This method only applies to dynamically created catalogs.
	 * 
	 * @param catalogID the name of the catalog to delete.
	 * @throws AGHttpException 
	 * @throws IOException 
	 * @throws UnauthorizedException 
	 */
	public void deleteCatalog(String catalogID) throws UnauthorizedException, IOException, AGHttpException {
		String catalogURL = AGProtocol.getNamedCatalogLocation(getServerURL(),
				catalogID);
		getHTTPClient().deleteCatalog(catalogURL);
	}

	public AGVirtualRepository virtualRepository(String spec) {
		return new AGVirtualRepository(this, spec, null);
	}

	public AGVirtualRepository federate(AGAbstractRepository... repositories) {
		String[] specstrings = new String[repositories.length];
		for (int i = 0; i < repositories.length; i++)
			specstrings[i] = repositories[i].getSpec();
		String spec = AGVirtualRepository.federatedSpec(specstrings);

		return new AGVirtualRepository(this, spec, null);
	}

    @Override
    public void close() {
        Util.close(httpClient);
    }
	
}
