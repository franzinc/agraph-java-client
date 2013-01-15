/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import org.openrdf.query.BindingSet;

/**
 * Represents a SPIN magic property.
 * @since v4.4
 */
public class AGSpinMagicProperty extends AGSpinFunction {

	/**
	 * 
	 * @param uri spin function identifier
	 * @param arguments name of arguments in the sparqlQuery, must include question mark
	 * @param query spin function query text
	 */
	public AGSpinMagicProperty(String uri, String[] arguments, String query) {
		super(uri, arguments, query);
	}

	public AGSpinMagicProperty(BindingSet bindings) {
		super(bindings);
	}
	
}
