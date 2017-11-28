/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.shared.PrefixMapping;
import org.apache.xerces.util.XMLChar;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implements the Jena PrefixMapping interface for AllegroGraph.
 */
public class AGPrefixMapping implements PrefixMapping {

    protected boolean locked;
    AGGraph graph;

    public AGPrefixMapping(AGGraph graph) {
        this.graph = graph;
    }

    AGGraph getGraph() {
        return graph;
    }

    @Override
    public String expandPrefix(String prefixed) {
        int colon = prefixed.indexOf(':');
        if (colon < 0) {
            return prefixed;
        } else {
            String uri = getNsPrefixURI(prefixed.substring(0, colon));
            return uri == null ? prefixed : uri + prefixed.substring(colon + 1);
        }
    }

    @Override
    public Map<String, String> getNsPrefixMap() {
        Map<String, String> map = new HashMap<String, String>();
        try {
            RepositoryResult<Namespace> result = getGraph().getConnection().getNamespaces();
            while (result.hasNext()) {
                Namespace ns = result.next();
                map.put(ns.getPrefix(), ns.getName());
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    @Override
    public String getNsPrefixURI(String prefix) {
        String uri = null;
        try {
            if (prefix != null) {
                uri = getGraph().getConnection().getNamespace(prefix);
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return uri;
    }

    @Override
    public String getNsURIPrefix(String uri) {
        // TODO speed this up!
        String prefix = null;
        try {
            RepositoryResult<Namespace> result = getGraph().getConnection().getNamespaces();
            while (prefix == null && result.hasNext()) {
                Namespace ns = result.next();
                if (uri.equalsIgnoreCase(ns.getName())) {
                    prefix = ns.getPrefix();
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return prefix;
    }

    @Override
    public PrefixMapping lock() {
        locked = true;
        return this;
    }

    @Override
    public boolean hasNoMappings() {
        return getNsPrefixMap().isEmpty();
    }

    @Override
    public String qnameFor(String uri) {
        int split = Util.splitNamespaceXML(uri);
        String ns = uri.substring(0, split), local = uri.substring(split);
        if (local.equals("")) {
            return null;
        }
        String prefix = getNsURIPrefix(ns);
        return prefix == null ? null : prefix + ":" + local;
    }

    @Override
    public PrefixMapping removeNsPrefix(String prefix) {
        checkUnlocked();
        try {
            getGraph().getConnection().removeNamespace(prefix);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public PrefixMapping clearNsPrefixMap() {
        getGraph().getConnection().clearNamespaces();
        return this;
    }

    @Override
    public boolean samePrefixMappingAs(PrefixMapping other) {
        return getNsPrefixMap().equals(other.getNsPrefixMap());
    }

    @Override
    public PrefixMapping setNsPrefix(String prefix, String uri) {
        checkUnlocked();
        // TODO support an empty prefix for the default namespace
        if (prefix.length() > 0 && !XMLChar.isValidNCName(prefix))
        // required by AbstractTestPrefixMapping#testCheckNames()
        {
            throw new PrefixMapping.IllegalPrefixException(prefix);
        }
        if (uri == null)
        // required by AbstractTestPrefixMapping#testNullURITrapped()
        // TODO: why not an IllegalArgumentException?
        {
            throw new NullPointerException("null URIs are prohibited as arguments to setNsPrefix");
        }
        try {
            getGraph().getConnection().setNamespace(prefix, uri);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    protected void checkUnlocked() {
        if (locked) {
            throw new PrefixMapping.JenaLockedException(this);
        }
    }

    @Override
    public PrefixMapping setNsPrefixes(PrefixMapping other) {
        return setNsPrefixes(other.getNsPrefixMap());
    }

    @Override
    public PrefixMapping setNsPrefixes(Map<String, String> map) {
        checkUnlocked();
        // TODO do this in a single http request when that is available
        for (String key : map.keySet()) {
            setNsPrefix(key, map.get(key));
        }
        return this;
    }

    @Override
    public String shortForm(String uri) {
        // TODO speed this up
        Map<String, String> map = getNsPrefixMap();
        for (Entry<String, String> s : map.entrySet()) {
            if (uri.startsWith(s.getValue())) {
                return s.getKey() + ":" + uri.substring(s.getValue().length());
            }
        }
        return uri;
    }

    @Override
    public int numPrefixes() {
        return getNsPrefixMap().size();
    }

    @Override
    public PrefixMapping withDefaultMappings(PrefixMapping map) {
        Map<String, String> thisMap = getNsPrefixMap();
        Map<String, String> otherMap = map.getNsPrefixMap();
        for (String key : otherMap.keySet()) {
            String value = otherMap.get(key);
            if (!thisMap.containsKey(key) && !thisMap.containsValue(value)) {
                setNsPrefix(key, value);
            }
        }
        return this;
    }

    public String toString() {
        return toString(20);
    }

    public String toString(int max) {
        Map<String, String> map = getNsPrefixMap();
        int size = map.size();
        int count = 0;
        Iterator<String> it = map.keySet().iterator();
        StringBuffer b = new StringBuffer(this.getClass().getSimpleName() + "(size: " + size + "){");
        String gap = "";
        for (; it.hasNext() && count < max; count++) {
            b.append(gap);
            gap = ", ";
            String key = it.next();
            b.append(key + "=" + map.get(key));
        }
        if (count == max && it.hasNext()) {
            b.append(",...");
        }
        b.append("}");
        return b.toString();
    }

}
