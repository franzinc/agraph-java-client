/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.util;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import test.TestSuites;

public class CljTest {

	@Test
    @Category(TestSuites.Prepush.class)
	public void str() throws Exception {
		Assert.assertEquals("1", Clj.str(1));
		Assert.assertEquals("12", Clj.str(1, 2));
		Assert.assertEquals("123", Clj.str(1, 2, 3));
		Assert.assertEquals("[1, 2]", Clj.str(Arrays.asList( new Object[] {1, 2})));
		Assert.assertEquals("[1, 2]", Clj.str(Arrays.asList(1, 2)));
		Assert.assertEquals("1-2", Clj.applyStr( Clj.interpose("-", Arrays.asList(1, 2))));
		// TODO Assert.assertEquals("[1, 2]", Clj.str(new Object[] {1, 2}));
	}
	
}
