/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DeleteDuplicatesModeTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void deleteDuplicates_rfe10373() throws Exception {
    	Assert.assertTrue("expected default mode false", "false".equals(repo.getDeleteDuplicatesMode()));
    	repo.setDeleteDuplicatesMode("true");
    	Assert.assertTrue("expected mode true", "true".equals(repo.getDeleteDuplicatesMode()));
    	repo.setDeleteDuplicatesMode("spo");
    	Assert.assertTrue("expected mode spo", "spo".equals(repo.getDeleteDuplicatesMode()));
    	repo.setDeleteDuplicatesMode("false");
    	Assert.assertTrue("expected mode false", "false".equals(repo.getDeleteDuplicatesMode()));
    	try {
    		repo.setDeleteDuplicatesMode("foo");
    		Assert.fail("expected IllegalArgumentException foo");
    	} catch (IllegalArgumentException e) {
    		// expected
    	}
   }

}
