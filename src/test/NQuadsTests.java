/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.URI;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.repository.AGRDFFormat;
import com.franz.openrdf.rio.nquads.NQuadsWriter;

public class NQuadsTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void nquads_sesame_rfe10201() throws Exception {
        conn.add(new File("src/test/example.nq"), null, AGRDFFormat.NQUADS);
        Assert.assertEquals("expected size 10", 10, conn.size());
        URI alice = vf.createURI("http://example.org/alice/foaf.rdf");
        Assert.assertEquals("expected size 7", 7, conn.size(alice));
        URI bob = vf.createURI("http://example.org/bob/foaf.rdf");
        Assert.assertEquals("expected size 3", 3, conn.size(bob));
   }

    @Test
    @Category(TestSuites.Prepush.class)
    public void nquads_jena_rfe10201() throws Exception {
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn) );
    	AGGraph graph = closeLater( maker.getUnionOfAllGraphs() );
    	AGModel model = closeLater( new AGModel(graph) );
    	model.read(new FileInputStream("src/test/example.nq"), null, "NQUADS");
    	Assert.assertEquals("expected size 10", 10, model.size());
    	model.write(new FileOutputStream("target/exampleModelWrite.nq"),"NQUADS");
    	model.removeAll();
    	Assert.assertEquals("expected size 0", 0, model.size());
    	model.read(new FileInputStream("target/exampleModelWrite.nq"), null, "NQUADS");
    	Assert.assertEquals("expected size 10", 10, model.size());
    }
    
    @Test
    @Category(TestSuites.Broken.class)
    public void sesameAddContextOverridesNQuadsContext() throws Exception {
    	URI bob = vf.createURI("http://example.org/bob/foaf.rdf");
    	// the add context is ignored -- it should override
    	conn.add(new File("src/test/example.nq"), null, AGRDFFormat.NQUADS, bob);
    	Assert.assertEquals("expected size 10", 10, conn.size(bob));
    }
    
    @Test
    @Category(TestSuites.Broken.class)
    public void jenaGraphOverridesNQuadsContext() throws Exception {
    	AGGraphMaker maker = closeLater( new AGGraphMaker(conn) );
    	AGGraph graph = closeLater( maker.getGraph() );
    	AGModel model = closeLater( new AGModel(graph) );
    	model.read(new FileInputStream("src/test/example.nq"), null, "NQUADS");
    	Assert.assertEquals("expected size 10", 10, model.size());
    }
    
}
