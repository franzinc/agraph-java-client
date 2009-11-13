package com.franz.agraph.repository;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;

import com.franz.agraph.http.AGResponseHandler;

public class AGTupleQuery extends AGQuery implements TupleQuery {

	public AGTupleQuery(AGRepositoryConnection con, QueryLanguage ql,
			String queryString, String baseURI) {
		super(con, ql, queryString, baseURI);
	}

	public TupleQueryResult evaluate() throws QueryEvaluationException {
		try {
			// TODO: make this efficient for large result sets
			TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
			evaluate(builder);
			return builder.getQueryResult();
		} catch (TupleQueryResultHandlerException e) {
			// Found a bug in TupleQueryResultBuilder?
			throw new RuntimeException(e);
		}
	}

	public void evaluate(TupleQueryResultHandler handler)
			throws QueryEvaluationException, TupleQueryResultHandlerException {
		try {
			httpCon.getHttpRepoClient().query(
					new AGResponseHandler(httpCon.getRepository(), handler),
					queryLanguage, queryString, dataset, includeInferred, planner,
					getBindingsArray());
		} catch (HttpException e) {
			throw new QueryEvaluationException(e);
		} catch (RepositoryException e) {
		    throw new QueryEvaluationException(e);
		} catch (RDFParseException e) {
		    throw new QueryEvaluationException(e);
		} catch (IOException e) {
		    throw new QueryEvaluationException(e);
		}
	}

	public long count() throws QueryEvaluationException {
		AGResponseHandler handler = new AGResponseHandler(0L);
		try {
			httpCon.getHttpRepoClient().query(
					handler,
					queryLanguage, queryString, dataset, includeInferred, planner,
					getBindingsArray());
		} catch (HttpException e) {
			throw new QueryEvaluationException(e);
		} catch (RepositoryException e) {
			throw new QueryEvaluationException(e);
		} catch (RDFParseException e) {
			throw new QueryEvaluationException(e);
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
		return handler.getLong();
	}
}
