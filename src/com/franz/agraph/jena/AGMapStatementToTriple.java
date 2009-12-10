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
