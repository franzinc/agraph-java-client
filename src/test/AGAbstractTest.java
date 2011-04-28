/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static test.Util.ifBlank;
import static test.Util.or;
import static test.Util.readLines;
import info.aduna.iteration.CloseableIteration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.agraph.repository.AGAbstractRepository;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGValueFactory;

public class AGAbstractTest extends Closer {

    static public final String CATALOG_ID = "java-tutorial";

    public Logger log = LoggerFactory.getLogger(this.getClass());
    
    protected static AGServer server;
    protected static AGCatalog cat;
    protected AGRepository repo;
    protected AGRepositoryConnection conn;
    protected static String repoId;
    
    protected AGValueFactory vf;
    
    private static String serverUrl;
    
    private String testName = null;

    public static String findServerUrl() {
    	if (serverUrl == null) {
    		serverUrl = findServerUrl1();
    	}
    	return serverUrl;
    }

	private static String findServerUrl1() {
		String host = or(ifBlank(System.getenv("AGRAPH_HOST"), null),
				ifBlank(System.getProperty("AGRAPH_HOST"), null));
		String port = or(ifBlank(System.getenv("AGRAPH_PORT"), null),
				ifBlank(System.getProperty("AGRAPH_PORT"), null));
		
		if ((host == null || host.equals("localhost")) && port == null) {
			File portFile = new File("../agraph/lisp/agraph.port");
			try {
				host = "localhost";
				if (portFile.exists()) {
					System.out.println("Reading agraph.port: " + portFile.getAbsolutePath());
					port = readLines(portFile).get(0);
				} else {
					port = "10035";
				}
			} catch (Exception e) {
				throw new RuntimeException("Trying to read PortFile: " + portFile.getAbsolutePath(), e);
			}
		}
		
		return "http://" + or(host, "localhost") + ":" + or(port, "10035");
	}
    
    public static String username() {
        return or(System.getenv("AGRAPH_USER"), "test");
    }
    
    public static String password() {
        return or(System.getenv("AGRAPH_PASSWORD"), "xyzzy");
    }
    
    @BeforeClass
    public static void setUpOnce() {
    	String url = findServerUrl();
        try {
            server = new AGServer(url, username(), password());
            cat = server.getCatalog(CATALOG_ID);
            repoId = "javatest";
			cat.deleteRepository(repoId);
			cat.deleteRepository(repoId);

	        // test connection once
	        ping();
		} catch (Exception e) {
			throw new RuntimeException("server url: " + url, e);
		}
    }

	private static void ping() throws RepositoryException {
		AGRepository repo = cat.createRepository(repoId);
        try {
            repo.initialize();
            AGRepositoryConnection conn = repo.getConnection();
            try {
                conn.ping();
            } finally {
                Util.close(conn);
            }
        } finally {
            Util.close(repo);
        }
	}
	
	public void setTestName(String testName) {
		this.testName = testName;
		log = LoggerFactory.getLogger(this.getClass().getName() + "." + testName);
	}
	
	public String getTestName() {
		return testName;
	}
    
    @Before
    public void setUp() throws Exception {
        repo = cat.createRepository(repoId);
        closeLater(repo);
        repo.initialize();
        vf = repo.getValueFactory();
        conn = getConnection();
        conn.clear();
        conn.clearMappings();
        conn.clearNamespaces();
        // these are the default namespaces in AG, which are not present after clearNamespaces:
        conn.setNamespace("fti", "http://franz.com/ns/allegrograph/2.2/textindex/");
        conn.setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    }
    
    @After
    public void tearDown() throws Exception {
        vf = null;
        super.close();
        conn = null;
        repo = null;
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        cat = null;
        server = Util.close(server);
    }

    AGRepositoryConnection getConnection() throws RepositoryException {
        AGRepositoryConnection conn = repo.getConnection();
        closeLater(conn);
        return conn;
   }
   
    AGRepositoryConnection getConnection(AGAbstractRepository repo) throws RepositoryException {
        AGRepositoryConnection conn = repo.getConnection();
        closeLater(conn);
        return conn;
   }
   
    public static void assertSetsEqual(Collection expected, Set actual) {
        assertSetsEqual("", expected, actual);
    }
    
    public static void assertSetsEqual(String msg, byte[] expected, byte[] actual) {
        assertSetsEqual(msg, Util.toList(expected), Util.toList(actual));
    }

    public static void assertSetsEqual(String msg, Collection expected, Collection actual) {
        expected = new ArrayList(expected);
        actual = new ArrayList(actual);
        assertEquals(actual.toString(), expected.size(), actual.size());
        for (Iterator iter = expected.iterator(); iter.hasNext();) {
            Object exp = iter.next();
            boolean found = false;
            for (Iterator ait = actual.iterator(); ait.hasNext();) {
                Object act =ait.next();
                if (exp.equals(act)) {
                    found = true;
                    ait.remove();
                    break;
                }
            }
            assertTrue(msg + ". Not found: " + exp + " in " + actual, found);
        }
        assertEquals(msg + ". Remaining: " + actual, 0, actual.size());
    }
    
