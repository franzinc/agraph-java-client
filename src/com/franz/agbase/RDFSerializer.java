package com.franz.agbase;

import com.franz.agbase.AllegroGraphException;




/**
 * The RDF serializer is described in more detail in the 
 *   AllegroGraph Reference Guide for Lisp users
 *   as the serialize-rdf/xml function.
 *
 */
public class RDFSerializer extends AllegroGraphSerializer {
	
	public RDFSerializer() {}
	
	
	protected Object run() throws AllegroGraphException {
		Object[] args = new Object[] {
			"source-type", sourceType,
			"source-id", sourceId,
			"serialization", "RDF",
			"destination", destination,
			"error-on-invalid-p", errorOnInvalid?(new Integer(1)):null,
			"if-does-not-exist", ifDoesNotExist,
			"if-exists", ifExists,
			"indent", new Integer(indent),
			"memoize-abbrev-lookups-p", memoizeAbbrevLookups?(new Integer(1)):null,
			"nestp", nest?(new Integer(1)):null,
			"output-types-p", outputTypes?(new Integer(1)):null,
			"prepare-namespaces-p", prepareNamespaces?(new Integer(1)):null,
			"xml-base", verifiedBase
		};
		return ag.verifyEnabled().serializeTriples(ag, args);
	}
	
	// memoize-abbrev-lookups-p 
	boolean memoizeAbbrevLookups = false;
//	prepare-namespaces-p 
	boolean prepareNamespaces = false;
	// output-types-p 
	boolean outputTypes = false;
//	nestp 
	boolean nest = false;
//	error-on-invalid-p
	boolean errorOnInvalid = false;
//	indent 
	int indent = 0;
//	if-exists 
	String ifExists = "error";
//	if-does-not-exist
	String ifDoesNotExist = "create";
	
	/**
	 * Query the XMLBase option.
	 * @return the XMLBase option value.
	 */
	public Object getXMLBase () { return userBase; }
	
	/**
	 * Set the XMLBase option.
	 * @param base
	 *      <ul>
	 *        <li>If null (the initial default), 
	 *            do not generate an xml:base attribute.
	 *        <li>If a string, assume it is a valid URI
	 *            and use it as the value of the generated xml:base attribute.
	 *        <li>If a UPI or Value instance, assume it denotes a URI
	 *            and use it as the value of the generated xml:base attribute.
	 *        <li>If an AllegroGraph instance, use the UUID associated with 
	 *            the store as the value of the generated xml:base attribute.
	 *        <li>If boolean true, generate a new UUID and use it
	 *            as the value of the generated xml:base attribute.
	 *      </ul>
	 */
	public void setXMLBase ( Object base ) { setBase(base); }
	
	/**
	 * @return the errorOnInvalid
	 */
	public boolean isErrorOnInvalid() {
		return errorOnInvalid;
	}
	/**
	 * @param errorOnInvalid the errorOnInvalid to set
	 */
	public void setErrorOnInvalid(boolean errorOnInvalid) {
		this.errorOnInvalid = errorOnInvalid;
	}
	/**
	 * @return the ifDoesNotExist
	 */
	public String getIfDoesNotExist() {
		return ifDoesNotExist;
	}
	/**
	 * @param ifDoesNotExist the ifDoesNotExist to set.
	 *  The allowed values are "create" or "error".  
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
	public String getIfExists() {
		return ifExists;
	}
	/**
	 * @param ifExists the ifExists to set.
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
	/**
	 * @return the memoizeAbbrevLookups
	 */
	public boolean isMemoizeAbbrevLookups() {
		return memoizeAbbrevLookups;
	}
	/**
	 * @return the nest
	 */
	public boolean isNest() {
		return nest;
	}
	/**
	 * @param nest the nest to set
	 */
	public void setNest(boolean nest) {
		this.nest = nest;
	}
	/**
	 * @return the outputTypes
	 */
	public boolean isOutputTypes() {
		return outputTypes;
	}
	/**
	 * @param outputTypes the outputTypes to set
	 */
	public void setOutputTypes(boolean outputTypes) {
		this.outputTypes = outputTypes;
	}
	/**
	 * @return the prepareNamespaces
	 */
	public boolean isPrepareNamespaces() {
		return prepareNamespaces;
	}
	/**
	 * @param prepareNamespaces the prepareNamespaces to set
	 */
	public void setPrepareNamespaces(boolean prepareNamespaces) {
		this.prepareNamespaces = prepareNamespaces;
	}
	
	/**
	 * @param memoizeAbbrevLookups the memoizeAbbrevLookups to set
	 */
	public void setMemoizeAbbrevLookups(boolean memoizeAbbrevLookups) {
		this.memoizeAbbrevLookups = memoizeAbbrevLookups;
	}


}
