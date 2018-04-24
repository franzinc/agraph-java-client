/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * A utility class for creating Jena Nodes and Triples
 * from Sesame Values and Statements.
 */
public class AGNodeFactory {

    public static Triple asTriple(Statement st) {
        Node s = asNode(st.getSubject());
        Node p = asNode(st.getPredicate());
        Node o = asNode(st.getObject());
        return new Triple(s, p, o);
    }

    public static Quad asQuad(Statement st) {
        Node s = asNode(st.getSubject());
        Node p = asNode(st.getPredicate());
        Node o = asNode(st.getObject());
        Node g = asNode(st.getContext());
        return new Quad(s, p, o, g);
    }

    public static Node asNode(Value v) {
        Node node;
        if (v == null) {
            node = Node.ANY; // TODO or Node.NULL or null?
        } else if (v instanceof IRI) {
            node = NodeFactory.createURI(v.stringValue());
        } else if (v instanceof BNode) {
            node = NodeFactory.createBlankNode(((BNode) v).getID());
        } else if (v instanceof Literal) {
            Literal lit = (Literal) v;
            IRI datatype = lit.getDatatype();
            String lang = lit.getLanguage().orElse(null);
            if (lang != null) {
                node = NodeFactory.createLiteral(lit.getLabel(), lang, null);
            } else if (datatype != null) {
                node = NodeFactory.createLiteral(lit.getLabel(), null, NodeFactory.getType(datatype.toString()));
            } else {
                node = NodeFactory.createLiteral(lit.stringValue());
            }
        } else {
            throw new IllegalArgumentException("Cannot create Node from Value: " + v);
        }
        return node;
    }

}
