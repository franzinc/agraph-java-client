/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGServer;
import org.junit.Test;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.openrdf.model.Statement;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import test.RepositoryConnectionTest.RepositoryConnectionTests;
import test.TestSuites.NonPrepushTest;

import com.franz.agraph.repository.AGRDFFormat;
import com.franz.agraph.repository.AGRepositoryConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;


public class AGRepositoryConnectionTests extends RepositoryConnectionTests {
    
    @RunWith(Categories.class)
    @ExcludeCategory(NonPrepushTest.class)
    @SuiteClasses( { AGRepositoryConnectionTests.class })
    public static class Prepush {}

    @RunWith(Categories.class)
    @IncludeCategory(TestSuites.Broken.class)
    @SuiteClasses( { AGRepositoryConnectionTests.class })
    public static class Broken {}

    protected Repository createRepository() throws Exception {
        AGServer server = new AGServer(AGAbstractTest.findServerUrl(), AGAbstractTest.username(), AGAbstractTest.password());
        AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
        AGRepository repo = catalog.createRepository("testRepo1");
        return repo;
    }

    @Test
	public void testHasStatementWithoutBNodes() throws Exception {
		testCon.add(name, name, nameBob);

		assertTrue("Repository should contain newly added statement", testCon
				.hasStatement(name, name, nameBob, false));
	}

    @Test
	public void testHasStatementWithBNodes() throws Exception {
		testCon.add(bob, name, nameBob);

		assertTrue("Repository should contain newly added statement", testCon
				.hasStatement(bob, name, nameBob, false));

	}

	// Add statements to the repository from STREAM. STREAM contains data formatted
	// as FORMAT.
	public void addInputStream(InputStream stream, RDFFormat format) throws Exception {
		testCon.add(stream, "", format);
	}

	// Add statements to the repository from the file pointed to by SOURCE. SOUCE
	// contains data formatted as FORMAT.
	public void addInputFile(File source, RDFFormat format) throws Exception {
		try (InputStream stream = new FileInputStream(source)) {
			addInputStream(stream, format);
		}
	}

	public File createTempGzipFileFrom(File from) throws IOException {
		File gz = File.createTempFile(from.getName() + ".", ".gz");
		gz.deleteOnExit();
		Util.gzip(from,gz);
		return gz;
	}

	public File createTempZipFileFrom(File from) throws IOException {
		File zip = File.createTempFile(from.getName() + ".", ".zip");
		zip.deleteOnExit();
		Util.zip(from,zip);
		return zip;
	}

    @Test
	public void testAddGzipInputStreamNTriples() throws Exception {
	    // add file default-graph.nt.gz to repository, no context
	    File nt = Util.resourceAsTempFile(TEST_DIR_PREFIX + "default-graph.nt");
	    addInputFile(createTempGzipFileFrom(nt), RDFFormat.NTRIPLES);

	    assertTrue("Repository should contain newly added statements", testCon
		       .hasStatement(null, publisher, nameBob, false));
	    assertTrue("Repository should contain newly added statements", testCon
		       .hasStatement(null, publisher, nameAlice, false));
	}

    @Test
	public void testAddZipFileNTriples() throws Exception {
		InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "graphs.nt.zip");
		addInputStream(in, RDFFormat.NTRIPLES);
		
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		assertTrue("alice should be known in the store", testCon.hasStatement(
				null, name, nameAlice, false));

