/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.Iterator;

/**
 * Implements the Jena QuerySolution interface for AllegroGraph.
 */
public class AGQuerySolution implements QuerySolution {

    private final BindingSet bs;
    private final AGModel model;

    public AGQuerySolution(BindingSet bs, AGModel model) {
        this.bs = bs;
        this.model = model;
    }

    @Override
    public boolean contains(String varName) {
        return bs.hasBinding(varName);
    }

    @Override
    public RDFNode get(String varName) {
        Value val = bs.getValue(varName);
        if (val == null) {
            return null;
        }
        Node node = AGNodeFactory.asNode(val);
        return model.asRDFNode(node);
    }

    @Override
    public Literal getLiteral(String varName) {
        return (Literal) get(varName);
    }

    @Override
    public Resource getResource(String varName) {
        return (Resource) get(varName);
    }

    @Override
    public Iterator<String> varNames() {
        return bs.getBindingNames().iterator();
    }

    @Override
    public String toString() {
        return bs.toString();
    }
}
