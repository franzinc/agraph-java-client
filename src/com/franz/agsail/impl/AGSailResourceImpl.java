
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
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.franz.agbase.AllegroGraphException;
import com.franz.agsail.AGSailResource;

/**
 * This intermediate class implements the methods in the 
 * interface defined by org.openrdf.model.Resource.
 */
public abstract class AGSailResourceImpl extends AGSailValueImpl implements Resource, AGSailResource
{

	protected AGSailResourceImpl () { super(); }
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2007686642441824762L;

	public void addProperty(URI property, Value value)
		throws AllegroGraphException {
		owner.addStatement(this, property, value);
	}	


}

