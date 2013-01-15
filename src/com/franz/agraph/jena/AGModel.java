/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openrdf.model.BNode;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.turtle.TurtleWriter;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.repository.AGRDFFormat;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.openrdf.rio.nquads.NQuadsWriter;
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
	
    /**
     * <p>Write a serialized representation of this model in a specified language.
     * </p>
     * <p>The language in which to write the model is specified by the
     * <code>lang</code> argument.  Predefined values are "RDF/XML",
     * "RDF/XML-ABBREV", "N-TRIPLE", "N-QUADS", "TURTLE", (and "TTL") and "N3".  The default value,
     * represented by <code>null</code>, is "RDF/XML".</p>
     * @param out The output stream to which the RDF is written
     * @param lang The output language
     * @return This model
     */
	@Override 
  	public Model write(OutputStream out, String lang) 
    {
		return write(out,lang, "");
    }
  	
    /**
     * <p>Write a serialized representation of a model in a specified language.
     * </p>
     * <p>The language in which to write the model is specified by the
     * <code>lang</code> argument.  Predefined values are "RDF/XML",
     * "RDF/XML-ABBREV", "N-TRIPLE", "N-QUADS", "TURTLE", (and "TTL") and "N3".  The default value,
     * represented by <code>null</code>, is "RDF/XML".</p>
     * @param out The output stream to which the RDF is written
     * @param base The base uri to use when writing relative URI's. <code>null</code>
     * means use only absolute URI's. This is used for relative
     * URIs that would be resolved against the document retrieval URL.
     * For some values of <code>lang</code>, this value may be included in the output. 
     * @param lang The language in which the RDF should be written
     * @return This model
     */
	@Override 
	public Model write(OutputStream out, String lang, String base) {
		RDFWriter writer;
		if (lang.contains("QUADS")) {
			writer = new NQuadsWriter(out);
		} else {
			return super.write(out, lang, base);
		}
		try {
			getGraph().getConnection().exportStatements(null, null, null, false, writer, getGraph().getGraphContexts());
		} catch (RDFHandlerException e) {
			throw new RuntimeException(e);
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
		return this;
	}
	
	/**
	 * Returns a new blank node with an AG-allocated id.
	 * 
	 * See also the javadoc for allowing external blank nodes for more discussion.
	 *    
	 * @see AGHttpRepoClient#setAllowExternalBlankNodeIds(boolean)
	 * @see AGRepositoryConnection#getHttpRepoClient()
	 */
	@Override
	public Resource createResource() {
		AGValueFactory vf = getGraph().getConnection().getValueFactory();
		BNode blank = vf.createBNode();
		return createResource(new AnonId(blank.stringValue()));
	}
	
	/**
	 * Returns a new blank node with the given (a.k.a "external") id.
	 * 
	 * Consider using createResource() instead to get an AG-allocated
	 * blank node id, as it is safer (avoids unintended blank node 
	 * conflicts) and can be stored more efficiently in AllegroGraph.
	 * 
	 * See also the javadoc for allowing external blank nodes for more 
	 * discussion.
	 *    
	 * @see AGHttpRepoClient#setAllowExternalBlankNodeIds(boolean)
	 * @see AGRepositoryConnection#getHttpRepoClient()
	 */
	@Override
	public Resource createResource(AnonId id) {
		return super.createResource(id);
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
