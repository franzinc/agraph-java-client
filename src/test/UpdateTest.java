package test;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.http.HTTPRepository;


public class UpdateTest extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void testAGRepository() throws Exception {
    	runTestUpdate(repo);
    }
    
    @Test
    @Category(TestSuites.Prepush.class)
    public void testHTTPRepository() throws Exception {
    	HTTPRepository httprepo = 
    		new HTTPRepository(repo.getRepositoryURL());
    	httprepo.initialize();
    	httprepo.setUsernameAndPassword(username(), password());
    	runTestUpdate(httprepo);
    	httprepo.shutDown();
    }
    
	private void runTestUpdate(Repository repo) throws Exception {
		RepositoryConnection conn = repo.getConnection();
		conn.clear();
		long start = System.currentTimeMillis();
        String updateString = "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n"
        	+ "INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  \"Fundamentals of Compiler Design\" } }";
		conn.prepareUpdate(QueryLanguage.SPARQL, updateString).execute();
		System.out.println(" Update performed in " + (System.currentTimeMillis()-start) + " ms.");
		Assert.assertEquals(1,conn.size());
		conn.close();
	}
	
}
