
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

package com.franz.agjena;

import java.util.NoSuchElementException;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.impl.TriplesIteratorImpl;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
 * Implement ExtendedIterator from AllegroGraphGraph Cursor.
 * 
 * @author mm
 *
 */
public class AGTripleIterator extends NiceIterator {

	private TriplesIteratorImpl agCursor;
	private AllegroGraphGraph graph;
	
	/** Constructor */
	public AGTripleIterator (AllegroGraphGraph graph, TriplesIterator agCursor) {
		super();
		this.agCursor = (TriplesIteratorImpl) agCursor;  
		this.graph = graph;
	}

	public void close() {
		agCursor.close();
	}

	public boolean hasNext() {
		return agCursor.hasNext();
	}
	
	/**
	 * Create a Jena Graph Node instance from AllegroGraph parts.
	 * 
	 * @param id The node id number
	 * @param type The node type
	 * @param val The node label
	 * @param mod The node language or datatype modifier
	 * @return a Jena Graph Node instance
	 */
	private Node assembleNodeFromParts( UPI id, int type, String val, String mod ) {
		Node r = graph.getJ2AG().assembleJenaGraphNode(id, type, val, mod);
		if ( ifDebug(2) )
		{
			try {
				agCursor.getStore().getConnection().getServer().evalInServer("(format t \"~&encodeNode: " + id + "  " + type + "  " + 
									val + "  " + mod + 
									" ==> " + r.getClass().getName() + "~%\")"
									);
			} catch (AllegroGraphException e) {	}
		}
		
		
		return r;
	}
		
	private boolean ifDebug( int index ) {
		return agCursor.getStore().getConnection().ifDebug(index);
	}
	
	public Triple nextInternal() {
		try {
			if ( agCursor.step() ) 
			{
				Triple tr = new Triple(
						assembleNodeFromParts(agCursor.getS(), agCursor.getPartType(1),
								agCursor.getSubjectLabel(), agCursor.getPartMod(1)),
						assembleNodeFromParts(agCursor.getP(), agCursor.getPartType(2),
								agCursor.getPredicateLabel(), agCursor.getPartMod(2)),
						assembleNodeFromParts(agCursor.getO(), agCursor.getPartType(3),
								agCursor.getObjectLabel(), agCursor.getPartMod(3))
						);

				return tr;
				}
			throw new NoSuchElementException();
		} catch (AllegroGraphException e) {
			throw new IllegalStateException(e.toString());
		}	
	}
	
	

	public void remove() {
		agCursor.remove();
	}
	
	public Triple next() {
		Triple triple = null;
		if ( ifDebug( 1 ) )
		{	
			try {
				agCursor.getStore().getConnection().serverTrace(true);
				triple = nextInternal();
			} finally { agCursor.getStore().getConnection().serverTrace(false); }
		}
		else
			triple = nextInternal();
		return triple;
	}
	
}
