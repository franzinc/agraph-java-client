/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;


public class LiteralAndResourceResultsTest extends AGAbstractTest {

	@Test
	@Category(TestSuites.Prepush.class)
	/**
	 * Shows how to get subclasses of Value (Literal, URI, BNode)
	 * back from results (rather than just getting Values), and 
	 * confirms that the results are as expected.
	 * 
	 * @throws Exception
	 */
	public void testGetTypedResults() throws Exception {
		BNode b = vf.createBNode();
		URI r = vf.createURI("http://r");
		Literal lit = vf.createLiteral("42", XMLSchema.INT);
		conn.add(b, r, lit);
		String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
		TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL,
		queryString);
		TupleQueryResult result = tupleQuery.evaluate();

		while (result.hasNext()) {
		   BindingSet bindingSet = result.next();
		   BNode s = (BNode)bindingSet.getValue("s");
		   Assert.assertEquals(b, s);
		   URI p = (URI)bindingSet.getValue("p");
		   Assert.assertEquals(r, p);
		   Value o = bindingSet.getValue("o");
		   if (o instanceof Literal) {
			   Literal l = (Literal)o;
			   Assert.assertEquals(lit, l);
			   int i = l.intValue();
			   Assert.assertEquals(42, i);
			   URI dt = l.getDatatype();
			   Assert.assertEquals(XMLSchema.INT, dt);
		   }
		}			
	}

}
