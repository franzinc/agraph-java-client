
//***** BEGIN LICENSE BLOCK *****
//Version: MPL 1.1
//
//The contents of this file are subject to the Mozilla Public License Version
//1.1 (the "License"); you may not use this file except in compliance with
//the License. You may obtain a copy of the License at
//http://www.mozilla.org/MPL/
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
//for the specific language governing rights and limitations under the
//License.
//
//The Original Code is the AllegroGraph Java Client interface.
//
//The Original Code was written by Franz Inc.
//Copyright (C) 2006 Franz Inc.  All Rights Reserved.
//
//***** END LICENSE BLOCK *****

package com.franz.agbase.transport;

import java.io.IOException;
import java.util.Arrays;

import com.franz.ag.UPI;
import com.franz.agbase.impl.UPIImpl;

/**
 * Basic test for direct socket interface.
 * Lisp part can run without AllegroStore.
 * <p>
 * Minimal Lisp required:  agdirect.lisp  agdtest.cl
 * <p>
 * @author mm
 *
 */
public class AGDTest {

	static int fail = 0;
	static int ldb  = -1;
	static int javad = 0;
	static boolean verbose = false;
	
	static void dj( int level ) {
		String r = AGDirectLink.debug(level);
		prv ( "Java debug " + r );
	}
	
	/**
	 * Change debug state in Lisp server.
	 * @param ts AGDirectLink instance
	 * @param db new debug state: 1 - turn on, 0 - turn off, otherwise toggle 
	 */
	static void dl ( AGDirectLink ts, int db ) {
		try {
			Object r = ts.sendOp1(":debug", 1, 0, db);
			prv ( "Lisp debug now: " + r );
		} catch (IOException e) {}
	}
	
	static void pr ( String m ) {
		System.out.println( m );
	}
	
	static void prv ( String m ) {
		if ( verbose ) System.out.println( m );
	}
	
	public static void main(String[] args)
	throws IOException
	{
		String host = "localhost";
		int port = 4567;
        int nn = 500;    // number of iterations in speed tests
        int aa = 1000;   // array size in speed tests
        boolean all = true; // when true, do all tests
        boolean p1 = true;  // when true for test p1
        boolean p2 = true;  // when true for test p2
		
		for (int i=0; i<args.length;) {
			if ( args[i]==null ) ;
			else if ( (args[i]).equalsIgnoreCase("-d") )
				javad = Integer.parseInt(args[++i]);
			else if ( (args[i]).equalsIgnoreCase("-p") )
				port = Integer.parseInt(args[++i]);
			else if ( (args[i]).equalsIgnoreCase("-l") )
				ldb = Integer.parseInt(args[++i]);
			else if ( (args[i]).equalsIgnoreCase("-n") )
				nn = Integer.parseInt(args[++i]);
			else if ( (args[i]).equalsIgnoreCase("-a") )
				aa = Integer.parseInt(args[++i]);
			else if ( (args[i]).equalsIgnoreCase("-v") ) verbose=true;
			else if ( (args[i]).equalsIgnoreCase("-p1") ) 
				{ all=false; p1=true; p2=false; }
			else if ( (args[i]).equalsIgnoreCase("-p2") ) 
			{ all=false; p2=true; p1=false; }
			else if ( (args[i]).equalsIgnoreCase("-all") ) 
			{ all=true; p2=false; p1=false; }
			i++;
		}
		
		AGDirectLink ts;
		
		dj(javad);
		
		if ( javad>0 )
			ts = new AGDirectLinkDebug(host, port, 3, 1000);
		else
			ts = new AGDirectLink(host, port, 3, 1000);
		
		if ( ldb>0 ) dl(ts, ldb);
		
		Object r; 
		Object[] ra;
		
		if (all)
		{
		// AGDTest 0 args 1 result
		r = ts.sendOp0(":verify", 1, -1);
		ra = (Object[])r;
		if ( 3!=ra.length )
			throw new  IOException(":verify length=" + ra.length);
		if ( !((String)(ra[0])).equals(":verify") )
			throw new  IOException(":verify ra[0]=" + ra[0]);
		
		
		// AGDTest 1 arg  1 result
		r = ts.sendOp1(":call", 1, 0, "user::test-0-1");
		if ( !(r instanceof Long) ) 
			throw new  IOException(":call test-0-1 return type " + r);
		if (!(17==(((Long)r).longValue())) ) 
			throw new  IOException(":call test-0-1 result " + ((Long)r).longValue());
		
		try {
			r = ts.sendOp1(":call", 1, 0, "user::undefined");
		} catch (IllegalArgumentException e) {
			pr( "catch Lisp err: " + e );
		}
		try {
			r = ts.sendOp2(":call", 1, 0, "cl:car", 1);
		} catch (IllegalArgumentException e) {
			pr( "catch Lisp err: " + e );
		}
		
		// AGDTest 2 args 1 result
		// AGDTest 2 args 2 results
		
		// AGDTest 1 arg  0 results
		r = ts.sendOp1(":call", 1, -1, "cl:values");
		ra = (Object[])r;
		if ( !(2==ra.length) )
			throw new  IOException(":call (values) r.length=" + ra.length);
		
		
		// AGDTest many args many results
		
		
		// AGDTest datatypes byte short int long
		for (int i = 0; i < 300; i++) {
			test11(ts, i);
			test11(ts,-i);
		}
		
		test11(ts, 100);
		test11(ts, 1000);
		test11(ts, 10000);
		test11(ts, 100000);
		test11(ts, 1000000);
		test11(ts, 10000000);
		test11(ts, 100000000);
		test11(ts, 1000000000);
		test11(ts, 10000000000l);
		test11(ts, 10000000000000000l);
		test11(ts, -345);
		test11(ts, -345234);
		test11(ts, -345345678);
		test11(ts, -34522222233333l);
		test11(ts, -3457777777777777777l);
		test11(ts, 0x7fffffffffffffffl);
		test11(ts, -0x8000000000000000l);
		
		// AGDTest datatypes byte[] short[] int[] long[]
		test11u(ts, new Float(123.4));
		test11u(ts, new Double(123.4));
		test11u(ts, new UPIImpl());
		test11u(ts, new byte[]{1, 2, 3});
		test11u(ts, new short[]{1, 2, 3});
		test11u(ts, new int[]{1, 2, 3});
		test11u(ts, new long[]{1, 2, 3});
		test11u(ts, new long[]{1, 2, 2, 2, 2, 2, 3, 3, 4, 5, 3});
		test11u(ts, new float[]{1, 2, 3});
		test11u(ts, new double[]{1, 2, 3});
		test11u(ts, "string");
		test11u(ts, "much longer string at least thirty two characters long");
		test11u(ts, "much longer string at least thirty two characters long with a run 88888888888888888888888 of many 8's and 999999999999999999999999999999   and then some more");
		test11u(ts, new String[]{"aa", "bb", "cc"});
		test11u(ts, new String[]{"much longer string at least thirty two characters long with a run 88888888888888888888888 of many 8's and 999999999999999999999999999999   and then some more", "bb", "cc"});
		test11u(ts, new String[]{null, "bb", "cc"});
		test11u(ts, new String[]{"aa", null, "cc"});
		test11u(ts, new String[]{"aa", "bb", null});
		test11u(ts, new String[]{null, null, null});
		test11u(ts, new UPIImpl[] { new UPIImpl(), new UPIImpl(), new UPIImpl () });
		test11u(ts, new UPIImpl[] { new UPIImpl(), new UPIImpl(-1), new UPIImpl (-2) });
		
		// AGDTest datatypes char char[] String  String[]
		// AGDTest datatypes float double float[] double[]
		
		}
		// AGDTest round-trips per second
		long begin; long delta; int i; int n;
		
		if (p1)
		{
			dl(ts, 0);
		begin = System.currentTimeMillis();
		n = 10*nn;
		for (i=0; i<n; i++) ts.sendOp0(":verify", 1, -1);
		delta = System.currentTimeMillis() - begin;
		pr( "" + n + " calls to :verify " + delta + " millisec.");
		pr( "     " + (int)(1.0/(delta*0.001*(1.0/n))) + " calls per second." );		
		}
		
		if (p2)
		{
			dl(ts, 0);
		int[] a = new int[aa];
		n = nn;
		for (i=1; i<aa; i++) a[i] = (1<<30)-i;
		begin = System.currentTimeMillis();
		for (i=0; i<n; i++) ts.sendOp2(":call", 1, 0, "user::test-1-1", a);
		delta = System.currentTimeMillis() - begin;
		pr( "" + n + " calls with int[" + aa + "] " + delta + " millisec.");
		pr( "     " + (int)(1.0/(delta*0.001*(1.0/(aa*n)))) + " int round-trips per second." );		
		}
		
		ts.disconnect();
		System.exit(fail);
	}
	
