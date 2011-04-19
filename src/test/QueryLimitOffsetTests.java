/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
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

import com.franz.agraph.repository.AGTupleQuery;

public class QueryLimitOffsetTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
	public void limitOffset_tests() throws Exception {
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

}
