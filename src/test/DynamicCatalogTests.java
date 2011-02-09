/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.franz.agraph.http.AGHttpException;
import com.franz.agraph.repository.AGCatalog;

public class DynamicCatalogTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void dynamicCatalogs_rfe10175() throws Exception {
    	String catalogID = "dynamicCatalog1";
    	String repoID = "repo1";
    	server.deleteCatalog(catalogID);
    	Assert.assertFalse("unexpected catalog", server.hasCatalog(catalogID));
    	AGCatalog catalog = server.createCatalog(catalogID);
    	Assert.assertTrue("missing catalog", server.hasCatalog(catalogID));
    	try {
    		server.createCatalog(catalogID,true);
    		Assert.fail("expected exception");
    	} catch (AGHttpException e) {
    		// expected
    	}
    	catalog.createRepository(repoID);
    	Assert.assertTrue("expected repository", catalog.hasRepository(repoID));
    	catalog.deleteRepository(repoID);
    	Assert.assertFalse("unexpected repository", catalog.hasRepository(repoID));
    	server.deleteCatalog(catalogID);
    	Assert.assertFalse("unexpected catalog", server.hasCatalog(catalogID));
    }

}
