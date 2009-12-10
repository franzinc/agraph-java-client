package com.franz.agraph.jena;

public class AGQuery {

	private final String queryString;
	
	AGQuery(String queryString) {
		this.queryString = queryString;
	}
	
	public String getQueryString() {
		return queryString;
	}

}
