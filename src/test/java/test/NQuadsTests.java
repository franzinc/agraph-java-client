/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import junit.framework.Assert;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class NQuadsTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void nquads_sesame_rfe10201() throws Exception {
        Util.add(conn, "/test/example.nq", null, RDFFormat.NQUADS);
        Assert.assertEquals("expected size 10", 10, conn.size());
        IRI alice = vf.createIRI("http://example.org/alice/foaf.rdf");
        Assert.assertEquals("expected size 7", 7, conn.size(alice));
        IRI bob = vf.createIRI("http://example.org/bob/foaf.rdf");
        Assert.assertEquals("expected size 3", 3, conn.size(bob));
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void nquads_jena_rfe10201() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getUnionOfAllGraphs());
        AGModel model = closeLater(new AGModel(graph));
        model.read(Util.resourceAsStream("/test/example.nq"), null, "NQUADS");
        Assert.assertEquals("expected size 10", 10, model.size());
        model.write(new FileOutputStream("target/exampleModelWrite.nq"), "NQUADS");
        model.removeAll();
        Assert.assertEquals("expected size 0", 0, model.size());
        model.read(new FileInputStream("target/exampleModelWrite.nq"), null, "NQUADS");
        Assert.assertEquals("expected size 10", 10, model.size());
    }

    @Test
    @Category(TestSuites.Broken.class)
    public void sesameAddContextOverridesNQuadsContext() throws Exception {
        IRI bob = vf.createIRI("http://example.org/bob/foaf.rdf");
        // the add context is ignored -- it should override
        Util.add(conn, "/test/example.nq", null, RDFFormat.NQUADS, bob);
        Assert.assertEquals("expected size 10", 10, conn.size(bob));
    }

    @Test
    @Category(TestSuites.Broken.class)
    public void jenaGraphOverridesNQuadsContext() throws Exception {
        AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
        AGGraph graph = closeLater(maker.getGraph());
        AGModel model = closeLater(new AGModel(graph));
        model.read(Util.resourceAsStream("/test/example.nq"), null, "NQUADS");
        Assert.assertEquals("expected size 10", 10, model.size());
    }

}
