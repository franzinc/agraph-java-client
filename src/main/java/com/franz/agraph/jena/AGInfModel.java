/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Derivation;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ValidityReport;

import java.util.Iterator;

/**
 * Implements the Jena InfModel interface for AllegroGraph.
 */
public class AGInfModel extends AGModel implements InfModel {

    private final AGReasoner reasoner;
    private final AGModel baseModel;

    public AGInfModel(AGReasoner reasoner, AGModel baseModel) {
        super(reasoner.bind(baseModel.getGraph()));
        this.reasoner = reasoner;
        this.baseModel = baseModel;
    }

    @Override
    public Model getDeductionsModel() {
        throw new AGUnsupportedOperationException();
    }

    @Override
    public Iterator<Derivation> getDerivation(Statement statement) {
        throw new AGUnsupportedOperationException();
    }

    @Override
    public Model getRawModel() {
        return baseModel;
    }

    @Override
    public Reasoner getReasoner() {
        return reasoner;
    }

    @Override
    public StmtIterator listStatements(Resource subject, Property predicate,
                                       RDFNode object, Model posit) {
        throw new AGUnsupportedOperationException();
    }

    @Override
    public void prepare() {
    }

    @Override
    public void rebind() {
    }

    @Override
    public void reset() {
    }

    @Override
    public void setDerivationLogging(boolean logOn) {
        throw new AGUnsupportedOperationException();
    }

    @Override
    public ValidityReport validate() {
        throw new AGUnsupportedOperationException();
    }


}
