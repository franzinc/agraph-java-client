/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import java.io.IOException;
import java.io.InputStream;

import org.openrdf.model.BNode;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

import com.franz.agraph.repository.AGValueFactory;
import com.franz.util.Closeable;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;

public class AGModel extends ModelCom implements Model, Closeable {

	public AGModel(AGGraph base) {
		super(base);
	}

	@Override
    public AGGraph getGraph() { 
		return (AGGraph)graph;
	}
    
	@Override 
	public AGModel read(InputStream reader, String base) {
		return read(reader,base,"RDF/XML");
	}

	@Override 
	public AGModel read(InputStream reader, String base, String lang) {
		RDFFormat format;
		if (lang.contains("TRIPLE")) {
			format = RDFFormat.NTRIPLES;
		} else if (lang.contains("RDF")) {
			format = RDFFormat.RDFXML;
		} else {
			// TODO: add other supported formats and improve this error message
			throw new IllegalArgumentException("Unsupported format: " + lang + " (expected RDF/XML or N-TRIPLE).");
		}
		try {
			getGraph().getConnection().add(reader, base, format, getGraph().getGraphContext());
		} catch (RDFParseException e) {
			throw new RuntimeException(e);
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}
	
	public Resource createResource() {
		AGValueFactory vf = getGraph().getConnection().getValueFactory();
		BNode blank = vf.createBNode();
		return createResource(new AnonId(blank.stringValue()));
	}
}
