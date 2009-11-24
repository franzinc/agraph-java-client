package test;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGServer;

public class AGRepositoryConnectionTest extends RepositoryConnectionTest {

	public String TEST_DIR_PREFIX = System.getProperty("com.franz.agraph.test.dataDir", System.getProperty("user.dir") + System.getProperty("file.separator"));
	public String SERVER_URL = System.getProperty("com.franz.agraph.test.serverURL","http://localhost:10035");
	public String USERNAME = System.getProperty("com.franz.agraph.test.username","test");
	public String PASSWORD = System.getProperty("com.franz.agraph.test.password","xyzzy");

	int repoNumber = 2;

	public AGRepositoryConnectionTest(String name) {
		super(name);
	}

	protected Repository createRepository() throws Exception {
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
		AGCatalog catalog = server.getRootCatalog();
		AGRepository repo = catalog.createRepository("testRepo" + repoNumber);
		return repo;
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

	public void testAddGzipInputStream() throws Exception {
		// add file default-graph.nt.gz to repository, no context
		InputStream defaultGraph = new FileInputStream(TEST_DIR_PREFIX + "default-graph.nt.gz");
		//RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "default-graph.nt.gz");
		try {
			testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
		} finally {
			defaultGraph.close();
		}

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

	}

	public void testAddZipFile() throws Exception {
		InputStream in = new FileInputStream(TEST_DIR_PREFIX + "graphs.zip");

		testCon.add(in, "", RDFFormat.NTRIPLES);

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		assertTrue("alice should be known in the store", testCon.hasStatement(
				null, name, nameAlice, false));

		assertTrue("bob should be known in the store", testCon.hasStatement(
				null, name, nameBob, false));
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
		testCon.add(alexander, name, Александър);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?person");
		queryBuilder.append(" WHERE { ?person foaf:name '");
		queryBuilder.append(Александър.getLabel()).append("' .}");

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
		query.setBinding("name", nameBob);

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
		testCon.add(alexander, name, Александър);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?person");
		queryBuilder.append(" WHERE { ?person foaf:name '");
		queryBuilder.append(Александър.getLabel()).append("' .}");

		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL,
				queryBuilder.toString());
		query.setBinding("name", Александър);

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

	public void testAddReader() throws Exception {
		InputStream defaultGraphStream = new FileInputStream(TEST_DIR_PREFIX
				+ "default-graph.nt");
		Reader defaultGraph = new InputStreamReader(defaultGraphStream, "UTF-8");

		testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);

		defaultGraph.close();

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		// add file graph1.nt to context1
		InputStream graph1Stream = new FileInputStream(TEST_DIR_PREFIX
				+ "graph1.nt");
		Reader graph1 = new InputStreamReader(graph1Stream, "UTF-8");

		try {
			testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
		} finally {
			graph1.close();
		}

		// add file graph2.nt to context2
		InputStream graph2Stream = new FileInputStream(TEST_DIR_PREFIX
				+ "graph2.nt");
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
		// add file default-graph.nt to repository, no context
		InputStream defaultGraph = new FileInputStream(TEST_DIR_PREFIX
				+ "default-graph.nt");

		try {
			testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
		} finally {
			defaultGraph.close();
		}

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		// add file graph1.nt to context1
		InputStream graph1 = new FileInputStream(TEST_DIR_PREFIX + "graph1.nt");

		try {
			testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
		} finally {
			graph1.close();
		}

		// add file graph2.nt to context2
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
		String validData = "<http://example.org/foo#a> <http://example.org/foo#b> <http://example.org/foo#c> .";

		try {
			testCon.add(new StringReader(invalidData), "", RDFFormat.NTRIPLES);
			fail("Invalid data should result in an exception");
		} catch (RDFParseException e) {
			// Expected behaviour
		}

		try {
			testCon.add(new StringReader(validData), "", RDFFormat.NTRIPLES);
		} catch (RDFParseException e) {
			fail("Valid data should not result in an exception");
		}

		assertEquals("Repository contains incorrect number of statements", 1,
				testCon.size());
	}

}
