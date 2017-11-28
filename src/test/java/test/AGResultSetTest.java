/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import junit.framework.Test;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.io.FileNotFoundException;
import java.util.Iterator;


public class AGResultSetTest extends AGModelTest {
    public AGResultSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        util = new JenaUtil(AGResultSetTest.class);
        return util;
    }

    public void testResultSet() throws FileNotFoundException {
        AGModel model = (AGModel) getModel();
        model.read(Util.resourceAsStream("/test/default-graph.nt"), null, "N-TRIPLE");
        try {
            String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
            AGQuery sparql = AGQueryFactory.create(queryString);
            QueryExecution qe = AGQueryExecutionFactory.create(sparql, model);
            try {
                ResultSet results = qe.execSelect();
                assertEquals(results.getResourceModel(), model);
                assertEquals(results.getResultVars().size(), 3);
                while (results.hasNext()) {
                    QuerySolution result = results.next();
                    RDFNode s = result.get("s");
                    RDFNode p = result.get("p");
                    RDFNode o = result.get("o");
                    RDFNode g = result.get("g");
                    assertTrue(s.isResource());
                    assertTrue(p.isResource());
                    assertTrue(o.isLiteral());
                    assertNull(g);
                }
            } finally {
                qe.close();
            }
            queryString = "SELECT ?s ?p ?o ?g  WHERE {GRAPH ?g {?s ?p ?o .}}";
            sparql = AGQueryFactory.create(queryString);
            qe = AGQueryExecutionFactory.create(sparql, model);
            try {
                ResultSet results = qe.execSelect();
                assertEquals(results.getResourceModel(), model);
                assertEquals(results.getResultVars().size(), 4);
                int count;
                for (count = 0; results.hasNext(); count++) {
                    QuerySolution result = results.next();
                    Iterator<String> vars = result.varNames();
                    while (vars.hasNext()) {
                        assertTrue(result.contains(vars.next()));
                    }
                    Resource s = result.getResource("s");
                    Resource p = result.getResource("p");
                    try {
                        result.getResource("o");
                        fail();
                    } catch (Exception e) {
                        // expected, "o" is a Literal, not a Resource
                    }
                    Literal o = result.getLiteral("o");
                    Resource g = result.getResource("g");
                    assertTrue(s.isResource());
                    assertTrue(p.isResource());
                    assertTrue(o.isLiteral());
                    assertTrue(g.isResource());
                }
                assertEquals(count, 4);
            } finally {
                qe.close();
            }
        } finally {
            model.close();
        }

    }
}
