
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

package com.franz.agjena.query;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.SPARQLQuery;
import com.franz.agbase.ValueObject;
import com.franz.agbase.ValueSetIterator;
import com.franz.agjena.AllegroGraphGraph;
import com.franz.agjena.AllegroGraphModel;
import com.franz.agjena.Utils;
import com.franz.agjena.exceptions.NiceException;
import com.franz.agjena.exceptions.UnimplementedMethodException;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Prologue;
import com.hp.hpl.jena.sparql.util.IndentedWriter;
import com.hp.hpl.jena.util.FileUtils;

/**
 * A 'Query' object holds a SPARQL query strings that is passed to the
 * AllegroGraph query engine.
 */
public class AllegroGraphQuery extends Query implements Cloneable {
	
	private String queryString = null;
	private AllegroGraphModel model = null;
	//private AllegroGraph agStore = null;
    private Syntax syntax = Syntax.syntaxSPARQL ; // The only possibility
    
    public static final int QueryTypeUnknown    = -123 ;
    public static final int QueryTypeSelect     = 111 ;
    public static final int QueryTypeConstruct  = 222 ;
    public static final int QueryTypeDescribe   = 333 ;
    public static final int QueryTypeAsk        = 444 ;
    int queryType = QueryTypeUnknown ; 

    public AllegroGraphQuery() {
        syntax = Syntax.syntaxSPARQL ;
    }
    
    public AllegroGraphQuery(Model model) {
        syntax = Syntax.syntaxSPARQL ;
    }

    // FOR NOW, DON'T IMPLEMENT PROLOGUE: - RMM
    public AllegroGraphQuery(Prologue prologue) {
        this();
        throw new UnimplementedMethodException("AllegroGraphQuery constructor with Prologue");
        //usePrologueFrom(prologue) ;
    }
    
    public String getQueryString () {return this.queryString;}
    
    protected void setQueryString (String queryString) {
    	this.queryString = queryString.trim();
    	if (queryString == null) return;
    	String qs = queryString.toLowerCase();
    	// set the query type:
    	// BUG: This logic could be fooled; fix requires removing prefixes, and
    	// using 'startsWith' on what remains:  - RMM
    	// Alt fix is to call AllegroGraph, parse, and ask it the type:
    	if (qs.contains("select")) this.queryType = QueryTypeSelect;
    	else if (qs.contains("construct")) this.queryType = QueryTypeConstruct;
    	else if (qs.contains("describe")) this.queryType = QueryTypeDescribe;
    	else if (qs.contains("ask")) this.queryType = QueryTypeAsk;
    	else this.queryType = QueryTypeUnknown;
    }
    
    public Model getModel () {return this.model;}
        
    public int getQueryType()                   { return queryType ; }
    
    public boolean isSelectType()      { return queryType == QueryTypeSelect ; }

    public boolean isConstructType()   { return queryType == QueryTypeConstruct ; }

    public boolean isDescribeType()    { return queryType == QueryTypeDescribe ; }

    public boolean isAskType()         { return queryType == QueryTypeAsk ; }

    public boolean isUnknownType()     { return queryType == QueryTypeUnknown ; }

//    protected void setAllegroGraphStore (AllegroGraph store) {
//    	this.agStore = store;
//    }
//    
    /**
     * Execute this query against the underlying RDF store.
     */
    public ResultSet executeSelectQuery () {
    	return this.executeSelectQuery(null);
    }
    
    private static List<String> extractGraphNames (String sparqlQuery, boolean includeAngles) {
    	List<String> names = new ArrayList<String>();
    	String workingString = sparqlQuery;
    	String lowerString = sparqlQuery.toLowerCase();
    	while (true) {
    		int pos = lowerString.indexOf("from named");
    		if (pos < 0) break;
    		lowerString = lowerString.substring(pos);
    		workingString = workingString.substring(pos);
    		int left = lowerString.indexOf("<");
    		int right = lowerString.indexOf(">");
    		if ((left < 0) || (right < 0) || (right <= left)) {
    			// bad syntax advance slightly and try again:
    			lowerString = lowerString.substring(3);
        		workingString = workingString.substring(3); 
    			continue;
    		}
    		if (includeAngles)
    			names.add(workingString.substring(left, right + 1));
    		else
    			names.add(workingString.substring(left + 1, right));
    		lowerString = lowerString.substring(right);
    		workingString = workingString.substring(right);    		
    	}    	
    	return names;    	
    }
    
    /**
     * Return a list containing the default graph and zero or more named graphs. 
     */
    private List<AllegroGraphGraph> graphsFromDataset (Dataset dataset) {
    	DatasetGraph dsg = dataset.asDatasetGraph();
    	AllegroGraphGraph defaultGraph = (AllegroGraphGraph)dsg.getDefaultGraph();
    	List<AllegroGraphGraph> graphs = new ArrayList<AllegroGraphGraph>();
    	graphs.add(defaultGraph);
    	for (Iterator it = dsg.listGraphNodes(); it.hasNext();) {
    		AllegroGraphGraph ng = (AllegroGraphGraph)dsg.getGraph((Node)it.next());
    		graphs.add(ng);
    	}
    	return graphs;
    }
    
