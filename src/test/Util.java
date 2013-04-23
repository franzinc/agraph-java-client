/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import static test.util.Clj.filter;
import info.aduna.io.IOUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import clojure.lang.AFn;
import clojure.lang.IFn;

import com.franz.agraph.repository.AGServer;
import com.franz.util.Closer;

public class Util {
    
    public static String get(String[] arr, int i, String defaultVal) {
        if (arr != null && arr.length > i) {
            return arr[i];
        }
        return defaultVal;
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
        	Closer.Close(f);
        	Closer.Close(s);
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
        	Closer.Close(is);
        	Closer.Close(os);
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
	
    public static long fromHumanInt(String value) {
        int len = value.length();
        if (len > 1) {
            char c = value.charAt(len-1);
            if ( ! Character.isDigit(c)) {
                int n = Integer.parseInt(value.substring(0, len-1));
                if (c == 'm')
                    return n * (long) Math.pow(10, 6);
                else if (c == 'b')
                    return n * (long) Math.pow(10, 9);
                else if (c == 't')
                    return n * (long) Math.pow(10, 12);
            }
        }
        return Long.parseLong(value);
    }
    
    public static String toHumanInt(long num, int type) {
    	long[] mult;
    	if (type == 2)
            mult = new long[] {1024, 1024, 1024, 1024, 1024};
    	else if (type == 10)
            mult = new long[] {1000, 1000, 1000, 1000, 1000};
    	else if (type == 60) // milliseconds
            mult = new long[] {1000, 60, 60, 24, 30};
    	else
    		throw new IllegalArgumentException("unknown type: " + type);
    	String[] abbrevs;
    	if (type == 2)
            abbrevs = new String[] {"b", "k", "m", "g", "t"};
    	else if (type == 10)
            abbrevs = new String[] {"", "k", "m", "b", "t"};
    	else if (type == 60) // time
            abbrevs = new String[] {"ms", "s", "m", "h", "d"};
    	else
    		throw new IllegalArgumentException("unknown type: " + type);
    	for (int i = 0; i < abbrevs.length; i++) {
        	if (num < (mult[i] * 10)) {
        		return num + abbrevs[i];
        	}
        	num = num/mult[i];
		}
    	return "" + num;
    }
    
    public static List reverse(List list) {
    	list = new ArrayList(list);
		Collections.reverse(list);
    	return list;
    }

    /**
     * Adds a method nextLong with a max value similar to {@link Random#nextInt(int)}.
     */
    public static class RandomLong extends Random {
		private static final long serialVersionUID = 4874437974204550876L;
		
        public long nextLong(long max) {
        	if (max <= 0) {
        		throw new IllegalArgumentException("max must be positive");
        	}
        	if (max <= Integer.MAX_VALUE) {
        		return nextInt((int) max);
        	} else {
        		int x = (int) (max >> 31);
        		if (x == 0) {
            		return nextInt();
        		} else {
            		return ((long)nextInt((int) (max >> 31)) << 32) + nextInt();
        		}
        	}
        }
    	
    }

    public static <ReturnType> ReturnType waitFor(TimeUnit unit, long sleep, long maxWait, Callable<ReturnType> fn) throws Exception {
    	return waitFor(unit.toMillis(sleep), unit.toNanos(maxWait), fn);
    }

    public static Object waitFor(TimeUnit unit, long sleep, long maxWait, IFn fn) throws Exception {
    	return waitFor(unit.toMillis(sleep), unit.toNanos(maxWait), fn);
    }

    /**
     * Call fn until it returns null or false.
     * @return the last value from fn
     */
    public static <ReturnType> ReturnType waitFor(long sleepMillis, long maxWaitNanos, Callable<ReturnType> fn) throws Exception {
    	long start = System.nanoTime();
    	while (true) {
    		ReturnType ret = fn.call();
    		if (ret == null || Boolean.FALSE.equals(ret)) {
    			return ret;
    		}
    		try {
				Thread.sleep(sleepMillis);
			} catch (InterruptedException e) {
				continue;
			}
			if ((System.nanoTime() - start) >= maxWaitNanos) {
				return ret;
			}
    	}
    }
    
    /**
     * Exec 'netstat -tap' and extract the lines which pertain to this java process.
     * @return output lines from netstat
     */
	public static List<String> netstat() throws IOException {
		String[] cmd = {"bash", "-c", "netstat -tap 2>/dev/null | egrep '\\<'$PPID/java'\\>'"};
		Process p = Runtime.getRuntime().exec(cmd);
		String string = IOUtil.readString(p.getInputStream());
		List<String> list = new ArrayList( Arrays.asList(string.split("\n")));
		list.remove("");
		return list;
	}
	
	public static List<String> closeWait(List<String> netstat) throws Exception {
		return filter(new AFn() {
			public Object invoke(Object line) {
				return ((String)line).contains("CLOSE_WAIT");
			}
		}, netstat);
	}

	public static List<String> waitForNetStat(int maxWaitSeconds, final List<String> excluding) throws Exception {
		return Util.waitFor(TimeUnit.SECONDS, 1, maxWaitSeconds, new Callable<List<String>>() {
        	public List<String> call() throws Exception {
                List<String> netstat = filter(new AFn() {
                	public Object invoke(Object line) {
                		for (String exclude : excluding) {
                			if (((String)line).matches(exclude)) {
                				return false;
                			}
						}
                		return true;
                	}
                }, netstat());
        		return netstat.isEmpty() ? null : netstat;
        	}
		});
	}

	public static Map<String, String> waitForSessions(final AGServer server, final String repoName) throws Exception {
		Map<String, String> sessions = Util.waitFor(TimeUnit.SECONDS, 1, 30, new Callable<Map<String, String>>() {
        	public Map<String, String> call() throws Exception {
        		Map<String, String> sessions = AGAbstractTest.sessions(server);
		        for (Entry<String, String> entry : sessions.entrySet()) {
					if (entry.getValue().contains(repoName)) {
						return sessions;
					}
				}
		        return null;
			}
		});
		return sessions;
	}

    public static void logTimeStamped(String message) {
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	System.out.println(sdf.format(Calendar.getInstance().getTime()) + " " + message);
    }
	
    

}
