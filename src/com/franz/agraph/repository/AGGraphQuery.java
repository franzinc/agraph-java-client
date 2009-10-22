package com.franz.agraph.repository;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.helpers.StatementCollector;

import com.franz.agraph.http.AGResponseHandler;

public class AGGraphQuery extends AGQuery implements GraphQuery {

	public AGGraphQuery(AGRepositoryConnection con, QueryLanguage ql,
			String queryString, String baseURI) {
		super(con, ql, queryString, baseURI);
	}

	public GraphQueryResult evaluate() throws QueryEvaluationException {
		try {
			// TODO: make this efficient for large result sets
			StatementCollector collector = new StatementCollector();
			evaluate(collector);
			return new GraphQueryResultImpl(collector.getNamespaces(),
					collector.getStatements());
		} catch (RDFHandlerException e) {
			// Found a bug in StatementCollector?
			throw new RuntimeException(e);
		}
	}

	public void evaluate(RDFHandler handler) throws QueryEvaluationException,
			RDFHandlerException {
		try {
			// TODO: deal with the hard coded return format
			httpCon.getHttpRepoClient().query(
					new AGResponseHandler(httpCon.getRepository(), handler,
							RDFFormat.NTRIPLES), queryLanguage, queryString,
					dataset, includeInferred, getBindingsArray());
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

}
