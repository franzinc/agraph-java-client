/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import org.openrdf.query.QueryLanguage;

import com.franz.agraph.repository.AGQueryLanguage;

/**
 * 
 * A class for creating AGQuery instances.
 *
 */
public class AGQueryFactory {

	public static AGQuery create(String queryString) {
		return create(AGQueryLanguage.SPARQL, queryString);
    }

	public static AGQuery create(QueryLanguage language, String queryString) {
		AGQuery query = new AGQuery(language, queryString);
		return query ;
	}

}
