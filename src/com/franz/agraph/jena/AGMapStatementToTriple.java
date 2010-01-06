/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import org.openrdf.model.Statement;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.Map1;

public class AGMapStatementToTriple implements Map1<Statement, Triple> {

	@Override
	public Triple map1(Statement st) {
		st.getSubject();
		Triple tr = new Triple(null, null, null);
		return tr;
	}

}
