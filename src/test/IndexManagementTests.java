/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class IndexManagementTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void manageIndices_rfe9930() throws Exception {
	int indexCount = 7;
		List<String> indices = conn.listValidIndices();
		Assert.assertTrue("expected more valid indices", indices.size() >= 24);
		indices = conn.listIndices();
		Assert.assertEquals("unexpected number of indices initially", indexCount, indices.size());
		conn.dropIndex("gospi");
		conn.dropIndex("spogi");
		indices = conn.listIndices();
		Assert.assertEquals("unexpected number of indices after drop", (indexCount-2), indices.size());
		conn.addIndex("gospi");
		indices = conn.listIndices();
		Assert.assertEquals("unexpected number of indices after add", (indexCount-1), indices.size());
		conn.addIndex("gospi");
		indices = conn.listIndices();
		Assert.assertEquals("unexpected number of indices after redundant add", (indexCount-1), indices.size());
    }

}
