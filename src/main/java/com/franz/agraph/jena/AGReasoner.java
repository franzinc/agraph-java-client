/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.graph.Capabilities;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerException;

/**
 * Implements the Jena Reasoner interface for AllegroGraph.
 */
public class AGReasoner implements Reasoner {

    /**
     * The default reasoner for AllegroGraph
     */
    public static final AGReasoner RDFS_PLUS_PLUS = new AGReasoner("rdfs++");

    /**
     * A reasoner that includes hasValue, someValuesFrom, and allValuesFrom
     * reasoning in addition to RDFS++ reasoning.
     */
    public static final AGReasoner RESTRICTION = new AGReasoner("restriction");

    /**
     * The name of the entailment regime in use by this reasoner.
     */
    protected final String entailmentRegime;

    /**
     * Creates a new reasoner, using RDFS++ entailment.
     * Consider using the static RDFS_PLUS_PLUS reasoner instead.
     *
     * @see #RDFS_PLUS_PLUS
     */
    public AGReasoner() {
        this(RDFS_PLUS_PLUS.entailmentRegime);
    }

    /**
     * Creates a new reasoner, using the specified entailmentRegime.
     * Consider using one of the static reasoners instead.
     *
     * @param entailmentRegime the entailment to be used by this reasoner
     * @see #RDFS_PLUS_PLUS
     * @see #RESTRICTION
     */
    public AGReasoner(String entailmentRegime) {
        this.entailmentRegime = entailmentRegime;
    }

    /**
     * Gets the name of the entailmentRegime for this reasoner.
     *
     * @return the name of the entailmentRegime for this reasoner
     */
    public String getEntailmentRegime() {
        return entailmentRegime;
    }

    @Override
    public void addDescription(Model configSpec, Resource base) {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);

    }

    @Override
    public AGInfGraph bind(Graph data) throws ReasonerException {
        if (!(data instanceof AGGraph)) {
            throw new IllegalArgumentException("Only AGGraphs are supported.");
        }
        return new AGInfGraph(this, (AGGraph) data);
    }

    @Override
    public Reasoner bindSchema(Graph tbox) throws ReasonerException {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public Reasoner bindSchema(Model tbox) throws ReasonerException {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public Capabilities getGraphCapabilities() {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public Model getReasonerCapabilities() {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public void setDerivationLogging(boolean logOn) {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);

    }

    @Override
    public void setParameter(Property parameterUri, Object value) {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);

    }

    @Override
    public boolean supportsProperty(Property property) {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }


}
