
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

import java.util.List;

import org.apache.log4j.Logger;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.DefaultGraph;
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.TriplesQuery;
import com.franz.agbase.impl.AGFactory;
import com.franz.agbase.impl.ResourceNodeImpl;
import com.franz.agbase.impl.UPIImpl;
import com.franz.agbase.util.AGInternals;
import com.franz.agjena.exceptions.NiceException;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Reifier;
import com.hp.hpl.jena.graph.TransactionHandler;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.SimpleReifier;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.graph.query.SimpleQueryHandler;
import com.hp.hpl.jena.reasoner.BaseInfGraph;
import com.hp.hpl.jena.reasoner.FGraph;
import com.hp.hpl.jena.reasoner.Finder;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

//----------------------------------------------------
// RMM TODO LIST:
//
// Convert 'blankNodeToUPIMap'  IN 'JenaToAGManager' to LRU cache
//
// QUESTION: WHY DOES ADD PLURAL TRIPLES CONVERT TO STRINGS
// BUT NOT USE CACHE??
//
// POSSIBLE BUGS:
//   PASSING Null context TO performDelete LOOKS WRONG
//   PASSING Null context to triple find also looks wrong
//
//----------------------------------------------------

public class AllegroGraphGraph extends BaseInfGraph  {
	
	/** log4j logger used for this class */
	private static Logger logger = Logger.getLogger(AllegroGraphGraph.class);

	
	private AllegroGraph agStore;
	private ResourceNodeImpl graphResource;
	private JenaToAGManager j2ag = null;
	private boolean isTheDefaultGraph = false;
	private boolean isGraphOfAllGraphs = false;
	private boolean inferenceEnabled = false;
	
	//mm 2008-06-23: Moved to A-G-GraphMaker
	//public static String DEFAULT_GRAPH_NAME = "";  // "www.franz.com/agraph#DefaulGraph";
	
	private static String INF_SUFFIX = "_INF";
	
	private String generateLegalURI (String graphName) {
		if (Utils.isLegalURI(graphName)) {
			return graphName;
		}
		else {
			// CAN'T GUARANTEE UNIQUENESS ACROSS SESSIONS, AND IF
			// UNIQUE, DON'T KNOW HOW TO GARBAGE COLLECT THE CONTEXT:
			return Utils.uriFromBNodeId(graphName);
		}
	}

	/**
	 * Base constructor for creating AllegroGraph graphs.
	 */
	public AllegroGraphGraph(AllegroGraph agStore, String name, Reasoner reasoner) {
		super(null, reasoner, ReificationStyle.Minimal );
		// mild hack; we want to pass 'this' to 'super', but that's not legal.  Fortunately,
		// 'BaseInfGraph' uses protected access so that we can mimic the same logic that
		// it implements:
		// this assignment is overwritten by an InfGraph; by default a raw Graph points at itself
		this.fdata = new FGraph(this);
		this.agStore = agStore;
		if ((reasoner != null) && !(reasoner instanceof AllegroGraphReasoner)) {
			logger.warn("Hooking up to a non-AllegroGraph reasoner has not been tested, and may not perform.");
		}
		try {
			if (AllegroGraphGraphMaker.DEFAULT_GRAPH_NAME.equals(name) || Utils.isNullString(name)) {
				// the AG call to 'getDefaultGraph' returns null if a UPI is not
				// supplied.  We made this constructor public to get around this:
				this.graphResource = (ResourceNodeImpl) AGFactory.makeDefaultGraph(this.agStore, null);
			}
			else {
				this.graphResource = (ResourceNodeImpl)this.agStore.addURI(generateLegalURI(name));
			}
		} catch (AllegroGraphException ex) {
			throw new NiceException("Failed to create the graph named " + name);
		}
		this.j2ag = JenaToAGManager.getInstance(agStore);
	}

	/**
	 * Create an AllegroGraph graph with URI 'name'.
	 */
	public AllegroGraphGraph(AllegroGraph agStore, String name) {
		this(agStore, name, null);
	}
	
   /**
	 * Constructor
	 * @param data the raw data file to be augmented with entailments
	 * @param reasoner the engine, with associated tbox data, whose find interface
	 * can be used to extract all entailments from the data.
	 */
	public AllegroGraphGraph(Graph rawGraph, Reasoner reasoner) {
	   this(((AllegroGraphGraph)rawGraph).getAllegroGraphStore (),
			((AllegroGraphGraph)rawGraph).getName() + "_INF",
			 reasoner);
	   if (!(rawGraph instanceof AllegroGraphGraph)) {
		   throw new NiceException("A non-AllegroGraphGraph may not be passed to the 'AllegroGraphGraph' constructor.");
	   }
	   this.fdata = new FGraph(rawGraph);
	}
	
	public AllegroGraphGraph( Graph rawGraph, Reasoner reasoner, ReificationStyle style ) {
		this(rawGraph, reasoner);
	}
	
