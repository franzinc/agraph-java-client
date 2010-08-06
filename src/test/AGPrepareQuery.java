/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
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
import org.openrdf.repository.RepositoryConnection;

import tutorial.TutorialExamples;

public class AGPrepareQuery extends TutorialExamples {

	/**
	 * Exercises prepared queries and occasionally forces gc 
	 * of old AGQuery instances to verify that orphaned saved 
	 * queries on the server are eventually deleted on the
	 * subsequent prepare requests.
	 *   
	 * @throws Exception
	 */
	public static void prepareQueryExample() throws Exception {
		RepositoryConnection conn = example2(false);
		ValueFactory f = conn.getValueFactory();
		conn.setAutoCommit(false);
		URI alice = f.createURI("http://example.org/people/alice");
		URI bob = f.createURI("http://example.org/people/bob");
		String queryString = "select ?s ?p ?o where { ?s ?p ?o} ";
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
			TupleQuery tupleQuery = conn.prepareTupleQuery(
					QueryLanguage.SPARQL, queryString);
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
			tupleQuery = null;
			}
			System.gc();
		}
	}
	
	public static void main(String[] args) throws Exception {
		prepareQueryExample();
 	}

}
