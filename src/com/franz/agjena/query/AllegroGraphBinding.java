
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.franz.agbase.AllegroGraph;
import com.franz.agbase.ValueObject;
import com.franz.agjena.JenaToAGManager;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingBase;

/**
 * Jena 'Binding' structure not yet implemented in AllegroGraph.
 */
public class AllegroGraphBinding extends BindingBase implements Binding {
	
	private String[] variablesArray;
	private ValueObject[] valueObjectsArray;
	private AllegroGraph agStore;
	private JenaToAGManager j2ag;
	private List<Var> variables = new ArrayList<Var>();
	private List<Node> nodes = new ArrayList<Node>();
	
//	protected AllegroGraphBinding(Binding _parent) {
//		super(_parent);
//		// TODO Auto-generated constructor stub
//	}

	/** Constructor */
	protected AllegroGraphBinding(String[] variables, ValueObject[] values, 
			AllegroGraph agStore) {
		super(null);
		this.variablesArray = variables;
		this.valueObjectsArray = values;
		this.agStore = agStore;
		this.j2ag = JenaToAGManager.getInstance(this.agStore);
		for (String v : this.variablesArray) {
			this.variables.add(Var.alloc(v));
			this.nodes.add(null);  // we use lazy evaluation of nodes
		}
	}

	@Override
	protected void checkAdd1(Var var, Node node) {
		// TODO Auto-generated method stub
		
	}
	
    /** Add a (var, value) pair- the value must not be null */
    public void add1(Var var, Node node) {
    	this.variables.add(var);
    	this.nodes.add(node);
    }

    /** Iterate over all variables of this binding. */
    public Iterator vars1() {
    	return this.variables.iterator();
    }

    /** Test whether a variable is bound to some object */
    public boolean contains1(Var var) {
    	return this.get1(var) != null;
    }

    /** Return the object bound to a variable, or null */
    public Node get1(Var var) {
    	for (int i = 0; i < this.variables.size(); i++) {
    		if (this.variables.get(i).equals(var)) {
    			Node node = this.nodes.get(i);
    			if (node == null) {
    				// need to evaluate it
    				node = this.j2ag.valueObjectToJenaNode(this.valueObjectsArray[i]);
    				this.nodes.set(i, node);
    			}
    			return node;
    		}
    	}
    	return null;
    }
    
    /** Number of (var, value) pairs. */
    public int size1() {
    	return this.variables.size();
    }

}