	/** Return the name for this graph. */
	public String getName () {
		if (this.graphResource instanceof com.franz.agbase.DefaultGraph) return null;
		if (this.isTheDefaultGraph) return null;
		String uri = ((com.franz.agbase.URINode)this.graphResource).getURI();
		if (uri.endsWith(INF_SUFFIX)) return null;
		else return uri;
	}
	
	/** Return the name for this graph. */
	protected String getCovertName () {
		// mm 2008-06-23: add special case for DefaultGraph
		if (this.graphResource instanceof DefaultGraph) return "";
		String uri = ((com.franz.agbase.URINode)this.graphResource).getURI();
		return uri;
	}
	
	public String toString () {return "|Graph|" + this.getCovertName();}

	
	/**
	 * Return TRUE if 'this' is the one and only default graph (there should not be more
	 * than one such in the system).
	 */
	public boolean isTheRealHonestToGoodnessDefaultGraph () {return this.isTheDefaultGraph;}
	
	protected void setIsTheDefaultGraph (boolean setting) {this.isTheDefaultGraph = setting;}
	
	/**
	 * Return 'true' if this graph represents the set of ALL of the triples in the store.
	 * A call to 'AllegroGraphGraphMaker.setDefaultIsGraphOfAllGraphs' sets the default graph to
	 * represent either the Graph-of-all-graphs, or just the triples asserted when no graph is specified.
	 * This exists because of a major oversight by the SPARQL committee, which omitted
	 * a practical means for querying across the set of all graphs.
	 */
	public boolean isGraphOfAllGraphs () {return this.isGraphOfAllGraphs;}
	
	/**
	 * See documentation for 'isGraphOfAllGraphs'.
	 */
	protected void setIsGraphOfAllGraphs (boolean setting) {
		this.isGraphOfAllGraphs = setting;
	}
	
	/**
	 * Return 'true' if this graph is an InfGraph with an AllegroGraph reasoner.
	 * This type of graph never has its own data; it exists to enable RDFS++ inference
	 * over the raw graph.
	 */
	public boolean isRDFSPlusPlusGraph () {
		return this.inferenceEnabled || (this.reasoner != null) && (this.reasoner instanceof AllegroGraphReasoner);
	}
	
	protected JenaToAGManager getJ2AG() {return this.j2ag;}
	
	public AllegroGraph getAllegroGraphStore () {return this.agStore;}

	/**
	 * Compute the value for the context argument in all quads for this graph.
	 *
	 */
	public String getContextArgumentString () {
		Object p = getContextArgumentTerm();
		if ( p instanceof String ) return (String) p;
		return agStore.refToString(p);

	}
	
	/**
	 * Compute the value for the context argument in all quads for this graph.
	 * @return a part ref at the user API level.
	 */
	public Object getContextArgumentObject () {
	    return this.graphResource;
	}

    	/**
	 * Convert to a value expected at the AGConnector interface.
	 * @return
	 */
	public Object getContextArgumentTerm () {
		try {
			return AGInternals.validRefOb(this.graphResource);
		} catch (IllegalArgumentException ex) {
			throw new NiceException(ex);
		}
	}


    /**
	    Add a triple to the triple store.
     */
	public void performAdd( Triple triple ) {
		try {
			this.agStore.verifyEnabled().addTriple(this.agStore,
					j2ag.jenaNodeToAGTerm(triple.getSubject()),
					j2ag.jenaNodeToAGTerm(triple.getPredicate()),
					j2ag.jenaNodeToAGTerm(triple.getObject()),
					this.getContextArgumentTerm()
			);
		} catch (AllegroGraphException e) {
			throw new NiceException("Illegal argument exception", e);
		}
	}
	
	

	/**
		Remove a triple from the triple store. 
	*/
	public void performDelete(Triple triple) { 
		try {
			this.agStore.verifyEnabled().delete(this.agStore,
					j2ag.jenaNodeToAGTerm(triple.getSubject()),
					j2ag.jenaNodeToAGTerm(triple.getPredicate()),
					j2ag.jenaNodeToAGTerm(triple.getObject()),
					this.getContextArgumentTerm(),
					false
			);
		} catch (AllegroGraphException e) {
			throw new IllegalArgumentException(e.toString());
		}
	}

	/**
	 * Remove all triples in this graph that match the pattern s,p,o.
	 */
	public void removeAll( Node s, Node p, Node r )  {
		try {
		this.agStore.verifyEnabled().delete(this.agStore,
				j2ag.jenaNodeToAGTerm(s, true),
				j2ag.jenaNodeToAGTerm(p, true),
				j2ag.jenaNodeToAGTerm(r, true),
				this.getContextArgumentTerm(),
				true);
		} catch (AllegroGraphException e) {
			throw new JenaException("Failure in removeAll", e);
		}
	}

	/**
	 * Note: AllegroConnection does not yet support a deleteTriples plural method
	 */

	/** 
	 * Return the number of triples in this graph.
	 */
	public int graphBaseSize() {
	    long count = 0;
		try {
			TriplesQuery q = new TriplesQuery();
			q.setContext(this.graphResource);
			count = q.count(this.agStore);			
		} catch (AllegroGraphException e) {
			throw new NiceException(e);
		}
		return (int) count;
	}
	
