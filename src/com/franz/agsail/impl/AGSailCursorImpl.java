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

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.TriplesIterator;
import com.franz.agsail.AGForSail;
import com.franz.agsail.AGSailCursor;
import com.franz.agsail.AGSailTriple;

/**
 * This class implements a generator for multiple Triple instances.
 * <p>
 * Many triple store search operations may generate an indeterminate number of
 * results. These operations return a Cursor instance which may be used to
 * iterate through the available results.
 * <p>
 * There are no public constructors.  Instances are created by search
 * operations.
 */
public class AGSailCursorImpl implements AGSailCursor {

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getLookAhead()
	 */
	public int getLookAhead() {
		return directInstance.getLookAhead();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#setLookAhead(int)
	 */
	public void setLookAhead(int lh) {
		directInstance.setLookAhead(lh);
	}

	private AGForSail ag;
	public AGForSail getAG () { return ag; }
	
	private TriplesIterator directInstance = null;
	
	AGSailCursorImpl ( AGForSail from, TriplesIterator trit ) {
		ag = from;
		directInstance = trit;
	}

	
	Resource ssail = null;
	URI psail = null;
	Value osail = null;
	Resource csail = null;
	
	private void stepInstances () {
		ssail = null; psail = null; osail = null; csail = null; 
	}
	
	
	// NEW ACCESSOR HERE, INSTEAD OF CALLING PUBLIC FIELD DIRECTLY - RMM
	public AGForSail getStore () {return this.ag;}



	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#get_id()
	 */
	public long get_id() {
		return directInstance.get_id();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getS()
	 */
	public synchronized UPI getS() {
		return directInstance.getS();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#querySubject()
	 */
	public String querySubject() {
		return directInstance.querySubject();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#queryObject()
	 */
	public String queryObject() {
		return directInstance.queryObject();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#queryPredicate()
	 */
	public String queryPredicate() {
		return directInstance.queryPredicate();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#queryContext()
	 */
	public String queryContext() {
		return directInstance.queryContext();
	} // quad-store

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getSubjectLabel()
	 */
	public synchronized String getSubjectLabel() throws AllegroGraphException {
		return directInstance.getSubjectLabel();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getSubject()
	 */
	public Resource getSubject() throws AllegroGraphException {
		if ( ssail==null )
			ssail = (Resource) ag.coerceToSailValue(directInstance.getSubject());
		return ssail;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getContext()
	 */
	public Resource getContext() throws AllegroGraphException {
		if ( csail==null )
			csail = (Resource) ag.coerceToSailValue(directInstance.getContext());
		return csail;
	}


	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getP()
	 */
	public synchronized UPI getP() {
		return directInstance.getP();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getPredicateLabel()
	 */
	public synchronized String getPredicateLabel() throws AllegroGraphException {
		return directInstance.getPredicateLabel();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getPredicate()
	 */
	public URI getPredicate() throws AllegroGraphException {
		if ( psail==null )
			psail = (URI) ag.coerceToSailValue(directInstance.getPredicate());
		return psail;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getO()
	 */
	public synchronized UPI getO() {
		return directInstance.getO();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getC()
	 */
	public synchronized UPI getC() {
		return directInstance.getC();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getObjectLabel()
	 */
	public synchronized String getObjectLabel() throws AllegroGraphException {
		return directInstance.getObjectLabel();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getContextLabel()
	 */
	public synchronized String getContextLabel() throws AllegroGraphException {
		return directInstance.getContextLabel();
	}


	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getObject()
	 */
	public Value getObject() throws AllegroGraphException {
		if ( osail==null )
			osail = ag.coerceToSailValue(directInstance.getObject());
		return osail;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#queryTriple()
	 */
	public synchronized AGSailTriple queryTriple() {
		return AGSailFactory.makeTriple(ag, directInstance.queryTriple());
	}
	
	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getTriple()
	 */
	public synchronized AGSailTriple getTriple() throws AllegroGraphException {
		return AGSailFactory.makeTriple(ag, directInstance.getTriple());
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#next()
	 */
	public synchronized Statement next() {
		if ( hasNext() )
			try {
				step();
			} catch (AllegroGraphException e) {
				throw new IllegalStateException("Cursor.next " + e);
			}
		return queryTriple();
	}
	
	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getNext()
	 */
	public synchronized AGSailTriple getNext() {
		if ( hasNext() )
			try {
				step();
			} catch (AllegroGraphException e) {
				throw new IllegalStateException("Cursor.next " + e);
			}
		return queryTriple();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#atTriple()
	 */
	public boolean atTriple() {
		return directInstance.atTriple();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#hasNext()
	 */
	public boolean hasNext() {
		return directInstance.hasNext();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#limitReached()
	 */
	public boolean limitReached() {
		return directInstance.limitReached();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#close()
	 */
	public void close() {
		stepInstances();
		directInstance.close();
	}
	


	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#step()
	 */
	synchronized public boolean step() throws AllegroGraphException {
		stepInstances();
		return directInstance.step();
	}






	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#step(int)
	 */
	synchronized public AGSailTriple[] step(int n) throws AllegroGraphException {
		com.franz.agbase.Triple[] trs = directInstance.step(n);
		AGSailTriple[] strs = new AGSailTriple[trs.length];
		for (int i = 0; i < strs.length; i++) {
			strs[i] = ag.coerceToSailTriple(trs[i]);
		}
		stepInstances();
		return strs;
		
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#toString()
	 */
	public synchronized String toString() {
		return directInstance.toString();
	}



	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#remove()
	 */
	public synchronized void remove() {
		stepInstances();
		directInstance.remove();
	}

}
