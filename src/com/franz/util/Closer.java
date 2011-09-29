/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.util;

import info.aduna.iteration.CloseableIteration;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Extend this class to add easy ability to safely close various resources.
 * 
 * <p>TODO: track lastUsed and add method to removeAbandoned (beyond a timeout)</p>
 * 
 * <p>Also, static Close functions for various object types.
 * These close functions are null safe and will catch Exception
 * and call log.warn instead of throwing.</p>
 * 
 * @since v4.3.3
 */
public class Closer implements Closeable {
	
	private final static Logger log = LoggerFactory.getLogger(Closer.class);
	
	private final Deque toClose = new LinkedList();
	
	/**
	 * Add a resource to be closed with {@link #close()}.
	 */
	public <Obj extends Object>
	Obj closeLater(Obj o) {
		toClose.push(o);
		return o;
	}

	/**
	 * Remove from {@link #closeLater(Object)}.
	 */
	public boolean remove(Object o) {
		return toClose.remove(o);
	}
	
	/**
	 * Must be called in a finally block, to close all resources
	 * added with closeLater().
	 */
	@Override
	public void close() {
		while (toClose.isEmpty() == false) {
			close( toClose.pop() );
		}
	}
	
	/**
	 * Close all objects immediately, will not be closed "later".
	 */
	public Collection closeAll(Collection objects) {
		for (Object object : objects) {
			close(object);
		}
		return null;
	}
	
	/**
	 * Close an object immediately, will not be closed "later".
	 */
	public <Obj extends Object>
	Obj close(Obj o) {
		o = Close(o);
		while (toClose.remove(o)) {
		}
		return o;
	}
	
	@Override
	public String toString() {
		return "{" + super.toString()
		+ " openObjects=" + toClose.size()
		+ "}";
	}

	public static <Obj extends Object>
	Obj Close(Obj o) {
		if (o instanceof Closeable) {
			return (Obj) Close((Closeable)o);
		} else if (o instanceof java.io.Closeable) {
			return (Obj) Close((java.io.Closeable)o);
		} else if (o instanceof CloseableIteration) {
			return (Obj) Close((CloseableIteration)o);
		} else if (o instanceof XMLStreamReader) {
			return (Obj) Close((XMLStreamReader)o);
		} else if (o instanceof MultiThreadedHttpConnectionManager) {
			return (Obj) Close((MultiThreadedHttpConnectionManager)o);
		} else if (o instanceof Model) {
			return (Obj) Close((Model)o);
		} else if (o != null) {
			try {
				o.getClass().getMethod("close").invoke(o);
			} catch (Exception e) {
				log.warn("ignoring error with close", e);
				return o;
			}
		}
		return null;
	}
	
	public static <CloseableType extends Closeable>
	CloseableType Close(CloseableType o) {
		if (o != null) {
			try {
				o.close();
			} catch (Exception e) {
				log.warn("ignoring error with close", e);
				return o;
			}
		}
		return null;
	}
	
	public static <CloseableType extends java.io.Closeable>
	CloseableType Close(CloseableType o) {
		if (o != null) {
			try {
				o.close();
			} catch (Exception e) {
				log.warn("ignoring error with close", e);
				return o;
			}
		}
		return null;
	}
	
	public static MultiThreadedHttpConnectionManager Close(MultiThreadedHttpConnectionManager o) {
		if (o != null) {
			try {
				o.shutdown();
			} catch (Exception e) {
				log.warn("ignoring error with close", e);
				return o;
			}
		}
		return null;
	}
	
	public static <Elem extends Object, Exc extends Exception>
	CloseableIteration<Elem, Exc> Close(CloseableIteration<Elem, Exc> o) {
		if (o != null) {
			try {
				o.close();
			} catch (Exception e) {
				log.warn("ignoring error with close", e);
				return o;
			}
		}
		return null;
	}
	
	public static XMLStreamReader Close(XMLStreamReader o) {
		if (o != null) {
			try {
				o.close();
			} catch (Exception e) {
				log.warn("ignoring error with close", e);
				return o;
			}
		}
		return null;
	}
	
	public static Model Close(Model o) {
		if (o != null) {
			try {
				o.close();
			} catch (Exception e) {
				log.warn("ignoring error with close", e);
				return o;
			}
		}
		return null;
	}
	
}