    public static void assertEqualsDeep(String msg, Object expected, Object actual) {
    	if (expected == null) {
    		Assert.assertEquals(msg, expected, actual);
    	} else if (actual == null) {
    		Assert.assertEquals(msg, expected, actual);
    	} else if (expected instanceof List) {
    		List expList = (List) expected;
    		Assert.assertTrue(msg + "; expected Collection type, actual: " + actual.getClass(), actual instanceof List);
    		List actList = (List) actual;
    		Assert.assertTrue(msg + "; expected same size=" + expList.size() + ", actual=" + actList.size(),
    				expList.size() == actList.size());
    		for (int i = 0; i < expList.size(); i++) {
				assertEqualsDeep("[" + i +"]" + msg, expList.get(i), actList.get(i));
			}
    	} else if (expected instanceof Object[]) {
    		Object[] expList = (Object[]) expected;
    		Assert.assertTrue(msg + "; expected Object[] type, actual: " + actual.getClass(), actual instanceof Object[]);
    		Object[] actList = (Object[]) actual;
    		Assert.assertTrue(msg + "; expected same size=" + expList.length + ", actual=" + actList.length,
    				expList.length == actList.length);
    		for (int i = 0; i < expList.length; i++) {
				assertEqualsDeep("[" + i +"]" + msg, expList[i], actList[i]);
			}
    	} else if (expected instanceof byte[]) {
    		byte[] expList = (byte[]) expected;
    		Assert.assertTrue(msg + "; expected byte[] type, actual: " + actual.getClass(), actual instanceof byte[]);
    		byte[] actList = (byte[]) actual;
    		Assert.assertTrue(msg + "; expected same size=" + expList.length + ", actual=" + actList.length,
    				expList.length == actList.length);
    		for (int i = 0; i < expList.length; i++) {
				assertEqualsDeep("[" + i +"]" + msg, expList[i], actList[i]);
			}
    	} else if (expected instanceof Set) {
    		assertSetsEqual(msg, (Set) expected, (Collection) actual);
    	} else {
    		assertEquals(msg, expected, actual);
    	}
    }
    
    public static void assertSetsSome(String msg, Collection expected, Collection actual) {
        for (Iterator ait = actual.iterator(); ait.hasNext();) {
            Object act = ait.next();
            boolean found = false;
            for (Iterator iter = expected.iterator(); iter.hasNext();) {
                Object exp =iter.next();
                if (exp.equals(act)) {
                    found = true;
                    break;
                }
            }
            assertTrue(msg + "; unexpected: " + act, found);
        }
    }
    
    public static void assertFiles(File expected, File actual) throws Exception {
        assertEquals("diff " + expected.getCanonicalPath() + " " + actual.getCanonicalPath(),
        		stripBlankNodes(readLines(expected)),
        		stripBlankNodes(readLines(actual)));
    }
    
    private static List<String> stripBlankNodes(List<String> strings) {
    	List<String> ret = new ArrayList<String>(strings.size());
    	for (String str : strings) {
    		String[] split = str.split("b........x.");
    		StringBuilder b = new StringBuilder();
    		for (int i = 0; i < split.length; i++) {
    			b.append(split[i]);
    		}
    		ret.add(b.toString());
		}
    	return ret;
	}

	public static Map mapKeep(Object[] keys, Map map) {
        Map ret = new HashMap();
        for (int i = 0; i < keys.length; i++) {
            ret.put(keys[i], map.get(keys[i]));
        }
        return ret;
    }
    
    public void addAll(Collection stmts, RepositoryConnection conn) throws RepositoryException {
        for (Iterator iter = stmts.iterator(); iter.hasNext();) {
            Statement st = (Statement) iter.next();
            conn.add(st);
        }
    }

    public static void println(Object x) {
    	System.out.println(x);
    }
    
    public static void printRows(CloseableIteration rows) throws Exception {
        while (rows.hasNext()) {
            println(rows.next());
        }
        Util.close(rows);
    }

    public static void printRows(String headerMsg, int limit, CloseableIteration rows) throws Exception {
        println(headerMsg);
        int count = 0;
        while (count < limit && rows.hasNext()) {
            println(rows.next());
            count++;
        }
        println("Number of results: " + count);
        Util.close(rows);
    }

//    static void close(RepositoryConnection conn) {
//        try {
//            conn.close();
//        } catch (Exception e) {
//            System.err.println("Error closing repository connection: " + e);
//            e.printStackTrace();
//        }
//    }
//    
//    private static List<RepositoryConnection> toClose = new ArrayList<RepositoryConnection>();
//    
//    /**
//     * This is just a quick mechanism to make sure all connections get closed.
//     */
//    private static void closeBeforeExit(RepositoryConnection conn) {
//        toClose.add(conn);
//    }
//    
//    private static void closeAll() {
//        while (toClose.isEmpty() == false) {
//            RepositoryConnection conn = toClose.get(0);
//            close(conn);
//            while (toClose.remove(conn)) {}
//        }
//    }
//    

}
