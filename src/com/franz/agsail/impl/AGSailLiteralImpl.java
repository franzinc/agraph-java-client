
// ***** BEGIN LICENSE BLOCK *****
// Version: MPL 1.1
//
// The contents of this file are subject to the Mozilla Public License Version
// 1.1 (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at
// http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
// for the specific language governing rights and limitations under the
// License.
//
// The Original Code is the AllegroGraph Java Client interface.
//
// The Original Code was written by Franz Inc.
// Copyright (C) 2006 Franz Inc.  All Rights Reserved.
//
// ***** END LICENSE BLOCK *****

package com.franz.agsail.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.datatypes.XMLDatatypeUtil;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.LiteralNode;
import com.franz.agsail.AGForSail;
import com.franz.agsail.AGSailLiteral;


/**
 * This class represents an instance of a literal node in AllegroGraph.
 * <p>
 * The AllegroGraph object defines two slots, id and label.  Both slots are copied 
 * to the Java instance.
 * <p>
 * The label member may be a lazy value in the Java instance.  If queryLabel returns
 * null, getLabel() must make a round-trip to the triple store for the actual value.
 * <p>
 * There is no public constructor.  Literal instances are created by calls
 * to AllegroGraph methods.
 */
public class AGSailLiteralImpl
    extends AGSailValueImpl
    implements org.openrdf.model.Literal, AGSailLiteral
{

    /**
	 * 
	 */
	private static final long serialVersionUID = -3106405889572308071L;
	private String label;

//  Use package access here because only use should be in AGFactory
    AGSailLiteralImpl ( AGForSail ts, LiteralNode aglit ) {
    	setDirectInstance(aglit);
    	owner = ts;
	}
    
    private LiteralNode getLocalInstance() { return (LiteralNode)getDirectInstance(); }

    /**
     * Return the string associated with the Literal instance.
     * @return A string or null.
     * <p>
     * If the returned value is null, the string value is not in the local
     * Java cache, and must be retrieved from the AllegroGraph server with
     * a call to getLabel().
     */
    public String queryLabel () { return getLabel(); }


    /**
     * Return the string associated with the Literal instance.
     * @return A string.
     * If the value is not in the Java cache, retrieve it from the triple store.
     */
    public String getLabel() {
    	if ( label==null ) label = getLocalInstance().getLabel();
    	return label;
    }

    /**
     * Retrieve the string label of the datatype of the Literal.
     * @return null if the information is not in the local cache or
     *    if the Literal does not have a datatype label.
     * <p>
     * If the returned value is null, getType() or getDatatype() must be called
     * to get the actual value.
     */
    public String queryType () { return getLocalInstance().queryType(); }

/**
 * Retrieve the datatype as a URI instance.
 * @return 
 * If the string label is not in the local Java cache, this method
 * requires a round-trip to the AllegroGraph server.
 */
    public URI getDatatype () { 
    	return (URI) owner.coerceToSailValue(getLocalInstance().getDatatype());
    }
    

    /**
     * Retrieve the string label for the datatype of the Literal.
     * @return a string, or null if the Literal does not have a datatype field.
     * This operation may require a round-trip to the AllegroGraph triple store.
     */
    public String getType () {
    	return getLocalInstance().getType();
    }

    /**
     * Retrieve the language field of the Literal.
     * @return null if the value is not in the local cache or
     *     if the Literal does not have a language label.
     * <p>
     * If the returned value is null, getLanguage() must be called
     * to get the actual value.
     */
    public String queryLanguage () { return getLocalInstance().queryLanguage(); }


    /**
     * Retrieve the language qualifier of the Literal.
     * 
     * @return null if the Literal does not have a language qualifier.
     */
    public String getLanguage () {  
    	return getLocalInstance().getLanguage();
    }

  

    /**
     * This method overrides the generic toString method.
     * This method generates a more readable output string of the 
     * form "&lt;Literal <i>id</i>: <i>label</i>[langortype]&gt;".
     */
    public String toString() {
    	return getLocalInstance().toString();
    	
    } 
   
    
    /**
     * Convert a Literal instance to a string in ntriples syntax.
     * <p>
     * This operation may require a roundtrip to the AllegroGraph
     * triple store to fetch the required string data.
     */

    
    /**
     * Implement equality for Literal instances.
     * <p>
     * Two Literal instances are equal if both are registered in the
     * AllegroGraph triple store and  they have identical
     * AllegroGraph part id numbers.
     * <p>
     * Otherwise, the string representations are compared.
     */
    public boolean equals(Object other) {
    	if ( other instanceof AGSailLiteralImpl ) 
    		return getLocalInstance().equals(((AGSailLiteralImpl)other).getDirectInstance());
        if ( other instanceof Literal ) {
           // as per the OpenRDF model API:
           return toString().equals(other.toString());
        }
		return false;
    }

    /**
     * Compute the hashcode of a Literal instance.
     * <p>
     * The hashcode of a Literal instance is the hashcode
     * of its string representation.
     */
    public int hashCode() {
        // as per the OpenRDF model API:
        return toString().hashCode();
    }
    
    /**
     * Add this literal to the AllegroGraph triple store.
     * If the literal already is in the triple store, do nothing.
     * <p>
     * A Literal instance is in the triple store if queryAGId() returns
     * a non-null value.
     * @throws AllegroGraphException 
     *
     */
    public void add () throws AllegroGraphException {
    	getLocalInstance().add();
    }
    
    public UPI getAGId () throws AllegroGraphException {
    	return getLocalInstance().getAGId();
    }

    public String stringValue() {
        return getLabel();
    }

    public byte byteValue() {
        return XMLDatatypeUtil.parseByte(getLabel());
    }

    public short shortValue() {
        return XMLDatatypeUtil.parseShort(getLabel());
    }

    public int intValue() {
        return XMLDatatypeUtil.parseInt(getLabel());
    }

    public long longValue() {
        return XMLDatatypeUtil.parseLong(getLabel());
    }

    public BigInteger integerValue() {
        return XMLDatatypeUtil.parseInteger(getLabel());
    }

    public BigDecimal decimalValue() {
        return XMLDatatypeUtil.parseDecimal(getLabel());
    }

    public float floatValue() {
        return XMLDatatypeUtil.parseFloat(getLabel());
    }

    public double doubleValue() {
        return XMLDatatypeUtil.parseDouble(getLabel());
    }

    public boolean booleanValue() {
        return XMLDatatypeUtil.parseBoolean(getLabel());
    }

    public XMLGregorianCalendar calendarValue() {
        return XMLDatatypeUtil.parseCalendar(getLabel());
    }

}
