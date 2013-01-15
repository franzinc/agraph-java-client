/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.json.JSONArray;
import org.openrdf.OpenRDFException;
import org.openrdf.http.protocol.Protocol;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGProtocol;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGJSONArrayHandler;
import com.franz.util.Closeable;
import com.franz.util.Closer;

/**
 * The starting point for interacting with an
 * <a target="_top"
 *    href="http://www.franz.com/agraph/support/documentation/current/agraph-introduction.html"
 *    >AllegroGraph server</a>.
 * 
 * An AGServer {@link #listCatalogs() references} {@link AGCatalog}s,
 * which {@link AGCatalog#listRepositories() reference}
 * {@link AGRepository AGRepositories}, from which
 * {@link AGRepositoryConnection connections} may be obtained,
 * on which data is manipulated and queried.
 * <p>
 * AGServer provides methods for <a target="_top"
 *    href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#header2-223"
 *    >User (and Role) Management</a>.
 * <p> 
 * AGServer also provides {@link #federate(AGAbstractRepository...) federated} repositories.
 */
public class AGServer implements Closeable {

	private final String serverURL;
	private final AGHTTPClient httpClient;
	private final AGCatalog rootCatalog;
	
	/**
	 * Creates an instance for interacting with an AllegroGraph server.
	 * 
	 * Uses Basic authentication.
	 *  
	 * @param serverURL the URL of the server (trailing slashes are removed).
	 * @param username a user id for authenticating with the server 
	 * @param password a password for authenticating with the server
	 * @see #AGServer(String)
	 */
	public AGServer(String serverURL, String username, String password) {
		this.serverURL = serverURL.replaceAll("/$", "");
		httpClient = new AGHTTPClient(this.serverURL); 
		httpClient.setUsernameAndPassword(username, password);
		rootCatalog = new AGCatalog(this,AGCatalog.ROOT_CATALOG);
	}
	
	/**
	 * Creates an instance for interacting with an AllegroGraph server.
	 * 
	 * Uses Basic authentication with the server as configured in the 
	 * httpClient instance.
	 *  
	 * @param username a user id for authenticating with the server 
	 * @param password a password for authenticating with the server  
	 * @param httpClient the AGHTTPClient instance to use
	 */
	public AGServer(String username, String password, AGHTTPClient httpClient) {
		this.serverURL = httpClient.getServerURL();
		this.httpClient = httpClient; 
		this.httpClient.setUsernameAndPassword(username, password);
		rootCatalog = new AGCatalog(this,AGCatalog.ROOT_CATALOG);
	}
	
	/**
	 * Creates an instance for interacting with an AllegroGraph server.
	 *<p>
	 * Attempts X.509 server and client authentication when no username and
	 * password have been set in the httpClient, and properties such as
	 * <p><code><pre>
	 * javax.net.ssl.keyStore, 
	 * javax.net.ssl.keyStorePassword, 
	 * javax.net.ssl.keyStoreType, and
	 * javax.net.ssl.trustStore
	 * </pre></code>
	 * have been set appropriately.
	 * <p>
	 * Also set SSL directives in the server's config file, e.g:
	 * <p><code><pre>
	 * SSLPort 10036
	 * SSLClientAuthRequired true
	 * SSLClientAuthUsernameField CN
	 * SSLCertificate /path/agraph.cert
	 * SSLCAFile /path/ca.cert
	 * </pre></code>
	 * For more details, see <a href="http://www.franz.com/agraph/support/documentation/v4/daemon-config.html#header2-10">Server configuration</a>.
	 * <p>
	 * @param httpClient the AGHTTPClient instance to use
	 * @see #AGServer(String)
	 */
	public AGServer(AGHTTPClient httpClient) {
		this.serverURL = httpClient.getServerURL();
		this.httpClient = httpClient; 
		rootCatalog = new AGCatalog(this,AGCatalog.ROOT_CATALOG);
	}
	
