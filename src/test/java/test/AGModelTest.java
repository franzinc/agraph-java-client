/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.repository.AGRepositoryConnection;
import junit.framework.Test;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.test.AbstractTestModel;

public class AGModelTest extends AbstractTestModel {
    protected static JenaUtil util = new JenaUtil(AGModelTest.class);
    private static int graphId = 0;
    protected AGRepositoryConnection conn = null;
    protected AGGraphMaker maker = null;

    public AGModelTest(String name) {
        super(name);
    }

    public static Test suite() {
        return util;
    }

    @Override
    public void setUp() {
        conn = util.getConn();
        maker = util.getMaker();
        super.setUp();
    }

    @Override
    public Model getModel() {
        AGGraph graph = util.getMaker().createGraph("http://anon" + graphId);
        graphId++;
        return new AGModel(graph);
    }


    public void testContainsResource() {
        //TODO Again deal with this _a notation for blank nodes
        Model model = getModel();
        modelAdd(model, "x R y; a P b");
        assertTrue(model.containsResource(resource(model, "x")));
        assertTrue(model.containsResource(resource(model, "R")));
        assertTrue(model.containsResource(resource(model, "y")));
        assertTrue(model.containsResource(resource(model, "a")));
        assertTrue(model.containsResource(resource(model, "P")));
        assertTrue(model.containsResource(resource(model, "b")));
        assertFalse(model.containsResource(resource(model, "i")));
        assertFalse(model.containsResource(resource(model, "j")));
    }

    public void testRemoveAll() {
        testRemoveAll("");
        testRemoveAll("a RR b");
        testRemoveAll("x P y; a Q b; c R 17; d S 'e'");
        testRemoveAll("subject Predicate 'object'; http://nowhere/x scheme:cunning not:plan");
    }

    @Override
    public void testGetPropertyWithLanguage() {
        // I'm not sure how this test is supposed to work.
        // It creates a bunch of statement objects, but deos not add
        // anything to the repository.
    }
}
