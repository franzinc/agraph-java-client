/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.agraph.jena;

import org.openrdf.model.*;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;

/**
 * 
 * A utility class for creating Jena Nodes and Triples
 * from Sesame Values and Statements.
 *
 */
public class AGNodeFactory {
	
	public static Triple asTriple(Statement st) {
		Node s = asNode(st.getSubject());
		Node p = asNode(st.getPredicate());
		Node o = asNode(st.getObject());
		return new Triple(s,p,o);
	}

	public static Node asNode(Value v) {
		Node node = null;
		if (v==null) {
			node = Node.ANY; // TODO or Node.NULL or null?
		} else if (v instanceof IRI) {
			node = Node.createURI(v.stringValue());
		} else if (v instanceof BNode) {
			node = Node.createAnon(new AnonId(v.stringValue()));
		} else if (v instanceof Literal) {
			Literal lit = (Literal)v;
			IRI datatype = lit.getDatatype();
			String lang = lit.getLanguage().orElse(null);
			if (lang!=null) {
				node = Node.createLiteral(lit.getLabel(), lang, null);
			} else if (datatype!=null) {
				node = Node.createLiteral(lit.getLabel(), null, Node.getType(datatype.toString()));
			} else {
				node = Node.createLiteral(lit.stringValue());
			}
		} else {
			throw new IllegalArgumentException("Cannot create Node from Value: " + v);
		}
		return node;
	}

}
