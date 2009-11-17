package com.franz.agraph.repository;

import org.openrdf.query.QueryLanguage;

/**
 * Extends the Sesame QueryLanguage class for AllegroGraph languages.
 *  
 */
public class AGQueryLanguage extends QueryLanguage {

	/**
	 * The Prolog Select query language for AllegroGraph. 
	 */
	public static final AGQueryLanguage PROLOG = new AGQueryLanguage("prolog");

	public AGQueryLanguage(String name) {
		super(name);
	}
	
}
