package com.franz.agraph.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGErrorType;
import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpException;
import com.franz.agraph.http.AGProtocol;

public class AGServer {

	private final String serverURL;
	private final AGHTTPClient httpClient;
	private final AGCatalog rootCatalog;
	private final AGCatalog federatedCatalog;
	
	
	public AGServer(String serverURL, String username, String password) {
		this.serverURL = serverURL;
		httpClient = new AGHTTPClient(serverURL); 
		httpClient.setUsernameAndPassword(username, password);
		rootCatalog = new AGCatalog(this,AGCatalog.ROOT_CATALOG);
		federatedCatalog = new AGCatalog(this,AGCatalog.FEDERATED_CATALOG);
		}
	
	public String getServerURL() {
		return serverURL;
	}
	
	public AGHTTPClient getHTTPClient() {
		return httpClient;
	}
	
	public AGCatalog getRootCatalog() {
		return rootCatalog;
	}
	
	public AGCatalog getFederatedCatalog() {
		return federatedCatalog;
	}
	
	// TODO: tutorial will want this
	public List<AGCatalog> listCatalogs () {
		return null;
	}
	
	public AGCatalog getCatalog(String catalogID) {
		return new AGCatalog(this, catalogID);
	}

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
	
	public void deleteFederation(String federationName)	throws RepositoryException {
		String url = AGProtocol.getFederationLocation(getServerURL(), federationName); 
		Header[] headers = new Header[0];
		try {
			getHTTPClient().delete(url,headers,null);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}
	
}
