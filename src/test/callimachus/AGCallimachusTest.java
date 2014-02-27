/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.callimachus;

import junit.framework.Assert;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.RepositoryException;

import test.AGAbstractTest;
import test.TestSuites;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;

public class AGCallimachusTest extends AGAbstractTest {

    public static final String CALLIMACHUS_REPO_ID = "callimachusTest";
    private static AGRepository callimachusRepo = null;

    /**
     * Returns a shared AG Callimachus repository to use for testing purposes.
     * The AGRepository is a Callimachus store, which comes ready with certain
     * triggers and auditing capabilites.
     * 
     * The shared repository is deleted/created on first use, and is
     * simply cleared on subsequent uses; this speeds up testing as
     * deleting/creating a new repository can take some time.
     * 
     * @return
     * @throws RepositoryException
     */
    public static AGRepository sharedCallimachusRepository() throws RepositoryException {
    	if (callimachusRepo==null) {
    		AGCatalog cat = newAGServer().getCatalog(CATALOG_ID);
    		cat.deleteRepository(CALLIMACHUS_REPO_ID);
    		callimachusRepo = cat.createRepository(CALLIMACHUS_REPO_ID);
    		convertToCallimachusRepository(callimachusRepo);
    	} else {
    		AGRepositoryConnection conn = callimachusRepo.getConnection();
    		conn.clear();
    		conn.clearNamespaces();
    	}
    	return callimachusRepo;
    }
    
	public static void convertToCallimachusRepository(AGRepository repo) throws RepositoryException {
		String url = repo.getRepositoryURL() + "/callimachus";
		Header[] headers = {};
		AGRepositoryConnection conn = repo.getConnection();
		conn.getHttpRepoClient().getHTTPClient().post(url, headers, new NameValuePair[0],
					(RequestEntity) null, null);
	}

    @Test
    @Category(TestSuites.Prepush.class)
    public void testUpdate() throws Exception {
    	AGRepositoryConnection conn = sharedCallimachusRepository().getConnection();
    	Assert.assertEquals("Expected empty Callimachus repo", 0, conn.size());
    	URI s = vf.createURI("http://example/book1");
    	URI p = vf.createURI("http://purl.org/dc/elements/1.1/title");
    	Literal o_wrong = vf.createLiteral("Fundamentals of Compiler Desing");
    	Literal o_right = vf.createLiteral("Fundamentals of Compiler Design");
    	URI g = vf.createURI("http://example/bookStore");
    	conn.add(s,p,o_wrong,g);
    	
    	// Perform a sequence of SPARQL UPDATE queries in one request to correct the title
        String queryString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
        	+ "DELETE DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Desing\" } } ; \n"
        	+ "\n"
        	+ "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
        	+ "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";

        Update u = conn.prepareUpdate(QueryLanguage.SPARQL, queryString);
        u.execute();
        Assert.assertTrue("Title should be correct", conn.hasStatement(s,p,o_right,false,g));
        Assert.assertFalse("Incorrect title should be gone", conn.hasStatement(s,p,o_wrong,false,g));
   }

/*  
// leaving off for now; cf. rfe12026
    @Test
    @Category(TestSuites.Prepush.class)
    public void testTriggerAuditing() throws Exception {
    	AGRepositoryConnection conn = sharedCallimachusRepository().getConnection();
    	Assert.assertEquals("Expected empty Callimachus repo", 0, conn.size());
    	URI e1 = vf.createURI("http://example.com/entity1");
    	Literal o_label = vf.createLiteral("Old Label");
    	URI g0 = vf.createURI("http://example.com/graph0");
    	conn.add(e1,RDFS.LABEL,o_label,g0);
    	Assert.assertEquals("Expected 4 statements after trigger", 4, conn.size());

    	String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
    			+ "DELETE {<http://example.com/entity1> rdfs:label ?oldLabel} \n"
    			+ "INSERT {<http://example.com/entity1> rdfs:label \"New Label\"} \n"
    			+ "WHERE {<http://example.com/entity1> rdfs:label ?oldLabel}; \n";
    			
    	Update u = conn.prepareUpdate(QueryLanguage.SPARQL, queryString);
    	DatasetImpl ds = new DatasetImpl();
    	URI a1 = vf.createURI("http://example.com/activity1");
    	ds.setDefaultInsertGraph(a1);
    	u.setDataset(ds);
    	u.execute();
    	Assert.assertEquals("g0 should be empty", 0, conn.size(g0));
    	Assert.assertEquals("activity1 should have 12 statements after update", 12, conn.size(a1));
    }
*/
  
}
