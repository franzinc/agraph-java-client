/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.view.libtable;

import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.franz.agraph.query.library.SPARQLLibrary;
import com.franz.agraph.query.library.SPARQLLibraryEntry;


public class LibraryTableContentProvider implements IStructuredContentProvider {

	
	public void dispose() {
	}

	
	public Object[] getElements(Object inputElement) {
		Set<SPARQLLibraryEntry> set = SPARQLLibrary.get().getEntries();
		return set.toArray();
	}

	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
