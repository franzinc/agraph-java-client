package test;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.Iterations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.AllegroRepository;
import org.openrdf.repository.sail.AllegroSail;
import org.openrdf.repository.sail.Catalog;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.helpers.RDFHandlerBase;

public class AGRepositoryConnectionTest extends RepositoryConnectionTest {

	private final String TEST_DIR_PREFIX = "./src/test/"; //System.getProperty("user.dir") + "\\";
	
	public AGRepositoryConnectionTest(String name) {
		super(name);
	}

	protected Repository createRepository() throws Exception {
	    AllegroSail server = new AllegroSail("localhost", 8080);
	    Catalog catalog = server.openCatalog("scratch");    
	    AllegroRepository myRepository = catalog.getRepository("test2", AllegroRepository.RENEW);
	    return myRepository;
	}

	public void testHasStatementWithoutBNodes() throws Exception {
		testCon.add(name, name, nameBob);

		assertTrue("Repository should contain newly added statement", testCon
				.hasStatement(name, name, nameBob, false));
	}

	public void testHasStatementWithBNodes() throws Exception {
		testCon.add(bob, name, nameBob);

		assertTrue("Repository should contain newly added statement", testCon
				.hasStatement(bob, name, nameBob, false));

	}

	public void testAddStatement() throws Exception {
		testCon.add(bob, name, nameBob);

		assertTrue("Repository should contain newly added statement", testCon
				.hasStatement(bob, name, nameBob, false));

		Statement statement = vf.createStatement(alice, name, nameAlice);
		testCon.add(statement);

		assertTrue("Repository should contain newly added statement", testCon
				.hasStatement(statement, false));
		assertTrue("Repository should contain newly added statement", testCon
				.hasStatement(alice, name, nameAlice, false));

	}
	public void testSimpleTupleQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?name ?mbox");
		queryBuilder.append(" WHERE { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");

