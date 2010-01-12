/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.util;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

public class Util {

    public static <CloseableType extends Closeable>
    CloseableType close(CloseableType o) {
        if (o != null) {
            try {
                o.close();
            } catch (Exception e) {
                System.err.println("ignoring error with close:" + e);
                e.printStackTrace();
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
                System.err.println("ignoring error with close:" + e);
                e.printStackTrace();
            }
        }
        return null;
    }

    public static MultiThreadedHttpConnectionManager close(MultiThreadedHttpConnectionManager o) {
        if (o != null) {
            try {
                o.shutdown();
            } catch (Exception e) {
                System.err.println("ignoring error with close:" + e);
                e.printStackTrace();
            }
        }
        return null;
    }
    
}
