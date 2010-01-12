/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.util;

/**
 * Similar to java.io.Closeable, but close throws Exception so
 * it can be used with extentions of third-party classes
 * (for example, Sesame and Jena) that define a close method
 * without implementing java.io.Closeable and with a different
 * checked exception.
 */
public interface Closeable {

    void close() throws Exception;
    
}
