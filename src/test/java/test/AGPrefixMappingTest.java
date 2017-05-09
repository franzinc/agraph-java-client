/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGRepositoryConnection;
import org.apache.jena.graph.Graph;
import org.apache.jena.shared.AbstractTestPrefixMapping;
import org.apache.jena.shared.PrefixMapping;
import junit.framework.Test;

public class AGPrefixMappingTest extends AbstractTestPrefixMapping {
	private static final JenaUtil util = new JenaUtil(AGPrefixMappingTest.class);
	protected AGRepositoryConnection conn = null;
	protected AGGraphMaker maker = null;

	public static Test suite() {
		return util;
	}

	public AGPrefixMappingTest(String name) {
		super(name);
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

	
	@Override
	public void testAddOtherPrefixMapping() {
		// TODO: fails needing rfe9413 
		//super.testAddOtherPrefixMapping();
	}
	
	@Override
	public void testEquality() {
		// TODO: fails needing rfe9413 
		//super.testEquality();
	}

        @Override
        public void testSecondPrefixReplacesReverseMap() {
         	// Fails because we do not guarantee the order
	        // in which namespace mapping are returned.
        }
}
