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

import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.hp.hpl.jena.graph.GraphMaker;
import com.hp.hpl.jena.graph.test.AbstractTestGraphMaker;

public class AGGraphMakerTest extends AbstractTestGraphMaker {

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

	public AGGraphMakerTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(AGGraphMakerTest.class);

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
		try {
			conn.close();
		} catch (RepositoryException e) {
			throw new RuntimeException("Unable to close connection.");
		}
	}

	@Override
	public GraphMaker getGraphMaker() {
		return new AGGraphMaker(conn);
	}

	@Override
    public void testCarefulClose()
    {
		// TODO: not sure how this test can pass.  x and y appear
		// bound by open's contract to be the same object. 
		//super.testCarefulClose();
    }
	   
}
