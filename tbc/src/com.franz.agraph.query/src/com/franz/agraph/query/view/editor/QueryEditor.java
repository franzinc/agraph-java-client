/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.view.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.topbraid.sparql.Activator;
import org.topbraid.sparql.functionmetadata.FunctionsMetadata;
import org.topbraidcomposer.ui.text.StyledTextCompletion;
import org.topbraidcomposer.ui.text.StyledTextNavigator;

import com.hp.hpl.jena.rdf.model.Model;


public class QueryEditor {
	
	private StyledText text;
	
	
	public QueryEditor(Composite parent) {
		text = new StyledText(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL); 
		text.setText("SELECT *\nWHERE {\n    ?subject rdfs:subClassOf ?object .\n} ORDER BY ?subject");
		new StyledTextSPARQLUpdater(text);
		new StyledTextNavigator(text);
		List<Model> extraModels = new ArrayList<Model>();
		extraModels.add(Activator.getTopsModel());
		extraModels.add(FunctionsMetadata.get().getMatcherModel());
		final StyledTextCompletion completion = new StyledTextCompletion(text, null, false, true, true, extraModels);
		text.addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				if(event.keyCode == SWT.CR && (completion.isShowing() || event.stateMask != 0)) {
					event.doit = false;
					completion.assign();
				}
			}
		});
	}
	
	
	public Control getControl() {
		return text;
	}
	
	
	public String getQueryText() {
		return text.getText();
	}
	
	
	public StyledText getStyledText() {
		return text;
	}
}
