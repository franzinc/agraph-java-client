package com.franz.agraph.jena;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerException;

public class AGReasoner implements Reasoner {

	@Override
	public void addDescription(Model arg0, Resource arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public AGInfGraph bind(Graph arg0) throws ReasonerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reasoner bindSchema(Graph arg0) throws ReasonerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reasoner bindSchema(Model arg0) throws ReasonerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Capabilities getGraphCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model getReasonerCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDerivationLogging(boolean arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setParameter(Property arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean supportsProperty(Property arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
