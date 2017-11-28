/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ReifiedStatementImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;

public class AGStatement extends StatementImpl {

    public AGStatement(Resource subject, Property predicate, RDFNode object) {
        super(subject, predicate, object);
    }

    public AGStatement(Resource subject, Property predicate, RDFNode object,
                       AGModel model) {
        super(subject, predicate, object, model);
    }

    /**
     * create a Statement from the triple <code>t</code> in the enhanced graph <code>eg</code>.
     * The Statement has subject, predicate, and object corresponding to those of
     * <code>t</code>.
     *
     * @param t  the triple to build into a Statement
     * @param eg the Graph in which the Statement will be created
     * @return Statement  the statement created
     */
    public static Statement toStatement(Triple t, AGModel eg) {
        Resource s = new ResourceImpl(t.getSubject(), eg);
        Property p = new PropertyImpl(t.getPredicate(), eg);
        RDFNode o = createObject(t.getObject(), eg);
        return new AGStatement(s, p, o, eg);
    }

    @Override
    public AGModel getModel() {
        return (AGModel) super.getModel();
    }

    /**
     * create a ReifiedStatement corresponding to this Statement
     */
    public ReifiedStatement createReifiedStatement() {
        Resource bnode = getModel().createResource();
        return ReifiedStatementImpl.create(this.getModel(), bnode.asNode(), this);
    }

}
