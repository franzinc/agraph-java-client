/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.util;

/**
 * Similar to {@link java.io.Closeable}, but close throws Exception so
 * it can be used with extentions of third-party classes
 * (for example, Sesame and Jena) that define a close method
 * without implementing {@link java.io.Closeable} and with a different
 * checked exception.
 */
public interface Closeable {
	
	/**
	 * Releases system resources associated with the object.
	 * 
	 * @throws Exception  any exception that is thrown when attempting to close
	 * @see java.io.Closeable#close()
	 */
	void close() throws Exception;
	
}
