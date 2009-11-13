
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.PrologSelectQuery;
import com.franz.agbase.ValueObject;
import com.franz.agbase.ValueSetIterator;
import com.franz.agbase.util.AGInternals;
import com.franz.agjena.exceptions.NiceException;
import com.franz.agjena.exceptions.UnimplementedMethodException;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.impl.BaseGraphMaker;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.AlreadyExistsException;
import com.hp.hpl.jena.shared.DoesNotExistException;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
  A factory for providing instances of named graphs backed by an AllegroGraph quad store.
  Names can be "arbitrary" character sequences.
<p>
  AllegroGraphGraphMaker represents a minimal reification style, since reification
  doesn't make sense anymore.
  <p>
  In order to benefit from the native capabilities of the AllegroGraph server 
  an application must use the constructors and factory methods in the AllegroGraph classes 
  that correspond to the general constructors and methods in Jena.
  <ul>
  <li>AllegroGraphGraphMaker instead of GraphMaker
  <li>AllegroGraphQueryExecutionFactory instead of QueryExecutionFactory
  <li>AllegroGraphQuery instead of Query
   </ul>
*/

public class AllegroGraphGraphMaker extends BaseGraphMaker {
	
	private AllegroGraph agStore;
	private AllegroGraphGraph defaultGraph = null;
	
	private static AllegroGraphGraphMaker ALLEGRO_GRAPH_GRAPH_MAKER = null; 
	
	// THIS IS A BUG, BECAUSE ITS NOT PERSISTANT.  PROPER FIX IT TO STORE
	// GRAPH INFORMATION AS AG TRIPLES. - RMM
	private Map<String, AllegroGraphGraph> namedGraphIndex = new HashMap<String, AllegroGraphGraph>();
	
	/** Constructor */
	public AllegroGraphGraphMaker (AllegroGraph agStore) {
		super(ReificationStyle.Minimal);
		this.agStore = agStore;
	}
	
	public AllegroGraph getStore() {return this.agStore;}
	
	/**
	 * Convenience method for testing.
	 * Create a factory instance of AllegroGraphGraphMaker that saves the value
	 * of 'agStore'.  Calling 'getInstance' returns this instance.  This avoids
	 * having to pass 'agStore' down the stack to whever a call to the 
	 * AllegroGraphGraphMaker constructor might be needed.
	 */
	public static void setDefaultMaker (AllegroGraph agStore) {
		ALLEGRO_GRAPH_GRAPH_MAKER = new AllegroGraphGraphMaker(agStore);
	}
	
	/**
	 * Return the default maker, already initialized with an AllegroGraph store. 
	 */
	public static AllegroGraphGraphMaker getInstance() {
		return ALLEGRO_GRAPH_GRAPH_MAKER;
	}
	
	/**
	 * Sets the default graph to represent either the Graph-of-all-graphs, or just 
	 * the triples asserted when no graph is specified.
	 * This exists because of a major oversight by the SPARQL committee, which omitted
	 * a practical means for querying across the set of all graphs.
	 */
	 public void setDefaultIsGraphOfAllGraphs (boolean setting) {
		AllegroGraphGraph defaultGraph = (AllegroGraphGraph)this.getDefaultGraph();
		defaultGraph.setIsGraphOfAllGraphs(setting);
	}
	    
//     /**
//     * Answer an "anonymous", freshly-created graph.
//	*/
//    // NEED TO SEE IF BLANK NODE IDS WORK HERE:
//	public Graph createGraph() {
//		Node bnode = Node.createAnon();
//		return createGraph( bnode.getBlankNodeId().getLabelString(), false ); 
//	}
	

    
    /**
        Create a new graph associated with the given name. If there is no such
        association, create one and return it. If one exists but <code>strict</code>
        is false, return the associated graph. Otherwise throw an AlreadyExistsException.
        
        @param name the name to give to the new graph
        @param strict true to cause existing bindings to throw an exception
        @exception AlreadyExistsException if that name is already bound.
    */
    public Graph createGraph( String name, boolean strict ) {     	
       	AllegroGraphGraph newGraph = this.namedGraphIndex.get(name);
       	if (newGraph != null) {
       		if (strict) {
       			throw new NiceException("Attempt to create graph named '" +
       					name + "'\n when one with the same name already exists.");
       		} else {
       			return newGraph;
       		}
       	}
    	newGraph = new AllegroGraphGraph(this.agStore, name);    		
    	this.namedGraphIndex.put(name, newGraph);
    	return newGraph;
    }
       
