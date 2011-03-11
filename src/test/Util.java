/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import info.aduna.iteration.CloseableIteration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import com.franz.util.Closeable;

public class Util extends com.franz.util.Util {

    public static String get(String[] arr, int i, String defaultVal) {
        if (arr != null && arr.length > i) {
            return arr[i];
        }
        return defaultVal;
    }
    
	/**
	 * TODO: move to com.franz.util.Util
	 */
	public static <Elem extends Object, Exc extends Exception>
	CloseableIteration<Elem, Exc> close(CloseableIteration<Elem, Exc> o) {
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

	/**
	 * TODO: move to com.franz.util.Util
	 */
    public static <Obj extends Object>
    Obj close(Obj o) {
        if (o instanceof Closeable) {
            com.franz.util.Util.close((Closeable)o);
        } else if (o instanceof java.io.Closeable) {
            com.franz.util.Util.close((java.io.Closeable)o);
        } else if (o instanceof CloseableIteration) {
        	close((CloseableIteration)o);
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
    
    public static List arrayList(Object...elements) {
    	List list = new ArrayList();
    	for (int i = 0; i < elements.length; i++) {
			list.add(elements[i]);
		}
    	return list;
    }
    
    /**
     * List Arrays.asList, but is not varargs,
     * also allows null (returns null), and will
     * convert primitive arrays to List of wrapper objects.
     * @return list or null
     */
    public static List toList(Object arr) {
    	if (arr == null) {
    		return null;
    	}
    	if (arr instanceof List) {
    		return (List) arr;
    	}
    	if (arr instanceof Object[]) {
    		return Arrays.asList((Object[])arr);
    	}
    	List list = new ArrayList();
    	if (arr instanceof byte[]) {
    		byte[] a = ((byte[])arr);
    		for (int i = 0; i < a.length; i++) {
				list.add(a[i]);
			}
    	} else if (arr instanceof char[]) {
    		char[] a = ((char[])arr);
    		for (int i = 0; i < a.length; i++) {
    			list.add(a[i]);
    		}
    	} else if (arr instanceof int[]) {
    		int[] a = ((int[])arr);
    		for (int i = 0; i < a.length; i++) {
    			list.add(a[i]);
    		}
    	} else if (arr instanceof long[]) {
    		long[] a = ((long[])arr);
    		for (int i = 0; i < a.length; i++) {
    			list.add(a[i]);
    		}
    	} else if (arr instanceof float[]) {
    		float[] a = ((float[])arr);
    		for (int i = 0; i < a.length; i++) {
    			list.add(a[i]);
    		}
    	} else if (arr instanceof double[]) {
    		double[] a = ((double[])arr);
    		for (int i = 0; i < a.length; i++) {
    			list.add(a[i]);
    		}
    	} else {
    		throw new IllegalArgumentException("type not handled: " + arr.getClass());
    	}
    	return list;
    }
    
    public static List toListDeep(Object obj) {
    	List in = toList(obj);
    	if (in == null) {
    		return null;
    	}
    	List out = new ArrayList(in.size());
    	for (Object o : in) {
    		if (o == null) {
    			out.add(null);
    		} else if (o instanceof List || o.getClass().isArray()) {
        		out.add(toListDeep(o));
    		} else {
    			out.add(o);
    		}
    	}
    	return out;
    }

}
