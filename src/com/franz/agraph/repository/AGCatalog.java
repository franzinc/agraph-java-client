package com.franz.agraph.repository;

import java.io.IOException;
import java.util.Collection;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGErrorType;
import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpException;
import com.franz.agraph.http.AGProtocol;

public class AGCatalog {

	private AGServer server;
	private final String catalogName;
	private final int catalogType;
	private final String catalogURL;
	private final String repositoriesURL;

	public static final int ROOT_CATALOG = 0;
	public static final int FEDERATED_CATALOG = 1;
	public static final int NAMED_CATALOG = 2;

	public AGCatalog(AGServer server, String catalogName) {
		this.server = server;
		this.catalogName = catalogName;
		this.catalogType = NAMED_CATALOG;
		catalogURL = AGProtocol.getNamedCatalogLocation(server.getServerURL(), catalogName);
		repositoriesURL = AGProtocol.getNamedCatalogRepositoriesLocation(catalogURL);
	}

	public AGCatalog(AGServer server, int catalogType) {
		switch (catalogType) {
		case ROOT_CATALOG:
			catalogName = "/";
			catalogURL = AGProtocol.getRootCatalogURL(server.getServerURL());
			repositoriesURL = AGProtocol.getRootCatalogRepositoriesLocation(catalogURL);
			break;
		case FEDERATED_CATALOG:
			catalogName = "federated";
			catalogURL = AGProtocol.getFederatedCatalogURL(server.getServerURL());
			repositoriesURL = AGProtocol.getFederatedRepositoriesLocation(catalogURL);
			break;
		default:
			throw new IllegalArgumentException("Invalid Catalog Type: "	+ catalogType);
		}
		this.server = server;
		this.catalogType = catalogType;
	}

	public AGServer getServer() {
		return server;
	}

	public String getCatalogName() {
		return catalogName;
	}

	public int getCatalogType() {
		return catalogType;
	}
	
	public String getCatalogURL() {
		return catalogURL;
	}

	public String getRepositoriesURL() {
		return repositoriesURL;
	}
	
	protected String getCatalogPrefixedRepositoryID(String repositoryID) {
		String catalogPrefixedRepositoryID;
		switch (getCatalogType()) {
		case ROOT_CATALOG:
			catalogPrefixedRepositoryID = repositoryID;
			break;
		case FEDERATED_CATALOG:
		case NAMED_CATALOG:
			catalogPrefixedRepositoryID = getCatalogName() + ":" + repositoryID;
			break;
		default:
			throw new IllegalArgumentException("Invalid Catalog Type: "	+ catalogType);
		}
		return catalogPrefixedRepositoryID;
	}

	// TODO this should be part of AGProtocol.
	public String getRepositoryURL(String repositoryID) {
		return repositoriesURL + "/" + repositoryID;
	}
	
	// TODO
	public Collection<AGRepository> getAllRepositories() {
		return null;
	}

	public AGHTTPClient getHTTPClient() {
		return getServer().getHTTPClient();
	}

	/**
	 * 
	 * @param repositoryID
	 * @return an uninitialized AGRepository
	 * @throws RepositoryException
	 */
	public AGRepository createRepository(String repositoryID)
			throws RepositoryException {
		String repoURL = AGProtocol.getRepositoryLocation(getCatalogURL(),
				repositoryID);
		try {
			getHTTPClient().putRepository(repoURL);
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
		return new AGRepository(this, repositoryID);
	}

	public void deleteRepository(String repositoryID)
			throws RepositoryException {
		String repoURL = AGProtocol.getRepositoryLocation(getCatalogURL(),
				repositoryID);
		try {
			getHTTPClient().deleteRepository(repoURL);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * 
	 * 
	 * @param args
	 *            unused
	 * @throws RepositoryException
	 */
	public static void main(String[] args) throws RepositoryException {
		String serverURL = "http://localhost:4166";
		String catalogID = "testcatalog";
		String username = "bmillar";
		String password = "xyzzy";

		AGServer server = new AGServer(serverURL, username, password);
		AGCatalog catalog = server.getCatalog(catalogID);

		AGRepository repo = catalog.createRepository("foobar");
		repo.initialize();

		Collection<AGRepository> repos = catalog.getAllRepositories();
		for (Repository r : repos) {
			System.out.println(r);
		}
	}

	
}
