/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.tbc.wizards.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.IWizardPage;
import org.topbraid.core.TBPlugin;
import org.topbraid.core.util.CancellableTripleIterator;
import org.topbraidcomposer.core.CorePlugin;
import org.topbraidcomposer.wizards.export.AbstractExportWizardPlugin;

import com.franz.agraph.AllegroConstants;
import com.franz.agraph.graphstore.AllegroGraphStore;
import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.tbc.Activator;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class AllegroExportWizardPlugin extends AbstractExportWizardPlugin {

	private ExportToAllegroWizardPage page;

	public boolean confirmed() {

		if (page.isOverwrite()) {
			if (!CorePlugin
					.confirmed(page.getShell(),
							"Are you sure you want to overwrite this AllegroGraph database?")) {
				return false;
			}
		}
		return true;
	}

	public IWizardPage createExportWizardPage() {
		page = new ExportToAllegroWizardPage();
		return page;
	}

	public void performFinish(IFile file, String baseURI, Model otherModel,
			IProgressMonitor monitor) throws InvocationTargetException {

		// Check that we have at least SE running
		if (!TBPlugin.hasValidStatusAndNotify()) {
			return;
		}

		final File ioFile = file.getRawLocation().toFile();
		Properties properties = page.getProperties();
		properties.setProperty(AllegroConstants.BASE_URI, baseURI);
		try {

			monitor.setTaskName("Exporting to an AllegroGraph 4 database ...");
			String defaultNamespace = baseURI;
			if (!baseURI.endsWith("/")) {
				defaultNamespace += "#";
			}

			String serverURL = properties.getProperty(AllegroConstants.SERVER_URL);
			String catalogName = properties.getProperty(AllegroConstants.CATALOG);
			String repoName = properties.getProperty(AllegroConstants.REPOSITORY);

			AGServer server = new AGServer(serverURL, page.getUsername(), page.getPassword());
			AGCatalog catalog = server.getCatalog(catalogName);
			boolean overwrite = page.isOverwrite();
			if (overwrite) {
				catalog.deleteRepository(repoName);
			}
			AGRepository repo = catalog.createRepository(repoName);
			AGRepositoryConnection conn = repo.getConnection();
			AGGraphMaker maker = new AGGraphMaker(conn);
			AllegroGraphStore.setGraphMaker(maker);
			AGGraph graph = maker.createGraph(baseURI);
			try {
				BulkUpdateHandler buh = graph.getBulkUpdateHandler();
				ExtendedIterator<Triple> bit = otherModel.getGraph().find(null,
						null, null);
				CancellableTripleIterator it = new CancellableTripleIterator(
						bit, monitor);
				buh.add(it);

				ensureOWLOntologyExists(maker.getGraph(), baseURI);

				AllegroGraphStore.updateNamespaceProperties(otherModel
						.getGraph(), baseURI, properties);
			} finally {
				//graph.close();
			}

			Activator.getDefault().getPluginPreferences().setValue(
					AllegroConstants.REPOSITORY, "");

			monitor.setTaskName("Writing ontology metadata to "
					+ ioFile.getName());
			if (!page.isOverwrite()) {
				AllegroGraphStore.addExistingPrefixes(ioFile, properties);
			}
			OutputStream os = new FileOutputStream(ioFile);
			properties.store(os, AllegroConstants.COMMENT);
			os.close();
			file.getParent().refreshLocal(1, monitor);
		} catch (Exception ex) {
			throw new InvocationTargetException(ex);
		}
	}
}
