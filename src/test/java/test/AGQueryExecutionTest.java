package test;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecution;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.repository.AGCatalog;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AGQueryExecutionTest extends AGAbstractTest {

    public static String CATALOG_ID = System.getProperty(
            "com.franz.agraph.test.catalogID", "/");

    protected static AGCatalog catalog;

    protected static AGGraphMaker maker = null;

    protected static AGModel model = null;

    protected static String baseURI = null;

    protected static ValueFactory vf;


    @BeforeEach
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
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            int count = 0;
            Iterator<Triple> iter = qe.execConstructTriples();
            while (iter.hasNext()) {
                iter.next();
                count += 1;
            }
            assertTrue(count != 0);
            assertTrue(count <= 1000000);
        }
    }


    @Test
    public void testExecConstructTriplesForBadQuery() throws RepositoryException {
        String queryString = "select * from emp where emp_id=1";
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            assertThrows(QueryException.class, qe::execConstructTriples);
        }
    }


    @Test
    public void testExecConstructTriplesForZeroResult() throws RepositoryException {
        String queryString = "PREFIX kdy: <http://www.franz.com/simple#> construct {?a kdy:has-grandchild ?d}" +
                "    where { ?a kdy:has-child ?b . " +
                "            ?b kdy:has-child ?c ." +
                "            ?c kdy:has-child ?d . }";
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            Iterator<Triple> iter = qe.execConstructTriples();
            assertFalse(iter.hasNext());
        }
    }


    @Test
    public void testExecConstructTriplesForNonConstructQuery() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            assertThrows(QueryException.class, qe::execConstructTriples);
        }
    }


    @Test
    public void testExecDescribeTriples() throws RepositoryException {
        String queryString = "describe ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            int count = 0;
            Iterator<Triple> iter = qe.execDescribeTriples();
            while (iter.hasNext()) {
                iter.next();
                count += 1;
            }
            assertTrue(count != 0);
            assertTrue(count <= 1000000);
        }
    }


    @Test
    public void testExecDescribeTriplesForZeroResult() throws RepositoryException {
        String queryString = "describe ?s ?p ?o where { ?s ?p 'madhu' . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {

            Iterator<Triple> iter = qe.execDescribeTriples();
            assertFalse(iter.hasNext());
        }
    }


    @Test
    public void testExecDescribeTriplesForNonDescribeQuery() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            assertThrows(QueryException.class, qe::execDescribeTriples);
        }
    }


    @Test
    public void testExecConstruct() throws RepositoryException {
        String queryString = "construct {?a kdy:has-grandchild ?c}" +
                "    where { ?a kdy:has-child ?b . " +
                "            ?b kdy:has-child ?c . }";
        model.setNsPrefix("kdy", "http://www.franz.com/simple#");
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
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
            model.removeNsPrefix("kdy");
        }
    }


    @Test
    public void testgetQueryWithOutNamespace() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . }";
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            Object gq = qe.getQuery();
            assertNotNull(gq);
        }
    }

    @Test
    public void testgetQueryWithOutNamespaceWrongQuery() throws RepositoryException {
        String queryString = "select sdfdsfdsf ?s ?p ?o where { ?s ?p ?o . }";
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            assertThrows(QueryParseException.class, qe::getQuery);
        }
    }

    @Test
    public void testgetQueryForNullQuery() {
        String queryString = null;
        assertThrows(QueryParseException.class, () ->
                AGQueryFactory.create(queryString)
        );
    }


    @Test
    public void testSetTimeoutForExecSelect() throws RepositoryException {
        String queryString = "SELECT * WHERE { ?s ?p ?o . ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 . ?s3 ?p3 ?o3 } ORDER BY ?s1 ?p1 ?o1 LIMIT 1000";
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            qe.setTimeout(1001);
            assertThrows(QueryException.class, qe::execSelect);
        }
    }


    @Test
    public void testSetTimeoutForExecSelectWithZero() throws RepositoryException {
        String queryString = "SELECT (COUNT(DISTINCT ?s ) AS ?no) { { ?s ?p ?o  } UNION { ?o ?p ?s } FILTER(!isBlank(?s) && !isLiteral(?s)) }";
        AGQuery query = AGQueryFactory.create(queryString);

        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            qe.setTimeout(0);
            ResultSet rs = qe.execSelect();
            assertNotNull(rs);
        }
    }

    @Test
    public void testSetTimeoutForExecSelectWithNegative() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);

        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            qe.setTimeout(-5);
            ResultSet rs = qe.execSelect();
            assertNotNull(rs);
        }
    }


    @Test
    public void testSetTimeoutWithTimeunitForExecSelect() throws RepositoryException {
        String queryString = "SELECT * WHERE { ?s ?p ?o . ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 . ?s3 ?p3 ?o3 } ORDER BY ?s1 ?p1 ?o1 LIMIT 1000";
        AGQuery query = AGQueryFactory.create(queryString);
        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            qe.setTimeout(1, TimeUnit.SECONDS);
            assertThrows(QueryException.class, qe::execSelect);
        }
    }

    @Test
    public void testSetTimeoutWithTimeunitForExecSelectWithZero() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);

        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            qe.setTimeout(0, TimeUnit.SECONDS);
            ResultSet rs = qe.execSelect();
            assertNotNull(rs);
        }
    }

    @Test
    public void testSetTimeoutWithTimeunitForExecSelectWithNegative() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);

        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            qe.setTimeout(-5, TimeUnit.SECONDS);
            ResultSet rs = qe.execSelect();
            assertNotNull(rs);
        }
    }

    @Test
    public void testSetTimeoutWithTimeunitForExecSelectWith1hour() throws RepositoryException {
        String queryString = "select ?s ?p ?o where { ?s ?p ?o . } limit 1";
        AGQuery query = AGQueryFactory.create(queryString);

        try (AGQueryExecution qe = AGQueryExecutionFactory.create(query, model)) {
            qe.setTimeout(1, TimeUnit.HOURS);
            ResultSet rs = qe.execSelect();
            assertNotNull(rs);
        }
    }

}



