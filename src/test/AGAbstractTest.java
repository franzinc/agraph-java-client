/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static test.Util.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.openrdf.model.Statement;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResult;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;

public class AGAbstractTest extends TestCase {

    static public final String CATALOG_ID = "java-tutorial";
    static public final String USERNAME = "test";
    static public final String PASSWORD = "xyzzy";
    static public final String TEMPORARY_DIRECTORY = "";
    
    protected AGServer server;
    protected AGCatalog cat;
    protected AGRepository repo;
    protected AGRepositoryConnection conn;
    protected String repoId;
    
    private List<RepositoryConnection> toClose = new ArrayList<RepositoryConnection>();
    
    public static String findServerUrl() {
        String host = ifBlank(System.getProperty("AGRAPH_HOST"), null);
        String port = ifBlank(System.getProperty("AGRAPH_PORT"), null);
        
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
    
    @Override
    protected void setUp() throws Exception {
        server = new AGServer(findServerUrl(), USERNAME, PASSWORD);
        cat = server.getCatalog(CATALOG_ID);
        repoId = "javatest";
        cat.deleteRepository(repoId);
        repo = cat.createRepository(repoId);
        repo.initialize();
        conn = getConnection();
    }

    @Override
    protected void tearDown() throws Exception {
        cat = null;
        server = null;
        while (toClose.isEmpty() == false) {
            RepositoryConnection conn = toClose.get(0);
            close(conn);
            while (toClose.remove(conn)) {}
        }
        conn = null;
        if (repo != null) {
            repo.shutDown();
        }
        repo = null;
    }

    AGRepositoryConnection getConnection() throws RepositoryException {
        AGRepositoryConnection conn = repo.getConnection();
        toClose.add(conn);
        return conn;
   }
   
    public void assertSetsEqual(Collection expected, Set actual) {
        assertSetsEqual("", expected, actual);
    }

    public void assertSetsEqual(String msg, Collection expected, Collection actual) {
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
    
    public void assertSetsSome(String msg, Collection expected, Collection actual) {
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
    
    public static Set<Stmt> stmts(Stmt... stmts) {
         HashSet<Stmt> set = new HashSet<Stmt>(Arrays.asList(stmts));
         set.remove(null);
         return set;
    }
    
    public void assertFiles(File expected, File actual) throws Exception {
        assertEquals("diff " + expected.getCanonicalPath() + " " + actual.getCanonicalPath(),
                readLines(expected),
                readLines(actual));
    }
    
    public Set<Stmt> statementSet(RepositoryResult<Statement> results) throws Exception {
        try {
            Set<Stmt> ret = new HashSet<Stmt>();
            while (results.hasNext()) {
                ret.add(new Stmt(results.next()));
            }
            return ret;
        } finally {
            close(results);
        }
    }
    
    public Set<Stmt> stmtsSP(Collection c) throws Exception {
        Set<Stmt> ret = new HashSet<Stmt>();
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            Statement s = (Statement) iter.next();
            ret.add(new Stmt(s.getSubject(), s.getPredicate(), null, null));
        }
        return ret;
    }
    
    public Set<Stmt> statementSet(QueryResult<Statement> results) throws Exception {
        try {
            Set<Stmt> ret = new HashSet<Stmt>();
            while (results.hasNext()) {
                ret.add(new Stmt(results.next()));
            }
            return ret;
        } finally {
            close(results);
        }
    }
    
    public static Map mapKeep(Object[] keys, Map map) {
        Map ret = new HashMap();
        for (int i = 0; i < keys.length; i++) {
            ret.put(keys[i], map.get(keys[i]));
        }
        return ret;
    }
    
    public Set<Stmt> statementSet(TupleQueryResult result, String... SPOGnames) throws Exception {
        try {
            Set<Stmt> ret = new HashSet<Stmt>();
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                ret.add(Stmt.spog(bindingSet, SPOGnames));
                //println(bindingSet);
            }
            return ret;
        } finally {
            close(result);
        }
    }

    public void addAll(Collection stmts, RepositoryConnection conn) throws RepositoryException {
        for (Iterator iter = stmts.iterator(); iter.hasNext();) {
            Statement st = (Statement) iter.next();
            conn.add(st);
        }
    }

    public void println(Object x) {
        System.out.println(getName() + ": " + x);
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
