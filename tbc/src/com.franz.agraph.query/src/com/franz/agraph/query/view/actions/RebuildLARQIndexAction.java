/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.view.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.topbraid.core.TB;
import org.topbraid.sparql.larq.LARQRegistry;
import org.topbraidcomposer.core.util.ThreadUtil;
import org.topbraidcomposer.ui.views.AbstractViewAction;

import com.hp.hpl.jena.query.larq.IndexBuilderString;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.rdf.model.Model;


public class RebuildLARQIndexAction extends AbstractViewAction{

	@Override
	protected void run() {
		final URI baseURI = TB.getSession().getSelectedBaseURI();
		if(baseURI != null) {
			ThreadUtil.runCancelable("Failed to " + getAction().getText(), new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.setTaskName("Closing existing LARQ index...");
					IFolder folder = LARQRegistry.getIndexFolder(baseURI);
					try {
						LARQRegistry.closeIndex(baseURI);
						monitor.setTaskName("Resetting index folder...");
						folder.getParent().refreshLocal(2, monitor);
						if(!folder.exists()) {
							folder.create(true, true, monitor);
						}
						Model model = TB.getSession().getModel();
						for(IResource file : folder.members()) {
							if(file instanceof IFile) {
								((IFile)file).delete(true, monitor);
							}
						}
						
						monitor.setTaskName("Rebuilding index...");
						File fileFolder = folder.getRawLocation().toFile();
						IndexBuilderString larqBuilder = new IndexBuilderString(fileFolder);
						larqBuilder.indexStatements(model.listStatements());
						larqBuilder.closeWriter();
						IndexLARQ index = larqBuilder.getIndex();
						// index.close();
						LARQRegistry.register(baseURI, index);
						
						monitor.setTaskName("Refreshing folder...");
						folder.getParent().refreshLocal(2, monitor);
					}
					catch(Throwable t) {
						throw new InvocationTargetException(t);
					}
				}
			});
		}
	}
}
