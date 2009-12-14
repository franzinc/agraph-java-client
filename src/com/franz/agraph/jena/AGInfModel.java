package com.franz.agraph.jena;

import java.util.Iterator;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Derivation> getDerivation(Statement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model getRawModel() {
		return baseModel;
	}

	@Override
	public Reasoner getReasoner() {
		// TODO Auto-generated method stub
		return reasoner;
	}

	@Override
	public StmtIterator listStatements(Resource subject, Property predicate,
			RDFNode object, Model posit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void prepare() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rebind() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDerivationLogging(boolean logOn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ValidityReport validate() {
		// TODO Auto-generated method stub
		return null;
	}


}
