
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
import com.franz.agbase.ResourceNode;
import com.franz.agbase.Triple;
import com.franz.agbase.URINode;
import com.franz.agbase.ValueNode;
import com.franz.agbase.transport.AGConnector;
import com.franz.agbase.util.AGC;

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
public class TripleImpl 
    extends ValueObjectImpl
    implements Triple
{
	
	static final long NO_TRIPLE = -1;


	private long AGId;
	boolean inferred = false;
	  
	  /**
	     * Return the unique integer identifier of the object.
	     * @return An integer object id.  If the value is negative, then the 
	     *    object has not been stored in the triple store and is simply a
	     *    place holder for a label.
	     */
	    public long queryAGId() { return AGId; }
	    
	    public long getAGId() throws AllegroGraphException {
	    	if ( canReference() ) return AGId;
	    	throw new AllegroGraphException("This object does not have a valid id.");
	    }
	    
	    /**
	     * Add a triple to the triple store.
	     * The Triple instance must have been created 
	     * with {@link AllegroGraph#createStatement(ResourceNode, URINode, ValueNode)}
	     * or with {@link AllegroGraph#createStatement(ResourceNode, URINode, ValueNode, ResourceNode)}.
	     * @return true if the triple was added, false if the triple was already added
	     *    and therefore nothing was done.
	     * @throws AllegroGraphException
	     */
	    public boolean add () throws AllegroGraphException {
	    	if ( canReference() ) return false;
	    	if ( null==owner ) throw new IllegalStateException
	    			("Cannot assert Triple with null triple store.");
	    	owner.addStatement(subject, predicate, object, context);
	    	return true;
 	    }
	    
	    
	    /**
	     * Qick AGId check for equality
	     * @param x
	     * @return +1 definite true, 0 definite false, -1 cannot tell
	     */
	    public int sameAGId ( Object x ) {
	    	if ( x==null ) return 0;
	    	if ( x instanceof TripleImpl )
	    	{
	    		long xid = ((TripleImpl)x).AGId;
	    		if ( canReference(AGId) && canReference(xid) )
	    		{
	    			if (AGId==xid) return 1;
					return 0;
	    		}
	    	}
	    	return -1;
	    }
	    
	    public boolean canReference() { return canReference(AGId); }
	    
    /**
	 * 
	 */
	private static final long serialVersionUID = -235560515765409236L;
	
	private UPI s, p, o, c;
	
	// These fields have package visibility because CursorImpl uses them a lot.
	String subject = null;
	String predicate = null;
	String object = null;
	String context = null;
	int sType = 0; int pType = 0; int oType = 0; int cType = 0;
	String subjMod = null;
	String predMod = null;
	String objMod = null;
	String cxMod = null;
	
	public ValueNode subjInstance = null;
    public ValueNode predInstance = null;
    public ValueNode objInstance = null;
    private ValueNode cxInstance = null;
    
    
    static final TripleImpl[] emptyArray = new TripleImpl[0];
    
//  Use package access here because only use should be in AGFactory
    TripleImpl ( AllegroGraph ts, UPI ss, UPI pp, UPI oo  ) {
    	super();
    	owner = ts; AGId = NO_TRIPLE;
    	s = ss; p = pp; o = oo;  c = UPIImpl.nullUPI();
        }
    
//  Use package access here because only use should be in AGFactory
    TripleImpl ( AllegroGraph ts, UPI ss, UPI pp, UPI oo, UPI cc  ) {
    	super();
    	owner = ts; AGId = NO_TRIPLE;
    	s = ss; p = pp; o = oo;  
    	c = UPIImpl.refNull(cc);
        }
    
//  Use package access here because only use should be in AGFactory
    TripleImpl ( AllegroGraph ts, long i, UPI ss, UPI pp, UPI oo  ) {
	super();
	owner = ts; AGId = i;
	s = ss; p = pp; o = oo;  c = UPIImpl.nullUPI();
    }
    
//  Use package access here because only use should be in AGFactory
    TripleImpl ( AllegroGraph ts, long i, UPI ss, UPI pp, UPI oo, UPI cc  ) {
    	super();
    	owner = ts; AGId = i;
    	s = ss; p = pp; o = oo;  
    	c = UPIImpl.refNull(cc);
        }

    /**
     * Retrieve the subject slot of the Triple instance.
     * @return A UPI instance that identifies the subject node.  A null value means that
     *    the subject is not in the local cache, and must be obtained from the
     *    server with a call to getS();
     */
    public UPI queryS() { return s; }
    
    /**
     * Retrieve the subject slot of the Triple instance.
     * If the value is not in the local cache, this call will require a round-trip
     * to the AllegroGraph server.
     * @return An integer subject node number.  
     * 
     */
    public UPI getS() { 
    	if ( s==null ) getParts();
    	return s; 
    	}

    /**
     * Retrieve the subject slot of the Triple instance.
     * @return A string containg the URI of the subject node.
     * If the returned value is null, the method getSubjectLabel() must be used
     * to get the true value.
     */
    public String querySubject () { return subject; }

    /**
     * Retrieve the URI string associated with the 
     * subject component of this Triple.
     * @return A string containg the URI of the subject node.
     */
    public String getSubjectLabel () throws AllegroGraphException
    {
	if ( subject==null ) getParts();
	return subject;
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
    public ResourceNode getSubject ()
    { 
    	return (ResourceNode)getSubjectInstance ();
    }
    
    public ValueNode getSubjectInstance ()
    { 
    	if ( null!=subjInstance ) return subjInstance;
    	if ( (sType>0) && (null!=subject) ) 
    		subjInstance = (ValueNode) owner.newValue(s, sType, subject, subjMod);
    	else
    		subjInstance = (ValueNodeImpl) (owner.newValue(getS()));
    	return subjInstance;
    }


    /**
     * Retrieve the predicate slot of the Triple instance.
     * @return A UPI instance that identifies the predicate node.  A null value means that
     *    the predicate is not in the local cache, and must be obtained from the
     *    server with a call to getP();
     */
    public UPI queryP() { return p; }
    
    /**
     * Retrieve the predicate slot of the Triple instance.
     * If the value is not in the local cache, this call will require a round-trip
     * to the AllegroGraph server.
     * @return A UPI instance that identifies the predicate node.  
     * 
     */
    public UPI getP() { 
    	if ( p==null ) getParts();
    	return p; 
    	}

    /**
     * Retrieve the predicate slot of the Triple instance.
     * @return The string form of the predicate node or null if the string
     * form is not available.  Use getPredicateLabel() to always return a string.
     */
    public String queryPredicate() { return predicate; }

    /**
     * Retrieve the URI string associated with the 
     * predicate component of this Triple.
     * @return A string containg the URI of the predicate node.
     * @throws AllegroGraphException 
     */
    public String getPredicateLabel () throws AllegroGraphException
    {
	if ( predicate==null ) getParts();
	return predicate;
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
    public URINode getPredicate ()
    { 
    	return (URINode)(getPredicateInstance ());
    }

    public ValueNode getPredicateInstance ()
    { 
    	if ( null!=predInstance ) return predInstance;
    	if ( (pType>0) && (null!=predicate) ) 
    		predInstance = (ValueNode) owner.newValue(p, pType, predicate, predMod);
    	else
    		predInstance = (ValueNodeImpl) (owner.newValue(getP()));
    	return predInstance;
    }

    /**
     * Retrieve the object slot of the Triple instance.
     * @return A UPI instance that identifies the object node.  A null value means that
     *    the subject is not in the local cache, and must be obtained from the
     *    server with a vcall to getS();
     */
    public UPI queryO() { return o; }
    
    /**
     * Retrieve the object slot of the Triple instance.
     * If the value is not in the local cache, this call will require a round-trip
     * to the AllegroGraph server.
     * @return An integer object node number.  
     * 
     */
    public UPI getO() { 
    	if ( o==null ) getParts();
    	return o; 
    	}

    /**
     * Retrieve the object slot of the Triple instance.
     * @return A string or null. Use getObjectLabel() to always return a string.
     */
    public String queryObject() { return object; }

    /**
     * Retrieve the URI string associated with the 
     * object component of this Triple.
     * @return A string containg the URI of the object node.
     * @throws AllegroGraphException 
     */
    public String getObjectLabel () throws AllegroGraphException
    {
	if ( object==null ) getParts();
	return object;
    }

    /**
     * Create a Value instance that describes the object component
     * of the triple.  
     */
    public  ValueNode getObject ()
    { 
    	if ( null!=objInstance ) return objInstance;
    	if ( (oType>0) && (null!=object) ) 
    		objInstance = (ValueNode) owner.newValue(o, oType, object, objMod);
    	else
    		objInstance = (ValueNodeImpl) (owner.newValue(getO()));
    	return objInstance;
    }
    
 
    
    /*
     * Retrieve the context slot of the Triple instance.
     * @return An integer context node number.
     */
    public UPI getC() { return c; }

    /*
     * Retrieve the context slot of the Triple instance.
     * @return A string containg the URI of the context node.
     * If the returned value is null, the method getContextLabel() must be used
     * to get the true value.
     */
    public String queryContext () { return context; }

    /*
     * Retrieve the URI string associated with the 
     * context component of this Triple.
     * @return A string containg the URI of the context node.
     */
    public String getContextLabel ()
    {
	if ( context==null ) getParts();
	return context;
    } 

    /**
     * Create a Resource instance that describes the context component
     * of the triple.  
     */
    public ValueNode getContext ()
    { 
    	if ( null!=cxInstance ) return cxInstance;
    	if ( (cType>0) && (null!=context) ) 
    		cxInstance = (ValueNode) owner.newValue(c, cType, context, cxMod);
    	else
    		cxInstance = (ValueNodeImpl) (owner.newValue(getC()));
    	return cxInstance;
    } 

    
    /**
     * Get all the components at once to save time.
     * @throws AllegroGraphException 
     * @throws AllegroGraphException 
     */
    void getParts() {
    	try {
    		if ( (!UPIImpl.canReference(s))
        			|| (!UPIImpl.canReference(p))
        			|| (!UPIImpl.canReference(o)) )
        			{
        				if ( ValueObjectImpl.canReference(AGId) )
        				{
        					UPIImpl[] v = owner.getTripleParts(AGId);
        					if ( v==null ) throw new IllegalStateException
        					                   ("Cannot reference this triple id " + AGId);
        					s = v[0]; p = v[1]; o = v[2];
        					c = v[3];
        				}
        				else throw new IllegalStateException
    	                   ("Cannot reference this triple id " + AGId);
        			}
		} catch (AllegroGraphException e) {
			throw new IllegalStateException
            ("Cannot reference this triple id " + AGId);
		}
    	
    	
    	
    	UPI[] ids = new UPI[]{ s, p, o, c };
    	int[] types = new int[4];
    	String[] labels = new String[4];
    	String[] mods = new String[4];
    	try {
			owner.getPartsInternal(ids, types, labels, mods);
		} catch (AllegroGraphException e) {
			throw new IllegalStateException
			         ("Failed to get all the parts of a triple " + e );
		}
    	subject = labels[0];
    	predicate = labels[1];
    	object = labels[2];
    	context = labels[3];
    	sType = types[0];
    	pType = types[1];
    	oType = types[2];
    	cType = types[3];
    	subjMod = mods[0];
    	predMod = mods[1];
    	objMod = mods[2];
    	cxMod = mods[3];
    }
    

    String showPart ( UPI id, String label, int type, String mod, ValueNode val ) {
    	if ( val!=null ) return val.toString();
    	if ( label!= null ) {
    		switch (type) {
    		case AGC.AGU_ANON:
    			return "_:" + label;
			case AGC.AGU_NODE:
				return label;
			case AGC.AGU_LITERAL_LANG:
				return "\"" + label + "\"@" + mod;
			case AGC.AGU_TYPED_LITERAL:
				return "\"" + label + "\"^^" + mod;
			default:
				return "\"" + label + "\"";
			}
    	}
    	if ( id!=null ) return id.toString();
    	return "null";
    }
    
    /**
     * This method overrides the generic toString method.
     * This method generates a more readable output string.
     */
    public String toString () {
    	String cp;
    	if ( c==null ) cp = "";
    	else if ( (owner.isDefaultGraph(c)) | (owner.isDefaultGraph(cType)) ) cp = "";
    	else if ( AGConnector.AGU_NULL_CONTEXT==((UPIImpl)c).code ) cp = "";
    	else  cp = " " + showPart(c, context, cType, cxMod, cxInstance);
    	return "<Triple " + AGId + ": " +
    			showPart(s, subject, sType, subjMod, subjInstance) + " " +
    			showPart(p, predicate, pType, predMod, predInstance) + " " +
    			showPart(o, object, oType, objMod, objInstance) +
    			cp +
    			">";
    }
    
    public boolean equals ( Object other ) {
    	switch(sameAGId(other)) {
    	case 1: return true;
    	case 0: return false;
    	}
    	if ( other instanceof Triple )
    	{
    		Triple os = (Triple)other;
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
    	return 961 * getSubject().hashCode()
    	       + 31 * getPredicate().hashCode()
    	       + getObject().hashCode();
    }
    
    
    public int compareTo ( TripleImpl to )  {
    		if ( this.AGId < to.AGId ) return -1;
    		if ( this.AGId > to.AGId ) return +1;
    		return 0;
    }


}
