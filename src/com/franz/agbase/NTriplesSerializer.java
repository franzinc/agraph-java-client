package com.franz.agbase;

import com.franz.agbase.AllegroGraphException;



/**
 * The N-Triples serializer is described in more detail in the 
 *   AllegroGraph Reference Guide for Lisp users
 *   as the print-triples function.
 *
 */
public class NTriplesSerializer extends AllegroGraphSerializer {

	private String ifDoesNotExist = "create";
	private String ifExists = "error";
	private String format = "ntriples";   // long | concise

	protected Object run() throws AllegroGraphException {

		Object[] args = new Object[] {
			"source-type", sourceType,
			"source-id", sourceId,
			"serialization", "ntriples",
			"destination", destination,
			"if-does-not-exist", ifDoesNotExist,
			"if-exists", ifExists,
			"format", format,
			"base-uri", verifiedBase
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
	 *   The allowed values are "create" or "error".  
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
	 * @param ifExists the ifExists to set.
	 *   The valid values are "error", "supersede", or "append.
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
	 * @return the format
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * @param format the format to set.
	 * The valid values are "ntriples", "long", or "concise".
	 * The default is "ntriples".
	 */
	public void setFormat(String format) {
		if ( null==format ) format = "null";
		if ( "ntriple".equalsIgnoreCase(format) ) format = "ntriples";
		if ( "short".equalsIgnoreCase(format) ) format = "concise";
		else if ( "ntriples".equalsIgnoreCase(format)
				||
				"long".equalsIgnoreCase(format)
				||
				"concise".equalsIgnoreCase(format)
				) 
		{}
		else
			throw new IllegalArgumentException("Invalid argument " + format);
		this.format = format;
	}
	
	/**
	 * Query the BaseURI option.
	 * @return the BaseURI option value.
	 */
	public Object getBaseURI () { return userBase; }
	
	/**
	 * Set the BaseURI option.
	 * @param base
	 *      <ul>
	 *        <li>If null (the initial default), 
	 *            do not generate a BaseURI comment.
	 *        <li>If a string, assume it is a valid URI
	 *            and use it as the value of the generated BaseURI comment.
	 *        <li>If a UPI or Value instance, assume it denotes a URI
	 *            and use it as the value of the generated BaseURI comment.
	 *        <li>If an AllegroGraph instance, use the UUID associated with 
	 *            the store as the value of the generated BaseURI comment.
	 *        <li>If boolean true, generate a new UUID and use it
	 *            as the value of the generated BaseURI comment.
	 *      </ul>
	 */
	public void setBaseURI ( Object base ) { setBase(base); }
	

}
