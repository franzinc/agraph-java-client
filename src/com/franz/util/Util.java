/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.util;

import info.aduna.iteration.CloseableIteration;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Close functions for various object types.
 * These close functions are null safe and will catch Exception
 * and call log.warn instead of throwing.
 */
public class Util {

	final static Logger logger = LoggerFactory.getLogger(Util.class);

	public static <CloseableType extends Closeable>
    CloseableType close(CloseableType o) {
        if (o != null) {
            try {
                o.close();
            } catch (Exception e) {
				if (logger.isWarnEnabled())
					logger.warn("ignoring error with close:" + e);
            }
        }
        return null;
    }
    
    public static <CloseableType extends java.io.Closeable>
    CloseableType close(CloseableType o) {
        if (o != null) {
            try {
                o.close();
            } catch (Exception e) {
				if (logger.isWarnEnabled())
					logger.warn("ignoring error with close:" + e);
            }
        }
        return null;
    }

    public static MultiThreadedHttpConnectionManager close(MultiThreadedHttpConnectionManager o) {
        if (o != null) {
            try {
                o.shutdown();
            } catch (Exception e) {
				if (logger.isWarnEnabled())
					logger.warn("ignoring error with close:" + e);
            }
        }
        return null;
    }
    
    public static <Elem extends Object, Exc extends Exception>
    CloseableIteration<Elem, Exc> close(CloseableIteration<Elem, Exc> o) {
        if (o != null) {
            try {
                o.close();
            } catch (Exception e) {
                if (logger.isWarnEnabled())
                    logger.warn("ignoring error with close:" + e);
            }
        }
        return null;
    }

    public static XMLStreamReader close(XMLStreamReader o) {
        if (o != null) {
            try {
                o.close();
            } catch (Exception e) {
				if (logger.isWarnEnabled())
					logger.warn("ignoring error with close:" + e);
            }
        }
        return null;
    }

}
