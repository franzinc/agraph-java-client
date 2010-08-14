/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
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
		List<String> indices = conn.listValidIndices();
		Assert.assertTrue("expected more valid indices", indices.size() >= 24);
		indices = conn.listIndices();
		Assert.assertEquals("unexpected number of indices initially", 6, indices.size());
		conn.dropIndex("gospi");
		conn.dropIndex("spogi");
		indices = conn.listIndices();
		Assert.assertEquals("unexpected number of indices after drop", 4, indices.size());
		conn.addIndex("gospi");
		indices = conn.listIndices();
		Assert.assertEquals("unexpected number of indices after add", 5, indices.size());
		conn.addIndex("gospi");
		indices = conn.listIndices();
		Assert.assertEquals("unexpected number of indices after redundant add", 5, indices.size());
    }

}
