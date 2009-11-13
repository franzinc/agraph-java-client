
//***** BEGIN LICENSE BLOCK *****
//Version: MPL 1.1
//
//The contents of this file are subject to the Mozilla Public License Version
//1.1 (the "License"); you may not use this file except in compliance with
//the License. You may obtain a copy of the License at
//http://www.mozilla.org/MPL/
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
//for the specific language governing rights and limitations under the
//License.
//
//The Original Code is the AllegroGraph Java Client interface.
//
//The Original Code was written by Franz Inc.
//Copyright (C) 2006 Franz Inc.  All Rights Reserved.
//
//***** END LICENSE BLOCK *****

package com.franz.agsail.impl;

import org.openrdf.model.BNode;

import com.franz.agbase.BlankNode;
import com.franz.agsail.AGForSail;
import com.franz.agsail.AGSailBlankNode;

/**
 * This class represents an instance of a blank (anonymous) node in AllegroGraph.
 * <p>
 * There is no public constructor.  Node instances are created by a call to 
 * the createBNode() methods in the AllegroGraph class.
 */
public class AGSailBlankNodeImpl
    extends AGSailResourceImpl
	    implements AGSailBlankNode
{

    /**
	 * 
	 */
	private static final long serialVersionUID = -1036533850896736724L;
	String idString;       

	// Use package access here because only use should be in AGFactory
	AGSailBlankNodeImpl ( AGForSail ts, BlankNode di ) {
    	setDirectInstance(di);
    	owner = ts;
    	idString = di.getID();
    }
    
    AGSailBlankNodeImpl ( AGForSail ts ) {
    	BlankNode di = ts.getDirectInstance().createBNode();
    	setDirectInstance(di);
    	owner = ts;
    	idString = di.getID();
    }
    	

    /**
     * Retrieve the identifying string of the BlankNode instance.
     * <p>
     * This identifying string exists only in the Java application.
     * AllegroGraph does not implement persistent labels in the triple
     * store.  The persisitent label of the BlankNode instance in the
     * triple store is determined by the AllegroGraph implementation.
     */
    public String getID () { return idString; }


    /**
     * This method overrides the generic toString method.
     * This method generates an output string of 
     * the form "&lt;_:blank<i>nnn</i>&gt;.
     */
    public String toString() { return "<" + idString + ">"; } 
    
    /**
     * Implement equality for BlankNode instances.
     * <p>
     * Two BlankNode instances are equal if they have identical
     * AllegroGraph part id numbers.
     * <p>
     * Otherwise, the string representations are compared.
     */
    public boolean equals(Object other) {
    	if ( other instanceof AGSailBlankNodeImpl )
    		return getDirectInstance().equals(((AGSailBlankNodeImpl)other).getDirectInstance());
        if (other instanceof BNode) {
           // as per the OpenRDF model API:
           return toString().equals(other.toString());
        }
		return false;
    }

    /**
     * Compute the hashcode of a BlankNode instance.
     * <p>
     * The hashcode of a BlankNode instance is the hashcode
     * of its string representation.
     */
    public int hashCode() {
        // as per the OpenRDF model API:
        return toString().hashCode();
    }
    
    public String stringValue() {
        return getID();
    }
}
