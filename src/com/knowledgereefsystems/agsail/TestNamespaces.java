package com.knowledgereefsystems.agsail;

import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.model.Namespace;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.RDF;
import info.aduna.iteration.CloseableIteration;

/**
 * Author: josh
 * Date: Jun 27, 2008
 * Time: 6:39:39 PM
 */
public class TestNamespaces extends AllegroSailTestCase {
    public TestNamespaces(final String name) throws Exception {
        super(name);
    }

    public void testClearNamespaces() throws Exception {
        SailConnection sc = sail.getConnection();

        assertTrue(0 < count(sc.getNamespaces()));
        sc.clearNamespaces();
        assertEquals(0, count(sc.getNamespaces()));

        sc.close();
    }

    public void testGetNamespace() throws Exception {
        SailConnection sc = sail.getConnection();
        Namespace ns1 = new NamespaceImpl("ns1", "http://example.org/ns1/");
//        Namespace ns2 = new NamespaceImpl("ns2", "http://example.org/ns2/");

        assertNull(sc.getNamespace(ns1.getPrefix()));
        sc.setNamespace(ns1.getPrefix(), ns1.getName());
        assertEquals(ns1.getName(), sc.getNamespace(ns1.getPrefix()));

        assertEquals(RDF.NAMESPACE, sc.getNamespace("rdf"));
        assertEquals(RDFS.NAMESPACE, sc.getNamespace("rdfs"));

        // ...

        sc.close();
    }

    public void testGetNamespaces() throws Exception {
        SailConnection sc = sail.getConnection();
        CloseableIteration<? extends Namespace, SailException> namespaces;
        int before, during = 0, after = 0;

        // just iterate through all namespaces
        before = count(sc.getNamespaces());

        // Note: assumes that these namespace prefixes are unused.
        int nTests = 10;
        String prefixPrefix = "testns";
        String namePrefix = "http://example.org/test";
        for (int i = 0; i < nTests; i++) {
            sc.setNamespace(prefixPrefix + i, namePrefix + i);
        }
        sc.commit();
        namespaces = sc.getNamespaces();
        while (namespaces.hasNext()) {
            Namespace ns = namespaces.next();
            during++;
            String prefix = ns.getPrefix();
            String name = ns.getName();
            if (prefix.startsWith(prefixPrefix)) {
                assertEquals(name, namePrefix + prefix.substring(prefixPrefix.length()));
            }
        }
        namespaces.close();

        for (int i = 0; i < nTests; i++) {
            sc.removeNamespace(prefixPrefix + i);
        }
        sc.commit();
        after = count(sc.getNamespaces());

        assertEquals(during, before + nTests);
        assertEquals(after, before);

        sc.close();
    }

    public void testSetNamespace() throws Exception {
        SailConnection sc = sail.getConnection();

        String prefix = "foo";
        String emptyPrefix = "";
        String name = "http://example.org/foo";
        String otherName = "http://example.org/bar";

        sc.removeNamespace(prefix);
        sc.removeNamespace(emptyPrefix);
        sc.commit();

        // Namespace initially absent?
        assertNull(sc.getNamespace(prefix));
        assertNull(sc.getNamespace(emptyPrefix));

        // Can we set the namespace?
        sc.setNamespace(prefix, name);
        sc.commit();
        assertEquals(sc.getNamespace(prefix), name);

        // Can we reset the namespace?
        sc.setNamespace(prefix, otherName);
        sc.commit();
        assertEquals(sc.getNamespace(prefix), otherName);

        // Can we use an empty namespace prefix?
        sc.setNamespace(emptyPrefix, name);
        sc.commit();
        assertEquals(sc.getNamespace(emptyPrefix), name);

        sc.close();
    }

    public void testRemoveNamespace() throws Exception {
        SailConnection sc = sail.getConnection();

        String prefix = "foo";
        String emptyPrefix = "";
        String name = "http://example.org/foo";

        // Set namespace initially.
        sc.setNamespace(prefix, name);
        sc.commit();
        assertEquals(sc.getNamespace(prefix), name);

        // Remove the namespace and make sure it's gone.
        sc.removeNamespace(prefix);
        sc.commit();
        assertNull(sc.getNamespace(prefix));

        // Same thing for the default namespace.
        sc.setNamespace(emptyPrefix, name);
        sc.commit();
        assertEquals(sc.getNamespace(emptyPrefix), name);
        sc.removeNamespace(emptyPrefix);
        sc.commit();
        assertNull(sc.getNamespace(emptyPrefix));

        sc.close();
    }
}