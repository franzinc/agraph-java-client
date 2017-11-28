/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGMaterializer;
import junit.framework.AssertionFailedError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AGMaterializerTests extends AGAbstractTest {

    protected static String printStatements(RepositoryConnection conn) throws RepositoryException {
        RepositoryResult<Statement> results = conn.getStatements(null, null, null, false);
        StringBuffer m = new StringBuffer();
        int limit = Integer.parseInt(System.getProperty("AGMaterializerTests.printStatements.limit", "50"));
        int i = 0;
        for (; results.hasNext() && i < limit; i++) {
            if (i == 0) {
                m.append("\nDumping all statements to help debug:\n");
            }
            m.append(results.next());
            m.append("\n");
        }
        if (results.hasNext()) {
            m.append("(there are more, stopping at " + limit + ")\n");
        }
        return m.toString();
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void materializeOverDefaultGraph() throws Exception {
        IRI a = vf.createIRI("http://a");
        IRI p = vf.createIRI("http://p");
        IRI A = vf.createIRI("http://A");
        try {
            conn.add(a, p, a);
            conn.add(p, RDFS.DOMAIN, A);
            AGMaterializer materializer = AGMaterializer.newInstance();
            materializer.withRuleset("all");
            Assert.assertEquals(
                    "unexpected number of materialized triples added", 13,
                    conn.materialize(materializer));
            Assert.assertEquals("expected size 15", 15, conn.size());
            Assert.assertTrue(conn.hasStatement(a, RDF.TYPE, A, false));
            Assert.assertEquals(
                    "unexpected number of materialized triples deleted", 13,
                    conn.deleteMaterialized());
            Assert.assertFalse(conn.hasStatement(a, RDF.TYPE, A, false));
            Assert.assertEquals("expected size 2", 2, conn.size());
        } catch (AssertionFailedError e) {
            throw new AssertionFailedError(e.getMessage() + printStatements(conn));
        }
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void materializeIntoNamedGraph() throws Exception {
        IRI a = vf.createIRI("http://a");
        IRI p = vf.createIRI("http://p");
        IRI A = vf.createIRI("http://A");
        IRI g = vf.createIRI("http://g");
        try {
            conn.add(a, p, a);
            conn.add(p, RDFS.DOMAIN, A);
            AGMaterializer materializer = AGMaterializer.newInstance();
            materializer.withRuleset("all");
            materializer.setInferredGraph(g);
            Assert.assertEquals(
                    "unexpected number of materialized triples added", 13,
                    conn.materialize(materializer));
            Assert.assertEquals("wrong size of G", 13, conn.size(g));
            Assert.assertEquals("wrong size of the default graph", 2,
                    conn.size((Resource) null));
            Assert.assertTrue(conn.hasStatement(a, RDF.TYPE, A, false, g));
            Assert.assertEquals(
                    "materialized triples deleted from wrong graph", 0,
                    conn.deleteMaterialized());
            Assert.assertEquals(
                    "unexpected number of materialized triples deleted", 13,
                    conn.deleteMaterialized(materializer));
            Assert.assertFalse(conn.hasStatement(a, RDF.TYPE, A, false, g));
            Assert.assertEquals("G not empty after delete", 0, conn.size(g));
        } catch (AssertionFailedError e) {
            throw new AssertionFailedError(e.getMessage() + printStatements(conn));
        }
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void materializeOverDefaultGraphTransactional() throws Exception {
        IRI a = vf.createIRI("http://a");
        IRI p = vf.createIRI("http://p");
        IRI A = vf.createIRI("http://A");
        conn.setAutoCommit(false);
        conn.add(a, p, a);
        conn.add(p, RDFS.DOMAIN, A);
        AGMaterializer materializer = AGMaterializer.newInstance();
        materializer.withRuleset("all");
        Assert.assertEquals(13, conn.materialize(materializer));
        Assert.assertEquals("expected size 15", 15, conn.size());
        Assert.assertTrue(conn.hasStatement(a, RDF.TYPE, A, false));
        Assert.assertEquals(13, conn.deleteMaterialized());
        Assert.assertFalse(conn.hasStatement(a, RDF.TYPE, A, false));
        Assert.assertEquals("expected size 2", 2, conn.size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void materializeOverDefaultGraphUseTypeSubproperty() throws Exception {
        IRI a = vf.createIRI("http://a");
        IRI A = vf.createIRI("http://A");
        IRI B = vf.createIRI("http://B");
        IRI mytype = vf.createIRI("http://mytype");
        conn.add(A, RDFS.SUBCLASSOF, B);
        conn.add(mytype, RDFS.SUBPROPERTYOF, RDF.TYPE);
        conn.add(a, mytype, A);
        AGMaterializer materializer = AGMaterializer.newInstance();
        materializer.setUseTypeSubproperty(false);
        conn.materialize(materializer);
        Assert.assertTrue(conn.hasStatement(a, RDF.TYPE, A, false));
        Assert.assertTrue(conn.hasStatement(a, RDF.TYPE, B, false));
        conn.deleteMaterialized();
        Assert.assertEquals("expected size 3", 3, conn.size());
        materializer.setUseTypeSubproperty(true);
        conn.materialize(materializer);
        Assert.assertTrue(conn.hasStatement(a, RDF.TYPE, A, false));
        Assert.assertTrue(conn.hasStatement(a, mytype, B, false));
        Assert.assertTrue(conn.hasStatement(a, RDF.TYPE, B, false));
    }

    @Test
    @Category(TestSuites.Broken.class)
    public void materializeOverNamedGraphs() throws Exception {
        Util.add(conn, "/test/example.nq", null, RDFFormat.NQUADS);
        conn.add(vf.createIRI("http://xmlns.com/foaf/0.1/name"), RDFS.DOMAIN, OWL.INDIVIDUAL);
        Assert.assertEquals("expected size 11", 11, conn.size());
        conn.materialize(null);
        Assert.assertFalse(conn.hasStatement(vf.createIRI("http://www.franz.com/materialized"), null, null, false));
        Assert.assertEquals("expected size 14", 14, conn.size());
    }

}
