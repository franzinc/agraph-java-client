/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.tbc.wizards.create;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.topbraid.javax.util.URIs;

import com.franz.agraph.tbc.Activator;
import com.franz.agraph.tbc.wizards.AbstractAllegroWizardPage;


@SuppressWarnings("deprecation")
public class CreateAllegroWizardPage extends AbstractAllegroWizardPage {
	
	private static final String OVERWRITE = "Overwrite";

	private Text fileNameText;
	private Text baseURIText;
	

	
	public CreateAllegroWizardPage() {
		super("Create an Ontology stored in an AllegroGraph 4 Database");
	}
	
	
	protected String createErrorMessage() {
		if(fileNameText != null) {
			String fileName = fileNameText.getText().trim();
			if(fileName.length() == 0) {
				return "File name required";
			}
		}
		
		String baseURI = baseURIText.getText();
		if(!URIs.isBaseURI(baseURI)) {
			return "Invalid Base URI";
		}

		return super.createErrorMessage();
	}
	
	
	protected void fillComposite(Composite composite) {
		Preferences pp = Activator.getDefault().getPluginPreferences();
		fileNameText = createText("Ontology file name (without .ag4 suffix):");
		fileNameText.setToolTipText("Specify a local name for the AG4-backed ontology, this .ag4 file will reside in the selected directory of your workspace.");
		baseURIText = createText("Base URI:", "http://example.org/my.owl", pp, BASE_URI);
		baseURIText.setToolTipText("This URI identifies the ontology.");
		createEmptyRow();
		super.fillComposite(composite);
		createEmptyRow();
		createCheckBox("Overwrite existing database", pp, OVERWRITE);
	}

	
	public String getBaseURI() {
		return baseURIText.getText();
	}
	
	
	public String getFileName() {
		return fileNameText.getText().trim() + ".ag4";
	}
	
	public boolean isOverwrite() {
		return Activator.getDefault().getPluginPreferences().getBoolean(OVERWRITE);
	}
	
}
