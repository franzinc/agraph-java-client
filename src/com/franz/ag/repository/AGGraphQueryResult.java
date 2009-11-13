package com.franz.ag.repository;

import java.util.Map;

import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;

import com.franz.agsail.AGSailCursor;

public class AGGraphQueryResult implements GraphQueryResult {

	AGSailCursor iter = null;
	
	AGGraphQueryResult(AGSailCursor iter) {
		this.iter = iter;
	}
	
	public Map<String, String> getNamespaces() {
		// TODO flesh out the spec for this
		return null;
	}

	public void close() throws QueryEvaluationException {
		iter.close();
	}

	public boolean hasNext() throws QueryEvaluationException {
		return iter.hasNext();
	}

	public Statement next() throws QueryEvaluationException {
		return iter.next();
	}

	public void remove() throws QueryEvaluationException {
		iter.remove();
	}

}
