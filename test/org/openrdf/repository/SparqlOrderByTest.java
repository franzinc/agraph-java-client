/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;

public abstract class SparqlOrderByTest extends TestCase {

	private String query1 = "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?name\n" + "WHERE { ?x foaf:name ?name }\n"
			+ "ORDER BY ?name\n";

	private String query2 = "PREFIX     :    <http://example.org/ns#>\n"
			+ "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
			+ "PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>\n"
			+ "SELECT ?name\n" + "WHERE { ?x foaf:name ?name ; :empId ?emp }\n"
			+ "ORDER BY DESC(?emp)\n";

	private String query3 = "PREFIX     :    <http://example.org/ns#>\n"
			+ "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?name\n" + "WHERE { ?x foaf:name ?name ; :empId ?emp }\n"
			+ "ORDER BY ?name DESC(?emp)\n";

	private Repository repository;

	private RepositoryConnection conn;

	public void testQuery1() throws Exception {
		assertTrue("James Leigh".compareTo("James Leigh Hunt") < 0);
		assertResult(query1, Arrays.asList("James Leigh", "James Leigh",
				"James Leigh Hunt", "Megan Leigh"));
	}

	public void testQuery2() throws Exception {
		assertResult(query2, Arrays.asList("Megan Leigh", "James Leigh",
				"James Leigh Hunt", "James Leigh"));
	}

	public void testQuery3() throws Exception {
		assertResult(query3, Arrays.asList("James Leigh", "James Leigh",
				"James Leigh Hunt", "Megan Leigh"));
	}

	@Override
	protected void setUp() throws Exception {
		repository = createRepository();
		createEmployee("james", "James Leigh", 123);
		createEmployee("jim", "James Leigh", 244);
		createEmployee("megan", "Megan Leigh", 1234);
		createEmployee("hunt", "James Leigh Hunt", 243);
		conn = repository.getConnection();
	}

	protected Repository createRepository() throws Exception {
		Repository repository = newRepository();
		repository.initialize();
		RepositoryConnection con = repository.getConnection();
		try {
			con.clear();
			con.clearNamespaces();
		} finally {
			con.close();
		}
		return repository;
	}

	protected abstract Repository newRepository() throws Exception;

	@Override
	protected void tearDown() throws Exception {
		conn.close();
		repository.shutDown();
	}

	private void createEmployee(String id, String name, int empId)
			throws RepositoryException {
		ValueFactory vf = repository.getValueFactory();
		String foafName = "http://xmlns.com/foaf/0.1/name";
		String exEmpId = "http://example.org/ns#empId";
		RepositoryConnection conn = repository.getConnection();
		conn.add(vf.createURI("http://example.org/ns#" + id), vf
				.createURI(foafName), vf.createLiteral(name));
		conn.add(vf.createURI("http://example.org/ns#" + id), vf
				.createURI(exEmpId), vf.createLiteral(empId));
		conn.close();
	}

	private void assertResult(String queryStr, List<String> names)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryStr);
		TupleQueryResult result = query.evaluate();
		for (String name : names) {
			Value value = result.next().getValue("name");
			assertEquals(name, ((Literal) value).getLabel());
		}
		assertFalse(result.hasNext());
		result.close();
	}
}
