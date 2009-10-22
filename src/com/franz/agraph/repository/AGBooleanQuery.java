package com.franz.agraph.repository;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;

import com.franz.agraph.http.AGResponseHandler;

public class AGBooleanQuery extends AGQuery implements BooleanQuery {

	public AGBooleanQuery(AGRepositoryConnection con, QueryLanguage ql,
			String queryString, String baseURI) {
		super(con, ql, queryString, baseURI);
	}

	public boolean evaluate() throws QueryEvaluationException {
		AGResponseHandler handler = new AGResponseHandler(true);
		try {
			httpCon.getHttpRepoClient().query(handler,
					queryLanguage, queryString, dataset, includeInferred,
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
		return handler.getBoolean();
	}

}
