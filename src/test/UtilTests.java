/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class UtilTests {
	
	@Test
	public void randomLong() throws Exception {
		Util.RandomLong r = new Util.RandomLong();
		final long max = (((long)Integer.MAX_VALUE + 10L));
		long near = 0;
		long count = 0;
		final long start = System.nanoTime();
		while ((System.nanoTime() - start) < TimeUnit.SECONDS.toNanos(5)) {
			count++;
			long next = r.nextLong(max);
			if (next >=  max) {
				Assert.fail("next=" + next + " i=" + max);
			}
			if (next > (max - 10000)) {
				near++;
			}
		}
		Assert.assertTrue("count=" + count + " near=" + near, near > 10);
	}
	
}
