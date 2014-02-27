/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;

import tutorial.TutorialExamples;

import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGTupleQuery;

public class AGPrepareQuery extends TutorialExamples {

	/**
	 * Exercises prepared and unprepared queries, and occasionally 
	 * forces gc of old AGQuery instances so that orphaned saved 
	 * queries can be deleted during subsequent prepare requests; 
	 * verify by watching the http traffic to the server.
	 *   
	 * @throws Exception
	 */
	public static void prepareQueryExample() throws Exception {
		AGRepositoryConnection conn = example2(false);
		ValueFactory f = conn.getValueFactory();
		conn.setAutoCommit(false);
		URI alice = f.createURI("http://example.org/people/alice");
		URI bob = f.createURI("http://example.org/people/bob");
		String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
			AGTupleQuery tupleQuery = conn.prepareTupleQuery(
					QueryLanguage.SPARQL, queryString);
			tupleQuery.setSaveName(String.valueOf(10*i+j));
			tupleQuery.setBinding("s", alice);
			TupleQueryResult result = tupleQuery.evaluate();
			println("\nFacts about Alice:");
			while (result.hasNext()) {
				println(result.next());
			}
			result.close();
			tupleQuery.setBinding("s", bob);
			println("\nFacts about Bob:");
			result = tupleQuery.evaluate();
			while (result.hasNext()) {
				println(result.next());
			}
			result.close();
			TupleQuery tupleQuery2 = new AGTupleQuery(conn, QueryLanguage.SPARQL, queryString, null);
			result = tupleQuery2.evaluate();
			println("\nAll Facts:");
			while (result.hasNext()) {
				println(result.next());
			}
			result.close();
			}
			System.gc();
		}
		conn.close();
		System.gc();
	}
	
	public static void main(String[] args) throws Exception {
		prepareQueryExample();
 	}

}
