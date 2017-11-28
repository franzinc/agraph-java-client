package test;

import junit.framework.Assert;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

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
        Resource g1 = conn.getValueFactory().createIRI("eh:graph1");
        Resource g2 = conn.getValueFactory().createIRI("eh:graph2");
        Resource g3 = conn.getValueFactory().createIRI("eh:graph3");

        String update = AGAbstractTest.readResourceAsString("/test/update-default.ru");
        conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();
        Assert.assertEquals(4, conn.size((Resource) null)); // just null context
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
        Assert.assertEquals(0, conn.size((Resource) null));
        Assert.assertEquals(8, conn.size());
    }

}
