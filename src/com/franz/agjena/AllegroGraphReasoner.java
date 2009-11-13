package com.franz.agjena;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerException;

public class AllegroGraphReasoner implements Reasoner {
	
	public AllegroGraphReasoner () {
		
	}

	public void addDescription(Model arg0, Resource arg1) {
		// TODO Auto-generated method stub

	}

    /**
     * Attach the reasoner to a set of RDF data to process.
     * The reasoner may already have been bound to specific rules or ontology
     * axioms (encoded in RDF) through earlier bindRuleset calls.
     * @param data the RDF data to be processed, some reasoners may restrict
     * the range of RDF which is legal here (e.g. syntactic restrictions in OWL).
     * @return an inference graph through which the data+reasoner can be queried.
     * @throws ReasonerException if the data is ill-formed according to the
     * constraints imposed by this reasoner.
     */
	// NOTE: IF 'baseGraph' IS NOT A RAW GRAPH, LOGIC ELSEWHERE IN
	// AllegroGraphGraph WILL BE "SURPRISED".  If nesting of reasoners is
	// desired, more work needs to be done.  - RFF
	public InfGraph bind(Graph baseGraph) throws ReasonerException {
		return new AllegroGraphGraph(baseGraph, this);
	}

	public Reasoner bindSchema(Graph arg0) throws ReasonerException {
		// TODO Auto-generated method stub
		return null;
	}

	public Reasoner bindSchema(Model arg0) throws ReasonerException {
		// TODO Auto-generated method stub
		return null;
	}

	public Capabilities getGraphCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	public Model getReasonerCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDerivationLogging(boolean arg0) {
		// TODO Auto-generated method stub

	}

	public void setParameter(Property arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	public boolean supportsProperty(Property arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
