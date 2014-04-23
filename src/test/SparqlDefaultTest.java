package test;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Resource;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class SparqlDefaultTest extends AGAbstractTest {

    /**
     * Tests DEFAULT using a Sesame MemoryStore.
     */
	@Test
	@Category(TestSuites.NotApplicableForAgraph.class)
	public void testMemoryStoreDEFAULT() throws Exception {
		Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		RepositoryConnection conn = repo.getConnection();
		testDEFAULT(conn);
	}

	/**
	 * Tests DEFAULT using an AG store.
	 */
	@Test
	@Category(TestSuites.Prepush.class)
	public void testAGDEFAULT() throws Exception {
		testDEFAULT(conn);
	}
	
	private void testDEFAULT(RepositoryConnection conn)
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
		
		// Add the default graph to graph3
		update = "ADD DEFAULT TO <eh:graph3>";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();
		Assert.assertEquals(4, conn.size(g3)); // so, just from the null context
		Assert.assertEquals(12, conn.size());

		conn.prepareUpdate(QueryLanguage.SPARQL, "DROP DEFAULT").execute();
		Assert.assertEquals(0, conn.size((Resource)null));
		Assert.assertEquals(8, conn.size());
	}
	
}
