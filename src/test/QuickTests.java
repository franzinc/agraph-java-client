/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static test.Stmt.statementSet;
import static test.Stmt.stmts;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Category;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

import test.TestSuites.NonPrepushTest;

import com.franz.agraph.http.AGDecoder;
import com.franz.agraph.http.AGDeserializer;
import com.franz.agraph.http.AGEncoder;
import com.franz.agraph.http.AGSerializer;
import com.franz.agraph.repository.AGCustomStoredProcException;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;

@SuppressWarnings("deprecation")
public class QuickTests extends AGAbstractTest {

    @RunWith(Categories.class)
    @ExcludeCategory(NonPrepushTest.class)
    @SuiteClasses( { QuickTests.class })
    public static class Prepush {}

    @RunWith(Categories.class)
    @IncludeCategory(TestSuites.Broken.class)
    @SuiteClasses( { QuickTests.class })
    public static class Broken {}

    public static final String NS = "http://franz.com/test/";

    @Test
    @Category(TestSuites.Prepush.class)
    public void bnode() throws Exception {
        assertEquals("size", 0, conn.size());
        BNode s = vf.createBNode();
        URI p = vf.createURI(NS, "a");
        Literal o = vf.createLiteral("aaa");
        conn.add(s, p, o);
        assertEquals("size", 1, conn.size());
        assertSetsEqual("a", stmts(new Stmt(null, p, o)),
                Stmt.dropSubjects(statementSet(conn.getStatements(s, p, o, true))));
        RepositoryResult<Statement> statements = conn.getStatements(null, null, null, false);
        Statement st = statements.next();
        System.out.println(new Stmt(st));
        AGAbstractTest.assertSetsEqual("",
                Stmt.stmts(new Stmt(st)),
                Stmt.statementSet(conn.getStatements(s, st.getPredicate(), st.getObject(), false)));
        AGAbstractTest.assertSetsEqual("",
                Stmt.stmts(new Stmt(st)),
                Stmt.statementSet(conn.getStatements(st.getSubject(), st.getPredicate(), st.getObject(), false)));
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void bnode_rfe9776() throws Exception {
    	URI orderedCollection = vf.createURI("http://lumas#orderedCollection");
    	URI property = vf.createURI("http://lumas#hasId");
    	BNode node = vf.createBNode("newId");
    	
    	conn.add(orderedCollection, property, node);
    	
    	RepositoryResult<Statement> result = closeLater( conn.getStatements(null, null, null, false));
    	BNode bnode = null;
    	if (result.hasNext()) {
    		Statement st = result.next();
    		bnode = (BNode) st.getObject();
    		Assert.assertNotNull(st);
    	} else {
    		fail("expected 1 result");
    	}
    	Assert.assertNotNull(bnode);
    	
    	// load triple from blank-node
    	result = closeLater( conn.getStatements(null, null, bnode, false));
    	if (result.hasNext()){
    		Statement st = result.next();
    		Assert.assertNotNull(st);
    	} else {
    		fail("no triple found!");
    	}
    }

    /**
     * Simplified from tutorial example13 to show the error.
     * Example13 now has a workaround: setting the prefix in the sparql query.
     * Namespaces are cleared in setUp(), otherwise the first errors don't happen.
     * After the (expected) failure for xxx, setting the ont namespace
     * does not hold, so the query with ont fails.
     */
    @Test
    @Category(TestSuites.Broken.class)
    public void namespaceAfterError() throws Exception {
        URI alice = vf.createURI("http://example.org/people/alice");
        URI name = vf.createURI("http://example.org/ontology/name");
        Literal alicesName = vf.createLiteral("Alice");
        conn.add(alice, name, alicesName);
        try {
            conn.prepareBooleanQuery(QueryLanguage.SPARQL,
            "ask { ?s xxx:name \"Alice\" } ").evaluate();
            fail("");
        } catch (Exception e) {
            // expected
            //e.printStackTrace();
        }
        conn.setNamespace("ont", "http://example.org/ontology/");
        assertTrue("Boolean result",
                conn.prepareBooleanQuery(QueryLanguage.SPARQL,
                "ask { ?s ont:name \"Alice\" } ").evaluate());
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void bulkDelete() throws Exception {
        URI alice = vf.createURI("http://example.org/people/alice");
        URI firstname = vf.createURI("http://example.org/ontology/firstname");
        URI lastname = vf.createURI("http://example.org/ontology/lastname");
        Literal alicesName = vf.createLiteral("Alice");
        List<Statement> input = new ArrayList<Statement>();
        input.add(vf.createStatement(alice, firstname, alicesName));
        input.add(vf.createStatement(alice, lastname, alicesName));
        conn.add(input);
        assertEquals("size", 2, conn.size());
        conn.remove(input);
        assertEquals("size", 0, conn.size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void openRepo_rfe9837() throws Exception {
    	AGRepository repo2 = closeLater( cat.openRepository(repo.getRepositoryID()));
    	AGRepositoryConnection conn2 = closeLater( repo2.getConnection());
    	assertEquals("size", conn.size(), conn2.size());
    	assertEquals("id", repo.getRepositoryID(), repo2.getRepositoryID());
    	try {
    		AGRepository repo3 = closeLater( cat.createRepository(repo.getRepositoryID(), true));
    		fail("strict should cause an exception: " + repo3.getRepositoryURL());
    	} catch (RepositoryException e) {
    		if (e.getMessage().contains("There is already a repository")) {
    			// good
    		} else {
    			throw e;
    		}
    	}
    	try {
    		AGRepository repo3 = closeLater( cat.openRepository("no-such-repo"));
    		fail("should not exist: " + repo3.getRepositoryURL());
    	} catch (RepositoryException e) {
    		if (e.getMessage().contains("Repository not found with ID:")) {
    			// good
    		} else {
    			throw e;
    		}
    	}
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void storedProcs_encoding_rfe10189() throws Exception {
		byte[][] cases = {{ 1, 3, 32, 11, 13, 123},
				{},
				{33},
				{33, 44},
				{33, 44, 55},
				{33, 44, 55, 66},
				{33, 44, 55, 66, 77},
				{33, 44, 55, 66, 77, 88},
				{-1, -2, -3, -4, -5},
				{-1, -2, -3, -4}
		};
		
		for (int casenum = 0 ; casenum < cases.length; casenum++){
			byte[] input = cases[casenum];
			String encoded = AGEncoder.encode(input);
			byte[] result = AGDecoder.decode(encoded);
			assertSetsEqual("encoding", input, result);
		}
	}
	
    /**
     * Example class of how a user might wrap a stored-proc for convenience.
     */
    class SProcTest {
    	static final String FASL = "ag-test-stored-proc.fasl";
    	
		private final AGRepositoryConnection conn;
    	SProcTest(AGRepositoryConnection conn) {
			this.conn = conn;
    	}
		
		String addTwoStrings(String a, String b) throws Exception {
			return (String) conn.callStoredProc("add-two-strings", FASL, a, b);
		}
		
		int addTwoInts(int a, int b) throws Exception {
	    	return (Integer) conn.callStoredProc("add-two-ints", FASL, a, b);
		}
		
		String addTwoVecStrings(String a, String b) throws Exception {
			return (String) conn.callStoredProc("add-two-vec-strings", FASL, a, b);
		}
		
		String addTwoVecStringsError() throws Exception {
			return (String) conn.callStoredProc("add-two-vec-strings", FASL);
		}
		
		int addTwoVecInts(int a, int b) throws Exception {
	    	return (Integer) conn.callStoredProc("add-two-vec-ints", FASL, a, b);
		}
		
		Object bestBeNull(String a) throws Exception {
	    	return conn.callStoredProc("best-be-nil", FASL, a);
		}
		
		Object returnAllTypes() throws Exception {
	    	return conn.callStoredProc("return-all-types", FASL);
		}
		
		Object identity(Object input) throws Exception {
	    	return conn.callStoredProc("identity", FASL, input);
		}
		
		Object checkAllTypes(Object input) throws Exception {
	    	return conn.callStoredProc("check-all-types", FASL, input);
		}
		
		Object addATripleInt(int i) throws Exception {
	    	return conn.callStoredProc("add-a-triple-int", FASL, i);
		}
		
		Statement getATripleInt(int i) throws Exception {
			String r = (String) conn.callStoredProc("get-a-triple-int", FASL, i);
			Statement st = parseNtriples(r);
	    	return st;
		}

		private Statement parseNtriples(String ntriples) throws IOException,
				RDFParseException, RDFHandlerException {
			RDFParser parser = Rio.createParser(RDFFormat.NTRIPLES, vf);
			parser.setPreserveBNodeIDs(true);
			StatementCollector collector = new StatementCollector();
	    	parser.setRDFHandler(collector);
	    	parser.parse(new StringReader(ntriples), "http://example.com/");
	    	Statement st = collector.getStatements().iterator().next();
			return st;
		}
		
//		Object addATriple(Object s, Object p, Object o) throws Exception {
//	    	return conn.callStoredProc("add-a-triple", FASL, s, p, o);
//		}
		
    }
    
	static final Object ALL_TYPES = new Object[] {
		123,
		0,
		-123,
		"abc",
		null,
		new Integer[] {9, 9, 9, 9},
		Util.arrayList(123,0, -123, "abc"),
		new byte[] {0, 1, 2, 3, 4, 5, 6, 7}
	};

    @Test
    @Category(TestSuites.Prepush.class)
    public void encoding_all_types_rfe10189() throws Exception {
    	Object[] o = new Object[] {ALL_TYPES};
    	assertEqualsDeep("all types", o,
    			AGDeserializer.decodeAndDeserialize(AGSerializer.serializeAndEncode(o)));
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void storedProcsEncoded_rfe10189() throws Exception {
    	String response = (String) AGDeserializer.decodeAndDeserialize(
    			conn.getHttpRepoClient().callStoredProcEncoded("add-two-strings", SProcTest.FASL,
    					AGSerializer.serializeAndEncode(
    							new String[] {"123", "456"})));
    	assertEquals(579, Integer.parseInt(response));
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void storedProcs_rfe10189() throws Exception {
    	SProcTest sp = new SProcTest(conn);
    	assertEquals("supports strings", "579", sp.addTwoStrings("123", "456"));
    	assertEquals("supports pos int", 579, sp.addTwoInts(123, 456));
    	assertEquals("supports neg int and zero", 0, sp.addTwoInts(123, -123));
    	assertEquals("supports neg int", -100, sp.addTwoInts(23, -123));
    	assertEquals("supports whole arg-vec strings", "579", sp.addTwoVecStrings("123", "456"));
    	assertEquals("supports whole arg-vec ints", 579, sp.addTwoVecInts(123, 456));
    	assertEquals("supports null", null, sp.bestBeNull(null));
    	try {
        	assertEquals(null, sp.bestBeNull("abc"));
        	fail("should be AGCustomStoredProcException");
    	} catch (AGCustomStoredProcException e) {
    		assertEquals("test null and error", "I expected a nil, but got: abc", e.getMessage());
    	}
    	try {
    		assertEquals("579", sp.addTwoVecStringsError());
    		fail("should be AGCustomStoredProcException");
    	} catch (AGCustomStoredProcException e) {
    		assertEquals("test error", "wrong number of args", e.getMessage());
    	}
    	try {
    		assertEquals("579", sp.addTwoVecStrings(null, null));
    		fail("should be AGCustomStoredProcException");
    	} catch (AGCustomStoredProcException e) {
    		assertEquals("test null and error", "There is no integer in the string nil (:start 0 :end 0)", e.getMessage());
    	}
    	try {
    		assertEquals("579", sp.addTwoVecStrings("abc", "def"));
    		fail("should be AGCustomStoredProcException");
    	} catch (AGCustomStoredProcException e) {
    		assertEquals("test error", "There's junk in this string: \"abc\".", e.getMessage());
    	}
    	System.out.println(ALL_TYPES);
    	assertEqualsDeep("supports all types, originating from java", ALL_TYPES, sp.checkAllTypes(ALL_TYPES));
    	assertEqualsDeep("supports all types, round-trip", ALL_TYPES, sp.identity(ALL_TYPES));
    	assertEqualsDeep("supports all types, originating from lisp", ALL_TYPES, sp.returnAllTypes());
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void storedProcs_triples_rfe10189() throws Exception {
    	SProcTest sp = new SProcTest(conn);
    	Assert.assertNotNull("add-a-triple-int", sp.addATripleInt(1));
    	assertEqualsDeep("get-a-triple-int", Stmt.stmts(new Stmt(vf.createURI("http://test.com/add-a-triple-int"),
    			vf.createURI("http://test.com/p"), vf.createLiteral(1))),
    			Stmt.stmts( new Stmt(sp.getATripleInt(1))));
    	// TODO: transferring triples does not work yet
    	// TODO: change the expected return value when that is known
    	//Assert.assertNotNull("add-a-triple", sp.addATriple(vf.createURI("http://test.com/s"), vf.createURI("http://test.com/p"), vf.createURI("http://test.com/p")));
    }

}
