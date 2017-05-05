/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.util;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

/**
 * @see Closer
 */
public class Util {
	// String that separated the catalog from the repository
	private static final String CAT_SEPARATOR = ":";
	
	/**
	 * @param <CloseableType>  .
	 * @param o  the object to close
	 * @return the closed object <code>o</code>
	 * @deprecated in v4.4 use Closer
	 * @see Closer
	 */
	public static <CloseableType extends Closeable>
	CloseableType close(CloseableType o) {
		return Closer.Close(o);
	}
	
	/**
	 * @param <CloseableType>  .
	 * @param o  the object to close
	 * @return the closed object <code>o</code>
	 * @deprecated in v4.4 use Closer
	 * @see Closer
	 */
	public static <CloseableType extends java.io.Closeable>
	CloseableType close(CloseableType o) {
		return Closer.Close(o);
	}
	
	/**
	 * 
	 * @param o  the connection manager to close
	 * @return the closed object <code>o</code>
	 * @deprecated in v4.4 use Closer
	 * @see Closer
	 */
	public static MultiThreadedHttpConnectionManager close(MultiThreadedHttpConnectionManager o) {
		return Closer.Close(o);
	}
	
	/**
	 * 
	 * @param <Elem>  the element type of the ClosableIteration
	 * @param <Exc>   the exception type of the ClosableIteration
	 * @param o  the object to close
	 * @return the closed object <code>o</code>
	 * @deprecated in v4.4 use Closer
	 * @see Closer
	 */
	public static <Elem extends Object, Exc extends Exception>
	CloseableIteration<Elem, Exc> close(CloseableIteration<Elem, Exc> o) {
		return Closer.Close(o);
	}
	
	/**
	 * @param o  the stream to close
	 * @return  the closed stream <code>o</code>
	 * @deprecated in v4.4 use Closer
	 * @see Closer
	 */
	public static XMLStreamReader close(XMLStreamReader o) {
		return Closer.Close(o);
	}
	
    /**
     * Parses a store spec of the form [CATALOG:]REPO and returns
     * the CATALOG. Returns {@code null} if there is no CATALOG.
     *
     * @param repoAndCatalog  Store specification ([CATALOG:]REPO).
     *
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
     * @param repoAndCatalog  Store specification ([CATALOG:]REPO).
     *
     * @return Repository name.
     */
    public static String getRepoFromSpec(final String repoAndCatalog) {
        final String[] components =
                repoAndCatalog.split(CAT_SEPARATOR, 2);
        return components[components.length - 1];       
    }
}
