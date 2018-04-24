/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGProtocol;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGJSONArrayHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.json.JSONArray;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * The starting point for interacting with an
 * <a target="_top"
 * href="http://www.franz.com/agraph/support/documentation/current/agraph-introduction.html"
 * >AllegroGraph server</a>.
 * <p>
 * An AGServer {@link #listCatalogs() references} {@link AGCatalog}s,
 * which {@link AGCatalog#listRepositories() reference}
 * {@link AGRepository AGRepositories}, from which
 * {@link AGRepositoryConnection connections} may be obtained,
 * on which data is manipulated and queried.
 * <p>
 * AGServer provides methods for <a target="_top"
 * href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#get-user-permissions"
 * >User (and Role) Management</a>.
 * <p>
 * AGServer also provides {@link #federate(AGAbstractRepository...) federated} repositories.
 */
public class AGServer implements Closeable {
    /**
     * Default AG server port for HTTP connections.
     */
    public static final int DEFAULT_HTTP_PORT = 10035;
    /**
     * Default AG server port for HTTPS connections.
     */
    public static final int DEFAULT_HTTPS_PORT = 10036;
    // Size of the thread pool for the global executor service
    private static final int THREAD_POOL_SIZE = 4;
    // A default, global executor service used to create pinger threads.
    // Created lazily in getSharedExecutorService().
    private static ScheduledThreadPoolExecutor sharedExecutor;

    private final String serverURL;
    private final String user;
    private final String password;
    private final AGHTTPClient httpClient;
    private final AGCatalog rootCatalog;
    private ScheduledExecutorService executor = getSharedExecutor();
    private AGServerVersion cachedServerVersion;

    /**
     * Creates an instance for interacting with an AllegroGraph server.
     * <p>
     * Uses Basic authentication.
     *
     * @param serverURL the URL of the server (trailing slashes are removed)
     * @param username  a user id for authenticating with the server
     * @param password  a password for authenticating with the server
     * @see #AGServer(String)
     */
    public AGServer(String serverURL, String username, String password) {
        this.serverURL = serverURL.replaceAll("/$", "");
        this.user = username;
        this.password = password;
        httpClient = new AGHTTPClient(this.serverURL);
        httpClient.setUsernameAndPassword(username, password);
        rootCatalog = new AGCatalog(this, AGCatalog.ROOT_CATALOG);
    }

    /**
     * Creates an instance for interacting with an AllegroGraph server.
     * <p>
     * Uses Basic authentication with the server as configured in the
     * httpClient instance.
     *
     * @param username   a user id for authenticating with the server
     * @param password   a password for authenticating with the server
     * @param httpClient the AGHTTPClient instance to use
     */
    public AGServer(String username, String password, AGHTTPClient httpClient) {
        this.serverURL = httpClient.getServerURL();
        this.user = username;
        this.password = password;
        this.httpClient = httpClient;
        this.httpClient.setUsernameAndPassword(username, password);
        rootCatalog = new AGCatalog(this, AGCatalog.ROOT_CATALOG);
    }

    /**
     * Creates an instance for interacting with an AllegroGraph server.
     * <p>
     * Attempts X.509 server and client authentication when no username and
     * password have been set in the httpClient, and properties such as</p>
     * <pre>{@code
     * javax.net.ssl.keyStore,
     * javax.net.ssl.keyStorePassword,
     * javax.net.ssl.keyStoreType, and
     * javax.net.ssl.trustStore
     * }</pre>
     * <p>have been set appropriately.</p>
     * <p>
     * Also set SSL directives in the server's config file, e.g:</p>
     * <pre>{@code
     * SSLPort 10036
     * SSLClientAuthRequired true
     * SSLClientAuthUsernameField CN
     * SSLCertificate /path/agraph.cert
     * SSLCAFile /path/ca.cert
     * }</pre>
     * For more details, see <a href="http://www.franz.com/agraph/support/documentation/current/daemon-config.html#client-index">Server configuration</a>.
     * <p>
     *
     * @param httpClient the AGHTTPClient instance to use
     * @see #AGServer(String)
     */
    public AGServer(AGHTTPClient httpClient) {
        this.serverURL = httpClient.getServerURL();

        String[] userInfo = httpClient.getUsernameAndPassword();

        this.user     = userInfo[0];
        this.password = userInfo[1];

        this.httpClient = httpClient;
        rootCatalog = new AGCatalog(this, AGCatalog.ROOT_CATALOG);
    }

    /**
     * Creates an instance for interacting with an AllegroGraph server.
     * <p>
     * Uses a new default AGHTTPClient instance having the given serverURL.
     * </p>
     * <p>Attempts X.509 server and client authentication when properties
     * such as</p>
     * <pre>{@code
     * javax.net.ssl.keyStore,
     * javax.net.ssl.keyStorePassword,
     * javax.net.ssl.keyStoreType, and
     * javax.net.ssl.trustStore
     * }</pre>
     * <p>have been set appropriately.</p>
     * <p>
     * Also set SSL directives in the server's config file, e.g:</p>
     * <pre>{@code
     * SSLPort 10036
     * SSLClientAuthRequired true
     * SSLClientAuthUsernameField CN
     * SSLCertificate /path/agraph.cert
     * SSLCAFile /path/ca.cert
     * }</pre>
     * <p>For more details, see <a href="http://www.franz.com/agraph/support/documentation/current/daemon-config.html#client-index">Server configuration</a>.
     * </p>
     *
     * @param serverURL the URL of the server (trailing slashes are removed)
     * @see #AGServer(String, String, String)
     * @see #AGServer(AGHTTPClient)
     */
    public AGServer(String serverURL) {
        this.serverURL = serverURL.replaceAll("/$", "");
        this.httpClient = new AGHTTPClient(serverURL);

        this.user     = null;
        this.password = null;

        rootCatalog = new AGCatalog(this, AGCatalog.ROOT_CATALOG);
    }

    private static synchronized ScheduledExecutorService getSharedExecutor() {
        if (sharedExecutor == null) {
            sharedExecutor = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE);
            // This will make sure that the executor will not prevent
            // the application from shutting down.
            sharedExecutor.setThreadFactory(runnable -> {
                final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setDaemon(true);
                return thread;
            });
            // Note this requires Java 7
            sharedExecutor.setRemoveOnCancelPolicy(true);
        }
        return sharedExecutor;
    }

    /**
     * Creates or opens a repository in the specified catalog.
     *
     * @param repoName    name of the repository to create
     * @param catalogName name of the catalog where repoName will be created
     * @param serverURL   URL at which the AG server is found
     * @param username    name of the authenticating user, or null
     * @param password    password (plaintext) of the authenticating user, or null
     * @return an initialized {@link AGRepository} instance for the newly created repository
     * @throws RepositoryException if there is an error with this request
     */
    public static AGRepository createRepository(String repoName,
                                                String catalogName, String serverURL, String username,
                                                String password) throws RepositoryException {
        return new AGServer(serverURL, username, password).createRepository(
                repoName, catalogName);
    }

    /**
     * Creates or opens a repository in the specified catalog.
     *
     * @param repoName    name of the repository to create
     * @param catalogName name of the catalog where repoName will be created
     * @param serverURL   URL at which the AG server is found
     * @param username    name of the authenticating user, or null
     * @param password    password (plaintext) of the authenticating user, or null
     * @return a connection to the newly created and initialized repository
     * @throws RepositoryException if there is an error with this request.
     */
    public static AGRepositoryConnection createRepositoryConnection(String repoName,
                                                                    String catalogName, String serverURL, String username,
                                                                    String password) throws RepositoryException {
        return createRepository(repoName, catalogName, serverURL, username, password).getConnection();
    }

    /**
     * Returns the URL of this AllegroGraph server.
     *
     * @return the URL of this AllegroGraph server
     */
    public String getServerURL() {
        return serverURL;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Returns the AGHTTPClient instance for this server.
     *
     * @return the AGHTTPClient instance for this server
     */
    public AGHTTPClient getHTTPClient() {
        return httpClient;
    }

    /**
     * @return the server version
     * @throws AGHttpException if there is an error with this request
     */
    public String getVersion() throws AGHttpException {
        return getHTTPClient().getString(serverURL + "/version");
    }

    /**
     * @return the server version as comparable AGServerVersion object.
     * @throws AGHttpException if there is no cachedServerVersion and there was an error with getVersion() request.
     */
    public AGServerVersion getComparableVersion() throws AGHttpException {
        if (cachedServerVersion == null) {
            cachedServerVersion = new AGServerVersion(getVersion());
        }
        return cachedServerVersion;
    }

    /**
     * @return the server's build date
     * @throws AGHttpException if there is an error with this request
     */
    public String getBuildDate() throws AGHttpException {
        return getHTTPClient().getString(serverURL + "/version/date");
    }

    /**
     * @return the server's revision info
     * @throws AGHttpException if there is an error with this request
     */
    public String getRevision() throws AGHttpException {
        return getHTTPClient().getString(serverURL + "/version/revision");
    }

    /**
     * Returns the unnamed root catalog for this AllegroGraph server.
     * Note: this method may be deprecated in an upcoming release.
     *
     * @return the root catalog
     * @see #getCatalog()
     */
    public AGCatalog getRootCatalog() {
        return rootCatalog;
    }

    /**
     * Returns a List of catalog ids known to this AllegroGraph server.
     *
     * @return List of catalog ids
     * @throws AGHttpException if there is an error with this request
     */
    public List<String> listCatalogs() throws AGHttpException {
        String url = AGProtocol.getNamedCatalogsURL(serverURL);
        TupleQueryResult tqresult = getHTTPClient().getTupleQueryResult(url);
        List<String> result = new ArrayList<>(5);
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
     * <p>
     * Returns the root catalog if the id is a root id.  If
     * the catalog Id is not found on the server,  returns
     * null.
     *
     * @param catalogID a catalog id
     * @return the corresponding catalog instance
     * @throws AGHttpException if there is an error with this request
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
     * @return the root catalog
     */
    public AGCatalog getCatalog() {
        return rootCatalog;
    }

    /**
     * Returns an AGCatalog instance for the given catalogID.
     * <p>
     * If the catalog already exists on the server, an AGCatalog
     * instance is simply returned.  If the catalog does not exist,
     * it is created if the server has been configured to allow
     * <a href="http://www.franz.com/agraph/support/documentation/current/daemon-config.html#DynamicCatalogs">
     * dynamic catalogs</a>; otherwise, an exception is thrown.
     *
     * @param catalogID the id (the name) of the catalog
     * @return an AGCatalog instance
     * @throws AGHttpException if there is an error with this request
     */
    public AGCatalog createCatalog(String catalogID) throws AGHttpException {
        AGCatalog catalog = getCatalog(catalogID);
        if (catalog == null) {
            String catalogURL = AGProtocol.getNamedCatalogLocation(getServerURL(), catalogID);
            getHTTPClient().putCatalog(catalogURL);
            catalog = new AGCatalog(this, catalogID);
        }
        return catalog;
    }

    /**
     * Deletes any catalog with the given repository id.
     * <p>
     * This method only applies to dynamically created catalogs.
     *
     * @param catalogID the name of the catalog to delete
     * @throws AGHttpException if there is an error with this request
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
     * @return a virtual repository based on <code>storeSpec</code>
     */
    public AGVirtualRepository virtualRepository(String storeSpec) {
        return new AGVirtualRepository(this, storeSpec, null);
    }

    /**
     * Creates a federated view of multiple repositories.
     * <p>
     * See <a href="http://www.franz.com/agraph/support/documentation/current/agraph-introduction.html#intro-federation">Managing
     * Massive Data - Federation</a>.
     *
     * @param repositories repositories that will compose the virtual repository
     * @return a virtual repository that federates queries across multiple physical repositories
     */
    public AGVirtualRepository federate(AGAbstractRepository... repositories) {
        String[] specstrings = new String[repositories.length];

        for (int i = 0; i < repositories.length; i++) {
            specstrings[i] = repositories[i].getSpec();
        }
        String spec = AGVirtualRepository.federatedSpec(specstrings);

        return new AGVirtualRepository(this, spec, null);
    }

    /**
     * Close the HTTP connection resources to the AllegroGraph server.
     */
    @Override
    public void close() {
        httpClient.close();
    }

    /**
     * Returns a List of user ids known to this AllegroGraph server.
     *
     * @return List of user ids
     * @throws AGHttpException if there is an error with this request
     */
    public List<String> listUsers() throws AGHttpException {
        return getHTTPClient().getListOfStrings(serverURL + "/users");
    }

    /**
     * Adds a user to the server.
     *
     * @param user     user id to add
     * @param password user's password
     * @throws AGHttpException if there is an error with this request
     */
    public void addUser(String user, String password) throws AGHttpException {
        String url = serverURL + "/users/" + user;
        Header[] headers = new Header[0];
        NameValuePair[] params = {new NameValuePair("password", password)};
        getHTTPClient().put(url, headers, params, null, null);
    }

    /**
     * Deletes a user from the server.
     *
     * @param user user id to delete
     * @throws AGHttpException if there is an error with this request
     */
    public void deleteUser(String user) throws AGHttpException {
        String url = serverURL + "/users/" + user;
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
     *
     * @param user       user id
     * @param read       read access
     * @param write      write access
     * @param catalog    catalog id, or "*" (or null) for all catalogs
     * @param repository repository id, or "*" (or null) for all repos, in the given catalog(s)
     * @throws AGHttpException if there is an error with this request
     */
    public void addUserAccess(String user, boolean read, boolean write, String catalog, String repository) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/access";
        Header[] headers = new Header[0];
        if (catalog == null) {
            catalog = "*";
        }
        if (repository == null) {
            repository = "*";
        }
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
     *
     * @param user       user id
     * @param read       read access
     * @param write      write access
     * @param catalog    catalog id, or "*" (or null) for all catalogs
     * @param repository repository id, or "*" (or null) for all repos, in the given catalog(s)
     * @throws AGHttpException if there is an error with this request
     */
    public void deleteUserAccess(String user, boolean read, boolean write, String catalog, String repository) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/access";
        Header[] headers = new Header[0];
        if (catalog == null) {
            catalog = "*";
        }
        if (repository == null) {
            repository = "*";
        }
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
     *
     * @param user user id
     * @return a JSONArray describing <code>user</code>'s access list
     * @throws AGHttpException if there is an error with this request
     */
    public JSONArray listUserAccess(String user) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/access";
        Header[] headers = {new Header(Protocol.ACCEPT_PARAM_NAME, "application/json")};
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
     * @param s    subject to allow/disallow, in NTriples format
     * @param p    predicate to allow/disallow, in NTriples format
     * @param o    object to allow/disallow, in NTriples format
     * @param g    graph  to allow/disallow, in NTriples format
     * @throws AGHttpException if there is an error with this request
     */
    public void addUserSecurityFilter(String user, String type, String s,
                                      String p, String o, String g) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/security-filters/" + type;
        Header[] headers = {};
        List<NameValuePair> params = new ArrayList<>(4);
        if (s != null) {
            params.add(new NameValuePair("s", s));
        }
        if (p != null) {
            params.add(new NameValuePair("p", p));
        }
        if (o != null) {
            params.add(new NameValuePair("o", o));
        }
        if (g != null) {
            params.add(new NameValuePair("g", g));
        }
        getHTTPClient().post(url, headers, params.toArray(new NameValuePair[params.size()]), null, null);
    }

    /**
     * Deletes a security filter for a user.
     *
     * @param user user id
     * @param type filter type is "allow" or "disallow"
     * @param s    subject to allow/disallow, in NTriples format
     * @param p    predicate to allow/disallow, in NTriples format
     * @param o    object to allow/disallow, in NTriples format
     * @param g    graph  to allow/disallow, in NTriples format
     * @throws AGHttpException if there is an error with this request
     */
    public void deleteUserSecurityFilter(String user, String type,
                                         String s, String p, String o, String g) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/security-filters/" + type;
        Header[] headers = {};
        List<NameValuePair> params = new ArrayList<>(4);
        if (s != null) {
            params.add(new NameValuePair("s", s));
        }
        if (p != null) {
            params.add(new NameValuePair("p", p));
        }
        if (o != null) {
            params.add(new NameValuePair("o", o));
        }
        if (g != null) {
            params.add(new NameValuePair("g", g));
        }
        getHTTPClient().post(url, headers, params.toArray(new NameValuePair[params.size()]), null, null);
    }

    /**
     * Returns a list of security filters of the given type for a user
     *
     * @param user user id
     * @param type filter type is "allow" or "disallow"
     * @return a JSONArray of <code>user</code>'s security filters
     * @throws AGHttpException if there is an error with this request
     */
    public JSONArray listUserSecurityFilters(String user, String type) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/security-filters/" + type;
        Header[] headers = {new Header(Protocol.ACCEPT_PARAM_NAME, "application/json")};
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
     * @return a JSONArray describing <code>user</code>'s effective access
     * @throws AGHttpException if there is an error with this request
     */
    public JSONArray listUserEffectiveAccess(String user) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/effectiveAccess";
        Header[] headers = {new Header(Protocol.ACCEPT_PARAM_NAME, "application/json")};
        NameValuePair[] params = {};
        AGJSONArrayHandler handler = new AGJSONArrayHandler();
        getHTTPClient().get(url, headers, params, handler);
        return handler.getResult();
    }

    /**
     * Returns a list of permissions for a user.
     * <p>
     * Permissions are documented
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#get-user-permissions"
     * target="_top">here</a>.
     * <p>
     *
     * @param user user id
     * @return list of permissions
     * @throws AGHttpException if there is an error with this request
     */
    public List<String> listUserPermissions(String user) throws AGHttpException {
        return getHTTPClient().getListOfStrings(serverURL + "/users/" + user + "/permissions");
    }

    /**
     * Returns a list of effective permissions for a user.
     * <p>
     * Includes the permission granted to roles that this user has.
     * <p>
     * Permissions are documented
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#get-user-permissions"
     * target="_top">here</a>.
     * <p>
     *
     * @param user user id
     * @return list of permissions
     * @throws AGHttpException if there is an error with this request
     */
    public List<String> listUserEffectivePermissions(String user) throws AGHttpException {
        return getHTTPClient().getListOfStrings(serverURL + "/users/" + user + "/effectivePermissions");
    }

    /**
     * Adds to a user's permission list.
     *
     * @param user       user id
     * @param permission "super" or "eval" or "session"
     * @throws AGHttpException if there is an error with this request
     */
    public void addUserPermission(String user, String permission) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/permissions/" + permission;
        Header[] headers = new Header[0];
        NameValuePair[] params = {};
        getHTTPClient().put(url, headers, params, null, null);
    }

    /**
     * Deletes from a user's permission list.
     *
     * @param user       user id
     * @param permission "super" or "eval" or "session"
     * @throws AGHttpException if there is an error with this request
     */
    public void deleteUserPermission(String user, String permission) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/permissions/" + permission;
        Header[] headers = new Header[0];
        NameValuePair[] params = {};
        getHTTPClient().delete(url, headers, params, null);

    }

    /**
     * Returns a list of roles known to this server.
     *
     * @return a list of roles
     * @throws AGHttpException if there is an error with this request
     */
    public List<String> listRoles() throws AGHttpException {
        return getHTTPClient().getListOfStrings(serverURL + "/roles");
    }

    /**
     * Adds a role to this server.
     *
     * @param role role id
     * @throws AGHttpException if there is an error with this request
     */
    public void addRole(String role) throws AGHttpException {
        String url = serverURL + "/roles/" + role;
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
     *
     * @param role       role id
     * @param read       read access
     * @param write      write access
     * @param catalog    catalog id, or "*" (or null) for all catalogs
     * @param repository repository id, or "*" (or null) for all repos, in the given catalog(s)
     * @throws AGHttpException if there is an error with this request
     */
    public void addRoleAccess(String role, boolean read, boolean write,
                              String catalog, String repository) throws AGHttpException {
        String url = serverURL + "/roles/" + role + "/access";
        Header[] headers = new Header[0];
        if (catalog == null) {
            catalog = "*";
        }
        if (repository == null) {
            repository = "*";
        }
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
     *
     * @param role role id
     * @return the access list for the specified role
     * @throws AGHttpException if there is an error with this request
     */
    public JSONArray listRoleAccess(String role) throws AGHttpException {
        String url = serverURL + "/roles/" + role + "/access";
        Header[] headers = {new Header(Protocol.ACCEPT_PARAM_NAME, "application/json")};
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
     * @param s    subject to allow/disallow, in NTriples format
     * @param p    predicate to allow/disallow, in NTriples format
     * @param o    object to allow/disallow, in NTriples format
     * @param g    graph  to allow/disallow, in NTriples format
     * @throws AGHttpException if there is an error with this request
     */
    public void addRoleSecurityFilter(String role, String type, String s,
                                      String p, String o, String g) throws AGHttpException {
        String url = serverURL + "/roles/" + role + "/security-filters/" + type;
        Header[] headers = {};
        List<NameValuePair> params = new ArrayList<>(4);
        if (s != null) {
            params.add(new NameValuePair("s", s));
        }
        if (p != null) {
            params.add(new NameValuePair("p", p));
        }
        if (o != null) {
            params.add(new NameValuePair("o", o));
        }
        if (g != null) {
            params.add(new NameValuePair("g", g));
        }
        getHTTPClient().post(url, headers, params.toArray(new NameValuePair[params.size()]), null, null);
    }

    /**
     * Returns a list of security filters of the given type for a role
     *
     * @param role role id
     * @param type filter type is "allow" or "disallow"
     * @return the security filters for the role
     * @throws AGHttpException if there is an error with this request
     */
    public JSONArray listRoleSecurityFilters(String role, String type) throws AGHttpException {
        String url = serverURL + "/roles/" + role + "/security-filters/" + type;
        Header[] headers = {new Header(Protocol.ACCEPT_PARAM_NAME, "application/json")};
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
     * @param s    subject to allow/disallow, in NTriples format
     * @param p    predicate to allow/disallow, in NTriples format
     * @param o    object to allow/disallow, in NTriples format
     * @param g    graph  to allow/disallow, in NTriples format
     * @throws AGHttpException if there is an error with this request
     */
    public void deleteRoleSecurityFilter(String role, String type,
                                         String s, String p, String o, String g) throws AGHttpException {
        String url = serverURL + "/roles/" + role + "/security-filters/" + type;
        Header[] headers = {};
        List<NameValuePair> params = new ArrayList<>(4);
        if (s != null) {
            params.add(new NameValuePair("s", s));
        }
        if (p != null) {
            params.add(new NameValuePair("p", p));
        }
        if (o != null) {
            params.add(new NameValuePair("o", o));
        }
        if (g != null) {
            params.add(new NameValuePair("g", g));
        }
        getHTTPClient().delete(url, headers, params.toArray(new NameValuePair[params.size()]), null);
    }

    /**
     * Returns a list of roles for a user.
     *
     * @param user the user to lookup
     * @return a list of roles
     * @throws AGHttpException if there is an error with this request
     */
    public List<String> listUserRoles(String user) throws AGHttpException {
        return getHTTPClient().getListOfStrings(serverURL + "/users/" + user + "/roles");
    }

    /**
     * Adds a role for this user.
     *
     * @param user user id
     * @param role role id
     * @throws AGHttpException if there is an error with this request
     */
    public void addUserRole(String user, String role) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/roles/" + role;
        Header[] headers = {};
        NameValuePair[] params = {};
        getHTTPClient().put(url, headers, params, null, null);
    }

    /**
     * Deletes a role for this user.
     *
     * @param user user id
     * @param role role id
     * @throws AGHttpException if there is an error with this request
     */
    public void deleteUserRole(String user, String role) throws AGHttpException {
        String url = serverURL + "/users/" + user + "/roles/" + role;
        Header[] headers = {};
        NameValuePair[] params = {};
        getHTTPClient().delete(url, headers, params, null);

    }

    /**
     * Deletes from a role's access list for this server.
     *
     * @param role       role id
     * @param read       read access
     * @param write      write access
     * @param catalog    catalog id, or "*" (or null) for all catalogs
     * @param repository repository id, or "*" (or null) for all repos, in the given catalog(s)
     * @throws AGHttpException if there is an error with this request
     */
    public void deleteRoleAccess(String role, boolean read, boolean write,
                                 String catalog, String repository) throws AGHttpException {
        String url = serverURL + "/roles/" + role + "/access";
        Header[] headers = new Header[0];
        if (catalog == null) {
            catalog = "*";
        }
        if (repository == null) {
            repository = "*";
        }
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
     * @throws AGHttpException if there is an error with this request
     */
    public void deleteRole(String role) throws AGHttpException {
        String url = serverURL + "/roles/" + role;
        Header[] headers = new Header[0];
        NameValuePair[] params = new NameValuePair[0];
        getHTTPClient().delete(url, headers, params, null);
    }

    /**
     * Adds to a role's permission list.
     *
     * @param role       role id
     * @param permission "super" or "eval" or "session"
     * @throws AGHttpException if there is an error with this request
     */
    public void addRolePermission(String role, String permission) throws AGHttpException {
        String url = serverURL + "/roles/" + role + "/permissions/" + permission;
        Header[] headers = new Header[0];
        NameValuePair[] params = {};
        getHTTPClient().put(url, headers, params, null, null);
    }

    /**
     * Delete from a role's permission list.
     *
     * @param role       role id
     * @param permission "super" or "eval" or "session"
     * @throws AGHttpException if there is an error with this request
     */
    public void deleteRolePermission(String role, String permission) throws AGHttpException {
        String url = serverURL + "/roles/" + role + "/permissions/" + permission;
        Header[] headers = new Header[0];
        NameValuePair[] params = {};
        getHTTPClient().delete(url, headers, params, null);

    }

    /**
     * Returns a list of permissions for a role.
     * <p>
     * Permissions are documented
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#get-role-permissions"
     * target="_top">here</a>.
     * <p>
     *
     * @param role role id
     * @return list of permissions
     * @throws AGHttpException if there is an error with this request
     */
    public List<String> listRolePermissions(String role) throws AGHttpException {
        return getHTTPClient().getListOfStrings(serverURL + "/roles/" + role + "/permissions");
    }

    /**
     * Gets the default executor object that will be used by connections
     * to schedule maintenance operations.
     *
     * @return An executor instance
     */
    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    /**
     * Changes the default executor object that will be used by connections
     * to schedule maintenance operations.
     *
     * @param executor An executor instance
     */
    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Creates or opens a repository in the specified catalog.
     *
     * @param reponame name of the repository to create
     * @param catalog  AGCatalog instance where the repository will be created
     * @param strict   if true, throw an exception if the repository exists.
     *                 Otherwise the existing repository will be opened.
     * @return an initialized {@link AGRepository} instance for the newly created repository
     * @throws RepositoryException if there is an error with this request
     */
    public AGRepository createRepository(String reponame, AGCatalog catalog,
                                         boolean strict) throws RepositoryException {
        AGRepository repo = catalog.createRepository(reponame.trim(), strict);
        repo.initialize();

        return repo;
    }

    /**
     * Creates or opens a repository in the specified catalog.
     *
     * @param reponame name of the repository to create
     * @param catname  name of the catalog in which to create the repository
     * @param strict   if true, throw an exception if the repository exists.
     *                 Otherwise the existing repository will be opened.
     * @return an initialized {@link AGRepository} instance for the newly created repository
     * @throws RepositoryException if there is an error with this request
     */
    public AGRepository createRepository(String reponame, String catname,
                                         boolean strict) throws RepositoryException {

        // check for rootCatalog'ness.
        if (catname == null || catname.trim().isEmpty()) {
            return createRepository(reponame, rootCatalog, strict);
        }

        AGCatalog cat = getCatalog(catname);
        if (cat == null) {
            throw new RepositoryException("Unable to create repository "
                    + catname + "/" + reponame + " because " + catname
                    + " does not exist.");
        }

        return createRepository(reponame, cat, strict);
    }

    /**
     * Creates or opens a repository in the specified catalog.
     *
     * @param reponame name of the repository to create
     * @param catname  name of the catalog in which to create the repository
     * @return an initialized {@link AGRepository} instance for the newly created repository
     * @throws RepositoryException if there is an error with this request
     */
    public AGRepository createRepository(String reponame, String catname)
            throws RepositoryException {
        return createRepository(reponame, catname, false);
    }

    /**
     * Creates or opens a repository in the root catalog.
     *
     * @param reponame name of the repository to create
     * @return an initialized {@link AGRepository} instance for the newly created repository
     * @throws RepositoryException if there is an error with this request
     */
    public AGRepository createRepository(String reponame)
            throws RepositoryException {
        return createRepository(reponame, rootCatalog, false);
    }

    /**
     * Creates or opens a repository in the specified catalog.
     *
     * @param reponame name of the repository to create
     * @param cat      catalog in which to create the repository
     * @param strict   if true, throw an exception if the repository exists.
     *                 Otherwise the existing repository will be opened.
     * @return a connection to the newly created and initialized repository
     * @throws RepositoryException if there is an error with this request.
     */
    public AGRepositoryConnection createRepositoryConnection(
            String reponame, AGCatalog cat, boolean strict) throws RepositoryException {
        return createRepository(reponame, cat, strict).getConnection();
    }

    /**
     * Creates or opens a repository in the specified catalog.
     *
     * @param reponame name of the repository to create
     * @param catname  name of the catalog in which to create the repository
     * @param strict   if true, throw an exception if the repository exists.
     *                 Otherwise the existing repository will be opened.
     * @return a connection to the newly created and initialized repository
     * @throws RepositoryException if there is an error with this request.
     */
    public AGRepositoryConnection createRepositoryConnection(String reponame, String catname,
                                                             boolean strict) throws RepositoryException {
        return createRepository(reponame, catname, strict).getConnection();
    }
}
