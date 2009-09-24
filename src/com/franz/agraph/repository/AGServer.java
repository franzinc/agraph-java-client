package com.franz.agraph.repository;

import java.util.List;

import com.franz.agraph.http.AGHTTPClient;

public class AGServer {

	private final String serverURL;
	private final AGHTTPClient httpClient;
	
	
	public AGServer(String serverURL, String username, String password) {
		this.serverURL = serverURL;
		httpClient = new AGHTTPClient(serverURL); 
		httpClient.setUsernameAndPassword(username, password);
	}
	
	public String getServerURL() {
		return serverURL;
	}
	
	public AGHTTPClient getHTTPClient() {
		return httpClient;
	}
	
	// TODO: tutorial will want this
	public List<AGCatalog> listCatalogs () {
		return null;
	}
	
	public AGCatalog getCatalog(String catalogID) {
		return new AGCatalog(this, catalogID);
	}

}
