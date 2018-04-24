/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGProtocol;
import com.franz.agraph.http.exception.AGHttpException;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.util.ArrayList;
import java.util.List;

import static com.franz.agraph.http.AGProtocol.encode;

/**
 * Catalogs are locations on disk where AllegroGraph Server keeps its repositories.
 * <p>Catalogs are created by the
 * <a href="http://www.franz.com/agraph/support/documentation/current/daemon-config.html#catalog">catalog
 * server configuration</a>.</p>
 * <p>A catalog is a grouping of repositories.  The root catalog is the
 * default, unnamed catalog that is available with every server.  It
 * is also possible to create any number of named catalogs.
 * </p>
 *
 * @see AGServer#getCatalog(String)
 */
public class AGCatalog {

    /**
     * @see #getCatalogType()
     */
    public static final int ROOT_CATALOG = 0;
    /**
     * @see #getCatalogType()
     */
    public static final int NAMED_CATALOG = 2;
    private final String catalogName;
    private final int catalogType;
    private final String catalogURL;
    private final String repositoriesURL;
    private AGServer server;

    /**
     * Creates an AGCatalog instance for a named catalog having catalogName
     * in the given server.
     *
     * @param server      the server housing this named catalog
     * @param catalogName the name for this named catalog
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
     * @param server      the server housing the catalog
     * @param catalogType the type of the special catalog
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
                throw new IllegalArgumentException("Invalid Catalog Type: " + catalogType);
        }
        this.server = server;
        this.catalogType = catalogType;
    }

    /**
     * Returns true iff the id identifies the root catalog.
     * <p>
     * Currently null, the empty string, and "/" are all considered to identify
     * the root catalog.
     *
     * @param catalogID the name of the catalog to check
     * @return true iff the id identifies the root catalog
     */
    public static boolean isRootID(String catalogID) {
        boolean result = false;
        if (catalogID == null || catalogID.equals("") || catalogID.equals("/")) {
            result = true;
        }
        return result;
    }

    /**
     * The AGServer instance for this catalog.
     *
     * @return the AGServer instance for this catalog
     */
    public AGServer getServer() {
        return server;
    }

    /**
     * The name of this catalog.  The root catalog has the name "/".
     *
     * @return the name of this catalog
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
     * @return the URL of this catalog
     */
    public String getCatalogURL() {
        return catalogURL;
    }

    /**
     * URL for accessing the repositories of this catalog.
     *
     * @return the URL for accessing the repositories of this catalog
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
                throw new IllegalArgumentException("Invalid Catalog Type: " + catalogType);
        }
        return catalogPrefixedRepositoryID;
    }

    /**
     * Returns a List of repository ids contained in this catalog.
     *
     * @return a List of repository ids contained in this catalog
     * @throws org.eclipse.rdf4j.RDF4JException if there is an error during the request
     */
    public List<String> listRepositories() throws RDF4JException {
        String url = getRepositoriesURL();
        List<String> result = new ArrayList<>(5);
        try (TupleQueryResult tqresult = getHTTPClient().getTupleQueryResult(url)) {
            while (tqresult.hasNext()) {
                BindingSet bindingSet = tqresult.next();
                Value id = bindingSet.getValue("id");
                result.add(id.stringValue());
            }
        }
        return result;
    }

    /**
     * Returns true if the repository id is contained in this catalog.
     *
     * @param repoId name of the repository to lookup
     * @return true if the repository id is contained in this catalog
     * @throws RDF4JException if repository does not exist
     *                          TODO: revisit the decision to throw instead of returning null
     *                          if does not exit. Why throw?
     *                          <p>
     *                          TODO: have this throw a RepositoryException instead, either
     *                          by modifying contains() or catching and rethrowing here.
     */
    public boolean hasRepository(String repoId) throws RDF4JException {
        List<String> repos = listRepositories();
        return repos.contains(repoId);
    }

    /**
     * Returns an {@link Repository#initialize() uninitialized}
     * AGRepository instance for the given
     * {@link #listRepositories() repository id}.
     * <p>
     * The repository is created if it does not exist.  If the
     * repository already exists, it is simply opened.
     *
     * @param repositoryID the id (the name) of the repository
     * @return an uninitialized AGRepository instance
     * @throws RepositoryException if there is an error while creating the repository
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
     * <p>
     * The repository is created if it does not exist.  If the
     * repository already exists, it is simply opened, or an exception
     * is thrown if strict=true.
     *
     * @param repositoryID the id (the name) of the repository
     * @param strict       if true and the repository already exists, throw an exception
     * @return an uninitialized AGRepository instance
     * @throws RepositoryException if there is an error while creating the repository
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
        } catch (RDF4JException e) {
            // TODO: modify hasRepository, shouldn't need to do this
            throw new RepositoryException(e);
        }
        return new AGRepository(this, repositoryID);
    }

    /**
     * Returns an uninitialized AGRepository instance for the given
     * {@link #listRepositories() repository id}.
     * <p>
     * If the repository already exists, it is simply opened.
     *
     * @param repositoryID the id (the name) of the repository
     * @return an uninitialized AGRepository instance
     * @throws RepositoryException if the repositoryID does not exist
     * @see #createRepository(String, boolean)
     */
    public AGRepository openRepository(String repositoryID)
            throws RepositoryException {
        try {
            if (!hasRepository(repositoryID)) {
                throw new RepositoryException("Repository not found with ID: " + repositoryID);
            }
        } catch (RDF4JException e) {
            // TODO: consider having methods in this class all throw OpenRDFExceptions
            throw new RepositoryException(e);
        }
        return new AGRepository(this, repositoryID);
    }

    /**
     * Deletes the repository with the given
     * {@link #listRepositories() repository id}.
     *
     * @param repositoryID the name of the repository to delete
     * @throws RepositoryException if there is an error while deleting the repository
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

}
