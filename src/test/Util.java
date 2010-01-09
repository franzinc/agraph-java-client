/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Util {

    public static String get(String[] arr, int i, String defaultVal) {
        if (arr != null && arr.length > i) {
            return arr[i];
        }
        return defaultVal;
    }
    
    public static Object close(Object o) {
        if (o instanceof Closeable) {
            close((Closeable)o);
        } else if (o != null) {
            try {
                o.getClass().getMethod("close").invoke(o);
            } catch (Exception e) {
                System.err.println("ignoring error with close:" + e);
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public static Object close(Closeable o) {
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
    
    public static List<String> readLines(File file) {
        List list = new ArrayList<String>();
        FileReader f = null;
        BufferedReader s = null;
        try {
            f = new FileReader(file);
            s = new BufferedReader(f);
            String l = s.readLine();
            while (l != null) {
                list.add(l);
                l = s.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(file.getAbsolutePath(), e);
        } finally {
            close(f);
            close(s);
        }
        return list;
    }

    /**
     * null-safe hashCode
     */
    public static int hashCode(Object o) {
        if (o == null) {
            return 0;
        }
        return o.hashCode();
    }
    
    public static <Type> Type or(Type... values) {
        for (int i = 0; i < values.length; i++) {
            Type e = values[i];
            if (e == null || e == Boolean.FALSE) {
                continue;
            } else {
                return e;
            }
        }
        return null;
    }
    
    public static String ifBlank(String str, String defaultValue) {
        if (str == null || str.trim().isEmpty()) {
            return defaultValue;
        } else {
            return str;
        }
    }
    
    public static void gzip(File in, File out) throws IOException {
        FileInputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(in);
            os = new GZIPOutputStream(new FileOutputStream(out));
            for (int ch = is.read(); ch != -1; ch = is.read()) {
                os.write(ch);
            }
            os.flush();
        } finally {
            close(is);
            close(os);
        }
    }
    
}
