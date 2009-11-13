package com.franz.ag.repository;

import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryEvaluationException;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.SPARQLQuery;
import com.franz.agsail.util.AGSInternal;

public class AGBooleanQuery extends AGQuery implements BooleanQuery {

	public AGBooleanQuery(AGSInternal ags, SPARQLQuery sq) {
		super(ags,sq);
	}
	
	public boolean evaluate() throws QueryEvaluationException {
		boolean result;
		try {
			result = sq.ask();
		} catch (AllegroGraphException e) {
			throw new QueryEvaluationException(e);
		}
		return result;
	}

}
