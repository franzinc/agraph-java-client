/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test.stress;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGValueFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.AGAbstractTest;
import test.TestSuites;

import java.util.LinkedList;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static test.AGAbstractTest.findServerUrl;
import static test.AGAbstractTest.password;
import static test.AGAbstractTest.username;

public class TransactionStressTest {
    static private final int WORKERS = 10;
    static private final int PER = 1000;

    public static AGRepositoryConnection connect() throws RepositoryException {
        AGServer server = new AGServer(findServerUrl(), username(), password());
        AGCatalog catalog = server.getCatalog(AGAbstractTest.CATALOG_ID);
        AGRepository repository = catalog.createRepository("transaction-stress");
        repository.initialize();
        AGRepositoryConnection conn = repository.getConnection();
        return conn;
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
                if (w.failed != null) {
                    failed = w.failed;
                }
            }
            if (failed != null) {
                fail(failed);
            }

            assertEquals(WORKERS * PER, conn.size());

            AGValueFactory vf = conn.getRepository().getValueFactory();
            RepositoryResult<Statement> unfinished =
                    conn.getStatements(null, vf.createIRI("http://example.org/finished"), vf.createLiteral("false"), false);
            assertTrue(!unfinished.hasNext());
        } finally {
            conn.close();
        }
    }

    private static class Worker extends Thread {
        int from, to;
        String failed = null;
        Random rnd;

        public Worker(int from1, int to1) {
            from = from1;
            to = to1;
            rnd = new Random(from1);
            rnd.nextInt();
        }

        public void run() {
            try {
                AGRepositoryConnection conn = connect();
                conn.setAutoCommit(false);
                try {
                    for (int i = from; i < to; i++) {
                        while (!transaction(conn, i)) {
                            ;
                        }
                    }
                } finally {
                    conn.close();
                }
            } catch (Exception e) {
                failed = e.toString();
            }
        }

        private boolean transaction(AGRepositoryConnection conn, int id)
                throws RepositoryException, MalformedQueryException, QueryEvaluationException {
            boolean okay = false;
            try {
                AGValueFactory vf = conn.getRepository().getValueFactory();
                IRI node = vf.createIRI("http://example.org/" + id);
                conn.add(node, vf.createIRI("http://example.org/finished"), vf.createLiteral("false"));

                String q = "SELECT ?n WHERE {?n <http://example.org/finished> \"false\"}";
                TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, q).evaluate();
                boolean found = false;
                try {
                    while (result.hasNext()) {
                        Value n = result.next().getValue("n");
                        if (n.equals(node)) {
                            found = true;
                        } else {
                            throw new RuntimeException("Unexpected unfinished node found: " + n);
                        }
                    }
                } finally {
                    result.close();
                }
                if (!found) {
                    throw new RuntimeException("Unfinished node not found.");
                }

                // Test aborted transactions.
                if (rnd.nextInt() % 20 == 0) {
                    return false;
                }

                conn.remove(node, vf.createIRI("http://example.org/finished"), vf.createLiteral("false"));
                conn.add(node, vf.createIRI("http://example.org/finished"), vf.createLiteral("true"));
                conn.commit();
                // commit turns on autocommit with sesame 2.7
                // transaction semantics, make sure it's off
                conn.setAutoCommit(false);
                okay = true;
            } finally {
                if (!okay) {
                    conn.rollback();
                    // rollback turns on autocommit with sesame 2.7
                    // transaction semantics, make sure it's off
                    conn.setAutoCommit(false);
                }
            }
            return true;
        }
    }

}
