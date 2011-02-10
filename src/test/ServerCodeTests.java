/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static test.Stmt.statementSet;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.Assert;

import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

import tutorial.TutorialExamples;

import com.franz.agraph.http.AGDecoder;
import com.franz.agraph.http.AGDeserializer;
import com.franz.agraph.http.AGEncoder;
import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGSerializer;
import com.franz.agraph.repository.AGCustomStoredProcException;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;

@SuppressWarnings("deprecation")
public class ServerCodeTests extends AGAbstractTest {
	
	static class AGServerCode {
		private final AGServer server;
		AGServerCode(AGServer server) {
			this.server = server;
		}
		
		private AGHTTPClient http() {
			return this.server.getHTTPClient();
		}
		
		public TupleQueryResult scripts() throws Exception {
	    	return http().getTupleQueryResult(server.getServerURL() + "/scripts");
		}

		public void putScript(String path, File script) throws Exception {
			http().put(server.getServerURL() + "/scripts/" + path, null, null,
					new FileRequestEntity(script, "text/plain"));
		}
		
		public TupleQueryResult initFile() throws Exception {
	    	return http().getTupleQueryResult(server.getServerURL() + "/initfile");
		}

		public void putInitFile(File script) throws Exception {
			http().put(server.getServerURL() + "/initfile", null, null,
					new FileRequestEntity(script, "text/plain"));
		}
		
		public void deleteInitFile() throws Exception {
	    	http().delete(server.getServerURL() + "/initfile", null, null);
		}

	}

	private static AGServerCode serverCode;

    @BeforeClass
    public static void installScripts() throws Exception {
    	serverCode = new AGServerCode(server);
    	serverCode.putScript(SProcTest.FASL, new File("src/test/ag-test-stored-proc.cl"));
    }

    @Test
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
    	static final String FASL = "ag-test-stored-proc.cl";
    	
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
    public void encoding_all_types_rfe10189() throws Exception {
    	Object[] o = new Object[] {ALL_TYPES};
    	assertEqualsDeep("all types", o,
    			AGDeserializer.decodeAndDeserialize(AGSerializer.serializeAndEncode(o)));
    }
    
    @Test
    public void storedProcsEncoded_rfe10189() throws Exception {
    	String response = (String) AGDeserializer.decodeAndDeserialize(
    			conn.getHttpRepoClient().callStoredProcEncoded("add-two-strings", SProcTest.FASL,
    					AGSerializer.serializeAndEncode(
    							new String[] {"123", "456"})));
    	assertEquals(579, Integer.parseInt(response));
    }
    
    @Test
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
    
    /**
     * @see TutorialExamples#example18()
     * @see TutorialTests#example18()
     */
    public TupleQuery rfe10256_setup() throws Exception{
        TutorialTests.example6_setup(conn, repo);
        conn.setNamespace("kdy", "http://www.franz.com/simple#");
        conn.setNamespace("rltv", "http://www.franz.com/simple#");
        // The rules are already loaded in initFile, so this line is commented:
        // conn.addRules(new FileInputStream("src/tutorial/java-rules.txt"));
        String queryString = 
        	"(select (?ufirst ?ulast ?cfirst ?clast)" +
            "(uncle ?uncle ?child)" +
            "(name ?uncle ?ufirst ?ulast)" +
            "(name ?child ?cfirst ?clast))";
    	return conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
    }

    @Test
    public void rfe10256_loadInitFile() throws Exception{
        serverCode.deleteInitFile(); // clean before test
        
    	serverCode.putInitFile(new File("src/tutorial/java-rules.txt"));
    	// note, setSessionLoadInitFile must be done before setAutoCommit in example6_setup
    	conn.setSessionLoadInitFile(true);
    	TupleQuery tupleQuery = rfe10256_setup();
        assertEquals(52, statementSet(tupleQuery.evaluate()).size());
    }
    
    @Test
    public void rfe10256_doNotPut_InitFile() throws Exception{
        serverCode.deleteInitFile(); // clean before test
        
    	TupleQuery tupleQuery = rfe10256_setup();
    	try {
        	tupleQuery.evaluate();
        	fail("expected QueryEvaluationException");
        } catch (QueryEvaluationException e) {
        	if (e.getMessage().contains("attempt to call `#:uncle/2' which is an undefined function. (500)")) {
        		// good
        	} else {
        		throw e;
        	}
        }
    }
    
    @Test
    public void rfe10256_doNotUse_InitFile() throws Exception{
        serverCode.deleteInitFile(); // clean before test
        
    	serverCode.putInitFile(new File("src/tutorial/java-rules.txt"));
    	TupleQuery tupleQuery = rfe10256_setup();
        try {
        	tupleQuery.evaluate();
        	fail("expected QueryEvaluationException");
        } catch (QueryEvaluationException e) {
        	if (e.getMessage().contains("attempt to call `#:uncle/2' which is an undefined function. (500)")) {
        		// good
        	} else {
        		throw e;
        	}
        }
    }
    
}
