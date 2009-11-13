
//***** BEGIN LICENSE BLOCK *****
//Version: MPL 1.1
//
//The contents of this file are subject to the Mozilla Public License Version
//1.1 (the "License"); you may not use this file except in compliance with
//the License. You may obtain a copy of the License at
//http://www.mozilla.org/MPL/
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
//for the specific language governing rights and limitations under the
//License.
//
//The Original Code is the AllegroGraph Java Client interface.
//
//The Original Code was written by Franz Inc.
//Copyright (C) 2006 Franz Inc.  All Rights Reserved.
//
//***** END LICENSE BLOCK *****

package com.franz.agjena;


import java.io.File;
import java.io.InputStream;
import java.io.Reader;

import org.apache.log4j.Logger;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.impl.ResourceNodeImpl;
import com.franz.agjena.exceptions.NiceException;
import com.franz.agjena.exceptions.UnimplementedMethodException;
import com.hp.hpl.jena.enhanced.Personality;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.shared.JenaException;

// Currently, 'AllegroGraphModel' passes nearly all method calls to 'ModelCom'. Some efficiency
// could be gained if it implemented its own Resources, Literals, etc. because
// it could embed UPIs in them, instead of rederiving them from string URIs. - RMM
public class AllegroGraphModel extends ModelCom implements Model {
	
	/**
	 * The name of this class identifies the version of the AllegroGraph
	 * Java implementation.
	 * This name is also visible in the list of members in a jar file
	 * when it is inspected with Emacs or WinZip.
	 */
	public static class V3_2Jan27 {}
	
	/**
	 * Query the current AllegroGraphModel version.
	 * @return a version string.
	 */
	public static String version () { 
		Class thisClass = AllegroGraphModel.class;
		Class[] mems = thisClass.getDeclaredClasses();
		String home = thisClass.getName();
		String s = "";
		home = home + "$V";
		for (int i = 0; i < mems.length; i++) {
			String sub = mems[i].getName();
			if ( sub.startsWith(home) )
				s = sub;
		}
		return s; }
	
	private static Logger logger = Logger.getLogger(AllegroGraphModel.class);
	
	private AllegroGraph agStore = null;

	/**
	 * Create an AllegroGraph model to front an AllegroGraph graph.
	 */ 
	public AllegroGraphModel(Graph base) {
		super(base);
		AllegroGraphGraph graph = (AllegroGraphGraph)base;
		this.setAllegroGraphStore(graph.getAllegroGraphStore());
	}

	// Currently we don't support self-describing graphs
	public AllegroGraphModel(Graph base, Personality personality) {
		super(base, personality);
		throw new UnimplementedMethodException("AllegroGraphModel two-argument constructor");
	}
	
	public AllegroGraph getAllegroGraphStore () {return this.agStore;}
	
	protected void setAllegroGraphStore (AllegroGraph store) {
		this.agStore = store;
	}
	
	public String getName () {
		return ((AllegroGraphGraph)this.getGraph()).getName();
	}
	
	public String toString () {
		return "|AllegroGraphModel|" + this.getName();
	}
	
// for debugging
//	public Graph getGraph () {
//		Graph g = super.getGraph();
//		System.out.println("GET GRAPH: " + g);
//		return g;
//	}
	
	//------------------------------------------------------------------------
	// Reading/loading triples into AllegroGraph
	// 
	// Here we pass calls we can handle directly through to the AG server,
	// and pass all others through Jena, which is slower but gets the job done.
	//------------------------------------------------------------------------
	
	/**
	 * If 'url' references a file, return the name of that file.
	 * Otherwise, return null.
	 */
	private String urlToFile (String url, boolean warnIfMissing) {
		// The AG server can handle both http URLs and file URLs and file
		// paths.  In all cases, cannot check if missing -- only the server can tell.
		return url;
//		if (url.startsWith("file://")) {
//			url = url.substring("file://".length());
//		}
//		if (url.contains("://")) return null;
//		// it looks promising; see if file exists:
//		File file = new File(url);		
//		if (file.exists()) return url;
//		else {
//			logger.warn("Looking for non-existent file " + file.getAbsolutePath());
//			return null;
//		}
	}
	
	public static String NTRIPLES_FORMAT = "NTRIPLE";
	public static String RDFXML_FORMAT = "RDF/XML";
	
	private String langToNTriplesOrRDF (String lang) {
		if (Utils.isNullString(lang)) return NTRIPLES_FORMAT;
    	lang = lang.toUpperCase();
    	if ("NTRIPLE".equals(lang)) return NTRIPLES_FORMAT;
    	else  if (NTRIPLES_FORMAT.equals(lang)) return NTRIPLES_FORMAT;
    	else  if ("N-TRIPLE".equals(lang)) return NTRIPLES_FORMAT;
    	else  if ("N-TRIPLES".equals(lang)) return NTRIPLES_FORMAT;    	
    	else if ("RDF/XML".equals(lang)) return RDFXML_FORMAT;
    	else if ("RDF/XML-ABBREV".equals(lang)) return RDFXML_FORMAT;
    	else return null;
	}
	
