/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.stress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static test.AGAbstractTest.findServerUrl;
import static test.AGAbstractTest.password;
import static test.AGAbstractTest.username;

import java.util.LinkedList;
import java.util.Random;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import test.TestSuites;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGValueFactory;

public class TransactionStressTest {
    static private final int WORKERS = 10;
    static private final int PER = 1000;

    public static AGRepositoryConnection connect() throws RepositoryException {
        AGServer server = new AGServer(findServerUrl(), username(), password());
        AGCatalog catalog = server.getCatalog("tests");
        AGRepository repository = catalog.createRepository("transaction-stress");
        repository.initialize();
        AGRepositoryConnection conn = repository.getConnection();
        return conn;
    }
    
    private static class Worker extends Thread {
        int from, to;
        String failed = null;
        Random rnd;

        public Worker(int from1, int to1) {
            from = from1; to = to1;
            rnd = new Random(from1);
            rnd.nextInt();
        }

        public void run() {
            try {
                AGRepositoryConnection conn = connect();
                conn.setAutoCommit(false);
                try {
                    for (int i = from; i < to; i++)
                        while (!transaction(conn, i));
                }
                finally{conn.close();}
            }
            catch (Exception e) {
                failed = e.toString();
            }
        }

        private boolean transaction(AGRepositoryConnection conn, int id)
          throws RepositoryException, MalformedQueryException, QueryEvaluationException {
            boolean okay = false;
            try {
                AGValueFactory vf = conn.getRepository().getValueFactory();
                URI node = vf.createURI("http://example.org/" + id);
                conn.add(node, vf.createURI("http://example.org/finished"), vf.createLiteral("false"));
                
                String q = "SELECT ?n WHERE {?n <http://example.org/finished> \"false\"}";
                TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, q).evaluate();
                boolean found = false;
                try {
                    while (result.hasNext()) {
                        Value n = result.next().getValue("n");
                        if (n.equals(node)) found = true;
                        else throw new RuntimeException("Unexpected unfinished node found: " + n);
                    }
                }
                finally {result.close();}
                if (!found) throw new RuntimeException("Unfinished node not found.");

                // Test aborted transactions.
                if (rnd.nextInt() % 20 == 0) return false;

                conn.remove(node, vf.createURI("http://example.org/finished"), vf.createLiteral("false"));
                conn.add(node, vf.createURI("http://example.org/finished"), vf.createLiteral("true"));
                conn.commit();
                okay = true;
            }
            finally {if (!okay) conn.rollback();}
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        new TransactionStressTest().test();
    }

    @Test
    @Category(TestSuites.Stress.class)
    public void test() throws Exception {
        AGRepositoryConnection conn = connect();
        try {
            conn.clear();  // remove previous triples, if any.

            LinkedList<Worker> workers = new LinkedList<Worker>();
            for (int i = 0; i < WORKERS; i++) {
                Worker w = new Worker(i * PER, (i + 1) * PER);
                workers.add(w);
                w.start();
            }
            String failed = null;
            for (Worker w : workers) {
                w.join();
                if (w.failed != null) failed = w.failed;
            }
            if (failed != null) fail(failed);

            assertEquals(WORKERS * PER, conn.size());

            AGValueFactory vf = conn.getRepository().getValueFactory();
            RepositoryResult<Statement> unfinished =
                conn.getStatements(null, vf.createURI("http://example.org/finished"), vf.createLiteral("false"), false);
            assertTrue(!unfinished.hasNext());
        }
        finally {conn.close();}
    }

}
