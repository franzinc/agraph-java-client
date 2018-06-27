/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BlankNodeTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaExternalBlankNodeRoundTrips_spr38494() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.createGraph("http://aldi1.com.au"));
        AGModel model1 = closeLater(new AGModel(graph));

        Resource bnode = ResourceFactory.createResource();
        Resource bnode2 = model1.createResource(AnonId.create("ex"));
        model1.begin();
        model1.removeAll();
        model1.add(bnode, RDF.type, bnode2);
        model1.commit();
        Assert.assertEquals(1, model1.size());
        Assert.assertTrue(model1.contains(bnode, RDF.type, bnode2));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void jenaModelBlankNodeRoundTrips_spr38494() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.createGraph("http://aldi1.com.au"));
        AGModel model1 = closeLater(new AGModel(graph));

        Resource bnode = model1.createResource();
        model1.begin();
        model1.removeAll();
        model1.add(bnode, RDF.type, OWL.Thing);
        model1.commit();
        Assert.assertEquals(1, model1.size());
        Assert.assertTrue(model1.contains(bnode, RDF.type, OWL.Thing));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void sesameExternalBlankNodeRoundTrips_spr38494() throws Exception {
        BNode bnode = ValueFactoryImpl.getInstance().createBNode();
        BNode bnode2 = vf.createBNode("external");
        IRI p = vf.createIRI("http://p");
        conn.clear();
        try {
            conn.add(bnode, p, bnode2, p);
            Assert.fail("Expected an exception: can't add external blank nodes");
        } catch (IllegalArgumentException e) {
            // expected
        }
        conn.prepareHttpRepoClient().setAllowExternalBlankNodeIds(true);
        conn.add(bnode, p, bnode, p);
        Assert.assertEquals(1, conn.size());
        Assert.assertTrue(conn.hasStatement(bnode, p, bnode, false, p));
        RepositoryResult<Statement> results = conn.getStatements(bnode, p, bnode, false, p);
        closeLater(results);
        Assert.assertTrue(results.hasNext());
        Statement st = results.next();
        Assert.assertEquals(bnode, st.getSubject());
        Assert.assertEquals(bnode, st.getObject());
        GraphQueryResult results2 = conn.prepareGraphQuery(QueryLanguage.SPARQL, "construct {?s <http://foo> ?o} where {?s ?p ?o}").evaluate();
        closeLater(results2);
        Assert.assertTrue(results2.hasNext());
        st = results2.next();
        Assert.assertEquals(bnode, st.getSubject());
        Assert.assertEquals(bnode, st.getObject());
        TupleQueryResult results3 = conn.prepareTupleQuery(QueryLanguage.SPARQL, "select * where {?s ?p ?o}").evaluate();
        closeLater(results3);
        Assert.assertTrue(results3.hasNext());
        BindingSet result = results3.next();
        Assert.assertEquals(bnode, result.getValue("s"));
        Assert.assertEquals(bnode, result.getValue("o"));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void sesameAGBlankNodeRoundTrips_spr38494() throws Exception {
        BNode bnode = vf.createBNode();
        IRI p = vf.createIRI("http://p");
        conn.clear();
        conn.add(bnode, p, bnode, p);
        Assert.assertEquals(1, conn.size());
        Assert.assertTrue(conn.hasStatement(bnode, p, bnode, false, p));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void blankNodesPerRequest() throws Exception {
        vf.setBlankNodesPerRequest(vf.getBlankNodesPerRequest() * 10);
        vf.createBNode();
        Assert.assertEquals(vf.getBlankNodesPerRequest(), vf.getBlankNodeIds().length);
    }
}
