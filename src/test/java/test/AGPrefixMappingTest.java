/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGRepositoryConnection;
import junit.extensions.TestSetup;
import org.apache.jena.graph.Graph;
import org.apache.jena.shared.AbstractTestPrefixMapping;
import org.apache.jena.shared.PrefixMapping;
import org.junit.Ignore;
import org.junit.Test;

public class AGPrefixMappingTest extends AbstractTestPrefixMapping {
    private static final JenaUtil util = new JenaUtil(AGPrefixMappingTest.class);
    protected AGRepositoryConnection conn = null;
    protected AGGraphMaker maker = null;

    public AGPrefixMappingTest(String name) {
        super(name);
    }

    public static TestSetup suite() {
        return util;
    }

    @Override
    public void setUp() throws Exception {
        conn = util.getConn();
        maker = util.getMaker();
        super.setUp();
    }

    @Override
    public PrefixMapping getMapping() {
        Graph graph = util.getMaker().createGraph();
        return graph.getPrefixMapping();
    }

    @Ignore @Test @Override
    public void testStrPrefix2() {
        // Fails due to unicode character in namespace prefix, which AG does not accept
    }

    @Ignore @Test @Override
    public void testAddOtherPrefixMapping() {
        // TODO: fails needing rfe9413
    }

    @Ignore @Test @Override
    public void testEquality() {
        // TODO: fails needing rfe9413
    }

    @Ignore @Test @Override
    public void testSecondPrefixReplacesReverseMap() {
        // Fails because we do not guarantee the order
        // in which namespace mapping are returned.
    }
}
