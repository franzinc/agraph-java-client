/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.util;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Use or extend this class to add easy ability to safely close various resources.
 * 
 * <p>Also, static Close functions for various object types.
 * These close functions are null safe and will catch Exception
 * and call log.warn instead of throwing.</p>
 * 
 * <p>TODO: track lastUsed and add method to removeAbandoned (beyond a timeout)</p>
 * 
 * @since v4.3.3
 */
public class Closer implements Closeable {
	
	private final static Logger log = LoggerFactory.getLogger(Closer.class);
	
	private final List toClose = Collections.synchronizedList(new LinkedList());
	
	/**
	 * Add a resource to be closed with {@link #close()}.
	 * 
	 * @param <Obj> The type of objects to be closed by this Closer
	 * @param o  Object to note for later closing
	 * @return Obj  always returns o
	 */
	public <Obj extends Object>
	Obj closeLater(Obj o) {
		toClose.add(0, o);
		return o;
	}

	/**
	 * Remove object from collection so close will not be called later.
	 * 
	 * @param o  Object to be removed from this Closer
	 * @return boolean  Returns true if o was removed, else false
	 * @see #closeLater(Object)
	 */
	public boolean remove(Object o) {
		boolean removed = false;
		while (toClose.remove(o)) {
			removed = true;
		}
		return removed;
	}
	
	/**
	 * Must be called in a finally block, to close all resources
	 * added with closeLater().
	 */
	@Override
	public void close() {
		try {
			while (!toClose.isEmpty()) {
				close( toClose.get(0) );
			}
		} catch (IndexOutOfBoundsException e) {
			// ignore, the list must be empty
		}
	}
	
	/**
	 * Close all objects immediately, will not be closed "later".
	 * 
	 * @param objects  The collection of objects to close
	 * @return Collection  the argument collection is returned
	 */
	public Collection closeAll(Collection objects) {
		for (Object object : objects) {
			close(object);
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "{" + super.toString()
		+ " toClose=" + toClose.size()
		+ "}";
	}

	/**
	 * Close an object immediately, will not be closed "later".
	 * 
	 * @param <Obj>  The type of objects that can be closed by this Closer
	 * @param o  The object being closed
	 * @return Obj  o is always returne.
	 */
	public <Obj extends Object>
	Obj close(Obj o) {
		if (o == null) {
			return null;
		} else if (o instanceof Closeable) {
			return (Obj) close((Closeable)o);
		} else if (o instanceof java.io.Closeable) {
			return (Obj) close((java.io.Closeable)o);
		} else if (o instanceof CloseableIteration) {
			return (Obj) close((CloseableIteration)o);
		} else if (o instanceof XMLStreamReader) {
			return (Obj) close((XMLStreamReader)o);
		} else if (o instanceof MultiThreadedHttpConnectionManager) {
			return (Obj) close((MultiThreadedHttpConnectionManager)o);
		} else if (o instanceof SimpleHttpConnectionManager) {
			return (Obj) close((SimpleHttpConnectionManager)o);
		} else {
			return closeReflection(o);
		}
	}

	/**
	 * Deal with exceptions thrown when closing an object.
	 * Subclass may override, default behavior is to log.warn and return the object.
	 * 
	 * @param <Obj> The class of object handles by this Closer
	 * @param o  The object that caused e to be thrown
	 * @param e  The exception thrown when o was closed
	 * @return Obj  o is always returned
	 */
	public <Obj extends Object>
	Obj handleCloseException(Obj o, Throwable e) {
		log.warn("ignoring error with close: " + o, e);
		return o;
	}

	public <CloseableType extends Closeable>
	CloseableType close(CloseableType o) {
		if (o != null) {
			try {
				o.close();
			} catch (Exception e) {
				return handleCloseException(o, e);
			} finally {
				remove(o);
			}
		}
		return null;
	}
	
	public <CloseableType extends java.io.Closeable>
	CloseableType close(CloseableType o) {
		if (o != null) {
			try {
				o.close();
			} catch (Exception e) {
				return handleCloseException(o, e);
			} finally {
				remove(o);
			}
		}
		return null;
	}
	
	public MultiThreadedHttpConnectionManager close(MultiThreadedHttpConnectionManager o) {
		if (o != null) {
			try {
				o.shutdown();
			} catch (Exception e) {
				return handleCloseException(o, e);
			} finally {
				remove(o);
			}
		}
		return null;
	}
	
	public SimpleHttpConnectionManager close(SimpleHttpConnectionManager o) {
		if (o != null) {
			try {
				o.shutdown();
			} catch (Exception e) {
				return handleCloseException(o, e);
			} finally {
				remove(o);
			}
		}
		return null;
	}
	
	public <Elem extends Object, Exc extends Exception>
	CloseableIteration<Elem, Exc> close(CloseableIteration<Elem, Exc> o) {
		if (o != null) {
			try {
				o.close();
			} catch (Exception e) {
				return handleCloseException(o, e);
			} finally {
				remove(o);
			}
		}
		return null;
	}
	
	public XMLStreamReader close(XMLStreamReader o) {
		if (o != null) {
			try {
				o.close();
			} catch (Exception e) {
				return handleCloseException(o, e);
			} finally {
				remove(o);
			}
		}
		return null;
	}
	
	public Model close(Model o) {
		if (o != null) {
			try {
				o.close();
			} catch (Exception e) {
				return handleCloseException(o, e);
			} finally {
				remove(o);
			}
		}
		return null;
	}

	public <Obj extends Object>
	Obj closeReflection(Obj o) {
		if (o != null) {
			try {
				o.getClass().getMethod("close").invoke(o);
			} catch (Exception e) {
				return handleCloseException(o, e);
			} finally {
				remove(o);
			}
		}
		return null;
	}
	
	// Static methods for convenience
	
	private static final Closer singleton = new Closer();
	
	public static <CloseableType extends Closeable>
	CloseableType Close(CloseableType o) {
		return singleton.close(o);
	}
	
	public static <CloseableType extends java.io.Closeable>
	CloseableType Close(CloseableType o) {
		return singleton.close(o);
	}
	
	public static MultiThreadedHttpConnectionManager Close(MultiThreadedHttpConnectionManager o) {
		return singleton.close(o);
	}
	
	public static SimpleHttpConnectionManager Close(SimpleHttpConnectionManager o) {
		return singleton.close(o);
	}
	
	public static <Elem extends Object, Exc extends Exception>
	CloseableIteration<Elem, Exc> Close(CloseableIteration<Elem, Exc> o) {
		return singleton.close(o);
	}
	
	public static XMLStreamReader Close(XMLStreamReader o) {
		return singleton.close(o);
	}
	
	public static Model Close(Model o) {
		return singleton.close(o);
	}
	
	public static <Obj extends Object>
	Obj Close(Obj o) {
		return singleton.close(o);
	}

}
