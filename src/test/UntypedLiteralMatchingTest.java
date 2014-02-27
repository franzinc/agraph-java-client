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

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecution;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

public class UntypedLiteralMatchingTest extends AGAbstractTest {

    /**
     * This was a failing ARQ test sent in by Holger (spr38458)
     * 
     */
	@Test
	@Category(TestSuites.Broken.class)
	public void testARQUntypedLiteralMatching() {
		// Model model = AG.getInstance().createModel("http://aldi.com.au");
		AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
		AGGraph graph = closeLater(maker.createGraph("http://aldi.com.au"));
		AGModel model = closeLater(new AGModel(graph));

		String str = "MatchThis";
		model.begin();
		Resource subject = OWL.Thing;
		Property predicate = RDFS.label;
		RDFNode typedObject = ResourceFactory.createTypedLiteral(str);
		model.add(subject, predicate, typedObject);
		model.commit();

		Query query = QueryFactory.create("ASK WHERE { <" + subject + "> <"
				+ predicate + "> ?x }");

		// This works fine
		{
			QuerySolutionMap initialBinding = new QuerySolutionMap();
			initialBinding.add("x", typedObject);
			QueryExecution qexec = QueryExecutionFactory.create(query, model,
					initialBinding);
			Assert.assertTrue(qexec.execAsk());
		}

		// This fails
		{
			QuerySolutionMap initialBinding = new QuerySolutionMap();
			initialBinding.add("x", ResourceFactory.createPlainLiteral(str));
			QueryExecution qexec = QueryExecutionFactory.create(query, model,
					initialBinding);
			Assert.assertTrue(qexec.execAsk());
		}

		model.begin();
		model.removeAll();
		model.commit();
		model.abort();
	}

	@Test
    @Category(TestSuites.Prepush.class)
    /**
     * This is the AG equivalent of that test (rfe10983)
     */
	public void testAGUntypedLiteralMatching() {
		// Model model = AG.getInstance().createModel("http://aldi.com.au");
		AGGraphMaker maker = closeLater(new AGGraphMaker(conn));
		AGGraph graph = closeLater(maker.createGraph("http://aldi.com.au"));
		AGModel model = closeLater(new AGModel(graph));

		String str = "MatchThis";
		model.begin();
		Resource subject = OWL.Thing;
		Property predicate = RDFS.label;
		RDFNode typedObject = ResourceFactory.createTypedLiteral(str);
		model.add(subject, predicate, typedObject);
		model.commit();

		AGQuery query = AGQueryFactory.create("ASK WHERE { <" + subject + "> <"
				+ predicate + "> ?x }");

		// This works fine
		{
			QuerySolutionMap initialBinding = new QuerySolutionMap();
			initialBinding.add("x", typedObject);
			AGQueryExecution qexec = AGQueryExecutionFactory.create(query,
					model, initialBinding);
			Assert.assertTrue(qexec.execAsk());
		}

		// This works too
		{
			QuerySolutionMap initialBinding = new QuerySolutionMap();
			initialBinding.add("x", ResourceFactory.createPlainLiteral(str));
			AGQueryExecution qexec = AGQueryExecutionFactory.create(query,
					model, initialBinding);
			Assert.assertTrue(qexec.execAsk());
		}

		model.begin();
		model.removeAll();
		model.commit();
		model.abort();
	}

}
