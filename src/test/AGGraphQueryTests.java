/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
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
import org.openrdf.query.QueryLanguage;

import com.franz.agraph.repository.AGGraphQuery;
import com.franz.agraph.repository.AGRDFFormat;

public class AGGraphQueryTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void graphQuery_count_rfe10447() throws Exception {
        conn.add(new File("src/test/example.nq"), null, AGRDFFormat.NQUADS);
        String queryString = "construct {?s ?p ?o} where {?s ?p ?o}";
        AGGraphQuery q = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected size 10", 10, q.count());
        queryString = 
        	"construct {?s ?p ?o} where " +
        	"{GRAPH <http://example.org/alice/foaf.rdf> {?s ?p ?o}}";
        q = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected size 7", 7, q.count());
        queryString = 
        	"construct {?s ?p ?o} where " +
        	"{GRAPH <http://example.org/bob/foaf.rdf> {?s ?p ?o}}";
        q = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected size 3", 3, q.count());
        queryString = 
        	"construct {?s ?p ?o} where " +
        	"{GRAPH <http://example.org/carol/foaf.rdf> {?s ?p ?o}}";
        q = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected size 0", 0, q.count());
        queryString = 
        	"describe <http://example.org/alice/foaf.rdf#me>";
        q = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        Assert.assertEquals("expected size 7", 7, q.count());
   }

}
