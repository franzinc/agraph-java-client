/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

/**
 * Represents a SPIN function.
 */
public class AGSpinFunction {

	private String uri;
	private String[] arguments;
	private String query;

	/**
	 * 
	 * @param uri spin function identifier
	 * @param arguments name of arguments in the sparqlQuery
	 * @param query spin function query text
	 */
	public AGSpinFunction(String uri, String[] arguments, String query) {
		this.uri = uri;
		this.arguments = arguments;
		this.query = query;
	}

	public String getUri() {
		return uri;
	}
	
	public String getQuery() {
		return query;
	}
	
	public String[] getArguments() {
		return arguments;
	}
	
	@Override
	public String toString() {
		return "{" + super.toString()
		+ " " + uri
		+ " " + arguments
		+ " " + query
		+ "}";
	}
}
