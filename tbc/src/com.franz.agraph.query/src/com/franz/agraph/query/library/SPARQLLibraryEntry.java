/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.library;

import org.topbraid.sparql.SPARQLFactory;
import org.topbraid.spin.system.ARQFactory;
import org.topbraid.spin.util.SPINUtil;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;


/**
 * A SPARQL query library entry - either a Query or an UpdateRequest.
 * 
 * @author Holger Knublauch
 */
public class SPARQLLibraryEntry {

	private Statement statement;
	
	private Query query;
	
	private UpdateRequest request;
	
	private String text;
	
	
	public SPARQLLibraryEntry(Statement statement) {
		this.statement = statement;
		String str = SPINUtil.getQueryString(statement.getObject(), true);
		OntModel ontModel = (OntModel) statement.getModel();
		try {
			query = ARQFactory.get().createQuery(ontModel, str);
		}
		catch(Throwable t) {
		}
		try {
			String queryString = SPARQLFactory.createPrefixDeclarations() + str;
			request = UpdateFactory.create(queryString);
		}
		catch(Throwable t) {
		}
		this.text = str.replaceAll("\t", "    ");
	}
	
	
	public Query getQuery() {
		return query;
	}
	
	
	public String getQueryString() {
		try {
			return SPINUtil.getQueryString(statement.getObject(), true);
		}
		catch(Throwable t) {
			return t.getMessage();
		}
	}
	
	
	public UpdateRequest getRequest() {
		return request;
	}
	
	
	public String getText() {
		return text;
	}
	
	
	public Statement getStatement() {
		return statement;
	}
}
