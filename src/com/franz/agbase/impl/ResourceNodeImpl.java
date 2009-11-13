
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
import com.franz.agbase.ResourceNode;
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.URINode;
import com.franz.agbase.ValueNode;

/**
 * Implement common methods of URINode, BlankNode and DefaultGraph.
 */
public abstract class ResourceNodeImpl extends ValueNodeImpl implements ResourceNode
{

	protected ResourceNodeImpl () { super(); }
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2007686642441824762L;

	public void addProperty(URINode property, ValueNode value)
		throws AllegroGraphException {
		owner.addStatement(this, property, value);
	}	

	public TriplesIterator getSubjectStatements()
		throws AllegroGraphException {
		return owner.getStatements(this, null, null);
	}

}

