/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFFormat;

public class UploadCommitPeriodTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void uploadCommitPeriod_rfe10059() throws Exception {
    	File file = new File("src/tutorial/java-kennedy.ntriples");
		Assert.assertEquals("expected commit period 0", 0, conn.getUploadCommitPeriod());
		URI g_0 = vf.createURI("urn:x-allegrograph:0");
		conn.add(file, null, RDFFormat.NTRIPLES,g_0);
		long size_0 = conn.size(g_0);
		conn.setUploadCommitPeriod(100);
		Assert.assertEquals("expected commit period 100", 100, conn.getUploadCommitPeriod());
		conn.add(file, null, RDFFormat.NTRIPLES,vf.createURI("urn:x-allegrograph:100auto"));
		conn.setAutoCommit(false);
		URI g_100nonauto = vf.createURI("urn:x-allegrograph:100nonauto");
		Assert.assertEquals("expected 0 triples in context", 0, conn.size(g_100nonauto));
		conn.add(file, null, RDFFormat.NTRIPLES,g_100nonauto);
		conn.rollback();
		Assert.assertEquals("expected triples committed", size_0, conn.size(g_100nonauto));
    }

}
