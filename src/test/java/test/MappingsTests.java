/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import junit.framework.Assert;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MappingsTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void testClearMappings() throws Exception {
        conn.clearMappings();
        String[] mappings = conn.getDatatypeMappings();
        int numAutoMappings = mappings.length;
        Assert.assertTrue("expected some automatic datatype mappings", numAutoMappings > 0);
        // one way of confirming that primitive mappings are present
        // or absent is to check for precision loss due to encoding
        // of the double below.
        IRI myDouble = vf.createIRI("http://example.org/mydouble");
        BNode s = vf.createBNode();
        IRI p = vf.createIRI("http://example.org/hasAge");
        String d = "1.86733E1";
        Literal o = vf.createLiteral(d, myDouble);
        conn.registerDatatypeMapping(myDouble, XMLSchema.DOUBLE);
        conn.add(s, p, o);
        RepositoryResult<Statement> results = conn.getStatements(s, p, null, false);
        Value v = results.next().getObject();
        Assert.assertNotSame("expected loss of precision due to encoding as a double", d, v.stringValue());
        conn.clear();
        Assert.assertEquals(0, conn.size());
        conn.clearMappings(true); // removes auto-mapping of xsd primitive types
        mappings = conn.getDatatypeMappings();
        Assert.assertEquals("expected no mappings", 0, mappings.length);
        // confirm xsd:double is no longer auto-mapped
        conn.add(s, p, o);
        results = conn.getStatements(s, p, null, false);
        v = results.next().getObject();
        Assert.assertEquals("expected no auto-mapping of xsd:double", d, v.stringValue());
        conn.clearMappings(); // reestablish auto-mappings of primitive types
        mappings = conn.getDatatypeMappings();
        Assert.assertEquals("expected auto-mappings", numAutoMappings, mappings.length);
    }

}