    /**
     * (1) Place the default graph in a FROM clause, and place any named graphs in
     *     FROM NAMED clauses.
     * (2) If graph referenced in 'query' include one or more named InfGraphs, substitute 
     *     their corresponding raw graphs into the query string.
     * Note: The terminology is inexcusably bad.  The term 'defaultGraph' is not the
     * default graph at all; its merely the graph that goes in the from clause,
     * which might be a named graph or THE default graph or an anonymous graph (hope not that).
     */
//  mm 2008-06-23: This is never called  -- in any case, substituting into the
//     query string does not work for many reasons
//    private String transformQuery (String query, List<AllegroGraphGraph> graphs) {
//    	System.out.println("++++++SUBSTUTUTE GRAPHS IN QUERY+++++++++");
//    	// first we substitute in declarations for the graphs in 'graphs'.
//    	// BUG1: WE HAVEN'T FIGURED OUT HOW TO SUBSTITUTE IN THE DEFAULT GRAPH
//    	String lowerCaseQuery = query.toLowerCase();
//    	boolean isDefaultGraph = true;
//    	int injectPos = lowerCaseQuery.indexOf("where");
//    	for (AllegroGraphGraph g : graphs) {
//    		if (g.isTheRealHonestToGoodnessDefaultGraph()) continue;
//    		String fromClause = isDefaultGraph ? " from " : " from named ";
//    		fromClause += g.getContextArgumentString(true) + " ";
//    		if (lowerCaseQuery.contains(fromClause)) continue;  // weak attempt to avoid duplicates
//    		query = query.substring(0, injectPos) + fromClause + query.substring(injectPos);
//    		isDefaultGraph = false;
//    	}
//    	// next we substitute names of raw graphs in place of INF graphs:
//    	for (AllegroGraphGraph g : graphs) {
//    		if (g.isRDFSPlusPlusGraph()) {
//    			AllegroGraphGraph raw = (AllegroGraphGraph)g.getRawGraph();
//    			query = query.replace(g.getContextArgumentString(false), raw.getContextArgumentString(false));
//    		}
//    	}
//    	return query;
//    }
    
    private static String BEHAVIOR =  "default-dataset-behavior";
    private static String ALL_BEHAVIOR = ":all";
    private static String DEFAULT_BEHAVIOR = ":default";
    
    // SET TO TRUE, AND THEN ELIMINATE OTHER LOGIC, WHEN 'setFrom' PATCH IS PERMANENT:
    private static boolean USE_QUERY_OBJECT = true;
    
