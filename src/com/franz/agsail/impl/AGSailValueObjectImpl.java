
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

import com.franz.agbase.AllegroGraph;
import com.franz.agbase.ValueObject;
import com.franz.agsail.AGForSail;
import com.franz.agsail.AGSailValueObject;


/**
 * This super class implements some of the common  methods 
 * defined in the org.openrdf.model interfaces.
 */
public abstract class AGSailValueObjectImpl implements AGSailValueObject 
{
    protected AGForSail owner;
    protected AllegroGraph directOwner;

    protected AGSailValueObjectImpl() { super(); }
    
    protected static boolean canReference ( long id ) {
    	return id>-1;
    }
    
    private Object directInstance = null;
    
    public ValueObject getDirectInstance () { return (ValueObject) directInstance; } 
    
    public void setDirectInstance ( Object x ) {
    	directInstance = x;
    }
    
    
    /* (non-Javadoc)
	 * @see com.franz.agbase.ValueObject#compareTo(java.lang.Object)
	 */
    public int compareTo ( Object to )
    {
    	return (toString()).compareTo(to.toString());
    }


}

