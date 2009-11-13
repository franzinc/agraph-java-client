
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

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.Triple;
import com.franz.agsail.AGForSail;
import com.franz.agsail.AGSailTriple;

/**
 * This class implements in Java a representation of an AllegroGraph triple.
 * <p>
 * The AllegroGraph instance has 5 components, id, subject, predicate, object
 *  and context.
 * All 4 are copied to the Java instance.
 * <p>
 * The components are copied when a Triple instance is created.
 * The associated URI or literal strings may or may not be filled at
 * creation time.
 * <p>
 * Triple instances are not unique.
 * There may be multiple Triple instances associated with one triple in
 * the triple store.
 */
public class AGSailTripleImpl 
    extends AGSailValueObjectImpl
    implements AGSailTriple
{
	
	static final long NO_TRIPLE = -1;


	private long AGId;
	boolean inferred = false;
	  
	  /**
	     * Return the unique integer identifier of the object.
	     * @return An integer object id.  If the value is negative, then the 
	     *    object has not been stored in the triple store and is simply a
	     *    place holder for a label.
	 * @throws AllegroGraphException 
	     */
	    public long queryAGId() { return directInstance.queryAGId(); }
	    
	    public long getAGId() throws AllegroGraphException {
	    	return directInstance.getAGId();
	    }
	    
	    /**
	     * Add a triple to the triple store.
	     * The Triple instance must have been created 
	     * with {@link AGForSail#createStatement(Resource, URI, Value)}
	     * or with {@link AGForSail#createStatement(Resource, URI, Value, Resource)}.
	     * @return true if the triple was added, false if the triple was already added
	     *    and therefore nothing was done.
	     * @throws AllegroGraphException
	     */
	    public boolean add () throws AllegroGraphException {
	    	if ( canReference() ) return false;
	    	if ( null==owner ) throw new IllegalStateException
	    			("Cannot assert Triple with null triple store.");
	    	owner.addStatement(getSubject(), getPredicate(), getObject(), getContext());
	    	return true;
 	    }
	    
	    
	    /**
	     * Qick AGId check for equality
	     * @param x
	     * @return +1 definite true, 0 definite false, -1 cannot tell
	     */
	    int sameAGId ( Object x ) {
	    	if ( x==null ) return 0;
	    	if ( x instanceof AGSailTripleImpl )
	    	{
	    		long xid = ((AGSailTripleImpl)x).AGId;
	    		if ( canReference(AGId) && canReference(xid) )
	    		{
	    			if (AGId==xid) return 1;
					return 0;
	    		}
	    	}
	    	return -1;
	    }
	    
	    boolean canReference() { return canReference(AGId); }
	    
    /**
	 * 
	 */
	private static final long serialVersionUID = -235560515765409236L;
	
	private Value subjInstance = null;
	private Value predInstance = null;
    private Value objInstance = null;
    private Value cxInstance = null;
    
    
    static final AGSailTripleImpl[] emptyArray = new AGSailTripleImpl[0];
    
    private Triple directInstance = null;
    
//  Use package access here because only use should be in AGFactory
    AGSailTripleImpl ( AGForSail ts, Triple base ) {
    	super();
    	owner = ts;
    	directInstance = base;
        }

    /**
     * Retrieve the subject slot of the Triple instance.
     * @return A UPI instance that identifies the subject node.  A null value means that
     *    the subject is not in the local cache, and must be obtained from the
     *    server with a call to getS();
     */
    public UPI queryS() { return directInstance.queryS(); }
    
    /**
     * Retrieve the subject slot of the Triple instance.
     * If the value is not in the local cache, this call will require a round-trip
     * to the AllegroGraph server.
     * @return An integer subject node number.  
     * 
     */
    public UPI getS() { return directInstance.getS(); }

    /**
     * Retrieve the subject slot of the Triple instance.
     * @return A string containg the URI of the subject node.
     * If the returned value is null, the method getSubjectLabel() must be used
     * to get the true value.
     */
    public String querySubject () { return directInstance.querySubject(); }

    /**
     * Retrieve the URI string associated with the 
     * subject component of this Triple.
     * @return A string containg the URI of the subject node.
     */
    public String getSubjectLabel () throws AllegroGraphException
    {
	return directInstance.getSubjectLabel();
    }

    /**
     * Create a Resource instance that describes the subject component
     * of the triple.
     * 
     *   This method must return a Resource instance as specified in the openrdf
     *   model specification.  Since AllegroGraph is more general, the subject
     *   slot can be a more general object.  In those cases, this method will
     *   throw a CLassCastException.
     *   
     *   Use the more general method 
     *   {@link #getSubjectInstance()}
     *   to retrieve any type.
     */
    public org.openrdf.model.Resource getSubject ()
    { 
    	return (org.openrdf.model.Resource)getSubjectInstance ();
    }
    
    public Value getSubjectInstance ()
    { 
    	if ( null==subjInstance )
    		subjInstance = owner.coerceToSailValue(directInstance.getSubjectInstance());
    	return subjInstance;
    }


    /**
     * Retrieve the predicate slot of the Triple instance.
     * @return A UPI instance that identifies the predicate node.  A null value means that
     *    the predicate is not in the local cache, and must be obtained from the
     *    server with a call to getP();
     */
    public UPI queryP() { return directInstance.queryP(); }
    
    /**
     * Retrieve the predicate slot of the Triple instance.
     * If the value is not in the local cache, this call will require a round-trip
     * to the AllegroGraph server.
     * @return A UPI instance that identifies the predicate node.  
     * 
     */
    public UPI getP() { 
    	return directInstance.getP(); 
    	}

    /**
     * Retrieve the predicate slot of the Triple instance.
     * @return The string form of the predicate node or null if the string
     * form is not available.  Use getPredicateLabel() to always return a string.
     */
    public String queryPredicate() { return directInstance.queryPredicate(); }

    /**
     * Retrieve the URI string associated with the 
     * predicate component of this Triple.
     * @return A string containg the URI of the predicate node.
     * @throws AllegroGraphException 
     */
    public String getPredicateLabel () throws AllegroGraphException
    {
	return directInstance.getPredicateLabel();
    }

    /**
     * Create a URI instance that describes the predicate component
     * of the triple.  
     * 
     * This method must return a Resource instance as specified in the openrdf
     *   model specification.  Since AllegroGraph is more general, the subject
     *   slot can be a more general object.  In those cases, this method will
     *   throw a CLassCastException.
     *   
     *   Use the more general method 
     *   {@link #getPredicateInstance()}
     *   to retrieve any type.
     */
    public org.openrdf.model.URI getPredicate ()
    { 
    	return (URI)(getPredicateInstance ());
    }

    public Value getPredicateInstance ()
    { 
    	if ( null==predInstance ) 
    		predInstance = owner.coerceToSailValue(directInstance.getPredicateInstance());
    		
    	return predInstance;
    }

    /**
     * Retrieve the object slot of the Triple instance.
     * @return A UPI instance that identifies the object node.  A null value means that
     *    the subject is not in the local cache, and must be obtained from the
     *    server with a vcall to getS();
     */
    public UPI queryO() { return directInstance.queryO(); }
    
    /**
     * Retrieve the object slot of the Triple instance.
     * If the value is not in the local cache, this call will require a round-trip
     * to the AllegroGraph server.
     * @return An integer object node number.  
     * 
     */
    public UPI getO() { 
    	return directInstance.getO(); 
    	}

    /**
     * Retrieve the object slot of the Triple instance.
     * @return A string or null. Use getObjectLabel() to always return a string.
     */
    public String queryObject() { return directInstance.queryObject(); }

    /**
     * Retrieve the URI string associated with the 
     * object component of this Triple.
     * @return A string containg the URI of the object node.
     * @throws AllegroGraphException 
     */
    public String getObjectLabel () throws AllegroGraphException
    {
	return directInstance.getObjectLabel();
    }

    /**
     * Create a Value instance that describes the object component
     * of the triple.  
     */
    public  org.openrdf.model.Value getObject ()
    { 
    	if ( null==objInstance ) 
    		objInstance = owner.coerceToSailValue(directInstance.getObject());
    	return objInstance;
    }
    
 
    
    /*
     * Retrieve the context slot of the Triple instance.
     * @return An integer context node number.
     */
    public UPI getC() { return directInstance.getC(); }

    /*
     * Retrieve the context slot of the Triple instance.
     * @return A string containg the URI of the context node.
     * If the returned value is null, the method getContextLabel() must be used
     * to get the true value.
     */
    public String queryContext () { return directInstance.queryContext(); }

    /*
     * Retrieve the URI string associated with the 
     * context component of this Triple.
     * @return A string containg the URI of the context node.
     */
    public String getContextLabel ()
    {
	return directInstance.getContextLabel();
    } 

    /**
     * Create a Resource instance that describes the context component
     * of the triple.  
     */
    public Resource getContext () { return (Resource)getContextInstance(); }
    
    public Value getContextInstance ()
    { 
    	if ( null==cxInstance ) 
    		cxInstance = owner.coerceToSailValue(directInstance.getContext());
        return cxInstance;
    } 

    
    /**
     * This method overrides the generic toString method.
     * This method generates a more readable output string.
     */
    public String toString () {
    	return directInstance.toString();
    }
    
    public boolean equals ( Object other ) {
    	if ( other instanceof AGSailTripleImpl ) 
    		return directInstance.equals(((AGSailTripleImpl)other).directInstance);
    	if ( other instanceof Statement )
    	{
    		Statement os = (Statement)other;
    		return   getSubject().equals(os.getSubject())
    		         &&
    		         getPredicate().equals(os.getPredicate())
    		         &&
    		         getObject().equals(os.getObject())
    		         ;
    	}
    	return false;
    }
    
    public int hashCode() {
    	return directInstance.hashCode();
    }
    
    
    public int compareTo ( AGSailTripleImpl to )  {
    		return directInstance.compareTo(to.directInstance);
    }


}
