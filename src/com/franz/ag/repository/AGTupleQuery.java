package com.franz.ag.repository;

import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.SPARQLQuery;
import com.franz.agbase.ValueSetIterator;
import com.franz.agsail.util.AGSInternal;

public class AGTupleQuery extends AGQuery implements TupleQuery {

	public AGTupleQuery(AGSInternal ags, SPARQLQuery sq) {
		super(ags,sq);
	}
	
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		ValueSetIterator vsi;
		try {
			vsi = sq.select();
		} catch (AllegroGraphException e) {
			throw new QueryEvaluationException(e);
		}
		return new AGTupleQueryResult(ags, vsi);
	}

	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {
		TupleQueryResult result = evaluate();
		handler.startQueryResult(result.getBindingNames());
		while (result.hasNext()) {
			handler.handleSolution(result.next());
		}
		handler.endQueryResult();
	}

}
