package com.franz.agraph.jena;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.franz.agraph.repository.AGBooleanQuery;
import com.franz.agraph.repository.AGGraphQuery;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGTupleQuery;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.util.FileManager;

public class AGQueryExecution implements QueryExecution {

	private final AGQuery query;
	private final AGModel model;
	
	public AGQueryExecution(AGQuery query, AGModel model) {
		this.query = query;
		this.model = model;
	}

	
	@Override
	public void abort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
	}

	@Override
	public boolean execAsk() {
		AGBooleanQuery bq = model.getGraph().getConnection().prepareBooleanQuery(AGQueryLanguage.SPARQL, query.getQueryString());
		boolean result;
		try {
			bq.setDataset(model.getGraph().getDataset());
			result = bq.evaluate();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	public Model execConstruct() {
		return execConstruct(null);
	}

	@Override
	public Model execConstruct(Model m) {
		AGGraphQuery gq = model.getGraph().getConnection().prepareGraphQuery(AGQueryLanguage.SPARQL, query.getQueryString());
		GraphQueryResult result;
		try {
			gq.setDataset(model.getGraph().getDataset());
			result = gq.evaluate();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
		if (m==null) {
			m = ModelFactory.createDefaultModel();
		}
		try {
			m.setNsPrefixes(result.getNamespaces());
			while (result.hasNext()) {
				m.add(model.asStatement(AGNodeFactory.asTriple(result.next())));
			}
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
		return m;
	}

	@Override
	public Model execDescribe() {
		return execDescribe(null);
	}

	@Override
	public Model execDescribe(Model m) {
		return execConstruct(m);
	}

	@Override
	public ResultSet execSelect() {
		AGTupleQuery tq = model.getGraph().getConnection().prepareTupleQuery(AGQueryLanguage.SPARQL, query.getQueryString());
		TupleQueryResult result;
		try {
			tq.setDataset(model.getGraph().getDataset());
			result = tq.evaluate();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);
		}
		return new AGResultSet(result, model);
	}

	@Override
	public Context getContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dataset getDataset() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFileManager(FileManager fm) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setInitialBinding(QuerySolution binding) {
		// TODO Auto-generated method stub

	}

}