    /**
        Find an existing graph that this factory knows about under the given
        name. If such a graph exists, return it. Otherwise, if <code>strict</code>
        is false, create a new graph, associate it with the name, and return it.
        Otherwise throw a DoesNotExistException. 
        
        @param name the name of the graph to find and return
        @param strict false to create a new one if one doesn't already exist
        @exception DoesNotExistException if there's no such named graph
    */
    public Graph openGraph( String name, boolean strict ) {
    	Graph graph = this.namedGraphIndex.get(name);
    	if (graph != null) return graph;
    	if (strict) {
    		throw new DoesNotExistException("Can't open non-existent graph named '" + name + "'");
    	}
    	else {
    		return createGraph(name);
    	}
    }
        
    /**
        Remove the association between the name and the graph. create
        will now be able to create a graph with that name, and open will no
        longer be able to find it. Throws an exception if there's no such graph.
        The graph itself is not touched.
        
        @param name the name to disassociate
        @exception DoesNotExistException if the name is unbound
    */
    // SOUNDS LIKE IT DELETES ALL QUADS WITH THE GRAPH NAME IN CONTEXT POSITION??? - RMM
    public void removeGraph( String name ) {     	
    	AllegroGraphGraph graph = (AllegroGraphGraph)this.openGraph(name, true);
    	graph.close();
    	throw new UnimplementedMethodException("removeGraph. Graph deletion is not implemented.");     
    }
    
    /**
        return true iff the factory has a graph with the given name
        
        @param name the name of the graph to look for
        @return true iff there's a graph with that name
    */
    public boolean hasGraph( String name ) {
    	return this.namedGraphIndex.get(name) != null;
    }
               
    /**
    Answer the Class node for this GraphMaker's description.
    
    @return a URI node which is some RDFS subclass of MakerSpec
	*/
	@SuppressWarnings("deprecation")
	public Node getMakerClass() {
		String spec = "AllegroGraphMakerSpec";
		return ResourceFactory.createResource( com.hp.hpl.jena.vocabulary.JenaModelSpec.baseURI + spec ).asNode();
	}

    
    /**
    Update the graph g with any other descriptive information for this GraphMaker.
    @param d the description to be augmented
    @param self the node that represents this GraphMaker
     */
    protected void augmentDescription( Graph d, Node self ) {
    	throw new UnimplementedMethodException("augmentDescription");     
    }
    

    
    /**
        Close the factory - no more requests need be honoured, and any clean-up
        can be done.
    */
    public void close() {     	throw new UnimplementedMethodException("getGraphMaker");     }
    
    private static String GRAPH_URIS_QUERY =
    	"select distinct ?c where { graph ?c {?s ?p ?o} }";
    
    /**
     *  Retrieve the URIs of all graphs in the store, i.e., of all resources
     *  in context position.
     */
	public List<String> getGraphURIs () {
    	try {
    		List<String> uris = new ArrayList<String>();
    		PrologSelectQuery pq = new PrologSelectQuery();
    		pq.setQuery(new String[] {"g"}, "(q- ? ? ? ?g)");
    		pq.setDistinct(true);
    		ValueSetIterator resit = pq.run(this.agStore);
    		// TODO: MAYBE REWRITE THIS if a faster accessor is available
    		while ( resit.hasNext() ) {
    			ValueObject node = resit.next(0);
    			if (node instanceof com.franz.agbase.URINode) {
    				uris.add(((com.franz.agbase.URINode)node).getURI());
    			}
    		}
    		return uris;
    	} catch (AllegroGraphException ex) {
    		throw new NiceException("", ex);
    	}
    }
    
