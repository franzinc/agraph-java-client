/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import junit.framework.Assert;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.junit.Test;
import org.junit.experimental.categories.Category;


public class LiteralAndResourceResultsTest extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    /**
     * Shows how to get subclasses of Value (Literal, URI, BNode)
     * back from results (rather than just getting Values), and
     * confirms that the results are as expected.
     *
     * @throws Exception
     */
    public void testGetTypedResults() throws Exception {
        BNode b = vf.createBNode();
        IRI r = vf.createIRI("http://r");
        Literal lit = vf.createLiteral("42", XMLSchema.INT);
        conn.add(b, r, lit);
        String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL,
                queryString);
        TupleQueryResult result = tupleQuery.evaluate();

        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            BNode s = (BNode) bindingSet.getValue("s");
            Assert.assertEquals(b, s);
            IRI p = (IRI) bindingSet.getValue("p");
            Assert.assertEquals(r, p);
            Value o = bindingSet.getValue("o");
            if (o instanceof Literal) {
                Literal l = (Literal) o;
                Assert.assertEquals(lit, l);
                int i = l.intValue();
                Assert.assertEquals(42, i);
                IRI dt = l.getDatatype();
                Assert.assertEquals(XMLSchema.INT, dt);
            }
        }
    }

}
