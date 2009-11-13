package com.franz.agbase;

import com.franz.agbase.AllegroGraphException;



/**
 * The RDF N3 serializer is described in more detail in the 
 *   AllegroGraph Reference Guide for Lisp users
 *   as the serialize-rdf-n3 function.
 *
 */
public class RDFN3Serializer extends AllegroGraphSerializer {

	private String ifDoesNotExist = "create";
	private String ifExists = "error";
	private int indent = 0;

	protected Object run() throws AllegroGraphException {

		Object[] args = new Object[] {
			"source-type", sourceType,
			"source-id", sourceId,
			"serialization", "rdf-n3",
			"destination", destination,
			"if-does-not-exist", ifDoesNotExist,
			"if-exists", ifExists,
			"indent", new Integer(indent)
		};
		return ag.verifyEnabled().serializeTriples(ag, args);
	
	}

	/**
	 * @return the ifDoesNotExist
	 */
	public Object getIfDoesNotExist() {
		return ifDoesNotExist;
	}

	/**
	 * @param ifDoesNotExist the ifDoesNotExist to set.
	 * The allowed values are "create" or "error".  
	 *   The default is "create".
	 */
	public void setIfDoesNotExist(String ifDoesNotExist) {
		if ( null==ifDoesNotExist )
			ifDoesNotExist = "create";
		else if ( "create".equalsIgnoreCase(ifDoesNotExist)
				||
				"error".equalsIgnoreCase(ifDoesNotExist) ) 
		{}
		else
			throw new IllegalArgumentException("Invalid argument " + ifDoesNotExist);
		this.ifDoesNotExist = ifDoesNotExist;
	}

	/**
	 * @return the ifExists
	 */
	public Object getIfExists() {
		return ifExists;
	}

	/**
	 * @param ifExists the ifExists to set\
	 * The valid values are "error", "supersede", or "append.
	 *   The default is "error".
	 */
	public void setIfExists(String ifExists) {
		if ( null==ifExists )
			ifExists = "error";
		else if ( "supersede".equalsIgnoreCase(ifExists)
				||
				"error".equalsIgnoreCase(ifExists)
				||
				"append".equalsIgnoreCase(ifExists)
				) 
		{}
		else
			throw new IllegalArgumentException("Invalid argument " + ifExists);
		this.ifExists = ifExists;
	}

	/**
	 * @return the indent
	 */
	public int getIndent() {
		return indent;
	}

	/**
	 * @param indent the indent to set
	 */
	public void setIndent(int indent) {
		this.indent = indent;
	}

}
