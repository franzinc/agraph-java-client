/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphUtil;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.test.AbstractTestGraph;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.rdf4j.repository.RepositoryException;

public class AGGraphTest extends AbstractTestGraph {

	public static String SERVER_URL = System.getProperty(
			"com.franz.agraph.test.serverURL", "http://localhost:10035");
	public static String CATALOG_ID = System.getProperty(
			"com.franz.agraph.test.catalogID", "/");
	public static String REPOSITORY_ID = System.getProperty(
			"com.franz.agraph.test.repositoryID", "testRepo");
	public static String USERNAME = System.getProperty(
			"com.franz.agraph.test.username", "test");
	public static String PASSWORD = System.getProperty(
			"com.franz.agraph.test.password", "xyzzy");

	protected static AGRepositoryConnection conn = null;
	protected static AGGraphMaker maker = null;

	private static int graphId = 0;
	
	public AGGraphTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(AGGraphTest.class);
		suite.addTestSuite(AGAnonGraphTest.class);

		TestSetup wrapper = new TestSetup(suite) {
			protected void setUp() {
				setUpOnce();
			}

			protected void tearDown() {
				tearDownOnce();
			}
		};

		return wrapper;
	}

	public static void setUpOnce() {
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
		AGCatalog catalog = server.getRootCatalog();
		try {
			catalog.deleteRepository(REPOSITORY_ID);
			AGRepository repo = catalog.createRepository(REPOSITORY_ID);
			repo.initialize();
			conn = repo.getConnection();
			maker = new AGGraphMaker(conn);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setUp() {
		try {
			conn.clear();
		} catch (RepositoryException e) {
			throw new RuntimeException("Unable to clear connection.");
		}
	}

	public static void tearDownOnce() {
		maker.close();
		try {
			conn.close();
		} catch (RepositoryException e) {
			throw new RuntimeException("Unable to close connection.");
		}
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
		testRemoveAll( "" );
        testRemoveAll( "a R b" );
        testRemoveAll( "c S d; e:ff GGG hhhh; i J 27; Ell Em 'en'" );
	}
	
	 public void testRemoveAll( String triples )
	    {
	        Graph g = getGraph();
	        graphAdd( g, triples );
	        g.clear();
	        assertTrue( g.isEmpty() );
	    }
	 
	@Override
	public void testIsomorphismFile()  {
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
    public void testContainsNode()
    {
        Graph g = getGraph();
        graphAdd( g, "a P b; c Q d; a R 12" );
        assertTrue( containsNode( g, node( "a" ) ) );
        assertTrue( containsNode( g, node( "P" ) ) );
        assertTrue( containsNode( g, node( "b" ) ) );
        assertTrue( containsNode( g, node( "c" ) ) );
        assertTrue( containsNode( g, node( "Q" ) ) );
        assertTrue( containsNode( g, node( "d" ) ) );       
        assertTrue( containsNode( g, node( "R" ) ) );
        assertTrue( containsNode( g, node( "12" ) ) );        
        assertFalse( containsNode( g, node( "x" ) ) );
        assertFalse( containsNode( g, node( "_y" ) ) );
        assertFalse( containsNode( g, node( "99" ) ) );
    }
	
	private boolean containsNode(Graph g, Node node)
	    {
	        return GraphUtil.containsNode(g, node) ;
	    }
    
}
