/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
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

import com.franz.agraph.repository.AGRDFFormat;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.util.Closeable;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;

/**
 * Implements the Jena Model interface for AllegroGraph.
 * 
 */
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
		} else if (lang.contains("TURTLE")) {
			format = RDFFormat.TURTLE;
		} else if (lang.contains("QUADS")) {
			format = AGRDFFormat.NQUADS;
		} else {
			// TODO: add other supported formats and improve this error message
			throw new IllegalArgumentException("Unsupported format: " + lang + " (expected RDF/XML, N-TRIPLE, TURTLE, or NQUADS).");
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
	
	@Override
	public Resource createResource() {
		AGValueFactory vf = getGraph().getConnection().getValueFactory();
		BNode blank = vf.createBNode();
		return createResource(new AnonId(blank.stringValue()));
	}
	

	/* 
	 * Override methods involving StatementImpls, 
	 * instead using AGStatements. 
	 */
	
	@Override 
	public AGModel add( Statement [] statements ) {
		getBulkUpdateHandler().add( AGStatement.asTriples( statements ) );
		return this;
    }
    
	@Override 
	public AGModel remove( Statement [] statements ) {
		getBulkUpdateHandler().delete( AGStatement.asTriples( statements ) );        
		return this;
    }
 
	@Override 
	public AGStatement createStatement(Resource r, Property p, RDFNode o) { 
		return new AGStatement(r, p, o, this ); 
	}

	@Override 
	public Statement asStatement( Triple t ) {
		return AGStatement.toStatement( t, this );
	}
	
	/*
	 * Override Reification methods
	 */
	
    /**
    A mapper that maps modes to their corresponding ReifiedStatement objects. This
    cannot be static: getRS cannot be static, because the mapping is model-specific.
	protected final Map1<QuerySolution, ReifiedStatement> mapToRS = new Map1<QuerySolution, ReifiedStatement>()
    {
    public ReifiedStatement map1( QuerySolution result ) { 
    	return getRS( result.get("s").asNode() ); 
    }
    };
     */

    /**
    Answer a ReifiedStatement that is based on the given node. 
    @param n the node which represents the reification (and is bound to some triple t)
    @return a ReifiedStatement associating the resource of n with the statement of t.    
    private ReifiedStatement getRS( Node n ) { 
    	return ReifiedStatementImpl.createExistingReifiedStatement( this, n );
    }     
     */
	

    /**
    	@return an iterator which delivers all the ReifiedStatements in this model
	public RSIterator listReifiedStatements() {
		String queryString = "SELECT ?s  WHERE {?s a rdf:Statement .}";
		AGQuery sparql = AGQueryFactory.create(queryString);
		QueryExecution qe = AGQueryExecutionFactory.create(sparql, this);
		ResultSet results;
		try {
			results = qe.execSelect();
		} finally {
			qe.close();
		}
		return new RSIteratorImpl(new NiceIterator<QuerySolution>().andThen(results).mapWith(mapToRS));
	}
     */

	/**
    	@return an iterator each of whose elements is a ReifiedStatement in this
        model such that it's getStatement().equals( st )
	public RSIterator listReifiedStatements( final Statement st ) {
		Filter<ReifiedStatement> f = new Filter<ReifiedStatement>() {public boolean accept(ReifiedStatement rs) {return rs.getStatement().equals(st);};};
		return new RSIteratorImpl(listReifiedStatements().filterKeep(f));
	}
	 */
            
	
}
