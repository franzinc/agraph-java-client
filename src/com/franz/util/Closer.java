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

/**
 * Extend this class to add easy ability to safely close various resources.
 * 
 * <p>
 * TODO: track lastUsed and add method to removeAbandoned (beyond a timeout)
 * </p>
 * 
 * @since v4.3
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
    	Close(o);
		while (toClose.remove(o)) {
		}
        return null;
    }
    
    public static <Obj extends Object>
    Obj Close(Obj o) {
        if (o instanceof Closeable) {
            Util.close((Closeable)o);
        } else if (o instanceof java.io.Closeable) {
            Util.close((java.io.Closeable)o);
        } else if (o instanceof CloseableIteration) {
            Util.close((CloseableIteration)o);
        } else if (o instanceof XMLStreamReader) {
            Util.close((XMLStreamReader)o);
        } else if (o instanceof MultiThreadedHttpConnectionManager) {
            Util.close((MultiThreadedHttpConnectionManager)o);
        } else if (o != null) {
            try {
                o.getClass().getMethod("close").invoke(o);
            } catch (Exception e) {
                if (log.isWarnEnabled())
                    log.warn("ignoring error with close:" + e);
            }
        }
        return null;
    }

}
