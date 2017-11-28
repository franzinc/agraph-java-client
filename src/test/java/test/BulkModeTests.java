/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import junit.framework.Assert;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

public class BulkModeTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void bulkMode_rfe10303() throws Exception {
        Assert.assertFalse("expected bulkMode false", repo.isBulkMode());
        Assert.assertTrue("expected autoCommit true", conn.isAutoCommit());
        repo.setBulkMode(true);
        Assert.assertTrue("expected autoCommit true", conn.isAutoCommit());
        Assert.assertTrue("expected bulkMode true", repo.isBulkMode());
        String path1 = "/tutorial/java-vcards.rdf";
        IRI context = vf.createIRI("http://example.org#vcards");
        Util.add(conn, path1, null, RDFFormat.RDFXML, context);
        assertEquals("expected 16 vcard triples", 16, conn.size(context));
        conn.setAutoCommit(false);
        Assert.assertFalse("expected autoCommit false", conn.isAutoCommit());
        Assert.assertTrue("expected bulkMode true", repo.isBulkMode());
        String path2 = "/tutorial/java-kennedy.ntriples";
        Util.add(conn, path2, null, RDFFormat.NTRIPLES);
        assertEquals("expected 1214 kennedy triples", 1214, conn.size((Resource) null));
        assertEquals("expected 1230 total triples", 1230, conn.size());
        conn.rollback();
        assertEquals("expected 0 kennedy triples", 0, conn.size((Resource) null));
        assertEquals("expected 16 total triples", 16, conn.size());
    }

}