	/**
	 * Creates an instance for interacting with an AllegroGraph server.
	 * <p>
	 * Uses a new default AGHTTPClient instance having the given serverURL.
	 * <p>
	 * Attempts X.509 server and client authentication when properties 
	 * such as
	 * <p><code><pre>
	 * javax.net.ssl.keyStore, 
	 * javax.net.ssl.keyStorePassword, 
	 * javax.net.ssl.keyStoreType, and
	 * javax.net.ssl.trustStore
	 * </pre></code>
	 * have been set appropriately.
	 * <p>
	 * Also set SSL directives in the server's config file, e.g:
	 * <p><code><pre>
	 * SSLPort 10036
	 * SSLClientAuthRequired true
	 * SSLClientAuthUsernameField CN
	 * SSLCertificate /path/agraph.cert
	 * SSLCAFile /path/ca.cert
	 * </pre></code>
	 * For more details, see <a href="http://www.franz.com/agraph/support/documentation/v4/daemon-config.html#header2-10">Server configuration</a>.
	 * <p>
	 * @param serverURL the URL of the server (trailing slashes are removed).
	 * @see #AGServer(String, String, String)
	 * @see #AGServer(AGHTTPClient)
	 */
	public AGServer(String serverURL) {
		this.serverURL = serverURL.replaceAll("/$", "");
		this.httpClient = new AGHTTPClient(serverURL); 
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
	
	/**
	 * Returns the AGHTTPClient instance for this server.
	 * 
	 * @return the AGHTTPClient instance for this server.
	 */
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
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#post-session"
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
	 * See <a href="http://www.franz.com/agraph/support/documentation/current/agraph-introduction.html#intro-federation">Managing
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
	
	/**
	 * Returns a List of user ids known to this AllegroGraph server.
	 * 
	 * @return List of user ids.
	 * @throws AGHttpException
	 */
	public List<String> listUsers() throws AGHttpException {
		return getHTTPClient().getListOfStrings(serverURL+"/users");
	}
	
	/**
	 * Adds a user to the server.
	 * 
	 * @param user user id to add
	 * @param password user's password
	 * @throws AGHttpException
	 */
	public void addUser(String user, String password) throws AGHttpException {
		String url = serverURL+"/users/"+user;
		Header[] headers = new Header[0];
		NameValuePair[] params = { new NameValuePair("password", password) };
		getHTTPClient().put(url, headers, params, null, null);
	}
	
	/**
	 * Deletes a user from the server.
	 * 
	 * @param user user id to delete
	 * @throws AGHttpException
	 */
	public void deleteUser(String user) throws AGHttpException {
		String url = serverURL+"/users/"+user;
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		getHTTPClient().delete(url, headers, params, null);
	}
	
	/**
	 * Adds to a user's access list for this server.
	 * <p>
	 * Access is documented  
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-user-access"
	 * target="_top">here</a>.
	 * <p>
	 * @param user user id
	 * @param read read access
	 * @param write write access
	 * @param catalog catalog id, or "*" (or null) for all catalogs
	 * @param repository repository id, or "*" (or null) for all repos, in the given catalog(s)
	 * @throws AGHttpException
	 */
	public void addUserAccess(String user, boolean read, boolean write, String catalog, String repository) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/access";
		Header[] headers = new Header[0];
		if (catalog==null) catalog="*";
		if (repository==null) repository="*";
		NameValuePair[] params = { 
   			new NameValuePair("read", Boolean.toString(read)),
   			new NameValuePair("write", Boolean.toString(write)),
   			new NameValuePair("catalog", catalog),
   			new NameValuePair("repository", repository)};
		getHTTPClient().put(url, headers, params, null, null);
	}
	
