package com.franz.ag.repository;

import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.SPARQLQuery;
import com.franz.agbase.TriplesIterator;
import com.franz.agsail.AGSailCursor;
import com.franz.agsail.util.AGSInternal;

public class AGGraphQuery extends AGQuery implements GraphQuery {

	public AGGraphQuery(AGSInternal ags, SPARQLQuery sq) {
		super(ags,sq);	
	}
	
	public GraphQueryResult evaluate() throws QueryEvaluationException {
		TriplesIterator iter;
		try {
			iter = sq.construct();
		} catch (AllegroGraphException e) {
			throw new QueryEvaluationException(e);
		}
		return new AGGraphQueryResult(ags.coerceToSailCursor(iter));
	}

	public void evaluate(RDFHandler handler)
			throws QueryEvaluationException, RDFHandlerException {
		// TODO flesh out the spec for this  (no test cases use it).
		TriplesIterator iter;
		try {
			iter = sq.construct();
			handler.startRDF();
			AGSailCursor c = ags.coerceToSailCursor(iter);
			while (c.hasNext()) {
				handler.handleStatement(c.getNext());
			}
			handler.endRDF();
		} catch (AllegroGraphException e) {
			throw new QueryEvaluationException(e);
		}
	}

}
