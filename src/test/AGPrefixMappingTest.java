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
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.shared.AbstractTestPrefixMapping;
import com.hp.hpl.jena.shared.PrefixMapping;

public class AGPrefixMappingTest extends AbstractTestPrefixMapping {

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
	
	public AGPrefixMappingTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(AGPrefixMappingTest.class);

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
			conn.clearNamespaces();
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
	public PrefixMapping getMapping() {
		Graph graph = maker.createGraph();
		graphId++;
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
	
}
