/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.tbc.wizards.create;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.topbraid.core.TBPlugin;
import org.topbraid.eclipsex.log.Log;
import org.topbraidcomposer.core.TBC;
import org.topbraidcomposer.ui.AbstractContainerWizard;

import com.franz.agraph.AllegroConstants;
import com.franz.agraph.graphstore.AllegroGraphStore;
import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.tbc.Activator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class CreateAllegroWizard extends AbstractContainerWizard implements
		INewWizard {

	private CreateAllegroWizardPage wizardPage;

	protected void addPages(IContainer container) {
		wizardPage = new CreateAllegroWizardPage();
		addPage(wizardPage);
		setNeedsProgressMonitor(true);
	}

	public boolean performFinish() {

		// Check that we have at least SE running
		if (!TBPlugin.hasValidStatusAndNotify()) {
			return false;
		}

		final String fileName = wizardPage.getFileName();
		final String username = wizardPage.getUsername();
		final String password = wizardPage.getPassword();
		final boolean overwrite = wizardPage.isOverwrite();
		final Properties properties = wizardPage.getProperties();
		final IFile file = getTargetContainer().getFile(new Path(fileName));
		final File ioFile = file.getRawLocation().toFile();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {

					monitor.beginTask("Creating an AG4-backed ontology...", 3);
					monitor.setTaskName("Creating AllegroGraph database");

					String baseURI = properties.getProperty(AllegroConstants.BASE_URI);
					String defaultNamespace = baseURI;
					if (!baseURI.endsWith("/")) {
						defaultNamespace += "#";
					}
					String serverURL = properties.getProperty(AllegroConstants.SERVER_URL);
					String catalogName = properties.getProperty(AllegroConstants.CATALOG);
					String repoName = properties.getProperty(AllegroConstants.REPOSITORY);

					AGServer server = new AGServer(serverURL, username, password);
					AGCatalog catalog = server.getCatalog(catalogName);
					if (overwrite) {
						catalog.deleteRepository(repoName);
					}
					AGRepository repo = catalog.createRepository(repoName);
					AGRepositoryConnection conn = repo.getConnection();
					AGGraphMaker maker = new AGGraphMaker(conn);
					AllegroGraphStore.setGraphMaker(maker);
					AGGraph graph = maker.createGraph(baseURI); ;
					AGModel model = new AGModel(maker.getGraph());
					try {
						model.add(model.getResource(baseURI), RDF.type,	OWL.Ontology);
						AllegroGraphStore.updateNamespaceProperties(graph,
								baseURI, properties);
					} finally {
						//model.close();
					}

					monitor.worked(1);

					monitor.setTaskName("Writing database metadata to "
							+ ioFile.getName());
					OutputStream os = new FileOutputStream(ioFile);
					properties.store(os, AllegroConstants.COMMENT);

					os.close();
					getTargetContainer().refreshLocal(1, monitor);
					monitor.worked(1);

					Activator.getDefault().getPluginPreferences().setValue(
							AllegroConstants.REPOSITORY, "");

					TBC.getFileRegistry().update(file);
					monitor.setTaskName("Opening file for editing...");
					getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							IWorkbenchPage page = PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getActivePage();
							try {
								IDE.openEditor(page, file, true);
							} catch (PartInitException e) {
							}
						}
					});
					monitor.done();
				} catch (Exception ex) {
					throw new InvocationTargetException(ex);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			Log.logError(Activator.PLUGIN_ID, "Could not create database",
					realException);
			return false;
		}
		return true;
	}

}
