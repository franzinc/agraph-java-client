/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGGraphQuery;
import junit.framework.Assert;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AGGraphQueryTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void graphQuery_count_rfe10447() throws Exception {
        Util.add(conn, "/test/example.nq", null, RDFFormat.NQUADS);
        String queryString = "construct {?s ?p ?o} where {?s ?p ?o}";
        AGGraphQuery q = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected size 10", 10, q.count());
        queryString =
                "construct {?s ?p ?o} where " +
                        "{GRAPH <http://example.org/alice/foaf.rdf> {?s ?p ?o}}";
        q = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected size 7", 7, q.count());
        queryString =
                "construct {?s ?p ?o} where " +
                        "{GRAPH <http://example.org/bob/foaf.rdf> {?s ?p ?o}}";
        q = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected size 3", 3, q.count());
        queryString =
                "construct {?s ?p ?o} where " +
                        "{GRAPH <http://example.org/carol/foaf.rdf> {?s ?p ?o}}";
        q = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected size 0", 0, q.count());
        queryString =
                "describe <http://example.org/alice/foaf.rdf#me>";
        q = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected size 7", 7, q.count());
    }

}
