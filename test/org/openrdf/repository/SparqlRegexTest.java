/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository;

import junit.framework.TestCase;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;

public abstract class SparqlRegexTest extends TestCase {
	public String queryInline = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?name ?mbox\n" + " WHERE { ?x foaf:name  ?name ;\n"
			+ "            foaf:mbox  ?mbox .\n"
			+ "         FILTER regex(str(?mbox), \"@Work.example\", \"i\") }";

	public String queryBinding = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?name ?mbox\n" + " WHERE { ?x foaf:name  ?name ;\n"
			+ "            foaf:mbox  ?mbox .\n"
			+ "         FILTER regex(str(?mbox), ?pattern) }";

	public String queryBindingFlags = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?name ?mbox\n"
			+ " WHERE { ?x foaf:name  ?name ;\n"
			+ "            foaf:mbox  ?mbox .\n"
			+ "         FILTER regex(str(?mbox), ?pattern, ?flags) }";

	public String queryExpr = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
			+ "SELECT ?name ?mbox\n" + " WHERE { ?x foaf:name  ?name ;\n"
			+ "            foaf:mbox  ?mbox .\n"
			+ "         ?y <http://example.org/ns#pattern>  ?pattern .\n"
			+ "         ?y <http://example.org/ns#flags>  ?flags .\n"
			+ "         FILTER regex(str(?mbox), ?pattern, ?flags) }";

	private Repository repository;

	private RepositoryConnection conn;

	private ValueFactory vf;

	private Literal hunt;

	public void testInline() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryInline);
		TupleQueryResult result = query.evaluate();
		assertEquals(hunt, result.next().getValue("name"));
		assertFalse(result.hasNext());
		result.close();
	}

	public void testBinding() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryBinding);
		query.setBinding("pattern", vf.createLiteral("@work.example"));
		TupleQueryResult result = query.evaluate();
		assertEquals(hunt, result.next().getValue("name"));
		assertFalse(result.hasNext());
		result.close();
	}

	public void testBindingFlags() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryBindingFlags);
		query.setBinding("pattern", vf.createLiteral("@Work.example"));
		query.setBinding("flags", vf.createLiteral("i"));
		TupleQueryResult result = query.evaluate();
		assertEquals(hunt, result.next().getValue("name"));
		assertFalse(result.hasNext());
		result.close();
	}

	public void testExpr() throws Exception {
		URI pattern = vf.createURI("http://example.org/ns#", "pattern");
		URI flags = vf.createURI("http://example.org/ns#", "flags");
		BNode bnode = vf.createBNode();
		conn.add(bnode, pattern, vf.createLiteral("@Work.example"));
		conn.add(bnode, flags, vf.createLiteral("i"));
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				queryExpr);
		TupleQueryResult result = query.evaluate();
		assertEquals(hunt, result.next().getValue("name"));
		assertFalse(result.hasNext());
		result.close();
	}

	@Override
	protected void setUp() throws Exception {
		repository = createRepository();
		vf = repository.getValueFactory();
		hunt = vf.createLiteral("James Leigh Hunt");
		createUser("james", "James Leigh", "james@leigh");
		createUser("megan", "Megan Leigh", "megan@leigh");
		createUser("hunt", "James Leigh Hunt", "james@work.example");
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

	private void createUser(String id, String name, String email)
			throws RepositoryException {
		RepositoryConnection conn = repository.getConnection();
		URI subj = vf.createURI("http://example.org/ns#", id);
		URI foafName = vf.createURI("http://xmlns.com/foaf/0.1/", "name");
		URI foafMbox = vf.createURI("http://xmlns.com/foaf/0.1/", "mbox");
		conn.add(subj, foafName, vf.createLiteral(name));
		conn.add(subj, foafMbox, vf.createURI("mailto:", email));
		conn.close();
	}
}
