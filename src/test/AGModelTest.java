/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.openrdf.repository.RepositoryException;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.test.AbstractTestModel;

public class AGModelTest extends AbstractTestModel {

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
	
	public AGModelTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(AGModelTest.class);

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
			super.setUp();
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
	public Model getModel() {
		AGGraph graph = maker.createGraph("http://anon" + graphId);
		graphId++;
		return new AGModel(graph);
	}

	
    public void testContainsResource()
    {
    	//TODO Again deal with this _a notation for blank nodes
    	Model model = getModel();
    modelAdd( model, "x R y; a P b" );
    assertTrue( model.containsResource( resource( model, "x" ) ) );
    assertTrue( model.containsResource( resource( model, "R" ) ) );
    assertTrue( model.containsResource( resource( model, "y" ) ) );
    assertTrue( model.containsResource( resource( model, "a" ) ) );
    assertTrue( model.containsResource( resource( model, "P" ) ) );
    assertTrue( model.containsResource( resource( model, "b" ) ) );
    assertFalse( model.containsResource( resource( model, "i" ) ) );
    assertFalse( model.containsResource( resource( model, "j" ) ) );
    }
    
    public void testRemoveAll()
    {
    testRemoveAll( "" );
    testRemoveAll( "a RR b" );
    testRemoveAll( "x P y; a Q b; c R 17; d S 'e'" );
    testRemoveAll( "subject Predicate 'object'; http://nowhere/x scheme:cunning not:plan" );
    }

}
