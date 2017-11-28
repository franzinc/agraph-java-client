/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.agraph.repository.AGVirtualRepository;
import junit.framework.Assert;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FederationTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void federationBNodes() throws Exception {
        BNode bnode = vf.createBNode();
        conn.add(bnode, RDF.TYPE, vf.createIRI("http://Foo"));
        AGVirtualRepository fed = server.federate(repo, repo);
        AGRepositoryConnection conn2 = fed.getConnection();
        // Should be able to create BNodes for a federation
        conn2.getValueFactory().createBNode();
        conn2.getValueFactory().createBNode("foo");
        try {
            conn2.add(bnode, RDF.TYPE, vf.createIRI("http://Boo"));
            Assert.fail("expected can't write to federation.");
        } catch (RepositoryException e) {
            //expected
        }
        AGTupleQuery q = conn2.prepareTupleQuery(QueryLanguage.SPARQL, "select ?s {?s ?p ?o}");
        TupleQueryResult result = q.evaluate();
        Assert.assertTrue(result.hasNext());
        BindingSet bind = result.next();
        Assert.assertEquals(bnode.stringValue(), bind.getValue("s").stringValue());
    }

}
