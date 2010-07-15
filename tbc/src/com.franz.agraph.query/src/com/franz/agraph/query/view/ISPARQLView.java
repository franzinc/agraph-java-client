/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.view;

import java.util.List;

import com.franz.agraph.jena.AGQuery;
import com.hp.hpl.jena.query.QuerySolution;


/**
 * Abstraction of the SPARQL View and the Results View so that
 * they can be treated uniformly by actions.
 * 
 */
public interface ISPARQLView {
	
	AGQuery getCurrentQuery();

	
	List<QuerySolution> getSelectedQuerySolutions();
}
