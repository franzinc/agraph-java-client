/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.util;

import org.eclipse.rdf4j.common.iteration.Iteration;

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
     *   for (BindingSet set : Util.iter(result)) { ... }
     * }
     * </code>
     *
     * @param i   RDF4J iterable object.
     * @param <T> Type of objects returned from the iteration.
     * @return An iterable to be used in a for-each loop.
     */
    public static <T> Iterable<T> iter(final Iteration<T, ? extends RuntimeException> i) {
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
}
