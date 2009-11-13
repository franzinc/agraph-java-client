package com.franz.ag.repository;

import java.util.Arrays;
import java.util.List;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

import com.franz.agbase.ValueSetIterator;
import com.franz.agsail.util.AGSInternal;

public class AGTupleQueryResult implements TupleQueryResult {

	AGSInternal ags = null;
	ValueSetIterator iter = null;
	
	AGTupleQueryResult(AGSInternal ags, ValueSetIterator iter) {
		this.ags = ags;
		this.iter = iter;
	}
	
	public List<String> getBindingNames() {
		return Arrays.asList(iter.getNames());
	}

	public void close() throws QueryEvaluationException {
		// TODO check that this can be a no-op.
	}

	public boolean hasNext() throws QueryEvaluationException {
		return iter.hasNext();
	}

	public BindingSet next() throws QueryEvaluationException {
		iter.next();
		return new AGBindingSet(ags, iter);
	}

	public void remove() throws QueryEvaluationException {
		iter.remove();
	}

}
