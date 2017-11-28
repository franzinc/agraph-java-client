/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecution;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.repository.AGTupleQuery;
import junit.framework.Assert;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class QueryLimitOffsetTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void sesameQueryLimitOffset_tests() throws Exception {
        Util.add(conn, "/tutorial/java-vcards.rdf", null, RDFFormat.RDFXML);
        String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
        AGTupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected 16 results", 16, tupleQuery.count());
        tupleQuery.setLimit(5);
        Assert.assertEquals("expected 5 results", 5, tupleQuery.count());
        tupleQuery.setOffset(15);
        Assert.assertEquals("expected 1 result", 1, tupleQuery.count());
        tupleQuery.setLimit(-1);
        tupleQuery.setOffset(10);
        Assert.assertEquals("expected 6 results", 6, tupleQuery.count());
        tupleQuery.setOffset(-1);
        Assert.assertEquals("expected 16 results", 16, tupleQuery.count());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaQueryLimitOffset_tests() throws Exception {
        Util.add(conn, "/tutorial/java-vcards.rdf", null, RDFFormat.RDFXML);
        AGGraphMaker maker = new AGGraphMaker(conn);
        AGModel model = new AGModel(maker.getGraph());
        String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
        AGQuery query = AGQueryFactory.create(queryString);
        AGQueryExecution qe = AGQueryExecutionFactory.create(query, model);
        Assert.assertEquals("expected 16 results", 16, qe.countSelect());
        query.setLimit(5);
        Assert.assertEquals("expected 5 results", 5, qe.countSelect());
        query.setOffset(15);
        Assert.assertEquals("expected 1 result", 1, qe.countSelect());
        query.setLimit(-1);
        query.setOffset(10);
        Assert.assertEquals("expected 6 results", 6, qe.countSelect());
        query.setOffset(-1);
        Assert.assertEquals("expected 16 results", 16, qe.countSelect());
    }

}