    public Model read(String url)  {
    	return this.read(url, null, null);
     }
    
    public Model read(Reader reader, String base)  {
        return super.read(reader, base);
    }
    
  	public Model read(InputStream reader, String base)  {
  		return super.read(reader, base);
  	} 
    
    public Model read(String url, String lang)  {
    	return this.read(url, null, lang);
    }
    
    /**
     * Load triples from the source 'url' into the graph/context for this model.
     * If that graph is an INF graph, load into the underlying raw graph instead.
     */
    public Model read( String url, String base, String lang ) {
	    String filePath = urlToFile(url, true);
	    String format = langToNTriplesOrRDF(lang);
	    if ((filePath != null) && (format != null)) {
	    	AllegroGraphGraph graph = (AllegroGraphGraph)this.getGraph();
	    	// if this is an AG RDFS++ graph, don't read the data into it; read it into
	    	// the underlying raw graph:
	    	if (graph.isRDFSPlusPlusGraph()) {
	    		graph = (AllegroGraphGraph)graph.getRawGraph();
	    	}
	    	Object contextRef = graph.getContextArgumentObject();
	    	try {
	    		if (format.equals(NTRIPLES_FORMAT))
	    			// PASSING "NTRIPLE" AS 'ext' ARG FAILS HERE.  THE DOCUMENTATION DOESN'T
	    			// SAY WHAT THE ACCEPTABLE VALUE(S) ARE:
	    			this.agStore.loadNTriples(filePath, contextRef, null, null, null);
	    		else
	    			this.agStore.loadRDFXML(filePath, contextRef, base);
	    	} catch (AllegroGraphException e) {
	    		throw new NiceException("Failure during loading from the file at '" + url + "'");
	    	}
	    	return this;
	    } else {
	    	return super.read(url, base, lang);
	    }
    }
    
    public Model read(Reader reader, String base, String lang) {
        return super.read(reader, base, lang);
       }
    
  	public Model read(InputStream reader, String base, String lang)  {
  		return super.read(reader, base, lang);
  	}

	//--------------------------------------------------------------------------------------------
  	// PATCHES
  	//--------------------------------------------------------------------------------------------
 
    public boolean supportsTransactions() {
    	return false;
    }

    /**
	Remove all the statements from this model.
	*/
	public Model removeAll() {
		return this.removeAll(null, null, null);
	}
	
	/**
	 	Remove all the statements matching (s, p, o) from this model.
	*/
    public Model removeAll( Resource s, Property p, RDFNode o ) {
		Node subject = (s == null) ? null : s.asNode();
		Node predicate = (p == null) ? null : p.asNode();
		Node object = (o == null) ? null : o.asNode();
		this.getBaseGraph().removeAll(subject, predicate, object);
		return this;
	}

    //--------------------------------------------------------------------------------------------
  	// PATCHES, ROUND TWO
  	//--------------------------------------------------------------------------------------------
	
	private AllegroGraphGraph getBaseGraph () {
		AllegroGraphGraph graph = (AllegroGraphGraph)this.getGraph();
    	// if this is an AG RDFS++ graph, don't read the data into it; read it into
    	// the underlying raw graph:
    	if (graph.isRDFSPlusPlusGraph()) {
    		graph = (AllegroGraphGraph)graph.getRawGraph();
    	}
    	return graph;
	}
	
	private ResourceNodeImpl getBaseContext () {
		AllegroGraphGraph g = this.getBaseGraph();
		ResourceNodeImpl cxt = g.getGraphResource();
		return cxt;
	}

    	/**
	 * There appears to be nothing in AllegroGraph that corresponds
	 * a call to close a model, so this call is a no-op.  Without it,
	 * the Jena code goes into an infinite recursion, so this method
	 * really does need to be here.
	 */
	public void close() {
		return;
//		try {			
//			this.agStore.closeTripleStore();
//		} catch (AllegroGraphException ex) {
//			throw new JenaException(ex);
//		}
	}
	

    	/**
	 * Index the triples in the AllegroGraph store.  This should be done after every
	 * batch of updates.  By default, only triples added after the last call
	 * to 'indexTriples' are indexed.  If 'indexAll' is set to 'true', all triples
	 * in the store are re-indexed.
	 * If 'asynchronous' is set to 'true', a separate thread will be created
	 * to index the triples, and the call to 'indexTriples' returns immediately
	 * (before the indexing has completed).
	 */
	public void indexTriples(boolean indexAll, boolean asynchronous) {
		try {
		if (indexAll) {
			agStore.indexAllTriples(!asynchronous);
		} else {
			agStore.indexNewTriples(!asynchronous);
		}
		} catch (AllegroGraphException ex) {
			throw new JenaException(ex);
		}
	}

    	/**
	 * Slowly and laboriously count the number of triples in the base graph associated
	 * with this model.
	 */
	public long size () {
		AllegroGraphGraph g = this.getBaseGraph();
		return g.graphBaseSize();
	}


}
