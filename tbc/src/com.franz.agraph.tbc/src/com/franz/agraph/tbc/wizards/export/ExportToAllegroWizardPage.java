/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.tbc.wizards.export;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.widgets.Composite;

import com.franz.agraph.tbc.Activator;
import com.franz.agraph.tbc.wizards.AbstractAllegroWizardPage;


public class ExportToAllegroWizardPage extends AbstractAllegroWizardPage {
	
	private static final String OVERWRITE = ExportToAllegroWizardPage.class.getName() + ".Overwrite";

	
	public ExportToAllegroWizardPage() {
		super("Export to AllegroGraph Database");
	}
	
	
	protected void fillComposite(Composite composite) {
		super.fillComposite(composite);
		createEmptyRow();
		Preferences pp = Activator.getDefault().getPluginPreferences();
		createCheckBox("Overwrite existing database", pp, OVERWRITE);
	}
	
	
	public boolean isOverwrite() {
		return Activator.getDefault().getPluginPreferences().getBoolean(OVERWRITE);
	}
}
