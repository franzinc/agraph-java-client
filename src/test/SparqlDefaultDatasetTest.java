package test;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Resource;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class SparqlDefaultDatasetTest extends AGAbstractTest {

    /**
     * Tests the default dataset in a memory store.
     */
	@Test
	@Category(TestSuites.NotApplicableForAgraph.class)
	public void testMemoryStoreDefaultDataset() throws Exception {
		Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		RepositoryConnection conn = repo.getConnection();
		testSesameDefaultDataset(conn);
	}

	/**
	 * Tests the default dataset in an AG store.
	 */
	@Test
	@Category(TestSuites.Prepush.class)
	public void testAGDefaultDataset() throws Exception {
		testSesameDefaultDataset(conn);
	}
	
	private void testSesameDefaultDataset(RepositoryConnection conn)
			throws IOException, UpdateExecutionException, RepositoryException,
			MalformedQueryException, QueryEvaluationException {
		Resource g1 = conn.getValueFactory().createURI("eh:graph1");
		Resource g2 = conn.getValueFactory().createURI("eh:graph2");
		Resource g3 = conn.getValueFactory().createURI("eh:graph3");

		String update = AGAbstractTest.readFileAsString("src/test/update-default.ru");
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();
		Assert.assertEquals(4, conn.size((Resource)null)); // just null context
		Assert.assertEquals(2, conn.size(g1));
		Assert.assertEquals(2, conn.size(g2));
		Assert.assertEquals(0, conn.size(g3));
		Assert.assertEquals(8, conn.size()); // whole store, all contexts
		
		// The default graph of the default dataset is the union of all graphs 
		String query = "CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}";
		GraphQueryResult result = conn.prepareGraphQuery(QueryLanguage.SPARQL, query).evaluate();
		int i=0;
		for (; result.hasNext(); i++) {
			result.next();
		}
		// some stores (like AG) remove dups in construct, others don't, allow both
		Assert.assertTrue(i==7 || i==8);
		
		// Every named graph is in the default dataset
		query = "CONSTRUCT {?s ?p ?o} WHERE {GRAPH <eh:graph1> {?s ?p ?o}}";
		result = conn.prepareGraphQuery(QueryLanguage.SPARQL, query).evaluate();
		i=0;
		for (; result.hasNext(); i++) {
			result.next();
		}
		Assert.assertEquals(2, i);
		
		update = "INSERT DATA { <eh:c> <eh:yyyy> <eh:here> }";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();
		Assert.assertEquals(5, conn.size((Resource)null));
		
		update = "DELETE DATA {  <eh:this> <eh:is> 'triple'  . }";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();
		Assert.assertEquals(5, conn.size((Resource)null));
		Assert.assertEquals(1, conn.size(g1));
		Assert.assertEquals(1, conn.size(g2));
		Assert.assertEquals(0, conn.size(g3));
		Assert.assertEquals(7, conn.size());
		
	}
		
}
