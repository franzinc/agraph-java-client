/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFFormat;

public class BulkModeTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void bulkMode_rfe10303() throws Exception {
    	Assert.assertFalse("expected bulkMode false", repo.isBulkMode());
    	Assert.assertTrue("expected autoCommit true", conn.isAutoCommit());
    	repo.setBulkMode(true);
    	Assert.assertTrue("expected autoCommit true", conn.isAutoCommit());
    	Assert.assertTrue("expected bulkMode true", repo.isBulkMode());
        String path1 = "src/tutorial/java-vcards.rdf";    
        URI context = vf.createURI("http://example.org#vcards");
        conn.add(new File(path1), null, RDFFormat.RDFXML, context);
        assertEquals("expected 16 vcard triples", 16, conn.size(context));
        conn.setAutoCommit(false);
        Assert.assertFalse("expected autoCommit false", conn.isAutoCommit());
        Assert.assertTrue("expected bulkMode true", repo.isBulkMode());
        String path2 = "src/tutorial/java-kennedy.ntriples";            
        conn.add(new File(path2), null, RDFFormat.NTRIPLES);
        assertEquals("expected 1214 kennedy triples", 1214, conn.size((Resource)null));
        assertEquals("expected 1230 total triples", 1230, conn.size());
        conn.rollback();
        assertEquals("expected 0 kennedy triples", 0, conn.size((Resource)null));
        assertEquals("expected 16 total triples", 16, conn.size());
   }

}
