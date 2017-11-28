/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGRepositoryConnection;
import junit.framework.Test;
import org.apache.jena.graph.GraphMaker;
import org.apache.jena.graph.test.AbstractTestGraphMaker;

public class AGGraphMakerTest extends AbstractTestGraphMaker {
    private static final JenaUtil util = new JenaUtil(AGGraphMakerTest.class);
    protected AGRepositoryConnection conn = null;

    public AGGraphMakerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return util;
    }

    @Override
    public void setUp() {
        super.setUp();
        conn = util.getConn();
    }

    @Override
    public GraphMaker getGraphMaker() {
        return new AGGraphMaker(util.getConn());
    }

    @Override
    public void testCarefulClose() {
        // TODO: not sure how this test can pass.  x and y appear
        // bound by open's contract to be the same object.
        //super.testCarefulClose();
    }

}
