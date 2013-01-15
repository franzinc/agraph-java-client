/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import java.util.Arrays;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;

/**
 * Represents a SPIN function.
 * @since v4.4
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

	public AGSpinFunction(BindingSet bindings) {
		Value uri = bindings.getValue("uri");
		Value query = bindings.getValue("query");
		Value arguments = bindings.getValue("arguments");
		this.uri = uri.stringValue();
		this.arguments = split(arguments.stringValue());
		this.query = query.stringValue();
	}

	private static String[] split(String string) {
		String[] split = string.split(",");
		for (int i = 0; i < split.length; i++) {
			split[i] = split[i].trim();
		}
		if (split.length == 1 && split[0].length() == 0) {
			return new String[] {};
		}
		return split;
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
		+ " " + (arguments == null ? null : Arrays.asList(arguments))
		+ " " + query
		+ "}";
	}
}
