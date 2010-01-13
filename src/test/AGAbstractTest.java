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
import static test.Util.close;
import static test.Util.get;
import static test.Util.ifBlank;
import static test.Util.or;
import static test.Util.readLines;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGValueFactory;
import com.franz.util.Closeable;

public class AGAbstractTest {

    static public final String CATALOG_ID = "java-tutorial";
    
    protected static AGServer server;
    protected static AGCatalog cat;
    protected AGRepository repo;
    protected AGRepositoryConnection conn;
    protected static String repoId;
    
    protected AGValueFactory vf;
    
    private Stack<Closeable> toClose = new Stack<Closeable>();

    public static String findServerUrl() {
        String host = or(ifBlank(System.getenv("AGRAPH_HOST"), null),
                ifBlank(System.getProperty("AGRAPH_HOST"), null));
        String port = or(ifBlank(System.getenv("AGRAPH_PORT"), null),
                ifBlank(System.getProperty("AGRAPH_PORT"), null));
        
        if ((host == null || host.equals("localhost")) && port == null) {
            File cfg = new File(new File(System.getProperty("java.io.tmpdir"),
                    System.getProperty("user.name")), "agraph-tests.cfg");
            if (cfg.exists()) {
                try {
                    for (String line: readLines(cfg)) {
                        if (line.trim().startsWith("PortFile")) {
                            File portFile = new File(get(line.split(" "), 1, "").trim());
                            if (portFile.exists()) {
                                port = readLines(portFile).get(0);
                                host = "localhost";
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Trying to read PortFile from config file: " + cfg.getAbsolutePath(), e);
                }
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
    public static void setUpOnce() throws Exception {
        server = new AGServer(findServerUrl(), username(), password());
        cat = server.getCatalog(CATALOG_ID);
        repoId = "javatest";
        cat.deleteRepository(repoId);
    }
    
    @Before
    public void setUp() throws Exception {
        repo = cat.createRepository(repoId);
        repo.initialize();
        closeLater(repo);
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
        while (toClose.isEmpty() == false) {
            Closeable conn = toClose.pop();
            close(conn);
        }
        conn = null;
        repo = null;
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        cat = null;
        server = close(server);
    }

    public <CloseableType extends Closeable>  CloseableType closeLater(CloseableType c) {
        toClose.add(c);
        return c;
    }

    AGRepositoryConnection getConnection() throws RepositoryException {
        AGRepositoryConnection conn = repo.getConnection();
        closeLater(conn);
        return conn;
   }
   
    AGRepositoryConnection getConnection(AGRepository repo) throws RepositoryException {
        AGRepositoryConnection conn = repo.getConnection();
        closeLater(conn);
        return conn;
   }
   
    public static void assertSetsEqual(Collection expected, Set actual) {
        assertSetsEqual("", expected, actual);
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
                readLines(expected),
                readLines(actual));
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

    public void println(Object x) {
        System.out.println(x);
    }
    
    public void printRows(RepositoryResult<Statement> rows) throws Exception {
        while (rows.hasNext()) {
            println(rows.next());
        }
        close(rows);
    }

    public void printRows(String headerMsg, int limit, RepositoryResult<Statement> rows) throws Exception {
        println(headerMsg);
        int count = 0;
        while (count < limit && rows.hasNext()) {
            println(rows.next());
            count++;
        }
        println("Number of results: " + count);
        close(rows);
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
