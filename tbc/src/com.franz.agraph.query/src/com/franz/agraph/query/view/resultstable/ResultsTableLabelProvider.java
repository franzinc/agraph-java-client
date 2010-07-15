/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.view.resultstable;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.topbraid.core.TB;
import org.topbraid.core.images.Images;
import org.topbraid.core.labels.DisplayLabels;
import org.topbraid.core.rdf.RDFModel;
import org.topbraidcomposer.core.TBC;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;


public class ResultsTableLabelProvider extends LabelProvider implements ITableLabelProvider {

	
	private ResultsTableViewer viewer;
	
	
	public ResultsTableLabelProvider(ResultsTableViewer viewer) {
		this.viewer = viewer;
	}
	

	public Image getColumnImage(Object element, int columnIndex) {
		RDFNode node = getColumnNode(element, columnIndex);
		if(node != null) {
			return TBC.getImage(Images.getImage(node));
		}
		else {
			return null;
		}
	}
	
	
	public RDFNode getColumnNode(Object element, int columnIndex) {
		QuerySolution solution = (QuerySolution) element;
		RDFNode node = getNode(solution, columnIndex);
		return node;
	}

	
	public String getColumnText(Object element, int columnIndex) {
		RDFNode node = getColumnNode(element, columnIndex);
		if(node != null) {
			return DisplayLabels.getLabel(node);
		}
		else {
			return null;
		}
	}


	private RDFNode getNode(QuerySolution solution, int columnIndex) {
		RDFNode node = solution.get(getVar(columnIndex));
		if(node != null) {
			RDFModel rdfModel = TB.getSession().getModel();
			if(rdfModel != null) {
				return node.inModel(rdfModel);
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}
	
	
	public String getVar(int index) {
		return viewer.getVar(index);
	}
}
