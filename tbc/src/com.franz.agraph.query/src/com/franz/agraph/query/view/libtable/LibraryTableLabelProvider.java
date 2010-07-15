/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.view.libtable;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.topbraid.core.labels.DisplayLabels;

import com.franz.agraph.query.library.SPARQLLibraryEntry;
import com.hp.hpl.jena.rdf.model.Resource;


public class LibraryTableLabelProvider extends LabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	
	public String getColumnText(Object element, int columnIndex) {
		SPARQLLibraryEntry entry = ((SPARQLLibraryEntry)element);
		if(columnIndex == ILibraryTableColumns.COL_QUERY) {
			String str = entry.getQueryString();
			return str.replace("\n", " ");
		}
		else if(columnIndex == ILibraryTableColumns.COL_LABEL) {
			Resource subject = entry.getStatement().getSubject();
			return DisplayLabels.getLabel(subject);
		}
		else {
			throw new IllegalArgumentException("Invalid column index " + columnIndex);
		}
	}
}
