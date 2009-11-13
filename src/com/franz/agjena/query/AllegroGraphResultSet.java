
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

import java.util.Arrays;
import java.util.List;

import com.franz.agbase.ValueObject;
import com.franz.agbase.ValueSetIterator;
import com.franz.agjena.AllegroGraphGraph;
import com.franz.agjena.exceptions.NiceException;
import com.franz.agjena.exceptions.UnimplementedMethodException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

/** Results from a query in a table-like manner for SELECT queries.
 *  Each row corresponds to a set of bindings which fulfil the conditions
 *  of the query.  Access to the results is by variable name.
 *
 * @see AllegroGraphQuery
 * @see QueryExecution
 * @see QuerySolution
 * @see ResultSet

 */

public class AllegroGraphResultSet implements ResultSet {
	
	private AllegroGraphGraph graph;
	private String[] rowNames;
	private ValueSetIterator rows;
	private ModelCom model;
	private String queryString;
	int cursor = 0;
	
	public AllegroGraphResultSet(ValueSetIterator results, AllegroGraphGraph graph, String queryString) {
		this.graph = graph;
		this.rowNames = results.getNames();
		this.rows = results;
		this.queryString = queryString;
		// create a presentation model from the default graph:
		this.model = new ModelCom(this.graph);
	}
	
    /**
     * Is there another possibility?
     */
    public boolean hasNext() {
    	return this.rows.hasNext();
    }

    /** Moves onto the next result possibility.
     *  The returned object should be of class QuerySolution
     */
    
    public Object next() {
    	if ( !rows.hasNext() ) {
    		throw new NiceException("Called 'next' with no remaining results.");    		
    	}
    	AllegroGraphQuerySolution solution 
    		= new AllegroGraphQuerySolution(this.rowNames, this.rows.next(), this.graph.getAllegroGraphStore(), this.model);    	
    	cursor++;
    	return solution;
    }

    /** Moves onto the next result possibility.
     */
    
    public QuerySolution nextSolution() {
    	return (QuerySolution)this.next();
    }

    /** Move to the next binding (low level) */
    public Binding nextBinding() {
    	if ( !rows.hasNext() ) {
    		throw new NiceException("Called 'next' with no remaining results.");    		
    	}	
    	AllegroGraphBinding binding 
		= new AllegroGraphBinding(this.rowNames, this.rows.next(), this.graph.getAllegroGraphStore()); 
    	cursor++;
	return binding;
    }
    
    /** Return the "row number" - a count of the number of possibilities returned so far.
     *  Remains valid (as the total number of possibilities) after the iterator ends.
     */
    public int getRowNumber() {return this.cursor ;}
    
    /** Get the variable names for the projection
     */
    public List getResultVars() {return Arrays.asList(this.rowNames);}

    /** Is this ResultSet known to be ordered? 
     * Usually, this means a query involved ORDER BY or a ResultSet read
     * from a serialization had indexing information.
     * (The ordering does not necessarily have to be total)
     */
    public boolean isOrdered() {
    	return this.queryString.toLowerCase().contains("order by");
    }
    
    public void remove() {
    	throw new UnimplementedMethodException("remove");
    }

	public Model getResourceModel() {
		// TODO Auto-generated method stub
		return null;
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

