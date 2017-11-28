/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGRepositoryConnection;
import junit.framework.Test;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphMaker;
import org.apache.jena.graph.test.AbstractTestReifier;

public class AGReifierTest extends AbstractTestReifier {
    private static final JenaUtil util = new JenaUtil(AGReifierTest.class);
    private static int graphId = 0;
    protected AGRepositoryConnection conn = null;
    protected AGGraphMaker maker = null;

    public AGReifierTest(String name) {
        super(name);
    }

    public static Test suite() {
        return util;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        conn = util.getConn();
        maker = util.getMaker();
    }

    @Override
    public Graph getGraph() {
        GraphMaker maker = new AGGraphMaker(conn);
        Graph graph = maker.createGraph("http://named" + graphId);
        graphId++;
        return graph;
    }


    /**
     * Override as AG doesn't test for reification clash.
     *
     * @see org.apache.jena.graph.test.AbstractTestReifier#testReificationClash(java.lang.String)
     */
    @Override
    protected void testReificationClash(String clashingStatement) {
    }

}
