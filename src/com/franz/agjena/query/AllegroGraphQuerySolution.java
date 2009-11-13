
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
import java.util.Iterator;

import com.franz.agbase.AllegroGraph;
import com.franz.agbase.ValueObject;
import com.franz.agjena.JenaToAGManager;
import com.franz.agjena.exceptions.NiceException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;


/**
 * A single row from a SELECT query.
 */

public class AllegroGraphQuerySolution implements QuerySolution {
	
	private String[] variables;
	private ValueObject[] values;
	private AllegroGraph agStore;
	private JenaToAGManager j2ag;
	private ModelCom model;
	
	/** Constructor */
	protected AllegroGraphQuerySolution(String[] variables, ValueObject[] values, 
			AllegroGraph agStore, ModelCom model) {
		this.variables = variables;
		this.values = values;
		this.agStore = agStore;
		this.model = model;
		this.j2ag = JenaToAGManager.getInstance(this.agStore);
	}
	
    /** Return the value of the named variable in this binding.
     *  A return of null indicates that the variable is not present in this solution.
     *  @param varName
     *  @return RDFNode
     */
    public RDFNode get(String varName) {
    	if (varName == null) return null;
    	for (int i = 0; i < this.variables.length; i++) {
    		if (varName.equals(this.variables[i])) {
    			ValueObject v = this.values[i];
    			return j2ag.valueObjectToRDFNode(v, model);
    		}
    	}
    	return null;
    }
    
    /**
     * Return the value of the nth column in this row.
     * Counting starts at one, emulating a JDBC resultset.
     */
    public RDFNode get(int columnIndex) {
    	if ((columnIndex < 0) || (columnIndex >= this.values.length))
    		throw new NiceException("Column index out of bounds " + columnIndex);
    	ValueObject v = this.values[columnIndex];
		return j2ag.valueObjectToRDFNode(v, model);
    }

    /** Return the value of the named variable in this binding, casting to a Resource.
     *  A return of null indicates that the variable is not present in this solution.
     *  An exception indicates it was present but not a resource.
     *  @param varName
     *  @return Resource
     */
    public Resource getResource(String varName) {
    	RDFNode node = this.get(varName);
    	if (node == null) return null;
    	if (node instanceof Resource) return (Resource)node;
    	throw new NiceException("Value for variable " + varName + " is not a resource " + node);
    }

    /** Return the value of the named variable in this binding, casting to a Literal.
     *  A return of null indicates that the variable is not present in this solution.
     *  An exception indicates it was present but not a literal.
     *  @param varName
     *  @return Resource
     */
    public Literal getLiteral(String varName) {
    	RDFNode node = this.get(varName);
    	if (node == null) return null;
    	if (node instanceof Literal) return (Literal)node;
    	throw new NiceException("Value for variable " + varName + " is not a literal " + node);
    }

    
    /** Return true if the named variable is in this binding */
    public boolean contains(String varName) {
    	if (varName == null) return false;
    	for (String vbl : this.variables) {
    		if (varName.equals(vbl)) return true;
    	}
    	return false;
    }

    /** Iterate over the variable names (strings) in this QuerySolution.
     * @return Iterator of strings
     */ 
    public Iterator varNames() {
    	return Arrays.asList(this.variables).iterator();
    }
    
}
