/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGRepositoryConnection;
import junit.framework.Test;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.test.AbstractTestGraph;
import org.junit.AfterClass;
import org.junit.Before;

public class AGNamedGraphTest extends AbstractTestGraph {
    protected static JenaUtil util = new JenaUtil(AGNamedGraphTest.class);
    private static int graphId = 0;
    protected AGRepositoryConnection conn = null;
    protected AGGraphMaker maker = null;

    public AGNamedGraphTest(String name) {
        super(name);
    }

    public static Test suite() {
        return util;
    }

    @AfterClass
    public static void tearDownOnce() {
        util.disconnect();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        conn = util.getConn();
        maker = util.getMaker();
    }

    @Override
    public Graph getGraph() {
        Graph graph = maker.createGraph("http://named" + graphId);
        graphId++;
        return graph;
    }


    @Override
    public void testRemoveAll() {
        //super.testRemoveAll();
        testRemoveAll("");
        testRemoveAll("a R b");
        testRemoveAll("c S d; e:ff GGG hhhh; i J 27; Ell Em 'en'");
    }

    public void testRemoveAll(String triples) {
        Graph g = getGraph();
        graphAdd(g, triples);
        g.clear();
        assertTrue(g.isEmpty());
    }

    @Override
    public void testIsomorphismFile() {
        // TODO: this test is confused by blank node id's not being
        // preserved; testIsomorphismFile uses a ModelCom via
        // ModelFactory.createModelForGraph, and its read method
        // iterates over single adds, losing a bnode id's "scope".
        //super.testIsomorphismFile();
    }

    @Override
    public void testBulkUpdate() {
        super.testBulkUpdate();
    }

    @Override
    public void testContainsByValue() {
        super.testContainsByValue();
    }

    /**
     * override to avoid using blank nodes -- their '_x' labels
     * appear to be illegal.  TODO add the blank nodes in a more
     * proper way.
     */
    public void testContainsConcrete() {
        Graph g = getGraphWith("s P o; x R y; x S 0");
        assertTrue(g.contains(triple("s P o")));
        assertTrue(g.contains(triple("x R y")));
        assertTrue(g.contains(triple("x S 0")));
        /* */
        assertFalse(g.contains(triple("s P Oh")));
        assertFalse(g.contains(triple("S P O")));
        assertFalse(g.contains(triple("s p o")));
        assertFalse(g.contains(triple("x r y")));
        assertFalse(g.contains(triple("x S 1")));
    }

    /**
     * override to avoid using blank nodes -- their '_x' labels
     * appear to be illegal, and avoid using literals in predicate
     * position.  TODO add the blank nodes in a more proper way.
     */
    @Override
    public void testContainsNode() {
        Graph g = getGraph();
        graphAdd(g, "a P b; c Q d; a R 12");
        assertTrue(containsNode(g, node("a")));
        assertTrue(containsNode(g, node("P")));
        assertTrue(containsNode(g, node("b")));
        assertTrue(containsNode(g, node("c")));
        assertTrue(containsNode(g, node("Q")));
        assertTrue(containsNode(g, node("d")));
        assertTrue(containsNode(g, node("R")));
        assertTrue(containsNode(g, node("12")));
        assertFalse(containsNode(g, node("x")));
        assertFalse(containsNode(g, node("_y")));
        assertFalse(containsNode(g, node("99")));
    }

    private boolean containsNode(Graph g, Node node) {
        return GraphUtil.containsNode(g, node);
    }

}
