/******************************************************************************
** Copyright (c) 2008-2012 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.io.FileNotFoundException;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.openrdf.repository.RepositoryException;

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.graph.test.AbstractTestGraph;

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
		super.testRemoveAll();
	}
	
	@Override
	public void testIsomorphismFile() throws FileNotFoundException {
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
        QueryHandler qh = g.queryHandler();
        assertTrue( qh.containsNode( node( "a" ) ) );
        assertTrue( qh.containsNode( node( "P" ) ) );
        assertTrue( qh.containsNode( node( "b" ) ) );
        assertTrue( qh.containsNode( node( "c" ) ) );
        assertTrue( qh.containsNode( node( "Q" ) ) );
        assertTrue( qh.containsNode( node( "d" ) ) );
//        assertTrue( qh.containsNode( node( "10" ) ) );
//        assertTrue( qh.containsNode( node( "11" ) ) );
        assertTrue( qh.containsNode( node( "12" ) ) );
    /* */
        assertFalse( qh.containsNode( node( "x" ) ) );
//        assertFalse( qh.containsNode( node( "_y" ) ) );
        assertFalse( qh.containsNode( node( "99" ) ) );
        }
    
}
