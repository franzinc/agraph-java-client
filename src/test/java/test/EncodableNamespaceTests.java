/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGFormattedNamespace;
import junit.framework.Assert;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

public class EncodableNamespaceTests extends AGAbstractTest {

    @Test
    @Category(TestSuites.Prepush.class)
    public void encodableNamespaces_rfe10197() throws Exception {
        String NS0 = "http://franz.com/ns0";
        String FORMAT0 = "[a-z][0-9]-[a-f]{3}";
        Assert.assertTrue("expected none", conn.listEncodableNamespaces().isEmpty());
        conn.registerEncodableNamespace(NS0, FORMAT0);
        List<AGFormattedNamespace> namespaces = conn.listEncodableNamespaces();
        Assert.assertEquals("expected one", 1, namespaces.size());
        AGFormattedNamespace ns = namespaces.get(0);
        Assert.assertEquals("unexpected prefix", NS0, ns.getNamespace());
        Assert.assertEquals("unexpected format", FORMAT0, ns.getFormat());
        IRI uri = vf.generateURI(NS0);
        Assert.assertTrue("expected prefix " + NS0, uri.stringValue().startsWith(NS0));
        Assert.assertFalse("expected uniqueness", uri.stringValue().equals(vf.generateURI(NS0).stringValue()));
        String NS1 = "http://franz.com/ns1";
        String NS2 = "http://franz.com/ns2";
        String NS3 = "http://franz.com/ns3";
        String NS4 = "urn:franz:";
        conn.registerEncodableNamespace(NS1, "[0-1]{59}"); // 59's the max
        conn.registerEncodableNamespace(NS2, "[0-9]{18}"); // 18's the max
        try {
            conn.registerEncodableNamespace(NS3, "[0-9]{19}"); // 19's too big
            Assert.fail("expected exception for format too large.");
        } catch (RepositoryException e) {
            // expected
            //System.out.println(e.getLocalizedMessage());
        }
        conn.registerEncodableNamespace(NS4, "[a-z][0-9]-[a-f]{1,3}");
        try {
            vf.generateURI(NS4);
            Assert.fail("expected exception for format not fixed size.");
        } catch (RepositoryException e) {
            // expected
            //System.out.println(e.getLocalizedMessage());
        }
        try {
            uri = vf.generateURI(NS0);
            conn.add(uri, uri, uri);
            conn.registerEncodableNamespace(NS0, "[0-9]{10}");
            Assert.fail("expected exception for attempting reregistration.");
        } catch (RepositoryException e) {
            // expected
            //System.out.println(e.getLocalizedMessage());
        }
        namespaces = new ArrayList<AGFormattedNamespace>();
        for (int i = 0; i < 10000; i++) {
            namespaces.add(new AGFormattedNamespace("urn:franz-" + i + ":", "[0-1]{3}"));
        }
        conn.registerEncodableNamespaces(namespaces);
        Assert.assertEquals(10004, conn.listEncodableNamespaces().size());
        for (int i = 0; i < 8; i++) {
            vf.generateURI("urn:franz-1234:");
        }
        try {
            System.out.println(vf.generateURI("urn:franz-1234:"));
            Assert.fail("expected exception for no more available id's.");
        } catch (RepositoryException e) {
            // expected
            //System.out.println(e.getLocalizedMessage());
        }
        IRI[] uris = vf.generateURIs("urn:franz-9999:", 5);
        Assert.assertEquals("expected 5 URI's", 5, uris.length);
        for (int i = 0; i < uris.length; i++) {
            //System.out.println(uris[i]);
        }
        uris = vf.generateURIs("urn:franz-9999:", 5);
        Assert.assertEquals("expected 3 URI's", 3, uris.length);
        for (int i = 0; i < uris.length; i++) {
            //System.out.println(uris[i]);
        }
        try {
            uris = vf.generateURIs("urn:franz-9999:", 5);
            Assert.fail("expected no URI's available");
        } catch (RepositoryException e) {
            // expected
            //System.out.println(e.getLocalizedMessage());
        }
        conn.unregisterEncodableNamespace(NS2);
        Assert.assertEquals(10003, conn.listEncodableNamespaces().size());
        try {
            conn.unregisterEncodableNamespace(NS0);
            Assert.fail("expected namespace in use exception");
        } catch (RepositoryException e) {
            // expected
            //System.out.println(e.getLocalizedMessage());
        }
        Assert.assertEquals(10003, conn.listEncodableNamespaces().size());
    }

}
