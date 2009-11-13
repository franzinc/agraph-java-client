/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public abstract class EquivalentTest extends TestCase {
	private static ValueFactory vf = ValueFactoryImpl.getInstance();
	private static Literal xyz_simple = vf.createLiteral("xyz");
	private static Literal xyz_en = vf.createLiteral("xyz", "en");
	private static Literal xyz_EN = vf.createLiteral("xyz", "EN");
	private static Literal xyz_string = vf.createLiteral("xyz", XMLSchema.STRING);
	private static Literal xyz_integer = vf.createLiteral("xyz", XMLSchema.INTEGER);
	private static Literal xyz_unknown = vf.createLiteral("xyz", vf
			.createURI("http://example/unknown"));
	private static URI xyz_uri = vf.createURI("http://example/xyz");
	private static Literal abc_simple = vf.createLiteral("abc");
	private static Literal abc_en = vf.createLiteral("abc", "en");
	private static Literal abc_EN = vf.createLiteral("abc", "EN");
	private static Literal abc_string = vf.createLiteral("abc", XMLSchema.STRING);
	private static Literal abc_integer = vf.createLiteral("abc", XMLSchema.INTEGER);
	private static Literal abc_unknown = vf.createLiteral("abc", vf
			.createURI("http://example/unknown"));
	private static URI abc_uri = vf.createURI("http://example/abc");
	private static URI t1 = vf.createURI("http://example/t1");
	private static URI t2 = vf.createURI("http://example/t2");
	private static final String IND = "?";
	private static final String EQ = "=";
	private static final String NEQ = "!=";
	private static final String PREFIX = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
			+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
			+ "PREFIX ex:<http://example/>";
	private static String matrix = "\"xyz\"	\"xyz\"	eq\n"
			+ "\"xyz\"	\"xyz\"@en	neq\n" + "\"xyz\"	\"xyz\"@EN	neq\n"
			+ "\"xyz\"	\"xyz\"^^xsd:string	eq\n"
			+ "\"xyz\"	\"xyz\"^^xsd:integer	ind\n"
			+ "\"xyz\"	\"xyz\"^^ex:unknown	ind\n" + "\"xyz\"	_:xyz	neq\n"
			+ "\"xyz\"	:xyz	neq\n" + "\"xyz\"@en	\"xyz\"	neq\n"
			+ "\"xyz\"@en	\"xyz\"@en	eq\n"
			+ "\"xyz\"@en	\"xyz\"@EN	eq\n"
			+ "\"xyz\"@en	\"xyz\"^^xsd:string	neq\n"
			+ "\"xyz\"@en	\"xyz\"^^xsd:integer	neq\n"
			+ "\"xyz\"@en	\"xyz\"^^ex:unknown	neq\n" + "\"xyz\"@en	_:xyz	neq\n"
			+ "\"xyz\"@en	:xyz	neq\n" + "\"xyz\"@EN	\"xyz\"	neq\n"
			+ "\"xyz\"@EN	\"xyz\"@en	eq\n"
			+ "\"xyz\"@EN	\"xyz\"@EN	eq\n"
			+ "\"xyz\"@EN	\"xyz\"^^xsd:string	neq\n"
			+ "\"xyz\"@EN	\"xyz\"^^xsd:integer	neq\n"
			+ "\"xyz\"@EN	\"xyz\"^^ex:unknown	neq\n" + "\"xyz\"@EN	_:xyz	neq\n"
			+ "\"xyz\"@EN	:xyz	neq\n"
			+ "\"xyz\"^^xsd:string	\"xyz\"	eq\n"
			+ "\"xyz\"^^xsd:string	\"xyz\"@en	neq\n"
			+ "\"xyz\"^^xsd:string	\"xyz\"@EN	neq\n"
			+ "\"xyz\"^^xsd:string	\"xyz\"^^xsd:string	eq\n"
			+ "\"xyz\"^^xsd:string	\"xyz\"^^xsd:integer	ind\n"
			+ "\"xyz\"^^xsd:string	\"xyz\"^^ex:unknown	ind\n"
			+ "\"xyz\"^^xsd:string	_:xyz	neq\n"
			+ "\"xyz\"^^xsd:string	:xyz	neq\n"
			+ "\"xyz\"^^xsd:integer	\"xyz\"	ind\n"
			+ "\"xyz\"^^xsd:integer	\"xyz\"@en	neq\n"
			+ "\"xyz\"^^xsd:integer	\"xyz\"@EN	neq\n"
			+ "\"xyz\"^^xsd:integer	\"xyz\"^^xsd:string	ind\n"
			+ "\"xyz\"^^xsd:integer	\"xyz\"^^xsd:integer	eq\n"
			+ "\"xyz\"^^xsd:integer	\"xyz\"^^ex:unknown	ind\n"
			+ "\"xyz\"^^xsd:integer	_:xyz	neq\n"
			+ "\"xyz\"^^xsd:integer	:xyz	neq\n"
			+ "\"xyz\"^^ex:unknown	\"xyz\"	ind\n"
			+ "\"xyz\"^^ex:unknown	\"xyz\"@en	neq\n"
			+ "\"xyz\"^^ex:unknown	\"xyz\"@EN	neq\n"
			+ "\"xyz\"^^ex:unknown	\"xyz\"^^xsd:string	ind\n"
			+ "\"xyz\"^^ex:unknown	\"xyz\"^^xsd:integer	ind\n"
			+ "\"xyz\"^^ex:unknown	\"xyz\"^^ex:unknown	eq\n"
			+ "\"xyz\"^^ex:unknown	_:xyz	neq\n"
			+ "\"xyz\"^^ex:unknown	:xyz	neq\n" + "_:xyz	\"xyz\"	neq\n"
			+ "_:xyz	\"xyz\"@en	neq\n" + "_:xyz	\"xyz\"@EN	neq\n"
			+ "_:xyz	\"xyz\"^^xsd:string	neq\n"
			+ "_:xyz	\"xyz\"^^xsd:integer	neq\n"
			+ "_:xyz	\"xyz\"^^ex:unknown	neq\n" + "_:xyz	_:xyz	eq\n"
			+ "_:xyz	:xyz	neq\n" + ":xyz	\"xyz\"	neq\n"
			+ ":xyz	\"xyz\"@en	neq\n" + ":xyz	\"xyz\"@EN	neq\n"
			+ ":xyz	\"xyz\"^^xsd:string	neq\n"
			+ ":xyz	\"xyz\"^^xsd:integer	neq\n"
			+ ":xyz	\"xyz\"^^ex:unknown	neq\n" + ":xyz	_:xyz	neq\n"
			+ ":xyz	:xyz	eq\n" + "\"xyz\"	\"abc\"		neq	\n"
			+ "\"xyz\"	\"abc\"@en		neq	\n" + "\"xyz\"	\"abc\"@EN		neq	\n"
			+ "\"xyz\"	\"abc\"^^xsd:string		neq	\n"
			+ "\"xyz\"	\"abc\"^^xsd:integer			ind\n"
			+ "\"xyz\"	\"abc\"^^:unknown			ind\n" + "\"xyz\"	_:abc		neq	\n"
			+ "\"xyz\"	:abc		neq	\n" + "\"xyz\"@en	\"abc\"		neq	\n"
			+ "\"xyz\"@en	\"abc\"@en		neq	\n" + "\"xyz\"@en	\"abc\"@EN		neq	\n"
			+ "\"xyz\"@en	\"abc\"^^xsd:string		neq	\n"
			+ "\"xyz\"@en	\"abc\"^^xsd:integer		neq	\n"
			+ "\"xyz\"@en	\"abc\"^^:unknown		neq	\n"
			+ "\"xyz\"@en	_:abc		neq	\n" + "\"xyz\"@en	:abc		neq	\n"
			+ "\"xyz\"@EN	\"abc\"		neq	\n" + "\"xyz\"@EN	\"abc\"@en		neq	\n"
			+ "\"xyz\"@EN	\"abc\"@EN		neq	\n"
			+ "\"xyz\"@EN	\"abc\"^^xsd:string		neq	\n"
			+ "\"xyz\"@EN	\"abc\"^^xsd:integer		neq	\n"
			+ "\"xyz\"@EN	\"abc\"^^:unknown		neq	\n"
			+ "\"xyz\"@EN	_:abc		neq	\n" + "\"xyz\"@EN	:abc		neq	\n"
			+ "\"xyz\"^^xsd:string	\"abc\"		neq	\n"
			+ "\"xyz\"^^xsd:string	\"abc\"@en		neq	\n"
			+ "\"xyz\"^^xsd:string	\"abc\"@EN		neq	\n"
			+ "\"xyz\"^^xsd:string	\"abc\"^^xsd:string		neq	\n"
			+ "\"xyz\"^^xsd:string	\"abc\"^^xsd:integer			ind\n"
			+ "\"xyz\"^^xsd:string	\"abc\"^^:unknown			ind\n"
			+ "\"xyz\"^^xsd:string	_:abc		neq	\n"
			+ "\"xyz\"^^xsd:string	:abc		neq	\n"
			+ "\"xyz\"^^xsd:integer	\"abc\"			ind\n"
			+ "\"xyz\"^^xsd:integer	\"abc\"@en		neq	\n"
			+ "\"xyz\"^^xsd:integer	\"abc\"@EN		neq	\n"
			+ "\"xyz\"^^xsd:integer	\"abc\"^^xsd:string			ind\n"
			+ "\"xyz\"^^xsd:integer	\"abc\"^^xsd:integer			ind\n"
			+ "\"xyz\"^^xsd:integer	\"abc\"^^:unknown			ind\n"
			+ "\"xyz\"^^xsd:integer	_:abc		neq	\n"
			+ "\"xyz\"^^xsd:integer	:abc		neq	\n"
			+ "\"xyz\"^^:unknown	\"abc\"			ind\n"
			+ "\"xyz\"^^:unknown	\"abc\"@en		neq	\n"
			+ "\"xyz\"^^:unknown	\"abc\"@EN		neq	\n"
			+ "\"xyz\"^^:unknown	\"abc\"^^xsd:string			ind\n"
			+ "\"xyz\"^^:unknown	\"abc\"^^xsd:integer			ind\n"
			+ "\"xyz\"^^:unknown	\"abc\"^^:unknown			ind\n"
			+ "\"xyz\"^^:unknown	_:abc		neq	\n"
			+ "\"xyz\"^^:unknown	:abc		neq	\n" + "_:xyz	\"abc\"		neq	\n"
			+ "_:xyz	\"abc\"@en		neq	\n" + "_:xyz	\"abc\"@EN		neq	\n"
			+ "_:xyz	\"abc\"^^xsd:string		neq	\n"
			+ "_:xyz	\"abc\"^^xsd:integer		neq	\n"
			+ "_:xyz	\"abc\"^^:unknown		neq	\n" + "_:xyz	_:abc		neq	\n"
			+ "_:xyz	:abc		neq	\n" + ":xyz	\"abc\"		neq	\n"
			+ ":xyz	\"abc\"@en		neq	\n" + ":xyz	\"abc\"@EN		neq	\n"
			+ ":xyz	\"abc\"^^xsd:string		neq	\n"
			+ ":xyz	\"abc\"^^xsd:integer		neq	\n"
			+ ":xyz	\"abc\"^^:unknown		neq	\n" + ":xyz	_:abc		neq	\n"
			+ ":xyz	:abc		neq	";

	public static TestSuite suite() throws Exception {
		return new TestSuite();
	}

	public static TestSuite suite(Class<? extends EquivalentTest> subclass)
			throws Exception {
		TestSuite suite = new TestSuite(subclass.getName());
		for (String row : matrix.split("\n")) {
			if (row.contains("_:"))
				continue;
			EquivalentTest test = subclass.newInstance();
			String[] fields = row.split("\t", 3);
			if (fields[2].contains("neq")) {
				test.setName(fields[0] + " " + NEQ + " " + fields[1]);
			} else if (fields[2].contains("eq")) {
				test.setName(fields[0] + " " + EQ + " " + fields[1]);
			} else if (fields[2].contains("ind")) {
				test.setName(fields[0] + " " + IND + " " + fields[1]);
			} else {
				throw new AssertionError(row);
			}
			suite.addTest(test);
		}
		return suite;
	}

	private Value term1;
	private Value term2;
	private String operator;
	private Repository repository;
	private RepositoryConnection con;

	public EquivalentTest() {
		super();
	}

	@Override
	public void setName(String name) {
		super.setName(name);
		String[] fields = name.split(" ", 3);
		term1 = getTerm(fields[0]);
		operator = fields[1];
		term2 = getTerm(fields[2]);
	}

	@Override
	protected void setUp() throws Exception {
		repository = createRepository();
		con = repository.getConnection();
		con.clear();
		con.add(t1, RDF.VALUE, term1);
		con.add(t2, RDF.VALUE, term2);
	}

	@Override
	protected void tearDown() throws Exception {
		con.close();
		repository.shutDown();
	}

	@Override
	protected void runTest() throws Throwable {
		assertEquals(null, operator, compare(term1, term2));
	}

	protected Repository createRepository() throws Exception {
		Repository repository = newRepository();
		repository.initialize();
		RepositoryConnection con = repository.getConnection();
		con.clear();
		con.clearNamespaces();
		con.close();
		return repository;
	}

	protected abstract Repository newRepository() throws Exception;

	private Value getTerm(String label) {
		if (label.contains("xyz")) {
			if (label.contains("integer"))
				return xyz_integer;
			if (label.contains("string"))
				return xyz_string;
			if (label.contains("unknown"))
				return xyz_unknown;
			if (label.contains("en"))
				return xyz_en;
			if (label.contains("EN"))
				return xyz_EN;
			if (label.contains(":xyz"))
				return xyz_uri;
			if (label.contains("\"xyz\""))
				return xyz_simple;
		}
		if (label.contains("abc")) {
			if (label.contains("integer"))
				return abc_integer;
			if (label.contains("string"))
				return abc_string;
			if (label.contains("unknown"))
				return abc_unknown;
			if (label.contains("en"))
				return abc_en;
			if (label.contains("EN"))
				return abc_EN;
			if (label.contains(":abc"))
				return abc_uri;
			if (label.contains("\"abc\""))
				return abc_simple;
		}
		throw new AssertionError(label);
	}

	private String compare(@SuppressWarnings("unused")
	Value term1, @SuppressWarnings("unused")
	Value term2) throws Exception {
		boolean eq = evaluate(EQ);
		boolean neq = evaluate(NEQ);
		assertTrue(!eq || !neq);
		if (eq && !neq)
			return EQ;
		if (!eq && neq)
			return NEQ;
		if (!eq && !neq)
			return IND;
		throw new AssertionError();
	}

	private boolean evaluate(String op) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {
		String qry = PREFIX + "SELECT ?term1 ?term2 "
				+ "WHERE {ex:t1 rdf:value ?term1 . ex:t2 rdf:value ?term2 "
				+ "FILTER (?term1 " + op + " ?term2)}";
		return evaluateSparql(qry);
	}

	private boolean evaluateSparql(String qry) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {
		TupleQuery query = con.prepareTupleQuery(QueryLanguage.SPARQL, qry);
		TupleQueryResult evaluate = query.evaluate();
		try {
			return evaluate.hasNext();
		} finally {
			evaluate.close();
		}
	}
}
