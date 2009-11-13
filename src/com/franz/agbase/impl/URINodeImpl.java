
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
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.URINode;
import com.franz.agbase.util.AGInternals;

/**
 * This class represents an instance of a labelled resource node in AllegroGraph.
 * <p>
 * The AllegroGraph object defines two slots, id and uri.  Both slots are copied 
 * to the Java instance.
 * <p>
 * The URI member may be a lazy value in the Java instance.  If queryURI returns
 * null, getURI() will need a round-trip to the triple store to fetch the
 * actual value.
 * <p>
 * There is no public constructor.  Node instances are created by calls
 * to AllegroGraph methods.
 */
public class URINodeImpl 
    extends ResourceNodeImpl
    implements URINode
{

    /**
	 * 
	 */
	private static final long serialVersionUID = 8675791173081086903L;

	String uri = null;
	
	public static final URINode nullContext = new URINodeImpl(null, UPIImpl.nullUPI(), null); 

//	 Use package access here because only use should be in AGFactory
	URINodeImpl ( AllegroGraph ts, UPI i, String u ) {
	super();
	owner = ts; nodeUPI = i; uri = u; 
	}
	
	
	/**
	 * Retrieve the AllegroGraph ID number of the Node.
	 * @return the ID number
	 * <p>
	 * If the Node is already registered in the AG triple store, return the locally
	 * cached value of the ID number.  Otherwise, register the Node in the
	 * AG triple store and return the new ID number.
	 * @throws AllegroGraphException
	 */
	public UPI getAGId () throws AllegroGraphException {
		UPI n = queryAGId();
		n = getUPI(owner, n, uri);
		setAGId(n);
		return n;
	}
	
	public static UPI getUPI( AGInternals owner, UPI n, String uri) throws AllegroGraphException {
		if ( UPIImpl.canReference(n) ) return  n; 
		if ( uri==null )
			throw new IllegalStateException("Cannot realize a node without a URI");
		n = owner.verifyEnabled().newResource(owner, uri);
		return n;
	}
	

    /**
     * Retrieve the URI string associated with the node instance.
     * @return A string or null.
     * If the returned value is null, the actual value must be obtained
     * by calling getURI().
     */
    public String queryURI () { return uri; }

  

    /**
     * Retrieve the local name component of the URI string associated 
     * with the node instance.
     * @return A string.
     * If the value is not in the Java cache, retrieve it from the triple store.
     */
    public String getLocalName ()
    {
    	uri = owner.getText(nodeUPI, uri);
    	int p = uri.indexOf("#");
    	if ( p<0 ) return "";
    	return uri.substring(p+1);
    }

    /**
     * Retrieve the namespace component of the URI string associated 
     * with the node instance.
     * @return A string or null if the URI does not have a namespace component.
     * If the value is not in the Java cache, retrieve it from the triple store.
     */
    public String getNamespace ()
    {
    	uri = owner.getText(nodeUPI, uri);
    	int p = uri.indexOf("#");
    	if ( p<0 ) return uri;
    	return uri.substring(0, p+1);
    }

    /**
     * Retrieve the URI string associated with the node instance.
     * @return A string.
     * If the value is not in the Java cache, retrieve it from the triple store.
     * 
     */
    public String toString() {
    	uri = owner.getText(nodeUPI, uri);
    	return uri;
    } 
    
    // These are not referenced, nad should not be because the work
    // needs to be more complex if done at all.
//    String stringRef() {
//    	if ( uri==null ) throw new IllegalStateException("Cannot stringify this node");
//    	return "<" + uri + ">";
//    }
//    
//    static String stringRef ( String uri ) {
//    	if ( uri==null ) throw new IllegalArgumentException("Cannot reference a null URI");
//    	return "<" + uri + ">";
//    }
    
    /**
     * Implement equality for Node instances.
     * <p>
     * Two Node instances are equal if both are registered in the
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
        if (other instanceof URINode) {
           // as per the OpenRDF model API:
           return toString().equals(other.toString());
        }
		return false;
    }

    /**
     * Compute the hashcode of a Node instance.
     * <p>
     * The hashcode of a Node instance is the hashcode
     * of its string representation.
     */
    public int hashCode() {
        // as per the OpenRDF model API:
        return toString().hashCode();
    }

	public TriplesIterator getPredicateStatements() throws AllegroGraphException {
		return owner.getStatements(null, this, null);
	}

	public String getURI() { return toString(); }
	
	/**
     * Add this node to the AllegroGraph triple store.
     * If the node already is in the triple store, do nothing.
     * <p>
     * A Node instance is in the triple store if queryAGId() returns
     * a non-null value.
	 * @throws AllegroGraphException 
     *
     */
    public void add () throws AllegroGraphException {
    	getAGId();
    }
    

}
