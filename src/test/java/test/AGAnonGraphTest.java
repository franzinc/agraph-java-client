/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import junit.framework.Test;
import org.apache.jena.graph.Graph;

public class AGAnonGraphTest extends AGNamedGraphTest {
    public AGAnonGraphTest(String name) {
        super(name);
    }

    public static Test suite() {
        util = new JenaUtil(AGAnonGraphTest.class);
        return util;
    }

    @Override
    public Graph getGraph() {
        return maker.createGraph();
    }

}