    /**
     * Execute a select query against the graphs in 'dataset'.
     */
    public ResultSet executeSelectQuery (Dataset dataset) {
    	if ((dataset != null) && !Utils.toList(dataset.listNames()).isEmpty()) {
//    		throw new NiceException("Queries against specific graphs not yet implemented.\n" +
//    				"   Please specify your graphs explicitly within your SPARQL query.");
    		System.out.println("Query execution against the externally-specified graphs \n   '" +
    				Utils.toList(dataset.listNames()) + "' is not yet implemented!.");
    	}
    	List<AllegroGraphGraph> graphs = graphsFromDataset(dataset); 
    	boolean enableInference = false;
    	boolean useGraphOfAllGraphs = false;
    	for (AllegroGraphGraph g : graphs) {
    		if (g.isRDFSPlusPlusGraph()) {
    			enableInference = true;
    		}
    		if (g.isGraphOfAllGraphs()) {
    			useGraphOfAllGraphs = true;
    		}
    	}
    	// SEEMS LIKE THERE WAS AN OLDER CONSTRUCTOR THAT TOOK AN ARGUMENT; NOT SURE: - RMM
    	//SPARQLQuery queryObject = new SPARQLQuery("SELECT");
    	SPARQLQuery queryObject = new SPARQLQuery();
    	queryObject.setQuery(this.queryString);
    	queryObject.setIncludeInferred(enableInference);
    	if (useGraphOfAllGraphs)
    		queryObject.setDefaultDatasetBehaviorAll();
    	else
    		// mm 2008-06-23: always set this option - do not rely on server default (:all)
    		queryObject.setDefaultDatasetBehaviorDefault();
    	AllegroGraphGraph defaultNamedGraph = null;
    	List<AllegroGraphGraph> namedGraphs = new ArrayList<AllegroGraphGraph>();
    	boolean isDefaultGraph = true;
    	for (AllegroGraphGraph graph : graphs) {
    		String name = graph.getName();
    		//name = "<" + name + ">";
    		if (name == null) continue;
    		if (isDefaultGraph)
    			defaultNamedGraph = graph;
    		else {
    			namedGraphs.add(graph);
    			isDefaultGraph = false;
    		}
    	}
    	if (defaultNamedGraph != null) {
    		String[] dga = {defaultNamedGraph.getName()};
    		queryObject.setFrom(dga);
    	}
    	if (!namedGraphs.isEmpty()) {
    		String[] nga = (String[])namedGraphs.toArray();
    		queryObject.setFromNamed(nga);
    	}    			
    	try {
    		AllegroGraphGraph defaultGraph = graphs.get(0);
// MY ORIGINAL CODE DID NOT USE A QUERY OBJECT, BUT WAS CALLING A NON-PUBLIC METHOD.
// WE WILL FIND OUT IF QUERY OBJECT WORKS FOR US NOW; IF SO, DELETE THE COMMENTETD-OUT CODE  - RMM
//    		if (!USE_QUERY_OBJECT) {
//    			String modifiedQuery = transformQuery(this.queryString, graphs);
//    			//EXPERIMENT
//    			//modifiedQuery = this.queryString;
//    			// END EXPERIMENT
//	    		System.out.println("EXECUTING MODIFIED QUERY: " + modifiedQuery + "  INFERENCE IS " + (enableInference ? "on" : "off"));    		
//    			Object[] more = {BEHAVIOR, (useGraphOfAllGraphs ? ALL_BEHAVIOR : DEFAULT_BEHAVIOR)};
//    			ValueObject[][] results = null;
//    			if (useGraphOfAllGraphs)
//    				results = defaultGraph.getAllegroGraphStore().twinqlSelect(enableInference, modifiedQuery , null, -1, 0, more);
//    			else
//    				results = defaultGraph.getAllegroGraphStore().twinqlSelect(enableInference, modifiedQuery , null, -1, 0, new Object[0]);
//		    	return new AllegroGraphResultSet(results, defaultGraph, modifiedQuery);
//    		} else {
    		ValueSetIterator results = queryObject.select(defaultGraph.getAllegroGraphStore());
		    	return new AllegroGraphResultSet(results, defaultGraph, this.queryString);
 //   		}
    	} catch (AllegroGraphException ex) {
    		throw new NiceException("Failed to execute query \n" + this.queryString, ex);
    	}
    }
 
    /** Convert the query to a string */
    
    public String serialize()  {
        return this.queryString;
    }
    
    /** Convert the query to a string in the given syntax
     * @param syntax
     */
    
    public String serialize(Syntax syntax) {
    	if (syntax != Syntax.syntaxSPARQL)
    		throw new NiceException("Only SPARQL query syntax is supported.");
        return this.queryString;
    }

    /** Output the query
     * @param out  OutputStream
     */
    public void serialize(OutputStream out) { 
    	System.out.println("SERIALIZE: " + this.queryString);//////
    	FileUtils.asPrintWriterUTF8(out).print(this.queryString);    	
    }
    
    public void serialize(IndentedWriter writer)
    {
        throw new NiceException("Indented printing of queries is not supported.");
    }
    
//    /** Output the query
//     * 
//     * @param out     OutputStream
//     * @param syntax  Syntax URI
//     */
//    
//    public void serialize(OutputStream out, Syntax syntax) { Serializer.serialize(this, out, syntax) ; }
//
//    /** Format the query into the buffer
//     * 
//     * @param buff    IndentedLineBuffer
//     */
//    
//    public void serialize(IndentedLineBuffer buff) { Serializer.serialize(this, buff) ; }
//    
//    /** Format the query
//     * 
//     * @param buff       IndentedLineBuffer in which to place the unparsed query
//     * @param outSyntax  Syntax URI
//     */
//    
//    public void serialize(IndentedLineBuffer buff, Syntax outSyntax) { Serializer.serialize(this, buff, outSyntax) ; }
//
//    /** Format the query
//     * 
//     * @param writer  IndentedWriter
//     */
//    
//    public void serialize(IndentedWriter writer) { Serializer.serialize(this, writer) ; }
//
//    /** Format the query
//     * 
//     * @param writer     IndentedWriter
//     * @param outSyntax  Syntax URI
//     */
//    
//    public void serialize(IndentedWriter writer, Syntax outSyntax)
//    {
//        Serializer.serialize(this, writer, outSyntax) ;
//    }
    

//-----------------------------------------------------------------------------------------
// Debugging code
//-----------------------------------------------------------------------------------------

	private static void practice (String string) {
		System.out.println("Input '" + string + "'   Output " + extractGraphNames(string, true));
	}
	
	public static void main (String[] args) {
		practice("select ?s where true");
		practice("select ?s from named <http://foo>");
		practice("select ?s from named <http://foo>from named<Http://bar>");
		practice("select ?s from named http://foo> from named<Http://bar>");
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