		TupleQueryResult result = testCon.prepareTupleQuery(
				QueryLanguage.SPARQL, queryBuilder.toString()).evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("name"));
				assertTrue(solution.hasBinding("mbox"));

				Value nameResult = solution.getValue("name");
				Value mboxResult = solution.getValue("mbox");

				assertTrue((nameAlice.equals(nameResult) || nameBob
						.equals(nameResult)));
				assertTrue((mboxAlice.equals(mboxResult) || mboxBob
						.equals(mboxResult)));
			}
		} finally {
			result.close();
		}
	}

	public void testSimpleTupleQueryUnicode() throws Exception {
		// TODO: revert strangeUnicodeVarname to original value
		testCon.add(alexander, name, strangeUnicodeVarname);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?person");
		queryBuilder.append(" WHERE { ?person foaf:name '");
		queryBuilder.append(			strangeUnicodeVarname.getLabel()).append("' .}");

		TupleQueryResult result = testCon.prepareTupleQuery(
				QueryLanguage.SPARQL, queryBuilder.toString()).evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("person"));
				assertEquals(alexander, solution.getValue("person"));
			}
		} finally {
			result.close();
		}
	}

	public void testPreparedTupleQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?name ?mbox");
		queryBuilder.append(" WHERE { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");

		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL,
				queryBuilder.toString());
		query.setBinding("name", nameBob);	// FIXME: needs bug18180 fixed


		TupleQueryResult result = query.evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("name"));
				assertTrue(solution.hasBinding("mbox"));

				Value nameResult = solution.getValue("name");
				Value mboxResult = solution.getValue("mbox");

				assertEquals("unexpected value for name: " + nameResult,
						nameBob, nameResult);
				assertEquals("unexpected value for mbox: " + mboxResult,
						mboxBob, mboxResult);
			}
		} finally {
			result.close();
		}
	}

	public void testPreparedTupleQueryUnicode() throws Exception {
		// TODO: revert strangeUnicodeVarname to original value
		testCon.add(alexander, name, strangeUnicodeVarname);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?person");
		queryBuilder.append(" WHERE { ?person foaf:name '");
		queryBuilder.append(			strangeUnicodeVarname.getLabel()).append("' .}");

		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL,
				queryBuilder.toString());
		query.setBinding("name", strangeUnicodeVarname);

		TupleQueryResult result = query.evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertTrue(solution.hasBinding("person"));
				assertEquals(alexander, solution.getValue("person"));
			}
		} finally {
			result.close();
		}
	}

	public void testSimpleGraphQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" CONSTRUCT { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");
		queryBuilder.append(" WHERE { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");

		GraphQueryResult result = testCon.prepareGraphQuery(
				QueryLanguage.SPARQL, queryBuilder.toString()).evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				Statement st = result.next();
				if (name.equals(st.getPredicate())) {
					assertTrue(nameAlice.equals(st.getObject())
							|| nameBob.equals(st.getObject()));
				} else {
					assertTrue(mbox.equals(st.getPredicate()));
					assertTrue(mboxAlice.equals(st.getObject())
							|| mboxBob.equals(st.getObject()));
				}
			}
		} finally {
			result.close();
		}
	}

	public void testPreparedGraphQuery() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" CONSTRUCT { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");
		queryBuilder.append(" WHERE { ?x foaf:name ?name .");
		queryBuilder.append("         ?x foaf:mbox ?mbox .}");

		GraphQuery query = testCon.prepareGraphQuery(QueryLanguage.SPARQL,
				queryBuilder.toString());
		query.setBinding("name", nameBob);

		GraphQueryResult result = query.evaluate();

		try {
			assertTrue(result != null);
			assertTrue(result.hasNext());

			while (result.hasNext()) {
				Statement st = result.next();
				assertTrue(name.equals(st.getPredicate())
						|| mbox.equals(st.getPredicate()));
				if (name.equals(st.getPredicate())) {
					assertTrue("unexpected value for name: " + st.getObject(),
							nameBob.equals(st.getObject()));
				} else {
					assertTrue(mbox.equals(st.getPredicate()));
					assertTrue("unexpected value for mbox: " + st.getObject(),
							mboxBob.equals(st.getObject()));
				}

			}
		} finally {
			result.close();
		}
	}
	
	public void testDataset() throws Exception {
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("PREFIX foaf: <" + FOAF_NS + "> ");
		queryBuilder.append("ASK ");
		queryBuilder.append("{ ?p foaf:name ?name }");

		BooleanQuery query = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, queryBuilder.toString());
		query.setBinding("name", nameBob);

		assertTrue(query.evaluate());

		DatasetImpl dataset = new DatasetImpl();

		// default graph: {context1}
		dataset.addDefaultGraph(context1);
		query.setDataset(dataset);
		assertTrue(query.evaluate());

		// default graph: {context1, context2}
		dataset.addDefaultGraph(context2);
		query.setDataset(dataset);
		assertTrue(query.evaluate());

		// default graph: {context2}
		dataset.removeDefaultGraph(context1);
		query.setDataset(dataset);
		assertFalse(query.evaluate());

		queryBuilder.setLength(0);
		queryBuilder.append("PREFIX foaf: <" + FOAF_NS + "> ");
		queryBuilder.append("ASK ");
		queryBuilder.append("{ GRAPH ?g { ?p foaf:name ?name } }");

		query = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, queryBuilder
				.toString());
		query.setBinding("name", nameBob);

		// default graph: {context2}; named graph: {}
		query.setDataset(dataset);
		assertFalse(query.evaluate());

		// default graph: {context1, context2}; named graph: {context2}
		dataset.addDefaultGraph(context1);
		dataset.addNamedGraph(context2);
		query.setDataset(dataset);
		assertFalse(query.evaluate());

		// default graph: {context1, context2}; named graph: {context1,
		// context2}
		dataset.addNamedGraph(context1);
		query.setDataset(dataset);
		assertTrue(query.evaluate());
	}
	
	public void testGetStatements() throws Exception {
		testCon.add(bob, name, nameBob);

		assertTrue("Repository should contain statement", testCon.hasStatement(
				bob, name, nameBob, false));

		RepositoryResult<Statement> result = testCon.getStatements(null, name,
				null, false);

		try {
			assertTrue("Iterator should not be null", result != null);
			assertTrue("Iterator should not be empty", result.hasNext());

			while (result.hasNext()) {
				Statement st = result.next();
				//assertNull("Statement should not be in a context ", st.getContext());  // bug18178
				assertTrue("Statement predicate should be equal to name ", st
						.getPredicate().equals(name));
			}
		} finally {
			result.close();
		}

		List<Statement> list = Iterations.addAll(testCon.getStatements(null,
				name, null, false), new ArrayList<Statement>());

		assertTrue("List should not be null", list != null);
		assertFalse("List should not be empty", list.isEmpty());
	}

	public void testGetStatementsInSingleContext() throws Exception {
		//testCon.setAutoCommit(false);
		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);
		testCon.setAutoCommit(true);

		assertTrue("Repository should contain statement", testCon.hasStatement(
				bob, name, nameBob, false));

		assertTrue("Repository should contain statement in context1", testCon
				.hasStatement(bob, name, nameBob, false, context1));

		assertFalse("Repository should not contain statement in context2",
				testCon.hasStatement(bob, name, nameBob, false, context2));

		// Check handling of getStatements without context IDs
		RepositoryResult<Statement> result = testCon.getStatements(bob, name,
				null, false);
		try {
			while (result.hasNext()) {
				Statement st = result.next();
				assertTrue(bob.equals(st.getSubject()));
				assertTrue(name.equals(st.getPredicate()));
				assertTrue(nameBob.equals(st.getObject()));
				assertTrue(context1.equals(st.getContext()));
			}
		} finally {
			result.close();
		}

		// Check handling of getStatements with a known context ID
		result = testCon.getStatements(null, null, null, false, context1);
		try {
			while (result.hasNext()) {
				Statement st = result.next();
				assertTrue(context1.equals(st.getContext()));
			}
		} finally {
			result.close();
		}

		// Check handling of getStatements with an unknown context ID
		result = testCon.getStatements(null, null, null, false, unknownContext);
		try {
			assertTrue(result != null);
			assertFalse(result.hasNext());
		} finally {
			result.close();
		}

		List<Statement> list = Iterations.addAll(testCon.getStatements(null,
				name, null, false, context1), new ArrayList<Statement>());

		assertTrue("List should not be null", list != null);
		assertFalse("List should not be empty", list.isEmpty());
	}

	public void testGetStatementsInMultipleContexts() throws Exception {
		testCon.clear();

		//testCon.setAutoCommit(false);
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);
		testCon.setAutoCommit(true);

		// get statements with either no context or context2
		CloseableIteration<? extends Statement, RepositoryException> iter = testCon
				.getStatements(null, null, null, false, null, context2);

		try {
			int count = 0;
			while (iter.hasNext()) {
				count++;
				Statement st = iter.next();

				assertTrue(st.getContext() == null 	// FIXME: need bug18178
						|| context2.equals(st.getContext()));
			}

			assertEquals("there should be three statements", 3, count);
		} finally {
			iter.close();
		}

		// get all statements with context1 or context2. Note that context1 and
		// context2 are both known
		// in the store because they have been created through the store's own
		// value vf.
		iter = testCon.getStatements(null, null, null, false, context1,
				context2);

		try {
			int count = 0;
			while (iter.hasNext()) {
				count++;
				Statement st = iter.next();
				// we should have _only_ statements from context2
				assertTrue(context2.equals(st.getContext()));
			}
			assertEquals("there should be two statements", 2, count);
		} finally {
			iter.close();
		}

		// get all statements with unknownContext or context2.
		iter = testCon.getStatements(null, null, null, false, unknownContext,
				context2);

		try {
			int count = 0;
			while (iter.hasNext()) {
				count++;
				Statement st = iter.next();
				// we should have _only_ statements from context2
				assertTrue(context2.equals(st.getContext()));
			}
			assertEquals("there should be two statements", 2, count);
		} finally {
			iter.close();
		}

		// add statements to context1
		//testCon.setAutoCommit(false);
		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);
		testCon.setAutoCommit(true);

		iter = testCon.getStatements(null, null, null, false, context1);
		try {
			assertTrue(iter != null);
			assertTrue(iter.hasNext());
		} finally {
			iter.close();
		}

		// get statements with either no context or context2
		iter = testCon.getStatements(null, null, null, false, null, context2);
		try {
			int count = 0;
			while (iter.hasNext()) {
				count++;
				Statement st = iter.next();
				// we should have _only_ statements from context2, or without
				// context
				assertTrue(st.getContext() == null	// FIXME: need bug18178
						|| context2.equals(st.getContext()));
			}
			assertEquals("there should be four statements", 4, count);
		} finally {
			iter.close();
		}

		// get all statements with context1 or context2
		iter = testCon.getStatements(null, null, null, false, context1,
				context2);

		try {
			int count = 0;
			while (iter.hasNext()) {
				count++;
				Statement st = iter.next();
				assertTrue(context1.equals(st.getContext())
						|| context2.equals(st.getContext()));
			}
			assertEquals("there should be four statements", 4, count);
		} finally {
			iter.close();
		}
	}

	public void testRemoveStatements() throws Exception {
		//testCon.setAutoCommit(false);
		testCon.add(bob, name, nameBob);
		testCon.add(alice, name, nameAlice);
		testCon.setAutoCommit(true);

		assertTrue(testCon.hasStatement(bob, name, nameBob, false));
		assertTrue(testCon.hasStatement(alice, name, nameAlice, false));

		testCon.remove(bob, name, nameBob);

		assertFalse(testCon.hasStatement(bob, name, nameBob, false));
		assertTrue(testCon.hasStatement(alice, name, nameAlice, false));

		testCon.remove(alice, null, null);
		assertFalse(testCon.hasStatement(alice, name, nameAlice, false));
		assertTrue(testCon.isEmpty());
	}

	public void testRemoveStatementCollection() throws Exception {
		//testCon.setAutoCommit(false);
		testCon.add(alice, name, nameAlice);
		testCon.add(bob, name, nameBob);
		testCon.setAutoCommit(true);

		assertTrue(testCon.hasStatement(bob, name, nameBob, false));
		assertTrue(testCon.hasStatement(alice, name, nameAlice, false));

		Collection<Statement> c = Iterations.addAll(testCon.getStatements(null,
				null, null, false), new ArrayList<Statement>());

		testCon.remove(c);

		assertFalse(testCon.hasStatement(bob, name, nameBob, false));
		assertFalse(testCon.hasStatement(alice, name, nameAlice, false));
	}

	public void testRemoveStatementIteration() throws Exception {
		//testCon.setAutoCommit(false);
		testCon.add(alice, name, nameAlice);
		testCon.add(bob, name, nameBob);
		testCon.setAutoCommit(true);

		assertTrue(testCon.hasStatement(bob, name, nameBob, false));
		assertTrue(testCon.hasStatement(alice, name, nameAlice, false));

		CloseableIteration<? extends Statement, RepositoryException> iter = testCon
				.getStatements(null, null, null, false);

		try {
			testCon.remove(iter);
		} finally {
			iter.close();
		}

		assertFalse(testCon.hasStatement(bob, name, nameBob, false));
		assertFalse(testCon.hasStatement(alice, name, nameAlice, false));

	}
	
	public void testAddReader() throws Exception {
		InputStream defaultGraphStream = new FileInputStream(TEST_DIR_PREFIX + "default-graph.nt");
		Reader defaultGraph = new InputStreamReader(defaultGraphStream, "UTF-8");

		testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);

		defaultGraph.close();

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		// add file graph1.ttl to context1
		InputStream graph1Stream = new FileInputStream(TEST_DIR_PREFIX + "graph1.nt");
		Reader graph1 = new InputStreamReader(graph1Stream, "UTF-8");

		try {
			testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
		} finally {
			graph1.close();
		}

		// add file graph2.ttl to context2
		InputStream graph2Stream = new FileInputStream(TEST_DIR_PREFIX + "graph2.nt");
		Reader graph2 = new InputStreamReader(graph2Stream, "UTF-8");

		try {
			testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
		} finally {
			graph2.close();
		}

		assertTrue("alice should be known in the store", testCon.hasStatement(
				null, name, nameAlice, false));

		assertFalse("alice should not be known in context1", testCon
				.hasStatement(null, name, nameAlice, false, context1));
		assertTrue("alice should be known in context2", testCon.hasStatement(
				null, name, nameAlice, false, context2));

		assertTrue("bob should be known in the store", testCon.hasStatement(
				null, name, nameBob, false));

		assertFalse("bob should not be known in context2", testCon
				.hasStatement(null, name, nameBob, false, context2));
		assertTrue("bib should be known in context1", testCon.hasStatement(
				null, name, nameBob, false, context1));

	}

	public void testAddInputStream() throws Exception {
		// add file default-graph.ttl to repository, no context
		InputStream defaultGraph = new FileInputStream(TEST_DIR_PREFIX + "default-graph.nt");

		try {
			testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
		} finally {
			defaultGraph.close();
		}

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		// add file graph1.ttl to context1
		InputStream graph1 = new FileInputStream(TEST_DIR_PREFIX + "graph1.nt");

		try {
			testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
		} finally {
			graph1.close();
		}

		// add file graph2.ttl to context2
		InputStream graph2 = new FileInputStream(TEST_DIR_PREFIX + "graph2.nt");

		try {
			testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
		} finally {
			graph2.close();
		}

		assertTrue("alice should be known in the store", testCon.hasStatement(
				null, name, nameAlice, false));

		assertFalse("alice should not be known in context1", testCon
				.hasStatement(null, name, nameAlice, false, context1));
		assertTrue("alice should be known in context2", testCon.hasStatement(
				null, name, nameAlice, false, context2));

		assertTrue("bob should be known in the store", testCon.hasStatement(
				null, name, nameBob, false));

		assertFalse("bob should not be known in context2", testCon
				.hasStatement(null, name, nameBob, false, context2));
		assertTrue("bib should be known in context1", testCon.hasStatement(
				null, name, nameBob, false, context1));

	}

	public void testRecoverFromParseError() throws RepositoryException,
			IOException {
		String invalidData = "bad";

		try {
			testCon.add(new StringReader(invalidData), "", RDFFormat.NTRIPLES);
			fail("Invalid data should result in an exception");
		} catch (RDFParseException e) {
			// Expected behaviour
		}
	}
	
	@Ignore @Test public void testStatementSerialization() throws Exception {
		testCon.add(bob, name, nameBob);

		Statement st;
		RepositoryResult<Statement> statements = testCon.getStatements(null,
				null, null, true);
		try {
			st = statements.next();
		} finally {
			statements.close();
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(st);
		out.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bais);
		Statement deserializedStatement = (Statement) in.readObject();
		in.close();

		assertTrue(st.equals(deserializedStatement));

		assertTrue(testCon.hasStatement(st, true));
		assertTrue(testCon.hasStatement(deserializedStatement, true));
	}

	public void testAddRemove() throws Exception {
		URI FOAF_PERSON = vf.createURI("http://xmlns.com/foaf/0.1/Person");
		final Statement stmt = vf.createStatement(bob, name, nameBob);

		testCon.add(bob, RDF.TYPE, FOAF_PERSON);

		//testCon.setAutoCommit(false);
		testCon.add(stmt);
		testCon.remove(stmt);
		testCon.commit();

		testCon.exportStatements(null, null, null, false, new RDFHandlerBase() {

			@Override
			public void handleStatement(Statement st)
					throws RDFHandlerException {
				assertTrue(!stmt.equals(st));
			}
		});
	}

	public void testGetContextIDs() throws Exception {
		assertEquals(0, testCon.getContextIDs().asList().size());

		// load data
		//testCon.setAutoCommit(false);
		testCon.add(bob, name, nameBob, context1);
		assertEquals(Arrays.asList(context1), testCon.getContextIDs().asList());

		testCon.remove(bob, name, nameBob, context1);
		assertEquals(0, testCon.getContextIDs().asList().size());
		testCon.setAutoCommit(true);

		assertEquals(0, testCon.getContextIDs().asList().size());

		testCon.add(bob, name, nameBob, context2);
		assertEquals(Arrays.asList(context2), testCon.getContextIDs().asList());
	}

}