	/**
	 * Deletes from a user's access list for this server.
	 * <p>
	 * Access is documented  
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#delete-user-access"
	 * target="_top">here</a>.
	 * <p>
	 * @param user user id
	 * @param read read access
	 * @param write write access
	 * @param catalog catalog id, or "*" (or null) for all catalogs
	 * @param repository repository id, or "*" (or null) for all repos, in the given catalog(s)
	 * @throws AGHttpException
	 */
	public void deleteUserAccess(String user, boolean read, boolean write, String catalog, String repository) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/access";
		Header[] headers = new Header[0];
		if (catalog==null) catalog="*";
		if (repository==null) repository="*";
		NameValuePair[] params = { 
   			new NameValuePair("read", Boolean.toString(read)),
   			new NameValuePair("write", Boolean.toString(write)),
   			new NameValuePair("catalog", catalog),
   			new NameValuePair("repository", repository)};
		getHTTPClient().delete(url, headers, params, null);
	}
	
	/**
	 * Returns a user's access list for this server.
	 * <p>
	 * Access is documented  
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#get-user-access"
	 * target="_top">here</a>.
	 * <p>
	 * @param user user id
	 * @throws AGHttpException
	 */
	public JSONArray listUserAccess(String user) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/access";
		Header[] headers = {new Header(Protocol.ACCEPT_PARAM_NAME,"application/json")};
		NameValuePair[] params = {}; 
		AGJSONArrayHandler handler = new AGJSONArrayHandler();
		getHTTPClient().get(url, headers, params, handler);
		return handler.getResult();
	}

	/**
	 * Adds a security filter for a user.
	 * 
	 * @param user user id
	 * @param type filter type is "allow" or "disallow"
	 * @param s subject to allow/disallow, in NTriples format
	 * @param p predicate to allow/disallow, in NTriples format
	 * @param o object to allow/disallow, in NTriples format
	 * @param g graph  to allow/disallow, in NTriples format
	 * @throws AGHttpException
	 */
	public void addUserSecurityFilter(String user, String type, String s,
			String p, String o, String g) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/security-filters/"+type;
		Header[] headers = {};
		List<NameValuePair> params = new ArrayList<NameValuePair>(4);
		if (s!=null) params.add(new NameValuePair("s", s));
		if (p!=null) params.add(new NameValuePair("p", p));
		if (o!=null) params.add(new NameValuePair("o", o));
		if (g!=null) params.add(new NameValuePair("g", g));
		getHTTPClient().post(url, headers, params.toArray(new NameValuePair[params.size()]), null, null);
	}

	/**
	 * Deletes a security filter for a user.
	 * 
	 * @param user user id
	 * @param type filter type is "allow" or "disallow"
	 * @param s subject to allow/disallow, in NTriples format
	 * @param p predicate to allow/disallow, in NTriples format
	 * @param o object to allow/disallow, in NTriples format
	 * @param g graph  to allow/disallow, in NTriples format
	 * @throws AGHttpException
	 */
	public void deleteUserSecurityFilter(String user, String type,
			String s, String p, String o, String g) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/security-filters/"+type;
		Header[] headers = {};
		List<NameValuePair> params = new ArrayList<NameValuePair>(4);
		if (s!=null) params.add(new NameValuePair("s", s));
		if (p!=null) params.add(new NameValuePair("p", p));
		if (o!=null) params.add(new NameValuePair("o", o));
		if (g!=null) params.add(new NameValuePair("g", g));
		getHTTPClient().post(url, headers, params.toArray(new NameValuePair[params.size()]), null, null);
	}

	/**
	 * Returns a list of security filters of the given type for a user
	 * 
	 * @param user user id
	 * @param type filter type is "allow" or "disallow"
	 * @throws AGHttpException
	 */
	public JSONArray listUserSecurityFilters(String user, String type) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/security-filters/"+type;
		Header[] headers = {new Header(Protocol.ACCEPT_PARAM_NAME,"application/json")};
		NameValuePair[] params = {}; 
		AGJSONArrayHandler handler = new AGJSONArrayHandler();
		getHTTPClient().get(url, headers, params, handler);
		return handler.getResult();
	}
	
	public void changeUserPassword(String user, String password) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Returns a user's effective access list for this server.
	 * <p>
	 * Includes the access granted to roles that this user has.
	 * 
	 * @param user user id
	 * @throws AGHttpException
	 */
	public JSONArray listUserEffectiveAccess(String user) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/effectiveAccess";
		Header[] headers = {new Header(Protocol.ACCEPT_PARAM_NAME,"application/json")};
		NameValuePair[] params = {}; 
		AGJSONArrayHandler handler = new AGJSONArrayHandler();
		getHTTPClient().get(url, headers, params, handler);
		return handler.getResult();
	}

	/**
	 * Returns a list of permissions for a user.
	 * <p>
	 * Permissions are documented  
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#header2-223"
	 * target="_top">here</a>.
	 * <p>
	 * 
	 * @param user user id
	 * @return list of permissions.
	 * @throws AGHttpException
	 */
	public List<String> listUserPermissions(String user) throws AGHttpException {
		return getHTTPClient().getListOfStrings(serverURL+"/users/"+user+"/permissions");
	}

	/**
	 * Returns a list of effective permissions for a user.
	 * <p>
	 * Includes the permission granted to roles that this user has.
	 * <p>
	 * Permissions are documented  
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#header2-223"
	 * target="_top">here</a>.
	 * <p>
	 * 
	 * @param user user id
	 * @return list of permissions.
	 * @throws AGHttpException
	 */
	public List<String> listUserEffectivePermissions(String user) throws AGHttpException {
		return getHTTPClient().getListOfStrings(serverURL+"/users/"+user+"/effectivePermissions");
	}

	/**
	 * Adds to a user's permission list.
	 * 
	 * @param user user id
	 * @param permission "super" or "eval" or "session"
	 * @throws AGHttpException
	 */
	public void addUserPermission(String user, String permission) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/permissions/"+permission;
		Header[] headers = new Header[0];
		NameValuePair[] params = {};
		getHTTPClient().put(url, headers, params, null, null);
	}

	/**
	 * Deletes from a user's permission list.
	 * 
	 * @param user user id
	 * @param permission "super" or "eval" or "session"
	 * @throws AGHttpException
	 */
	public void deleteUserPermission(String user, String permission) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/permissions/"+permission;
		Header[] headers = new Header[0];
		NameValuePair[] params = {};
		getHTTPClient().delete(url, headers, params, null);
		
	}

	/**
	 * Returns a list of roles known to this server.
	 * 
	 * @return a list of roles.
	 * @throws AGHttpException
	 */
	public List<String> listRoles() throws AGHttpException {
		return getHTTPClient().getListOfStrings(serverURL+"/roles");
	}

	/**
	 * Adds a role to this server.
	 * 
	 * @param role role id
	 * @throws AGHttpException
	 */
	public void addRole(String role) throws AGHttpException {
		String url = serverURL+"/roles/"+role;
		Header[] headers = new Header[0];
		NameValuePair[] params = {};
		getHTTPClient().put(url, headers, params, null, null);
	}

	/**
	 * Adds to a role's access list for this server.
	 * <p>
	 * Access is documented  
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-user-access"
	 * target="_top">here</a>.
	 * <p>
	 * @param role role id
	 * @param read read access
	 * @param write write access
	 * @param catalog catalog id, or "*" (or null) for all catalogs
	 * @param repository repository id, or "*" (or null) for all repos, in the given catalog(s)
	 * @throws AGHttpException
	 */
	public void addRoleAccess(String role, boolean read, boolean write,
			String catalog, String repository) throws AGHttpException {
		String url = serverURL+"/roles/"+role+"/access";
		Header[] headers = new Header[0];
		if (catalog==null) catalog="*";
		if (repository==null) repository="*";
		NameValuePair[] params = { 
   			new NameValuePair("read", Boolean.toString(read)),
   			new NameValuePair("write", Boolean.toString(write)),
   			new NameValuePair("catalog", catalog),
   			new NameValuePair("repository", repository)};
		getHTTPClient().put(url, headers, params, null, null);
	}

	/**
	 * Returns a role's access list for this server.
	 * <p>
	 * Access is documented  
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#get-user-access"
	 * target="_top">here</a>.
	 * <p>
	 * @param role role id
	 * @throws AGHttpException
	 */
	public JSONArray listRoleAccess(String role) throws AGHttpException {
		String url = serverURL+"/roles/"+role+"/access";
		Header[] headers = {new Header(Protocol.ACCEPT_PARAM_NAME,"application/json")};
		NameValuePair[] params = {}; 
		AGJSONArrayHandler handler = new AGJSONArrayHandler();
		getHTTPClient().get(url, headers, params, handler);
		return handler.getResult();
	}

	/**
	 * Adds a security filter for a role.
	 * 
	 * @param role role id
	 * @param type filter type is "allow" or "disallow"
	 * @param s subject to allow/disallow, in NTriples format
	 * @param p predicate to allow/disallow, in NTriples format
	 * @param o object to allow/disallow, in NTriples format
	 * @param g graph  to allow/disallow, in NTriples format
	 * @throws AGHttpException
	 */
	public void addRoleSecurityFilter(String role, String type, String s, 
			String p, String o, String g) throws AGHttpException {
		String url = serverURL + "/roles/" + role + "/security-filters/" + type;
		Header[] headers = {};
		List<NameValuePair> params = new ArrayList<NameValuePair>(4);
		if (s != null) params.add(new NameValuePair("s", s));
		if (p != null) params.add(new NameValuePair("p", p));
		if (o != null) params.add(new NameValuePair("o", o));
		if (g != null) params.add(new NameValuePair("g", g));
		getHTTPClient().post(url, headers, params.toArray(new NameValuePair[params.size()]), null, null);
	}

	/**
	 * Returns a list of security filters of the given type for a role
	 * 
	 * @param role role id
	 * @param type filter type is "allow" or "disallow"
	 * @throws AGHttpException
	 */
	public JSONArray listRoleSecurityFilters(String role, String type) throws AGHttpException {
		String url = serverURL+"/roles/"+role+"/security-filters/"+type;
		Header[] headers = {new Header(Protocol.ACCEPT_PARAM_NAME,"application/json")};
		NameValuePair[] params = {}; 
		AGJSONArrayHandler handler = new AGJSONArrayHandler();
		getHTTPClient().get(url, headers, params, handler);
		return handler.getResult();
	}

	/**
	 * Deletes a security filter for a role.
	 * 
	 * @param role role id
	 * @param type filter type is "allow" or "disallow"
	 * @param s subject to allow/disallow, in NTriples format
	 * @param p predicate to allow/disallow, in NTriples format
	 * @param o object to allow/disallow, in NTriples format
	 * @param g graph  to allow/disallow, in NTriples format
	 * @throws AGHttpException
	 */
	public void deleteRoleSecurityFilter(String role, String type,
			String s, String p, String o, String g) throws AGHttpException {
		String url = serverURL+"/roles/"+role+"/security-filters/"+type;
		Header[] headers = {};
		List<NameValuePair> params = new ArrayList<NameValuePair>(4);
		if (s!=null) params.add(new NameValuePair("s", s));
		if (p!=null) params.add(new NameValuePair("p", p));
		if (o!=null) params.add(new NameValuePair("o", o));
		if (g!=null) params.add(new NameValuePair("g", g));
		getHTTPClient().delete(url, headers, params.toArray(new NameValuePair[params.size()]), null);
	}

	/**
	 * Returns a list of roles for a user.
	 * 
	 * @return a list of roles.
	 * @throws AGHttpException
	 */
	public List<String> listUserRoles(String user) throws AGHttpException {
		return getHTTPClient().getListOfStrings(serverURL+"/users/"+user+"/roles");
	}
	
	/**
	 * Adds a role for this user.
	 * 
	 * @param user user id
	 * @param role role id
	 * @throws AGHttpException
	 */
	public void addUserRole(String user, String role) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/roles/"+role;
		Header[] headers = {};
		NameValuePair[] params = {}; 
		getHTTPClient().put(url, headers, params, null, null);
	}

	/**
	 * Deletes a role for this user.
	 * 
	 * @param user user id
	 * @param role role id
	 * @throws AGHttpException
	 */
	public void deleteUserRole(String user, String role) throws AGHttpException {
		String url = serverURL+"/users/"+user+"/roles/"+role;
		Header[] headers = {};
		NameValuePair[] params = {}; 
		getHTTPClient().delete(url, headers, params, null);
		
	}

	/**
	 * Deletes from a role's access list for this server.
	 * 
	 * @param role role id
	 * @param read read access
	 * @param write write access
	 * @param catalog catalog id, or "*" (or null) for all catalogs
	 * @param repository repository id, or "*" (or null) for all repos, in the given catalog(s)
	 * @throws AGHttpException
	 */
	public void deleteRoleAccess(String role, boolean read, boolean write,
			String catalog, String repository) throws AGHttpException {
		String url = serverURL+"/roles/"+role+"/access";
		Header[] headers = new Header[0];
		if (catalog==null) catalog="*";
		if (repository==null) repository="*";
		NameValuePair[] params = { 
   			new NameValuePair("read", Boolean.toString(read)),
   			new NameValuePair("write", Boolean.toString(write)),
   			new NameValuePair("catalog", catalog),
   			new NameValuePair("repository", repository)};
		getHTTPClient().delete(url, headers, params, null);
	}

	/**
	 * Deletes a role from this server.
	 * 
	 * @param role role id
	 * @throws AGHttpException
	 */
	public void deleteRole(String role) throws AGHttpException {
		String url = serverURL+"/roles/"+role;
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		getHTTPClient().delete(url, headers, params, null);
	}

	
	/**
	 * Adds to a role's permission list.
	 * 
	 * @param role role id
	 * @param permission "super" or "eval" or "session"
	 * @throws AGHttpException
	 */
	public void addRolePermission(String role, String permission) throws AGHttpException {
		String url = serverURL+"/roles/"+role+"/permissions/"+permission;
		Header[] headers = new Header[0];
		NameValuePair[] params = {};
		getHTTPClient().put(url, headers, params, null, null);
	}

	/**
	 * Delete from a role's permission list.
	 * 
	 * @param role role id
	 * @param permission "super" or "eval" or "session"
	 * @throws AGHttpException
	 */
	public void deleteRolePermission(String role, String permission) throws AGHttpException {
		String url = serverURL+"/roles/"+role+"/permissions/"+permission;
		Header[] headers = new Header[0];
		NameValuePair[] params = {};
		getHTTPClient().delete(url, headers, params, null);
		
	}

	/**
	 * Returns a list of permissions for a role.
	 * <p>
	 * Permissions are documented  
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#header2-223"
	 * target="_top">here</a>.
	 * <p>
	 * 
	 * @param role role id
	 * @return list of permissions.
	 * @throws AGHttpException
	 */
	public List<String> listRolePermissions(String role) throws AGHttpException {
		return getHTTPClient().getListOfStrings(serverURL+"/roles/"+role+"/permissions");
	}


}
