/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.view.editor;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.topbraid.core.paths.IPropertyPath;
import org.topbraid.spin.editor.SPINCommandRowEditorDriver;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraidcomposer.ui.text.StyledTextRowEditorStyle;

import com.hp.hpl.jena.rdf.model.RDFNode;


public class StyledTextSPARQLUpdater implements ModifyListener {
	
	private StyledText text;
	
	
	public StyledTextSPARQLUpdater(StyledText text) {
		this.text = text;
		updateStyledText();
		text.addModifyListener(this);
	}
	
	
	public void dispose() {
		if(!text.isDisposed()) {
			text.removeModifyListener(this);
		}
	}
	

	public void modifyText(ModifyEvent e) {
		updateStyledText();
	}


	private void updateStyledText() {
		StyledTextRowEditorStyle.run(text, null, null, new SPINCommandRowEditorDriver() {

			@Override
			protected boolean isSpecialVariable(IPropertyPath path,
					RDFNode value, String fullName) {
				return ("?" + SPIN.THIS_VAR_NAME).equals(fullName);
			}
		});
	}
}
