/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.query.QueryLanguage;
import org.openrdf.rio.RDFFormat;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecution;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.repository.AGTupleQuery;

public class QueryLimitOffsetTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
	public void sesameQueryLimitOffset_tests() throws Exception {
		conn.add(new File("src/tutorial/java-vcards.rdf"), null, RDFFormat.RDFXML);
		String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
		AGTupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		Assert.assertEquals("expected 16 results", 16, tupleQuery.count());
		tupleQuery.setLimit(5);
		Assert.assertEquals("expected 5 results", 5, tupleQuery.count());
		tupleQuery.setOffset(15);
		Assert.assertEquals("expected 1 result", 1, tupleQuery.count());
		tupleQuery.setLimit(-1);
		tupleQuery.setOffset(10);
		Assert.assertEquals("expected 6 results", 6, tupleQuery.count());
		tupleQuery.setOffset(-1);
		Assert.assertEquals("expected 16 results", 16, tupleQuery.count());
	}

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaQueryLimitOffset_tests() throws Exception {
    	conn.add(new File("src/tutorial/java-vcards.rdf"), null, RDFFormat.RDFXML);
    	AGGraphMaker maker = new AGGraphMaker(conn);
    	AGModel model = new AGModel(maker.getGraph());
    	String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
		AGQuery query = AGQueryFactory.create(queryString);
		AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
    	Assert.assertEquals("expected 16 results", 16, qe.countSelect());
    	query.setLimit(5);
    	Assert.assertEquals("expected 5 results", 5, qe.countSelect());
    	query.setOffset(15);
    	Assert.assertEquals("expected 1 result", 1, qe.countSelect());
    	query.setLimit(-1);
    	query.setOffset(10);
    	Assert.assertEquals("expected 6 results", 6, qe.countSelect());
    	query.setOffset(-1);
    	Assert.assertEquals("expected 16 results", 16, qe.countSelect());
    }
    
}
