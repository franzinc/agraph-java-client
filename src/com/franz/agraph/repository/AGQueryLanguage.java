package com.franz.agraph.repository;

import org.openrdf.query.QueryLanguage;

public class AGQueryLanguage extends QueryLanguage {

	public static final AGQueryLanguage PROLOG = new AGQueryLanguage("Prolog");
	
	public AGQueryLanguage(String name) {
		super(name);
	}
	
}
