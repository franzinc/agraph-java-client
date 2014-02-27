/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;

public class RDFTransactionTest extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void sendRDFTransaction() throws Exception {
    	URI context1 = vf.createURI("http://example.org/context1");
    	conn.sendRDFTransaction(new FileInputStream("src/test/rdftransaction.xml"));
    	assertEquals("size", 2, conn.size((Resource)null));
    	assertEquals("size", 0, conn.size(context1));
    	conn.sendRDFTransaction(new FileInputStream("src/test/rdftransaction-1.xml"));
    	assertEquals("size", 1, conn.size((Resource)null));
    	assertEquals("size", 2, conn.size(context1));
    	assertEquals("size", 3, conn.size());
    }

}
