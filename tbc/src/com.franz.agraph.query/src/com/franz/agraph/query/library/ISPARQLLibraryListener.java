/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.library;



/**
 * A listener that wants to be notified if SPARQL library entries
 * have been added or removed.
 * 
 * @author Holger Knublauch
 */
public interface ISPARQLLibraryListener {

	/**
	 * Called when a library entry has been added.
	 * @param entry  the new entry
	 */
	void sparqlLibraryEntryAdded(SPARQLLibraryEntry entry);

	
	/**
	 * Called when a library entry has been removed.
	 * @param entry  the old entry
	 */
	void sparqlLibraryEntryRemoved(SPARQLLibraryEntry entry);
}
