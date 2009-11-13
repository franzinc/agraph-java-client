
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

package com.franz.agbase.impl;


import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.LiteralNode;
import com.franz.agbase.URINode;


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
public class LiteralNodeImpl
    extends ValueNodeImpl
    implements LiteralNode
{

    /**
	 * 
	 */
	private static final long serialVersionUID = -3106405889572308071L;
	private String label;
   
  
    
    /**
     *    typeId     type
     *      null     null    Not a typed literal
     *      null    string   Typed literal, string is URI
     *    res-UPI    null    Typed literal, UPI is type
     *    res-UPI   string   Typed literal, UPI is type, string is URI
     */
    public UPI typeId;
    public String type;
    
    
    public static final class Lang {
    	int tag;
    	private Lang ( int n ) { tag = n; }
    	public static final Lang KNOWN = new Lang(1);
    	public static final Lang NONE  = new Lang(0);
    }
    public static final Lang LANG_KNOWN = Lang.KNOWN;
    public static final Lang LANG_NONE  = Lang.NONE;
    private Lang langSlot = null;  
    private String language;

//  Use package access here because only use should be in AGFactory
    LiteralNodeImpl ( AllegroGraph ts, UPI i, String newLabel,
    				UPI newTypeId, String newType, 
    				Lang newLangSlot, String newLanguage ) {
	super();
	//System.out.println("i="+i+"  u="+u+"  tp="+tp+"  tt="+tt+"  lp="+lp+"  ll="+ll);
	owner = ts; nodeUPI = i; label = newLabel; type = newType; language = newLanguage;
	typeId = newTypeId;   langSlot = newLangSlot;
	
	}

    /**
     * Return the string associated with the Literal instance.
     * @return A string or null.
     * <p>
     * If the returned value is null, the string value is not in the local
     * Java cache, and must be retrieved from the AllegroGraph server with
     * a call to getLabel().
     */
    public String queryLabel () { return label; }


    /**
     * Return the string associated with the Literal instance.
     * @return A string.
     * If the value is not in the Java cache, retrieve it from the triple store.
     */
    public String getLabel() 
    {
	label = owner.getText(nodeUPI, label); 
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
    public String queryType () { return type; }

/**
 * Retrieve the datatype as a URI instance.
 * @return 
 * If the string label is not in the local Java cache, this method
 * requires a round-trip to the AllegroGraph server.
 */
    public URINode getDatatype ()
    { 
    	String tp = getType();
    	if ( tp == null ) return null;
    	return owner.createURI(tp); 
    }
    

    /**
     * Retrieve the string label for the datatype of the Literal.
     * @return a string, or null if the Literal does not have a datatype field.
     * This operation may require a round-trip to the AllegroGraph triple store.
     */
    public String getType () {
    	if ( type!=null ) return type;
    	if ( typeId==null ) return null;
    	if ( UPIImpl.canReference(typeId) )
    	{
    		type = owner.getText(typeId, null);
    	}
    	else try 
    	{
    		type = owner.getTypePart(nodeUPI);
    		typeId = null;
    	}
    	catch (AllegroGraphException e) {}
    	return type;
    }

    /**
     * Retrieve the language field of the Literal.
     * @return null if the value is not in the local cache or
     *     if the Literal does not have a language label.
     * <p>
     * If the returned value is null, getLanguage() must be called
     * to get the actual value.
     */
    public String queryLanguage () { return language; }


    /**
     * Retrieve the language qualifier of the Literal.
     * 
     * @return null if the Literal does not have a language qualifier.
     */
    public String getLanguage () {
    	if ( language!=null ) return language;
    	if ( LANG_KNOWN==langSlot ) return language;
    	if ( LANG_NONE==langSlot ) return null;
    	try 
    	{
    		language = owner.getLangPart(nodeUPI);
    		langSlot = LANG_KNOWN;
    	}
    	catch (AllegroGraphException e) {}
    	return language;
    }

  

    /**
     * This method overrides the generic toString method.
     * This method generates a more readable output string of the 
     * form "&lt;Literal <i>id</i>: <i>label</i>[langortype]&gt;".
     */
    public String toString() {
    	String tail = "";
    	if ( (typeId==null) && type==null ) {
    		if ( langSlot==LANG_NONE ) tail = "";
        	else if ( langSlot==null ) tail = "@?";
        	else if ( langSlot!=LANG_KNOWN ) tail = "@<err>";
        	else if ( language==null ) tail = "";
        	else tail = "@" + language;
    	}
    	else if ( type!=null ) tail = "^^" + type;
    	else if ( UPIImpl.canReference(typeId) )
    		tail = "^^<" + typeId + ">";
    	else 
    		tail = "^^?";
    	return "<Literal " + nodeUPI + ": " + label + tail + ">";
    	
    } 
   
    
    

    
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
    	switch(sameAGId(other)) {
    	case 1: return true;
    	case 0: return false;
    	}
        if (other instanceof LiteralNodeImpl) {
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
    	if ( canReference() ) return;
    	if ( label==null )
    		throw new IllegalStateException("Cannot add Literal with null label.");	
    	if ( type!=null ) {
    		nodeUPI = owner.verifyEnabled().newLiteral(owner, label, type, null);
    		return;
    	}
    	if ( UPIImpl.canReference(typeId) ) {
    		nodeUPI = owner.verifyEnabled().newLiteral(owner, label, typeId, null);
    		return;
    	}
    	if ( typeId!=null )
    		throw new IllegalStateException("Cannot add Literal with unknown type.");
    	
    	if ( (langSlot==LANG_KNOWN) && language!=null  ) {
    		nodeUPI = owner.verifyEnabled().newLiteral(owner, label, type, language);
    		return;
    	}
    	if ( langSlot==LANG_NONE ) {
    		nodeUPI = owner.verifyEnabled().newLiteral(owner, label, type, language);
    		return;
    	}
    	throw new IllegalStateException("Cannot add uninitialized Literal.");
    }
    
    public UPI getAGId () throws AllegroGraphException {
    	UPI x = queryAGId();
    	if ( null!=x ) return x;
    	add();
    	return super.getAGId();
    }

}
