/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.tbc.wizards;

import java.util.Properties;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.topbraidcomposer.ui.wizards.AbstractWizardPage;

import com.franz.agraph.AllegroConstants;
import com.franz.agraph.tbc.Activator;


@SuppressWarnings("deprecation")
public class AbstractAllegroWizardPage extends AbstractWizardPage implements AllegroConstants {
	
	private static final String USERNAME = AbstractAllegroWizardPage.class.getName() + ".Username";
	private static final String PASSWORD = AbstractAllegroWizardPage.class.getName() + ".Password";

	private Text usernameText;
	private Text passwordText;
	
	
	public AbstractAllegroWizardPage(String title) {
		super(title);
		setDescription("Specify the Ontology and AllegroGraph 4 connection parameters.");
		setPageComplete(false);
	}
	
	
	protected String createErrorMessage() {

		String name = usernameText.getText();
		if(name.length() == 0) {
			return "Ontology name (.ag4) is required.";
		}

		return null;
	}

	
	protected void fillComposite(Composite composite) {
		Preferences pp = Activator.getDefault().getPluginPreferences();
		createText("Server URL:", "http://localhost:10035", pp, SERVER_URL);
		usernameText = createText("Username:","",pp,USERNAME);
		passwordText = createText("Password:","",pp,PASSWORD);
		passwordText.setEchoChar('\u2022');
		createEmptyRow();
		createText("Catalog name:", "", pp, CATALOG);
		usernameText = createText("Repository name:", "repo1", pp, REPOSITORY);
	}
	
	
	public Properties getProperties() {
		Preferences pp = Activator.getDefault().getPluginPreferences();
		Properties p = new Properties();
		p.setProperty(BASE_URI, pp.getString(BASE_URI));
		p.setProperty(SERVER_URL, pp.getString(SERVER_URL));
		p.setProperty(CATALOG, pp.getString(CATALOG));
		p.setProperty(REPOSITORY, pp.getString(REPOSITORY));
		return p;
	}
	
	
	public String getUsername() {
		return Activator.getDefault().getPluginPreferences().getString(USERNAME);
	}


	public String getPassword() {
		return Activator.getDefault().getPluginPreferences().getString(PASSWORD);
	}

}
