/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.util;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

import java.util.Iterator;

/**
 * Various static utility functions.
 */
public final class Util {
    // String that separated the catalog from the repository
    private static final String CAT_SEPARATOR = ":";

    /**
     * Private constructor - no instances can be created.
     */
    private Util() {
    }

    /**
     * Converts an RDF4J iteration object into a Java iterable.
     * <p>
     * Note that the 'iterable' can be iterated over only once.
     * The purpose of this method is to enable RDF4J iterable objects
     * to be used in for-each loops:
     * <p>
     * <code>
     * try (TupleQueryResult result = ...) {
     * for (BindingSet set : Util.iter(result)) { ... }
     * }
     * </code>
     *
     * @param i   RDF4J iterable object.
     * @param <T> Type of objects returned from the iteration.
     * @return An iterable to be used in a for-each loop.
     */
    public static <T> Iterable<T> iter(final CloseableIteration<T> i) {
        return () -> new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public T next() {
                return i.next();
            }

            @Override
            public void remove() {
                i.remove();
            }
        };
    }

    /**
     * Parses a store spec of the form [CATALOG:]REPO and returns
     * the CATALOG. Returns {@code null} if there is no CATALOG.
     *
     * @param repoAndCatalog Store specification ([CATALOG:]REPO).
     * @return Catalog name or {@code null}.
     */
    public static String getCatalogFromSpec(final String repoAndCatalog) {
        final String[] components =
                repoAndCatalog.split(CAT_SEPARATOR, 2);
        return components.length == 1 ? null : components[0];

    }

    /**
     * Parses a store spec of the form [CATALOG:]REPO and returns
     * the REPO part.
     *
     * @param repoAndCatalog Store specification ([CATALOG:]REPO).
     * @return Repository name.
     */
    public static String getRepoFromSpec(final String repoAndCatalog) {
        final String[] components =
                repoAndCatalog.split(CAT_SEPARATOR, 2);
        return components[components.length - 1];
    }

    // The methods below are (less-efficient) implementations
    // of java.util.Arrays methods added in Java 9
    // New overloads should be added as needed.

    /**
     * Computes the first index where two arrays differ.
     * <p>
     * If the arrays are identical -1 is returned.
     * <p>
     * If one array is a prefix of the other then the length
     * of the shorter array is returned.
     *
     * @param a First array.
     * @param b Second array.
     * @return First differing index or -1.
     * @throws NullPointerException If either array is null.
     */
    public static int mismatch(int[] a, int[] b) {
        final int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        return a.length != b.length ? n : -1;
    }

    /**
     * Compares two array lexicographically.
     *
     * @param a First array.
     * @param b Second array.
     * @return Comparison result as specified by
     * {@link Comparable#compareTo(Object)}.
     * @throws NullPointerException If either array is null.
     */
    public static int compare(int[] a, int[] b) {
        final int m = mismatch(a, b);
        if (m >= 0 && m < a.length && m < b.length) {
            return Integer.compare(a[m], b[m]);
        }
        return Integer.compare(a.length, b.length);
    }

    public static void verifyContextNotNull(Resource... contexts) {
        if (contexts == null) {
            String message = "Illegal value null array for contexts argument; either the value should be cast to Resource or an empty array should be supplied";
            throw new IllegalArgumentException(message);
        }
    }

    public static class NTriples {
        private static final boolean XSD_STRING_TO_PLAIN_LITERAL = BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL.getDefaultValue();

        public static String toNTriplesString(Value storableValue) {
            if (storableValue instanceof Literal literal) {
                return NTriplesUtil.toNTriplesString(literal, XSD_STRING_TO_PLAIN_LITERAL);
            } else if (storableValue instanceof IRI iri) {
                return NTriplesUtil.toNTriplesString(iri);
            } else if (storableValue instanceof BNode node) {
                return toNTriplesString(node);
            } else if (storableValue instanceof Triple triple) {
                return toNTriplesString(triple);
            } else {
                throw new IllegalArgumentException("Unknown value type: " + storableValue.getClass());
            }
        }

        /**
         * The implementation of this method in org.eclipse.rdf4j.rio.helpers.NTriplesUtil
         * (previously org.eclipse.rdf4j.rio.ntriples.NTriplesUtil)
         * performs an expensive id validation/transformation for BNode ids, and we don't want to do that
         */
        private static String toNTriplesString(BNode bNode) {
            String id = bNode.getID();
            if (id == null || id.isEmpty()) {
                return "_:genid" + Integer.toHexString(bNode.hashCode());
            } else {
                return "_:" + id;
            }
        }

        private static String toNTriplesString(Triple triple) {
            return "<<" + toNTriplesString(triple.getSubject()) + " "
                    + toNTriplesString(triple.getPredicate()) + " "
                    + toNTriplesString(triple.getObject()) + ">>";
        }
    }
}
