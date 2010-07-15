/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.view.resultstable;

import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;


public class ResultsSetContentProvider implements IStructuredContentProvider {

	
	public void dispose() {
	}

	
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {
		if(inputElement instanceof Collection) {
			return ((Collection)inputElement).toArray();
		}
		else {
			throw new IllegalArgumentException("ResultSet expected");
		}
	}

	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