    /**
        Answer an [extended] iterator where each element is the name of a graph in
        the maker, and the complete sequence exhausts the set of names. No particular
        order is expected from the list.
     	@return an extended iterator over the names of graphs known to this Maker.
     */
    public ExtendedIterator listGraphs() {
    	final List<AllegroGraphGraph> graphs = new ArrayList<AllegroGraphGraph>();
    	final AllegroGraphGraphMaker maker = this;
    	
    	
    	for (AllegroGraphGraph g : this.namedGraphIndex.values()) {    		
    		graphs.add(g);
    	}
	    return new NiceIterator() {
	        private Iterator<AllegroGraphGraph> cursor = graphs.iterator();
	        private AllegroGraphGraph currentGraph = null;
	        
	        public boolean hasNext()  { 
	            return cursor.hasNext();
	        }
	            
	        public Object next() {
	        	if (!hasNext()) return null; // choke on it if you didn't obey protocol
	        	this.currentGraph = cursor.next();
	            return this.currentGraph.getName();
	        }
	        
	        public void close() {
	            //close( cursor ); // do nothing
	        }
	            
	        // NOT CLEAR IF THIS WILL WORK PROPERLY IN AG (THINGS JUST TOSSED TOGETHER HERE) - RMM
	        public void remove() { cursor.remove();
	        if (this.currentGraph != null)
	        	maker.removeGraph(this.currentGraph.getName());
	        }
	        
	        // USED FOR CHAINING ITERATORS, BUT WE HAVEN'T WORKED OUT THE DEFAULT IMPLEMENTATION - RMM
	        public ExtendedIterator andThen( ClosableIterator other ) {	        	
	        	if (true) throw new UnimplementedMethodException("listGraphs.andThen");  
	            return this;
	        }
	    };
    }
    
	//----------------------------------------------------------------------------------------
	// These calls short-circuit the calls to 'getGraph' and 'openGraph' in BaseGraphMaker
	// Those define a default graph with an anonymous name, e.g., 'anon_0'.  That's a really
    // dumb idea.  We want OUR  default graph to: 
    //    (1) Denote the one-and-only default graph, containing all triples
    //        asserted without specifying a graph, and
	//    (2) to optionally denote the Graph-of-all-graphs, in case users need to query for that.
	//----------------------------------------------------------------------------------------
	
    /**
     *  Name for the default graph.  Used only for debugging, not to overcome the idiocy
     *  of having a graph with no name.
     */
    protected static String DEFAULT_GRAPH_NAME = "DeFaUlTgRaPh";
    	
    /**
	    Answer the default graph for this maker. If we haven't already made it, make it
	    now.  Overrides the logic in 'BaseGraphMaker' which returns a default graph
	    with an anonymous name (what a dumb idea!).
	 */
	public Graph getGraph() { 
		return this.getDefaultGraph();
	    }
	    
	public Graph openGraph() {
		if (this.defaultGraph == null) throw new DoesNotExistException
	        ( "no default graph in this GraphMaker [" + this.getClass() + "]" ); 
	    return this.defaultGraph;
	}
	
	/**
	 * Return the default graph for this maker.  Same as 'getGraph', but 'getGraph' applied
	 * to a model has a completely different meaning; confusing for users.  This call
	 * avoids the confusion.
	 */
	public Graph getDefaultGraph() { 
	    if (this.defaultGraph == null) { 
	    	this.defaultGraph = (AllegroGraphGraph)this.createGraph(DEFAULT_GRAPH_NAME);
			this.defaultGraph.setIsTheDefaultGraph(true);
			// the name should not be in the index:
			this.namedGraphIndex.remove(DEFAULT_GRAPH_NAME);
	    }
	    return this.defaultGraph;
	}


}
