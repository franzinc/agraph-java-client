/******************************************************************************
** Copyright (c) 2008-2012 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SpogiTripleCacheTests extends AGAbstractTest {

	public static long SIZE = 12345;
	
    @Test
    @Category(TestSuites.Prepush.class)
    public void spogiCache_rfe10059() throws Exception {
		Assert.assertEquals("expected no spogi cache", 0, conn.getTripleCacheSize());
		conn.enableTripleCache(12345);
		Assert.assertEquals("expected spogi cache size"+SIZE, SIZE, conn.getTripleCacheSize());
		conn.setAutoCommit(false);
		Assert.assertEquals("expected spogi cache size"+SIZE, SIZE, conn.getTripleCacheSize());
		conn.disableTripleCache();
		Assert.assertEquals("expected no spogi cache", 0, conn.getTripleCacheSize());
    }

}
