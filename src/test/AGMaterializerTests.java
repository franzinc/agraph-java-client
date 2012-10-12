/******************************************************************************
** Copyright (c) 2008-2012 Franz Inc.
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
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import com.franz.agraph.repository.AGMaterializer;
import com.franz.agraph.repository.AGRDFFormat;

public class AGMaterializerTests extends AGAbstractTest {

	@Test
	@Category(TestSuites.Prepush.class)
	public void materializeOverDefaultGraph() throws Exception {
		URI a = vf.createURI("http://a");
		URI p = vf.createURI("http://p");
		URI A = vf.createURI("http://A");
		conn.add(a,p,a);
		conn.add(p,RDFS.DOMAIN,A);
		AGMaterializer materializer = AGMaterializer.newInstance();
		materializer.withRuleset("all");
		Assert.assertEquals(1, conn.materialize(materializer));
		Assert.assertEquals("expected size 3", 3, conn.size());
		Assert.assertTrue(conn.hasStatement(a, RDF.TYPE, A, false));
		Assert.assertEquals(1, conn.deleteMaterialized());
		Assert.assertFalse(conn.hasStatement(a, RDF.TYPE, A, false));
		Assert.assertEquals("expected size 2", 2, conn.size());
	}
	
	@Test
	@Category(TestSuites.Prepush.class)
	public void materializeOverDefaultGraphTransactional() throws Exception {
		URI a = vf.createURI("http://a");
		URI p = vf.createURI("http://p");
		URI A = vf.createURI("http://A");
		conn.setAutoCommit(false);
		conn.add(a,p,a);
		conn.add(p,RDFS.DOMAIN,A);
		AGMaterializer materializer = AGMaterializer.newInstance();
		materializer.withRuleset("all");
		Assert.assertEquals(1, conn.materialize(materializer));
		Assert.assertEquals("expected size 3", 3, conn.size());
		Assert.assertTrue(conn.hasStatement(a, RDF.TYPE, A, false));
		Assert.assertEquals(1, conn.deleteMaterialized());
		Assert.assertFalse(conn.hasStatement(a, RDF.TYPE, A, false));
		Assert.assertEquals("expected size 2", 2, conn.size());
	}
	
	@Test
	@Category(TestSuites.Broken.class)
	public void materializeOverDefaultGraphUseTypeSubproperty() throws Exception {
		URI a = vf.createURI("http://a");
		URI A = vf.createURI("http://A");
		URI B = vf.createURI("http://B");
		URI mytype = vf.createURI("http://mytype");
		conn.add(A,RDFS.SUBCLASSOF,B);
		conn.add(mytype,RDFS.SUBPROPERTYOF,RDF.TYPE);
		conn.add(a,mytype,A);
		AGMaterializer materializer = AGMaterializer.newInstance();
		//materializer.withRuleset("all");
		materializer.setUseTypeSubproperty(true);
		conn.materialize(materializer);
		Assert.assertEquals("expected size 4", 4, conn.size());
		Assert.assertTrue(conn.hasStatement(a, RDF.TYPE, A, false));
		conn.deleteMaterialized();
		Assert.assertFalse(conn.hasStatement(a, RDF.TYPE, A, false));
		Assert.assertEquals("expected size 3", 3, conn.size());
	}
	
    @Test
    @Category(TestSuites.Broken.class)
    public void materializeOverNamedGraphs() throws Exception {
    	conn.add(new File("src/test/example.nq"), null, AGRDFFormat.NQUADS);
    	conn.add(vf.createURI("http://xmlns.com/foaf/0.1/name"),RDFS.DOMAIN, OWL.INDIVIDUAL);
    	Assert.assertEquals("expected size 11", 11, conn.size());
    	conn.materialize(null);
    	Assert.assertFalse(conn.hasStatement(vf.createURI("http://www.franz.com/materialized"), null, null,false));
    	Assert.assertEquals("expected size 14", 14, conn.size());
    }
    
}
