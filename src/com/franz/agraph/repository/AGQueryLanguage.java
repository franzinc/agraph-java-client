/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import org.openrdf.query.QueryLanguage;

/**
 * Extends the Sesame QueryLanguage class for AllegroGraph languages.
 *  
 */
public class AGQueryLanguage extends QueryLanguage {

	/**
	 * The Prolog Select query language for AllegroGraph. 
	 */
	public static final AGQueryLanguage PROLOG = new AGQueryLanguage("prolog");

	public AGQueryLanguage(String name) {
		super(name);
	}
	
}
