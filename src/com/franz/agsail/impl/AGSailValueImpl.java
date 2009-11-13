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

import org.openrdf.model.Value;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.ValueNode;
import com.franz.agbase.impl.ValueNodeImpl;
import com.franz.agsail.AGSailValue;

/**
 * This is the superclass of all Value instances.
 */
public abstract class AGSailValueImpl extends AGSailValueObjectImpl implements Value, AGSailValue {

	protected AGSailValueImpl() {
		super();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -461411711745012850L;

	/* (non-Javadoc)
	 * @see com.franz.agbase.ValueNode#queryAGId()
	 */
	public UPI queryAGId() {
		return ((ValueNode)getDirectInstance()).queryAGId();
	}

	public UPI getAGId() throws AllegroGraphException {
		return ((ValueNodeImpl)getDirectInstance()).getAGId();
	}


	// String stringRef() throws AllegroGraphException {
	// throw new AllegroGraphException
	// ("This class does not have an ntriples string representation: "
	// + getClass());
	// }

	
	/* (non-Javadoc)
	 * @see com.franz.agbase.ValueNode#compareTo(com.franz.agbase.ValueNodeImpl)
	 */
	public int compareTo ( AGSailValueImpl to ) {
		return ((ValueNodeImpl)getDirectInstance()).compareTo(to.getDirectInstance());
	}


}
