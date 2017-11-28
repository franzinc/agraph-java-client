/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGTupleQuery;
import junit.framework.Assert;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TripleIdTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void tripleIds_rfe10177() throws Exception {
        BNode bnode = vf.createBNode();
        IRI foo = vf.createIRI("http://Foo");
        conn.add(bnode, RDF.TYPE, foo);
        conn.add(foo, foo, foo);
        String queryString = "(select (?id) (q ?s !rdf:type ?o ?g ?id))";
        AGTupleQuery q = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
        TupleQueryResult r = q.evaluate();
        Assert.assertTrue("Expected a result", r.hasNext());
        Value id = r.next().getBinding("id").getValue();
        Assert.assertFalse("Unexpected second result", r.hasNext());
        RepositoryResult<Statement> stmts = conn.getStatements(id.stringValue());
        Assert.assertTrue("Expected a match", stmts.hasNext());
        Assert.assertEquals("Expected rdf:type", RDF.TYPE, stmts.next().getPredicate());
        Assert.assertFalse("Unexpected second match", stmts.hasNext());
    }

}
