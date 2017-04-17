/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.util;

import info.aduna.iteration.CloseableIteration;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

/**
 * @see Closer
 */
public class Util {
	
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
	
}