		assertTrue("bob should be known in the store", testCon.hasStatement(
				null, name, nameBob, false));
	}

    @Test
	public void testAddReaderNTriples() throws Exception {
		InputStream defaultGraphStream = Util.resourceAsStream(TEST_DIR_PREFIX
				+ "default-graph.nt");

		try (Reader defaultGraph = new InputStreamReader(defaultGraphStream, "UTF-8")) {
			testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
		}

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		// add file graph1.nt to context1
		InputStream graph1Stream = Util.resourceAsStream(TEST_DIR_PREFIX
				+ "graph1.nt");
		try (Reader graph1 = new InputStreamReader(graph1Stream, "UTF-8")) {
			testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
		}

		// add file graph2.nt to context2
		InputStream graph2Stream = Util.resourceAsStream(TEST_DIR_PREFIX
				+ "graph2.nt");
		try (Reader graph2 = new InputStreamReader(graph2Stream, "UTF-8")) {
			testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
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

    @Test
	public void testAddInputStreamNTriples() throws Exception {
	        // add file default-graph.nt to repository, no context

	        try (InputStream defaultGraph =
		     Util.resourceAsStream(TEST_DIR_PREFIX + "default-graph.nt")) {
			testCon.add(defaultGraph, "", RDFFormat.NTRIPLES);
		}
	    
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		// add file graph1.nt to context1
		try (InputStream graph1 = Util.resourceAsStream(TEST_DIR_PREFIX + "graph1.nt")) {
			testCon.add(graph1, "", RDFFormat.NTRIPLES, context1);
		}

		// add file graph2.nt to context2
		try (InputStream graph2 = Util.resourceAsStream(TEST_DIR_PREFIX + "graph2.nt")) {
			testCon.add(graph2, "", RDFFormat.NTRIPLES, context2);
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

    @Test
	public void testRecoverFromParseErrorNTriples() throws RepositoryException,
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

	@Test
	public void testImportAllSupportedFormats() throws Exception {

		class SampleInput {
			public String file;
			public RDFFormat type;

			public SampleInput(RDFFormat format, String sampleFile) {
				type = format;
				file = sampleFile;
			}
		}

		AGRepository repo = (AGRepository)createRepository();
		AGRepositoryConnection conn = repo.getConnection();

		SampleInput[] formats = { new SampleInput(RDFFormat.NTRIPLES, "default-graph.nt"),
					  new SampleInput(RDFFormat.NQUADS, "example.nq"),
					  // import via 'application/trig'
					  new SampleInput(AGRDFFormat.TRIG, "sample.trig"),
					  // import via 'application/x-trig'
					  new SampleInput(RDFFormat.TRIG, "sample.trig"),
					  new SampleInput(RDFFormat.TRIX, "sample.trix"),
					  new SampleInput(RDFFormat.TURTLE, "default-graph.ttl"),
					  new SampleInput(RDFFormat.RDFXML, "tutorial-test8-expected.rdf"),
					  new SampleInput(AGRDFFormat.NQX, "sample.nqx")
		};

		for(SampleInput format: formats) {
			// delete all triples.
			conn.remove((Resource)null, (URI)null, (Value)null, (Resource)null);
			// Import data file. If no exception is thrown, we consider the test successful
			try(InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + format.file)) {
				addInputStream(in, format.type);
			}
		}

	}

    @Test
	public void testSimpleTupleQuerySparql() throws Exception {
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

    @Test
	public void testSimpleTupleQueryUnicodeSparql() throws Exception {
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

    @Test
	public void testPreparedTupleQuerySparql() throws Exception {
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

    @Test
	public void testPreparedTupleQueryUnicodeSparql() throws Exception {
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

    @Test
	public void testSimpleGraphQuerySparql() throws Exception {
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

    @Test
	public void testPreparedGraphQuerySparql() throws Exception {
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

    @Test
    public void testBegin() throws Exception {    	
    	assertFalse(testCon.isActive());
    	testCon.begin();
    	assertTrue(testCon.isActive());
    	testCon.add(bob, name, nameBob);
    	testCon.commit();
    	// Sesame 2.6 transaction semantics are inverse of Sesame 2.7 transaction semantics.
    	// Therefore, this test as it passes for 2.6 will fail in 2.7
    	// So I have commented out below assert statement
    	// assertFalse(testCon.isActive());
   	}
    
    @Test
   	public void testBeginRollback() throws Exception {
    	assertFalse(testCon.isActive());
    	testCon.begin();
    	assertTrue(testCon.isActive());
    	testCon.add(bob, name, nameBob);
    	testCon.rollback();  
    	// Sesame 2.6 transaction semantics are inverse of Sesame 2.7 transaction semantics.
    	// Therefore, this test as it passes for 2.6 will fail in 2.7
    	// So I have commented out below assert statement
    	// assertFalse(testCon.isActive());
    }
            
    @Test
   	public void testisActive() throws Exception {
    	assertFalse(testCon.isActive());
    	testCon.begin();
    	assertTrue(testCon.isActive());
    	testCon.add(bob, name, nameBob);
    	testCon.commit();
    	// Sesame 2.6 transaction semantics are inverse of Sesame 2.7 transaction semantics.
    	// Therefore, this test as it passes for 2.6 will fail in 2.7
    	// So I have commented out below assert statement
    	// assertFalse(testCon.isActive());   		
   	}
    
    @Test
	public void testAddFile() throws Exception {
		// add file default-graph.nt to repository, no context
		Util.add(testCon, TEST_DIR_PREFIX + "default-graph.nt", "", RDFFormat.NTRIPLES);

		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameBob, false));
		assertTrue("Repository should contain newly added statements", testCon
				.hasStatement(null, publisher, nameAlice, false));

		// add file graph1.nt to context1
		String graph1 = TEST_DIR_PREFIX + "graph1.nt";
		
		
		Util.add(testCon, graph1, "", RDFFormat.NTRIPLES, context1);
		
		
		// add file graph2.nt to context2
		String graph2 = TEST_DIR_PREFIX + "graph2.nt";

		
		Util.add(testCon, graph2, "", RDFFormat.NTRIPLES, context2);
		

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
    
    @Test
    public void testAddNQXFile() throws Exception {
    	// add file sample.nqx to repository, no context
    	
    	AGRepository repo = (AGRepository)createRepository();
    	AGRepositoryConnection conn = repo.getConnection();
    	
    	// define all attributes used in the import data set.
    	conn.new AttributeDefinition("color").add();
    	
    	try (final InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "sample.nqx")) {
    		conn.add(in, null, AGRDFFormat.NQX);
    	}
    }
    
    @Test
    public void testClientImportWithAttributes() throws Exception {
    	AGRepository repo = (AGRepository)createRepository();
    	AGRepositoryConnection conn = repo.getConnection();
    	
    	// assume attributes already defined.
    	
    	// load ntriples w/ default attributes
    	try (InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "default-graph.nt")) {
    		conn.add(in, null, RDFFormat.NTRIPLES, new JSONObject("{ color: blue }"));
    	}
    	
    	// load turtle w/ default attributes
    	try (InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "default-graph.ttl")) {
    		conn.add(in, null, RDFFormat.TURTLE, new JSONObject("{ color: green }"));
    	}
    	
    	conn.clear();
    	
    	// load turtle w/ default attributes
    	try (InputStream in = Util.resourceAsStream(TEST_DIR_PREFIX + "rdftransaction.xml")) {
    		conn.sendRDFTransaction(in, new JSONObject("{ color: red }"));
    	}
    }
    

    // Test rollback via the x-rollback request header.
    // See RepositoryConnectionTest#testSizeRollback() for related tests.
    @Test
    public void testSizeXRollback() throws Exception {
        AGRepository repo = (AGRepository)createRepository();
	AGRepositoryConnection conn = repo.getConnection();
		
        assertEquals(0, conn.size());
        conn.setAutoCommit(false);
	conn.add(bob, name, nameBob);
        assertEquals(1, conn.size());
	conn.getHttpRepoClient().setSendRollbackHeader(true);
	assertEquals(0, conn.size());
	conn.getHttpRepoClient().setSendRollbackHeader(false);
	conn.add(bob, name, nameBob);
	assertEquals(1, conn.size());
	conn.rollback();
    }
}
