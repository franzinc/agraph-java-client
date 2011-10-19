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

/**
 * @deprecated in v4.4 use Closer
 * @see Closer
 */
public class Util {
	
	public static <CloseableType extends Closeable>
	CloseableType close(CloseableType o) {
		return Closer.Close(o);
	}
	
	public static <CloseableType extends java.io.Closeable>
	CloseableType close(CloseableType o) {
		return Closer.Close(o);
	}
	
	public static MultiThreadedHttpConnectionManager close(MultiThreadedHttpConnectionManager o) {
		return Closer.Close(o);
	}
	
	public static <Elem extends Object, Exc extends Exception>
	CloseableIteration<Elem, Exc> close(CloseableIteration<Elem, Exc> o) {
		return Closer.Close(o);
	}
	
	public static XMLStreamReader close(XMLStreamReader o) {
		return Closer.Close(o);
	}
	
}
