/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;


/**
 * 
 * A class for creating QueryExecution instances.
 *
 */
public class AGQueryExecutionFactory {

	public static AGQueryExecution create(AGQuery query, AGModel model) {
		return new AGQueryExecution(query,model);
	}

}
