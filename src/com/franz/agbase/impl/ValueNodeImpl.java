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
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.ValueNode;

/**
 * This is the superclass of all Value instances.
 */
public class ValueNodeImpl extends ValueObjectImpl implements ValueNode {

	protected ValueNodeImpl() {
		super();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -461411711745012850L;

	public UPI nodeUPI;

	/* (non-Javadoc)
	 * @see com.franz.agbase.ValueNode#queryAGId()
	 */
	public UPI queryAGId() {
		return nodeUPI;
	}
	
	public void setAGId( UPI n ) { nodeUPI = n; }

	public UPI getAGId() throws AllegroGraphException {
		if (canReference())
			return nodeUPI;
		throw new AllegroGraphException("This node does not have a valid id.");
	}

	/**
	 * Qick AGId check for equality
	 * 
	 * @param x
	 * @return +1 definite true, 0 definite false, -1 cannot tell
	 */
	public int sameAGId(Object x) {
		if (x == null)
			return 0;
		if (this.getClass() == x.getClass()) {
			UPI xid = ((ValueNodeImpl) x).nodeUPI;
			if (UPIImpl.canReference(nodeUPI) && UPIImpl.canReference(xid)) {
				if (nodeUPI.equals(xid))
					return 1;
				return 0;
			}
		}
		return -1;
	}

	public boolean canReference() {
		return UPIImpl.canReference(nodeUPI);
	}

	// String stringRef() throws AllegroGraphException {
	// throw new AllegroGraphException
	// ("This class does not have an ntriples string representation: "
	// + getClass());
	// }

	/* (non-Javadoc)
	 * @see com.franz.agbase.ValueNode#getObjectStatements()
	 */
	public TriplesIterator getObjectStatements() throws AllegroGraphException {
		return owner.getStatements(null, null, this);
	}
	
	/* (non-Javadoc)
	 * @see com.franz.agbase.ValueNode#compareTo(com.franz.agbase.ValueNodeImpl)
	 */
	public int compareTo ( ValueNodeImpl to ) {
		return UPIImpl.compare(nodeUPI, to.nodeUPI);
	}


}
