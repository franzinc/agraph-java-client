/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import junit.extensions.TestSetup;
import org.apache.jena.graph.Graph;

public class AGAnonGraphTest extends AGNamedGraphTest {
    private static final JenaUtil util = new JenaUtil(AGAnonGraphTest.class);
    public AGAnonGraphTest(String name) {
        super(name);
    }

    public static TestSetup suite() {
        return util;
    }

    @Override
    public Graph getGraph() {
        return util.getMaker().createGraph();
    }

}
