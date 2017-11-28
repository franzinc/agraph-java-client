package test;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecution;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.repository.AGCatalog;
import junit.framework.Assert;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Test;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class AGQueryExecutionTest extends AGAbstractTest {

    protected static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

    public static String SERVER_URL = System.getProperty(
            "com.franz.agraph.test.serverURL", "http://localhost:10035");
    public static String CATALOG_ID = System.getProperty(
            "com.franz.agraph.test.catalogID", "/");
    public static String REPOSITORY_ID = System.getProperty(
            "com.franz.agraph.test.repositoryID", "testRepo");
    public static String USERNAME = System.getProperty(
            "com.franz.agraph.test.username", "test");
    public static String PASSWORD = System.getProperty(
            "com.franz.agraph.test.password", "xyzzy");

    protected static AGCatalog catalog;

    protected static AGGraphMaker maker = null;

    protected static AGModel model = null;

    protected static String baseURI = null;

    protected static ValueFactory vf;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        maker = new AGGraphMaker(conn);
        model = new AGModel(maker.getGraph());
        String path = "/tutorial/java-kennedy.ntriples";
        baseURI = "http://example.org/example/local";
        model.read(Util.resourceAsStream(path), baseURI, "N-TRIPLE");
    }

    @Test
    public void testExecConstructTriples() throws RepositoryException {
        String queryString = "PREFIX kdy: <http://www.franz.com/simple#> construct {?a kdy:has-grandchild ?c}" +
                "    where { ?a kdy:has-child ?b . " +
                "            ?b kdy:has-child ?c . }";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            int count = 0;
            Iterator<Triple> iter = qe.execConstructTriples();
            while (iter.hasNext()) {
                iter.next();
                count += 1;
            }
            assertTrue(count != 0);
            assertTrue(count <= 1000000);
        } finally {
            qe.close();
        }
    }

    @Test//(expected=QueryException.class)
    public void testExecConstructTriplesForNullQuery() throws RepositoryException {
        String queryString = null;
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            qe.execConstructTriples();
            Assert.fail("expected exception for null query string.");
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            qe.close();
        }
    }


    @Test//(expected=QueryException.class)
    public void testExecConstructTriplesForBadQuery() throws RepositoryException {
        String queryString = "select * from emp where emp_id=1";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            qe.execConstructTriples();
            Assert.fail("expected exception for bad query string.");
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            qe.close();
        }
    }


    @Test
    public void testExecConstructTriplesForZeroResult() throws RepositoryException {
        String queryString = "PREFIX kdy: <http://www.franz.com/simple#> construct {?a kdy:has-grandchild ?d}" +
                "    where { ?a kdy:has-child ?b . " +
                "            ?b kdy:has-child ?c ." +
                "            ?c kdy:has-child ?d . }";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            Iterator<Triple> iter = qe.execConstructTriples();
            assertFalse(iter.hasNext());
        } finally {
            qe.close();
        }
    }


    @Test//(expected=QueryException.class)
    public void testExecConstructTriplesForNonConstructQuery() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            qe.execConstructTriples();
            Assert.fail("expected exception for non construct query.");
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            qe.close();
        }
    }


    @Test
    public void testExecDescribeTriples() throws RepositoryException {
        String queryString = "describe ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            int count = 0;
            Iterator<Triple> iter = qe.execDescribeTriples();
            while (iter.hasNext()) {
                iter.next();
                count += 1;
            }
            assertTrue(count != 0);
            assertTrue(count <= 1000000);
        } finally {
            qe.close();
        }
    }

    @Test//(expected=QueryException.class)
    public void testExecDescribeTriplesForNullQuery() throws RepositoryException {
        String queryString = null;
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            qe.execDescribeTriples();
            Assert.fail("expected exception for null query string.");
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            qe.close();
        }
    }

    @Test//(expected=QueryException.class)
    public void testExecDescribeTriplesForBadQuery() throws RepositoryException {
        String queryString = "select * from emp where emp_id=1";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            qe.execDescribeTriples();
            Assert.fail("expected exception for bad query string.");
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            qe.close();
        }
    }

    @Test
    public void testExecDescribeTriplesForZeroResult() throws RepositoryException {
        String queryString = "describe ?s ?p ?o where { ?s ?p 'madhu' . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {

            Iterator<Triple> iter = qe.execDescribeTriples();
            assertFalse(iter.hasNext());
        } finally {
            qe.close();
        }
    }


    @Test//(expected=QueryException.class)
    public void testExecDescribeTriplesForNonDescribeQuery() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            qe.execDescribeTriples();
            Assert.fail("expected exception for non describe query.");
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            qe.close();
        }
    }


    @Test
    public void testExecConstruct() throws RepositoryException {
        String queryString = "construct {?a kdy:has-grandchild ?c}" +
                "    where { ?a kdy:has-child ?b . " +
                "            ?b kdy:has-child ?c . }";
        model.setNsPrefix("kdy", "http://www.franz.com/simple#");
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            int count = 0;
            Model iter = qe.execConstruct();
            StmtIterator it = iter.listStatements();
            while (it.hasNext()) {
                it.next();
                count += 1;
            }
            assertTrue(count != 0);
            assertTrue(count <= 1000000);
        } finally {
            qe.close();
            model.removeNsPrefix("kdy");
        }
    }


    @Test
    public void testgetQueryWithOutNamespace() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . }";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            Object gq = qe.getQuery();
            assertNotNull(gq);
        } finally {
            qe.close();
        }
    }

    @Test//(expected=QueryParseException.class)
    public void testgetQueryWithOutNamespaceWrongQuery() throws RepositoryException {
        String queryString = "select sdfdsfdsf ?s ?p ?o where { ?s ?p ?o . }";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            qe.getQuery();
            Assert.fail("expected exception for bad query string.");
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            qe.close();
        }
    }

    @Test//(expected=QueryParseException.class)
    public void testgetQueryForNullQuery() throws RepositoryException {
        String queryString = null;
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        try {
            qe.getQuery();
            Assert.fail("expected exception for null query string.");
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            qe.close();
        }
    }


    // Needs Sparql a query which takes more than 1 sec to execute
    @Test//(expected=QueryException.class)
    public void testSetTimeoutForExecSelect() throws RepositoryException {
        String queryString = "SELECT * WHERE { ?s ?p ?o . ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 . ?s3 ?p3 ?o3 } ORDER BY ?s1 ?p1 ?o1 LIMIT 1000";

        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);

        try {
            qe.setTimeout(1001);
            qe.execSelect();
            Assert.fail("expected exception for query execution time.");
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            qe.close();
        }
    }

    @Test
    public void testSetTimeoutForExecSelectWithZero() throws RepositoryException {
        String queryString = "SELECT (COUNT(DISTINCT ?s ) AS ?no) { { ?s ?p ?o  } UNION { ?o ?p ?s } FILTER(!isBlank(?s) && !isLiteral(?s)) }";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);

        try {
            qe.setTimeout(0);
            ResultSet rs = qe.execSelect();
            assertNotNull(rs);
        } finally {
            qe.close();
        }
    }

    @Test
    public void testSetTimeoutForExecSelectWithNegative() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);

        try {
            qe.setTimeout(-5);
            ResultSet rs = qe.execSelect();
            assertNotNull(rs);
        } finally {
            qe.close();
        }
    }


    // Needs Sparql a query which takes more than 1 sec to execute
    @Test//(expected=QueryException.class)
    public void testSetTimeoutWithTimeunitForExecSelect() throws RepositoryException {
        String queryString = "SELECT * WHERE { ?s ?p ?o . ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 . ?s3 ?p3 ?o3 } ORDER BY ?s1 ?p1 ?o1 LIMIT 1000";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);

        try {
            qe.setTimeout(1, TimeUnit.SECONDS);
            qe.execSelect();
            Assert.fail("expected exception for query execution time.");
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            qe.close();
        }
    }

    @Test
    public void testSetTimeoutWithTimeunitForExecSelectWithZero() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);

        try {
            qe.setTimeout(0, TimeUnit.SECONDS);
            ResultSet rs = qe.execSelect();
            assertNotNull(rs);
        } finally {
            qe.close();
        }
    }

    @Test
    public void testSetTimeoutWithTimeunitForExecSelectWithNegative() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);

        try {
            qe.setTimeout(-5, TimeUnit.SECONDS);
            ResultSet rs = qe.execSelect();
            assertNotNull(rs);
        } finally {
            qe.close();
        }
    }

    @Test
    public void testSetTimeoutWithTimeunitForExecSelectWith1hour() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);

        try {
            qe.setTimeout(1, TimeUnit.HOURS);
            ResultSet rs = qe.execSelect();
            assertNotNull(rs);
        } finally {
            qe.close();
        }
    }

}