	public boolean isEmpty () {
		//  "Empty" means "has as few triples as it can manage" in Jena docs
		return 0==graphBaseSize();
	}
	
    /**
	    Return an iterator over all the triples held in this graph's non-reified triple store
	    that match <code>m</code>.
     */
	public ExtendedIterator graphBaseFind(TripleMatch m) {
		TriplesIterator cc;
		try {
		    cc = this.agStore.verifyEnabled().getInfTriples(this.agStore,
					j2ag.jenaNodeToAGTerm(m.getMatchSubject(), true),
					j2ag.jenaNodeToAGTerm(m.getMatchPredicate(), true),
					j2ag.jenaNodeToAGTerm(m.getMatchObject(), true),
					this.getContextArgumentTerm(),					
					0,
					this.isRDFSPlusPlusGraph());
		} catch (AllegroGraphException e) {
			throw new IllegalArgumentException(e.toString());
		}
		return new AGTripleIterator(this, cc);
	}
	
	/**
	 * Return 'true' if inference is enabled for this graph. 
	 */
	public boolean inferenceEnabled () {
		return this.isRDFSPlusPlusGraph();
	}
	
	/**
	 * Enable or disable inference for this graph.  If it has an attached reasoner,
	 * inference cannot be disabled.
	 */
	// TODO: NOT CLEAR IF WE WANT TO SUPPORT THIS METHOD, BUT THE REASONER
	// SETUP IS OVERLY CLUMSY FOR OUR NEEDS:
	public void setInferenceEnabled (boolean setting) {this.inferenceEnabled = setting;}

	/**
		Return a QueryHandler bound to this graph. The default implementation
	    returns the same SimpleQueryHandler each time it is called; sub-classes
	    may override if they need specialised query handlers.
	 */
	public QueryHandler queryHandler() { 
	    if (queryHandler == null) queryHandler = new SimpleQueryHandler(this);
	    return queryHandler;
    }
	
	
	//----------------------------------------------------------------------------------------
	// These calls short-circuit the recursion that happens when an Inf graph calls for 
	// and attribute of the raw graph, not knowing that they are the same for AG
	//----------------------------------------------------------------------------------------
	
   /**
     * Answer a reifier appropriate to this graph. Subclasses override if
     *they need non-SimpleReifiers.
	 */
	public Reifier constructReifier()
	   { return new SimpleReifier( this, ReificationStyle.Minimal ); }

    /**
      * Answer the PrefixMapping object for this graph, the same one each time.
      * Subclasses are unlikely to want to modify this.
      */
	public PrefixMapping getPrefixMapping()
	{ return this.pm; }



	//----------------------------------------------------------------------------------------
	// Methods inherited from BaseInfGraph
	//----------------------------------------------------------------------------------------
	
    /**
     * Return the Reasoner which is being used to answer queries to this graph.
     */
    public Reasoner getReasoner() {
    	if (this.reasoner == null) {
    		// TODO: PASS SOMETHING IN TO THE AG REASONER???, E.G. 'this':
    		this.reasoner = new AllegroGraphReasoner();
    	}
        return this.reasoner;
    }

		
	/**
	 * Extended find interface used in situations where the implementator
	 * may or may not be able to answer the complete query. It will
	 * attempt to answer the pattern but if its answers are not known
	 * to be complete then it will also pass the request on to the nested
	 * Finder to append more results.
	 * @param pattern a TriplePattern to be matched against the data
	 * @param continuation either a Finder or a normal Graph which
	 * will be asked for additional match results if the implementor
	 * may not have completely satisfied the query.
	 * 
	 * AllegroGraph doesn't use extended iterators, so this one returns the
	 * empty iterator.
	 */
	public ExtendedIterator findWithContinuation(TriplePattern pattern, Finder continuation) {
		if (continuation == null)
			return new EmptyExtendedIterator();
		else
			return continuation.find(pattern);
	}
	
	   /**
     * Return the schema graph, if any, bound into this inference graph.
     */
    public Graph getSchemaGraph() {
    	return null;
    }
    
    public void close () {
    	// it may be sufficient to do nothing  [bug18137]
    }

   
 public ResourceNodeImpl getGraphResource () {return this.graphResource;}
 
public TransactionHandler getTransactionHandler () {
	return new InfTransactionHandler(this);
}

 @Override
public BulkUpdateHandler getBulkUpdateHandler() {
	 if (bulkHandler == null) bulkHandler = new AGBulkUpdateHandler( this );
     return bulkHandler;
}

public static class InfTransactionHandler extends BaseInfGraph.InfTransactionHandler {

	public InfTransactionHandler(BaseInfGraph arg0) {
		super(arg0);
	}
	public boolean transactionsSupported() {
		 return false;
	 }
 }
 
 
   
}




/*
 *  (c) Copyright 2004, 2005, 2006, 2007, 2008 Hewlett-Packard Development Company, LP
 *  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