	static void test11 ( AGDirectLink ts, long n ) throws IOException {
		Object r;
		r = ts.sendOp2(":call", 1, 0, "user::test-1-1", n);
		if ( !(r instanceof Long) ) 
			throw new  IOException(":call test-1-1 return type " + r);
		if (!(n==(((Long)r).longValue())) ) 
			throw new  IOException(":call test-1-1(" + n + ") result " + ((Long)r).longValue());
	}
	
	static void test11u ( AGDirectLink ts, Object n ) throws IOException {
		Object r;
		r = ts.sendOp2(":call", 1, 0, "user::test-1-1", n);
		if (!( (n.getClass())==r.getClass() ))
			throw new  IOException(":call test-1-1(" + n + ") result " + r);
		if ( (r instanceof String) && n.equals(r) ) return;
		if ( (r instanceof Float) && n.equals(r) ) return;
		if ( (r instanceof Double) && n.equals(r) ) return;
		if ( (r instanceof UPIImpl) && n.equals(r) ) return;
		if ( r instanceof byte[] && Arrays.equals((byte[])n, (byte[])r) ) return;
		if ( r instanceof short[] && Arrays.equals((short[])n, (short[])r) ) return;
		if ( r instanceof int[] && Arrays.equals((int[])n, (int[])r) ) return;
		if ( r instanceof long[] && Arrays.equals((long[])n, (long[])r) ) return;
		if ( r instanceof float[] && Arrays.equals((float[])n, (float[])r) ) return;
		if ( r instanceof double[] && Arrays.equals((double[])n, (double[])r) ) return;
		if ( r instanceof String[] && Arrays.equals((String[])n, (String[])r) ) return;
		if ( r instanceof UPI[] && testUPI((UPI[])n, (UPI[])r) ) return;
		throw new  IOException(":call AGDTest-1-1(" + n + ") result not equal " + r);
	}
	
	static boolean testUPI ( UPI[] n, UPI[] r ) {
		if ( n.length != r.length ) return false;
		for (int i = 0; i < r.length; i++) {
			if ( !n[i].equals(r[i]) ) return false;
		}
		return true;
	}
	
	
	
}
