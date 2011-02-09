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

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGCatalog;

public class DynamicCatalogTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void dynamicCatalogs_rfe10175() throws Exception {
    	String catalogID = "dynamicCatalog1";
    	String repoID = "repo1";
    	AGCatalog catalog = server.createCatalog(catalogID);
    	Assert.assertTrue("missing the expected catalog", server.hasCatalog(catalogID));
    	// should be ok to create an existing catalog
    	int numCatalogs = server.listCatalogs().size();
   		catalog = server.createCatalog(catalogID);
    	Assert.assertEquals("expected no change in catalogs", numCatalogs, server.listCatalogs().size());
    	catalog.createRepository(repoID);
    	Assert.assertTrue("expected repository", catalog.hasRepository(repoID));
    	server.deleteCatalog(catalogID);
    	Assert.assertFalse("expected catalog to be deleted", server.hasCatalog(catalogID));
    	try {
    		// catalog object shouldn't work either
    		catalog.hasRepository(repoID);
    		Assert.fail("expected catalog not found exception.");
    	} catch (AGHttpException e) {
    		// TODO: want a subclass of AGHttpException here?
    	}
    	// should be ok to delete a non-existent catalog
    	numCatalogs = server.listCatalogs().size();
    	server.deleteCatalog(catalogID);
    	Assert.assertEquals("expected no change in catalogs", numCatalogs, server.listCatalogs().size());
    	server.createCatalog(catalogID);
    	// catalog object now works again (allow that for now. TODO: reconsider?)
    	// When a catalog is deleted, repositories aren't accessible from the catalog
    	// TODO: check that repositories are also deleted from disk?
    	Assert.assertTrue("Expected no repositories in catalog.",!catalog.hasRepository(repoID));
    	server.deleteCatalog(catalogID);
    	Assert.assertFalse("unexpected catalog found", server.hasCatalog(catalogID));
    	server.deleteCatalog("/");
    	server.deleteCatalog("blahdiblah");    }

}
