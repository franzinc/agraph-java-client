
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import com.franz.ag.UPI;
import com.franz.agbase.impl.UPIImpl;
import com.franz.agbase.util.AGC;


/**
 * This class implements a custom socket transport layer for the Lisp/Java
 * interface to the AllegroGraph triple-store implementation.
 * 
 * Lisp advertises at host:port Java connects
 * Inspired by Jlinker Transport.java as of 2006-05-17
 *  
 */

 public class AGDirectLink extends AGC {

	//	 0 none, 1 top level, 2 tagged part, 3 any part, 5 byte level.
	protected static int debugClient = 0;
	
	int agServerLevel = 0;


	static class Op {
		private String op;
		private long opix;
		private Object[] vals;
		
		String getOp() {
			return op;
		}

		long getOpix() {
			return opix;
		}

		Object[] getVals() {
			return vals;
		}

		Op ( String op, long opix, Object[] vals ) {
			this.op = op;  this.opix = opix;  this.vals = vals;
		}
	}


	

	int state = PORT_CLOSED;

	static final int ERR_PORT_CLOSED = -101;
	
	 static final int ERR_PORT_STATE = -102;

	 static final int ERR_PROTOCOL = -104;

	 static final int ERR_PORT_IO = -107;

	 static final int ERR_FLUSH_IO = -108;

	 static final int ERR_THROW = -109;

	 static final int ERR_BUSY = -111;

	static void throwIOErr(String where, int e) throws IOException {
		String err = "Unknown";
		switch (e) {
		case -1:
			err = "End_of_file";
			break;
		case ERR_PORT_STATE:
			err = "ERR_PORT_STATE";
			break;
		case ERR_PROTOCOL:
			err = "ERR_PROTOCOL";
			break;
		case ERR_PORT_IO:
			err = "ERR_PORT_IO";
			break;
		case ERR_FLUSH_IO:
			err = "ERR_FLUSH_IO";
			break;
		case ERR_THROW:
			err = "ERR_THROW";
			break;
		case ERR_BUSY:
			err = "ERR_BUSY";
			break;
		case ERR_PORT_CLOSED:
			err = "ERR_PORT_CLOSED";
				break;
		default:
			err = "Unknown err(" + e + ")";
		}
		throw new IOException(err + " in " + where);
	}


	java.net.Socket socket;

	java.io.InputStream inStream;

	java.io.OutputStream outStream;

	byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

	int endpos = 0;

	Thread softLock = null;
	
	int timeout = 5000;

	 static String debug(int cl) {
		int oldc = debugClient;
		if (!(cl < 0))
			debugClient = cl;
		return ("Client was " + oldc + " now " + debugClient);
	}

	AGDirectLink() {
	} // dummy just for the sake of making instance
	
	
	AGDirectLink(String host, int port, int pollCount, int pollInterval) 
	throws IOException 
	{
		this(host, port, pollCount, pollInterval, 5000);
	}

	 AGDirectLink(String host, int port, int pollCount, int pollInterval, int timeout) 
	 	throws IOException 
	 {
		// Here we make a server port by connecting to a listening
		// socket on the other end
		java.net.Socket client = null;
		Exception ee = null;
		for (int i = 0; (client==null) && (i < pollCount); i++) {
			try {
				if ( i>0 ) Thread.sleep(pollInterval);
				Socket testClient = new java.net.Socket();
				SocketAddress addr = new InetSocketAddress(host, port);
				//System.out.println("Socket timeout=" + timeout);
				testClient.connect(addr, timeout);
				client = testClient;
			}
			catch (IOException e) { ee = e; }
			catch (InterruptedException e) {}
		}
		if ( client==null )
		{
			if ( ee==null )
				throw new IOException("Failed to connect to server.");
			throw new IOException(ee.toString());
		}
		client.setTcpNoDelay(true);
		socket = client;
		
		inStream = new BufferedInputStream(client.getInputStream());
		outStream = new BufferedOutputStream(client.getOutputStream());
//		inStream = client.getInputStream();
//		outStream = client.getOutputStream();
		
		
		state = PORT_IDLE;
		softLock = null;
		int flag = timeout/10;  // wait for 10 times the timeout value
		while ( flag>0 )
			try {
			Thread.sleep(100);
			flag--;
			if ( 0<inStream.available() ) flag = -1;
		} catch (InterruptedException e) {}
		if ( flag==0 ) {
			socket.close();
			throw new java.io.IOException( "Connected but timed out.");
		}
		int reply = inStream.read();
		switch ( reply ) {
		case TAG_ENDER:
			socket.close();
			throw new java.io.IOException( "Too many connections.");
		case TAG_NULL:
			socket.close();
			throw new java.io.IOException( "Connection rejected.");

		default:
			if ( reply==(TAG_START+AGC.AG_DIRECT_LEVEL) ) break;
		socket.close();
		throw new java.io.IOException( "Unexpected initial reply " + reply);
		}
	}

	static boolean connectFlag = false;

	 boolean query() throws IOException {
		Object r = sendOp1(OP_VERIFY, 1, 0, AGC.AG_DIRECT_LEVEL);
		if ( r instanceof String ) {
			String s = (String)r;
			if ( s.startsWith("AGDirect Version") )
			{
				String pl = "server level";
				int plx = s.indexOf(pl);
				if ( plx<0 ) return true;
				try {
					agServerLevel = 
						Integer.parseInt(s.substring(plx+pl.length()+1));
				} catch (Exception e) {}				
				return true;
			}
		}
		disconnect();
		return false;
	}

	 synchronized boolean disconnect() throws IOException {

		// this one is ok to be synchronized because
		//  call is oneway

		switch (state) {
		case PORT_CLOSED:
			return false;
		case PORT_IDLE: {

			sendOp0(OP_DISCONNECT, -1, -1);

			// give message a chance to arrive???
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}
		}

		default:
			try {
				socket.close();
				state = PORT_CLOSED;
				socket = null;
				inStream = null;
				outStream = null;
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

	synchronized String grabSoftLock(String from) {
		Thread cur = Thread.currentThread();
		if (softLock == null)
			softLock = cur;
		else if (softLock == cur)
			return ("Recursive call to" + from);
		else {
			while (softLock != null)
				try {
					wait();
				} catch (Exception e) {
				}
			softLock = cur;
		}
		return "";
	}

	synchronized void dropSoftLock() {
		if (softLock == null)
			return;
		softLock = null;
		notifyAll();
	}

	/**
	 * Send an operation with zero parts to the server.
	 * 
	 * @param op A string that names the operation.
	 * @param style 1 normal, 0 ignore results, -1 one-way
	 * @return An Object instance that contains the result of the call. The
	 *         class of the object depends on the call.
	 */
	 Object sendOp0(String op, int style, int rx) throws IOException {
		try {
			int opix = sendOpHeader(op, style, 0);
			return sendOpTail(op, opix, style, rx);
		} finally {
			state = PORT_IDLE;
			dropSoftLock();
		}
	}

	 /**
	  * Send an operation with one part to the server.
	  * 
	  * @param op
	  * @param style
	  * @param rx
	  * @param arg
	  * @return
	  * @throws IOException
	  */
	 Object sendOp1(String op, int style, int rx, String arg)
			throws IOException {
		try {
			int opix = sendOpHeader(op, style, 1);
			portOut(arg);
			return sendOpTail(op, opix, style, rx);
		} finally {
			state = PORT_IDLE;
			dropSoftLock();
		}
	}

	 /**
	  * Send an operation with one part to the server.
	  * 
	  * @param op
	  * @param style
	  * @param rx
	  * @param arg
	  * @return
	  * @throws IOException
	  */
	 Object sendOp1(String op, int style, int rx, long arg)
			throws IOException {
		try {
			int opix = sendOpHeader(op, style, 1);
			portOut(arg);
			return sendOpTail(op, opix, style, rx);
		} finally {
			state = PORT_IDLE;
			dropSoftLock();
		}
	}

	 /**
	  * Send an operation with two parts to the server.
	  * @param op
	  * @param style
	  * @param rx
	  * @param arg0
	  * @param arg1
	  * @return
	  * @throws IOException
	  */
	 Object sendOp2(String op, int style, int rx, String arg0, long arg1)
			throws IOException {
		try {
			int opix = sendOpHeader(op, style, 2);
			portOut(arg0);
			portOut(arg1);
			return sendOpTail(op, opix, style, rx);
		} finally {
			state = PORT_IDLE;
			dropSoftLock();
		}
	}

	 /**
	  * Send an operation with two parts to the server.
	  * 
	  * @param op
	  * @param style
	  * @param rx
	  * @param arg0
	  * @param arg1
	  * @return
	  * @throws IOException
	  */
	 Object sendOp2(String op, int style, int rx, String arg0,
			Object arg1) throws IOException {
		try {
			int opix = sendOpHeader(op, style, 2);
			portOut(arg0);
			portOutUnwrapped(arg1);
			return sendOpTail(op, opix, style, rx);
		} finally {
			state = PORT_IDLE;
			dropSoftLock();
		}
	}
	
	 /**
	  * Send an operation with 1+n parts to the server
	  * @param op
	  * @param style
	  * @param rx
	  * @param arg0
	  * @param args
	  * @return
	  * @throws IOException
	  */
	 Object sendOp1n(String op, int style, int rx, String arg0,
			Object[] args) throws IOException {
		try {
			int opix = sendOpHeader(op, style, 1+args.length);
			portOut(arg0);
			for (int i=0; i<args.length; i++) portOutUnwrapped(args[i]);
			return sendOpTail(op, opix, style, rx);
		} finally {
			state = PORT_IDLE;
			dropSoftLock();
		}
	}
	 
	 /**
	  * Send an operation with 2+n parts to the server
	  * 
	  * @param op
	  * @param style
	  * @param rx
	  * @param arg0
	  * @param arg1
	  * @param args
	  * @return
	  * @throws IOException
	  */
	 Object sendOp2n(String op, int style, int rx, String arg0, long arg1,
				Object[] args) throws IOException {
			try {
				int opix = sendOpHeader(op, style, 2+args.length);
				portOut(arg0);
				portOut(arg1);
				for (int i=0; i<args.length; i++) portOutUnwrapped(args[i]);
				return sendOpTail(op, opix, style, rx);
			} finally {
				state = PORT_IDLE;
				dropSoftLock();
			}
		}
	
	 Object sendOp3n(String op, int style, int rx,
			 		 String arg0, long arg1, String arg2,
			 		 Object[] args) throws IOException {
			try {
				int opix = sendOpHeader(op, style, 3+args.length);
				portOut(arg0);
				portOut(arg1);
				portOut(arg2);
				for (int i=0; i<args.length; i++) portOutUnwrapped(args[i]);
				return sendOpTail(op, opix, style, rx);
			} finally {
				state = PORT_IDLE;
				dropSoftLock();
			}
		}
	
	 

	static int opIndex = 0;

	synchronized int getOpIndex() {
		return ++opIndex;
	}

	/**
	 * @param op
	 * @param style
	 *            -1 for one-way call, 0 to ignore result, 1 for normal call
	 * @param argCount
	 * @return operation index
	 * @throws IOException
	 */
	int sendOpHeader(String op, int style, int argCount) throws IOException {
		// override in AGDirectLinkDebug
		int ret = 0;
		int opix = getOpIndex();
		String gv = grabSoftLock("sendOpHeader()");
		if (gv.length() > 0)
			throw new IOException("sendOpHeader " + op + "cannot grab lock: "
					+ gv);
		switch (state) {
		case PORT_IDLE:
			state = PORT_MESSAGE;
			ret = portOutTag(TAG_OP);
			if (!(ret < 0))
				ret = portOut(op);
			if (ret < 0)
				return ret;
			if (style < 0)
				ret = portOut(0);
			else if (style == 0)
				ret = portOut(-opix);
			else
				ret = portOut(opix);
			if (!(ret < 0))
				ret = portOut(argCount);
			break;
		case PORT_CLOSED:
			ret = ERR_PORT_CLOSED;
			break;
		default:
			ret = ERR_PORT_STATE;
		}
		if (ret < 0) throwIOErr("sendOpHeader ", ret);
		return opix;
	}

	Object sendOpTail(String op, int opix, int style, int rx)
			throws IOException {
		// override in AGDirectLinkDebug
		int ret = 0;
		ret = streamOutFlush();
		if (ret < 0)
			throwIOErr("sendOp", ret);
		if (style < 0)
			return null;
		state = PORT_WAITING_REPLY;
		return opResIn(op, opix, rx);
	}

	/**
	 * @param op
	 *            A string that names the operation.
	 * @param reply
	 *            A string that names the result of the operation.
	 * @param rx
	 *            A result index.
	 *            <ul>
	 *            <li>-2 -- expect zero values, return null
	 *            <li>-1 -- return the entire result array which consists of
	 *                {op_name, op-seq-num, value0, value1, ...}
	 *            <li>a positive integer i denotes valuei from the array. The
	 *            other values are discarded.
	 *            </ul>
	 * @return The requested object.
	 * @throws IOException
	 *             if the operation failed in any way.
	 */
	Object opResIn(String op, int opix, int rx) throws IOException {
		// override in AGDirectLinkDebug
		Object[] res = portInOp();
		if ( 2>res.length )
			throw new IOException("opResIn " + op + "[" + opix + "]" +
		              " received " + res.length );
		if (!op.equals(res[0]))
			throw new IOException("opResIn " + op + "[" + opix + "]" +
					              " received " + res[0]);
		long rrx = ((Long) res[1]).longValue();
		if ( rrx<0 && (-rrx)==opix && 3<res.length )
			throw new IOException("opResIn " + op + "[" + opix + "]" +
					     " error in AllegroGraph server " + res[2] +
					     " " + res[3] );
		if ( rrx<0 )
			throw new IOException("opResIn " + op + "[" + opix + "]" +
				     " error in AllegroGraph server " + rrx + " " 
				     + res.length  );
		if (!( rrx==opix))
			throw new IOException("opResIn " + op + "[" + opix + "]" +
					      " expected " + opix + " received " + rrx );
		if (rx == -1)
			return res;
		if (rx==-2)
		{
			if (2==res.length) return null;
			throw new IOException("opResIn " + op + "[" + opix + "]" +
				      " expected zero values, received " + res.length); 
		}
		if ((0 <= rx) && (rx < ((res.length) - 2)))
			return res[rx + 2];
		throw new IOException("opResIn mismatch:" + op + "[" + opix + "]" + 
				" expected " + (rx + 1) + " results, received "
				+ ((res.length) - 2));
	}

	// Low-level I/O

	int streamOutFlush() {
		return portFlush();
	}

	int haveCode = 1000000;

	int streamInCode() throws IOException {
		if (haveCode == 1000000)
			return portIn_8();
		int r = haveCode;
		haveCode = 1000000;
		return r;
	}

	int streamInCode(int code) {
		if (haveCode == 1000000) {
			haveCode = code;
			return code;
		}
		return ERR_PROTOCOL;
	}
		
		String tagToString (int tag) 
		{
			if (tag < TAG_START)
				return "NOT_A_TAG";
			if (tag < TAG_INT_END) 
				return "INTEGER";
			switch (tag) {
				case TAG_NULL:
					return "NULL";
				case TAG_BYTE:
					return "BYTE";
				case TAG_SHORT:
					return "SHORT";
				case TAG_INT:
					return "INT";
				case TAG_CHAR:
					return "CHAR";
				case TAG_FLOAT:
					return "FLOAT";
				case TAG_DOUBLE:
					return "DOUBLE";
				case TAG_SEQ:
					return "SEQ";
				case TAG_SPARSE:
					return "SPARSE";
				case TAG_OBJECT:
					return "OBJECT";
				case TAG_TRUE:
					return "BOOLEANtrue";
				case TAG_FALSE:
					return "BOOLEANfalse";
				case TAG_OPENd:
					return "OPENd";
				case TAG_OP:
					return "OP";
				case TAG_END:
					return "TAG_END";
				default:
					return "TAG_STRING";
				}
		}	
		

	/**
	 * Return one complete tagged item from the input stream.
	 * 
	 * Primitive values are returned as object instances. Operation is returned
	 * as Object[]={opname, resname, part,...} Sequences are returned as vectors
	 * of (primitive) types Currently, there are no Object instances defined
	 */
	Object streamInValue() throws java.io.IOException {
		return streamInValue(streamInCode());
	}

	Object streamInValue(int tag) throws java.io.IOException {
		// override in AGDirectLinkDebug
		Object w = null;
		if (tag < TAG_START)
			throw new java.io.IOException("streamInValue tag " + tag);
		if (tag < TAG_INT_END) {
			w = new Long(portInLong(tag));
		} else
			switch (tag) {
			case TAG_NULL:
				break;
			case TAG_BYTE:
				w = new Byte((byte) portInLong());
				break;
			case TAG_SHORT:
				w = new Short((short) portInLong());
				break;
			case TAG_INT:
				w = new Integer((int) portInLong());
				break;
			case TAG_CHAR:
				StringBuffer wb = new StringBuffer(2);
				//JAVA5 wb.appendCodePoint((int) portInLong());
				wb.append((char) portInLong());
				w = new Character(wb.charAt(0));
				break;
			case TAG_FLOAT:
				w = new Float(portInFloat(tag));
				break;
			case TAG_DOUBLE:
				w = new Double(portInDouble(tag));
				break;
			case TAG_SEQ:
				w = portInSequence(tag);
				break;
			case TAG_SPARSE:
				w = portInSparse(tag);
				break;
			case TAG_OBJECT:
				w = portInObject(tag);
				break;
			case TAG_TRUE:
				w = new Boolean(true);
				break;
			case TAG_FALSE:
				w = new Boolean(false);
				break;
			case TAG_OPENd:
				throw new java.io.IOException("streamInValue tag " + tag);
			case TAG_OP:
				w = portInOp(tag);
				break;
			case TAG_END:
				throw new java.io.IOException("streamInValue tag " + tag);
			case TAG_UPI:
				w = portInUPI(tag);
				break;
			case TAG_BYTES:
				w = portInBytes(tag);
				break;
			default:
				w = portInString(tag);
				break;
			}
		return w;
	}

	Object portInSequence() throws java.io.IOException {
		return portInSequence(streamInCode());
	}

	Object portInSequence(int tag) throws java.io.IOException {
		int len = (int) portInLong();
		int sub = portIn_8();
		return portInSeqBody(tag, len, sub);
	}
	
	boolean isIntTag ( int tag ) {
		if ( tag<TAG_START ) return false;
		if ( tag<TAG_INT_END ) return true;
		return false;
	}
	
	int portInDupRep ( int next, int i, Object w, String from ) throws IOException {
		if ( next==TAG_DUP )
		{
			int j = (int)portInLong();
			Array.set(w, i, Array.get(w, i-j));
			return i+1;
		}
		if ( next==TAG_REP )
		{
			int j = (int)portInLong();
			int n = (int)portInLong();
			Object v = Array.get(w, i-j);
			for (int k = 0; k < n; k++) Array.set(w, i++, v);
			return i;
		}
		throw new java.io.IOException(from + " subtag " + next);
	}
	
	Object portInSeqLong ( int len ) throws IOException {
		long[] wl = new long[len];
		for ( int i=0; i < len; ) {
			int next = portIn_8();
			if ( isIntTag(next) ) wl[i++] = portInLong(next);
			else i = portInDupRep(next, i, wl, "portInSeqLong");
		}		
		return wl;
	}
	
	Object portInSeqByte ( int len ) throws IOException {
		byte[] wl = new byte[len];
		for ( int i=0; i < len; ) {
			int next = portIn_8();
			if ( isIntTag(next) ) wl[i++] = (byte)portInLong(next);
			else i = portInDupRep(next, i, wl, "portInSeqByte");
		}		
		return wl;
	}
	
	Object portInSeqShort ( int len ) throws IOException {
		short[] wl = new short[len];
		for ( int i=0; i < len; ) {
			int next = portIn_8();
			if ( isIntTag(next) ) wl[i++] = (short)portInLong(next);
			else i = portInDupRep(next, i, wl, "portInSeqShort");
		}		
		return wl;
	}
	
	Object portInSeqInt ( int len ) throws IOException {
		int[] wl = new int[len];
		for ( int i=0; i < len; ) {
			int next = portIn_8();
			if ( isIntTag(next) ) wl[i++] = (int)portInLong(next);
			else i = portInDupRep(next, i, wl, "portInSeqInt");
		}		
		return wl;
	}
	
	Object portInSeqFloat ( int len ) throws IOException {
		float[] wl = new float[len];
		for ( int i=0; i < len; ) {
			int next = portIn_8();
			if ( next==TAG_FLOAT ) wl[i++] = portInFloat(next);
			else i = portInDupRep(next, i, wl, "portInSeqFloat");
		}		
		return wl;
	}
	
	Object portInSeqDouble ( int len ) throws IOException {
		double[] wl = new double[len];
		for ( int i=0; i < len; ) {
			int next = portIn_8();
			if ( next==TAG_DOUBLE ) wl[i++] = portInDouble(next);
			else i = portInDupRep(next, i, wl, "portInSeqDouble");
		}		
		return wl;
	}
	
	Object portInSeqString ( int len ) throws IOException {
		String[] wl = new String[len];
		for ( int i=0; i < len; ) {
			int next = portIn_8();
			if ( next==TAG_DUP ) 
			{
				int j = (int)portInLong();
				wl[i] = wl[i-j];
				i++;
			}
			else if ( next==TAG_REP )
			{
				int j = (int)portInLong();
				int n = (int)portInLong();
				String v = wl[i-j];
				for (int k = 0; k < n; k++) wl[i++] = v;
			}
			else 
				{
				CharSequence s = portInString(next);
				if ( s==null )
					wl[i] = null;
				else
					wl[i] = s.toString();
				i++;
				}
		}		
		return wl;
	}
	
	
	Object portInSeqObject ( int len ) throws IOException {
		Object[] wl = new Object[len];
		for ( int i=0; i < len; ) {
			int next = portIn_8();
			if ( next==TAG_DUP ) 
			{
				int j = (int)portInLong();
				wl[i] = wl[i-j];
				i++;
			}
			else if ( next==TAG_REP )
			{
				int j = (int)portInLong();
				int n = (int)portInLong();
				Object v = wl[i-j];
				for (int k = 0; k < n; k++) wl[i++] = v;
			}
			else wl[i++]=streamInValue(next);
		}		
		return wl;
	}
	
	UPIImpl[] portInSeqUPI ( int len ) throws IOException {
		UPIImpl[] wl = new UPIImpl[len];
		for ( int i=0; i < len; ) {
			int next = portIn_8();
			if ( next==TAG_DUP ) 
			{
				int j = (int)portInLong();
				wl[i] = wl[i-j];
				i++;
			}
			else if ( next==TAG_REP )
			{
				int j = (int)portInLong();
				int n = (int)portInLong();
				UPIImpl v = wl[i-j];
				for (int k = 0; k < n; k++) wl[i++] = v;
			}
			else if ( next==TAG_NULL )
				wl[i++]=null;
			else if ( next==TAG_UPI )
				wl[i++]=portInUPI(next);
			else
				wl[i++]=new UPIImpl(portInLong(next));
		}		
		return wl;
	}
	
		
	Object portInSeqBody ( int tag, int len, int sub ) throws java.io.IOException {
//		 override in AGDirectLinkDebug
		if (tag != TAG_SEQ)
			throw new java.io.IOException("portInSequence tag " + tag);
		int i;
		if (sub < TAG_START)
			throw new java.io.IOException("portInSequence subtag " + sub);
		if (sub < TAG_INT_END) return portInSeqLong(len);
		else if (sub < TAG_LSTR)
			switch (sub) {
			case TAG_BYTE:
				return portInSeqByte(len);
			case TAG_UPI:
				return portInSeqUPI(len);
			case TAG_SHORT:
				return portInSeqShort(len);
			case TAG_INT:
				return portInSeqInt(len);
			case TAG_LONG:
				return portInSeqLong(len);
			case TAG_CHAR:
				char[] wc = new char[len];
				for (i = 0; i < len; i++)
					wc[i] = (char) portInLong();
				return wc;
			case TAG_FLOAT:
				return portInSeqFloat(len);
			case TAG_DOUBLE:
				return portInSeqDouble(len);
			case TAG_TRUE:
			case TAG_FALSE:
				boolean[] wbl = new boolean[len];
				for (i = 0; i < len; i++) {
					int b = portIn_8();
					switch (b) {
					case TAG_TRUE:
						wbl[i] = true;
						break;
					case TAG_FALSE:
						wbl[i] = false;
						break;
					default:
						throw new java.io.IOException("portInSequence boolean "
								+ b);
					}
				}
				return wbl;
			default:
				return portInSeqObject(len);
			}
		else return portInSeqString(len);
	}

	Object portInSparse(int tag) throws java.io.IOException {
		throw new java.io.IOException("portInSparse not implemented " + tag);
	}

	Object portInObject(int tag) throws java.io.IOException {
		throw new java.io.IOException("portInObject not implemented" + tag);
	}

	Object[] portInOp() throws java.io.IOException {
		return portInOp(streamInCode());
	}

	static String stringValue ( Object x ) {
		if ( x==null ) return "Null";
		return x.toString();
	}
	Object[] portInOp(int tag) throws java.io.IOException {
		// override in AGDirectLinkDebug
		if (tag != TAG_OP)
			throw new java.io.IOException("portInOp tag " + tag);
		CharSequence op = portInString();
		long opix = portInLong();
		int len = (int) portInLong();
		Object[] w = new Object[len + 2];
		w[0] = op;
		w[1] = new Long(opix);
		for (int i = 0; i < len; i++)
			w[i + 2] = streamInValue();
		String s1 = "Unknown";
		String s2 = "Unknown";
		if (len > 0)
			s1 = stringValue(w[2]);
		if (len > 1)
			s2 = stringValue(w[3]);
		if (opix < 0)
			throw new IllegalArgumentException("Operation " + op + "["
					+ (-opix) + "] signalled an error in server: " + s1
					+ " -- " + s2);
		return w;
	}
	
	/**
	 * Return the results in a separate array.
	 * @param tag
	 * @return an Op instance 
	 * @throws java.io.IOException
	 */
	Object[] portInOpOb(int tag) throws java.io.IOException {
		// override in AGDirectLinkDebug ???
		if (tag != TAG_OP)
			throw new java.io.IOException("portInOp tag " + tag);
		CharSequence op = portInString();
		long opix = portInLong();
		int len = (int) portInLong();
		Object[] rr = new Object[len];
		Object[] ww = new Object[] { op, new Long(opix), rr };
		for (int i = 0; i < len; i++)
			rr[i] = streamInValue();
		if (opix < 0)
		{
			String s1 = "Unknown";
			String s2 = "Unknown";
			if (len > 0)
				s1 = stringValue(rr[0]);
			if (len > 1)
				s2 = stringValue(rr[1]);
			throw new IllegalArgumentException("Operation " + op + "["
					+ (-opix) + "] signalled an error in server: " + s1
					+ " -- " + s2);
		}
		return ww;
	}
	

	int portIn_8() throws IOException {
		// override in AGDirectLinkDebug
		int res;
		try {
			res = inStream.read();
		} catch (java.io.IOException e) {
			res = ERR_PORT_IO;
		}
		if (res < 0)
			throwIOErr("portIn", res);
		return res;
	}
	
	byte[] portInBytes () throws IOException {
		return portInBytes(streamInCode());
	}
	
	byte[] portInBytes ( int tag ) throws IOException {
//		 override in AGDirectLinkDebug
		if (tag != TAG_BYTES)
			throw new java.io.IOException("portInOp tag " + tag);
		byte[] b = new byte[(int)portInLong()];
		for (int i = 0; i < b.length; i++) {
			b[i] = (byte)(0xff & portIn_8());
		}
		return b;
	}
	
	
	UPIImpl portInUPI () throws IOException {
		return portInUPI(streamInCode());
	}
	
	UPIImpl portInUPI ( int tag ) throws IOException {
		//		 override in AGDirectLinkDebug
		if (tag != TAG_UPI)
			throw new java.io.IOException("portInOp tag " + tag);
		UPIImpl u = new UPIImpl();
		for (int i = 0; i < UPI_WIDTH; i++) {
			u.addByte(portIn_8());
		}
		return u;
	}

	int portOutInteger(long x) {
		// override in AGDirectLinkDebug
		long v = x;
		int tag;
		int top;
		int len;
		int sign = 0;

		if (v < 0) {
			v = -(1 + v);
			sign = TAG_SIGN_MASK;
		}
		if (v < TAG_IMM_TOP) {
			tag = -1;
			top = 0;
			len = 1;
		} else if (v < (0x100 + TAG_IMM_TOP)) {
			tag = 0;
			top = 8;
			len = 2;
			v = v - TAG_IMM_TOP;
		} else if (v < 0x10000) {
			tag = 1;
			top = 16;
			len = 3;
		} else if (v < 0x1000000) {
			tag = 2;
			top = 24;
			len = 4;
		} else if (v < 0x100000000l) {
			tag = 3;
			top = 32;
			len = 5;
		} else if (v < 0x10000000000l) {
			tag = 4;
			top = 40;
			len = 6;
		} else if (v < 0x1000000000000l) {
			tag = 5;
			top = 48;
			len = 7;
		} else if (v < 0x100000000000000l) {
			tag = 6;
			top = 56;
			len = 8;
		} else {
			tag = 7;
			top = 64;
			len = 9;
		}
		int rc = portReserveSpace(len);
		if (rc < 0)
			return rc;
		if (tag < 0)
			tag = (int) v;
		else
			tag = TAG_IMM_TOP + tag;
		tag = tag | sign | TAG_START;
		bufferOut_8(tag);
		for (int shift = 0; shift < top; shift += 8)
			bufferOut_8((int) (0xff & (v >> shift)));
		return endpos;
	}

	long portInLong() throws java.io.IOException {
		//NEW
		return portInLong(portIn_8());
	}

	long portInLong(int tag) throws java.io.IOException {
		// override in AGDirectLinkDebug
		if ((tag < TAG_INT_START) || !(tag < TAG_INT_END))
			throw new java.io.IOException("portInLong tag " + tag);
		int s = tag & (TAG_INT_MASK | TAG_SIGN_MASK);
		int count = 0;
		boolean neg = false;
		if (s < TAG_IMM_TOP)
			return s;
		if (s > (TAG_SIGN_MASK - 1)) {
			neg = true;
			s = s - TAG_SIGN_MASK;
			if (s < TAG_IMM_TOP)
				return ((-s) - 1);
		}
		count = s - TAG_IMM_TOP + 1;

		long v = 0;
		int shift = 0;
		long w;
		for (int j = 0; j < count; j++) {
			w = portIn_8();
			if (w < 0)
				throw new java.io.IOException("portInLong->portIn_8=" + w);
			v = v | (w << shift);
			shift += 8;
		}
		if (count == 1)
			v = v + TAG_IMM_TOP;
		if (neg)
			v = (-v) - 1;
		return v;
	}

	CharSequence portInString() throws java.io.IOException {
		return portInString(portIn_8());
	}

	// JAVA 5 version
	//	String portInString(int tag) throws java.io.IOException {
	//		String v;
	//		int[] codes;
	//		int len;
	//		if (tag < TAG_LSTR)
	//			throw new java.io.IOException("portInString tag " + tag);
	//		if (tag == TAG_LSTR) {
	//			len = (int) portInLong();
	//		} else if (!(tag < TAG_SSTR_START) && (tag < TAG_SSTR_END)) {
	//			// Data is sequence of tagged integer Unicode codepoints.
	//			len = tag - TAG_SSTR_START;
	//		} else throw new java.io.IOException("portInString tag " + tag);
	//		codes = new int[len];
	//		int run = 0;
	//		int runChar = 0;
	//		for (int i = 0; i < len; i++) {
	//			if (run == 0) {
	//				int x = streamInCode();
	//				if (x == TAG_FRAG) {
	//					run = (int) portInLong();
	//					runChar = (int) portInLong();
	//					codes[i] = runChar;
	//					run--;
	//				} else
	//					codes[i] = (int) portInLong(x);
	//			} else {
	//				codes[i] = runChar;
	//				run--;
	//			}
	//		}
	//		v = new String(codes, 0, len);
	//		return v;
	//	}

	CharSequence portInString(int tag) throws java.io.IOException {
		// override in AGDirectLinkDebug
		StringBuffer v;
		int len;
		if ( tag==TAG_NULL ) return null;
		if (tag < TAG_LSTR)
			throw new java.io.IOException("portInString tag " + tag);
		if (tag == TAG_LSTR) {
			len = (int) portInLong();
		} else if (!(tag < TAG_SSTR_START) && (tag < TAG_SSTR_END)) {
			// Data is sequence of tagged integer Unicode codepoints.
			len = tag - TAG_SSTR_START;
		} else
			throw new java.io.IOException("portInString tag " + tag);
		v = new StringBuffer(len);
		int run = 0;
		char runChar = 0;
		for (int i = 0; i < len; i++) {
			if (run == 0) {
				int x = streamInCode();
				if (x == TAG_FRAG) {
					run = (int) portInLong();
					runChar = (char) portInLong();
					v.append(runChar);
					run--;
				} else
					v.append((char) portInLong(x));
			} else {
				v.append(runChar);
				run--;
			}
		}
		return new String(v);
	}

	double portInDouble() throws java.io.IOException {
		return portInDouble(streamInCode());
	}

	double portInDouble(int tag) throws java.io.IOException {
		// override in AGDirectLinkDebug
		if (tag != TAG_DOUBLE)
			throw new java.io.IOException("portInDouble tag " + tag);
		int s = portIn_8();
		if (s < 0)
			throw new java.io.IOException("portInDouble->portIn_8=" + s);
		long e0 = portIn_8();
		if (e0 < 0)
			throw new java.io.IOException("portInDouble->portIn_8=" + e0);
		long e1 = portIn_8();
		if (e1 < 0)
			throw new java.io.IOException("portInDouble->portIn_8=" + e1);
		long w;
		long v = 0;
		for (int shift = 0; shift < 51; shift += 8) {
			w = portIn_8();
			if (w < 0)
				throw new java.io.IOException("portInDouble->portIn_8=" + w);
			v = v | (w << shift);
		}
		v = v | (((e1 << 8) | e0) << 52);
		if (s != 0)
			v = v | (-1L << 63);
		double r = Double.longBitsToDouble(v);
		return r;
	}

	float portInFloat() throws java.io.IOException {
		return portInFloat(streamInCode());
	}

	float portInFloat(int tag) throws java.io.IOException {
		// override in AGDirectLinkDebug
		if (tag != TAG_FLOAT)
			throw new java.io.IOException("portInFloat tag " + tag);
		int s = portIn_8();
		if (s < 0)
			throw new java.io.IOException("portInFloat->portIn_8=" + s);
		int e0 = portIn_8();
		if (e0 < 0)
			throw new java.io.IOException("portInFloat->portIn_8=" + e0);
		int e1 = portIn_8();
		if (e1 < 0)
			throw new java.io.IOException("portInFloat->portIn_8=" + e1);
		int w;
		int v = 0;
		for (int shift = 0; shift < 23; shift += 8) {
			w = portIn_8();
			if (w < 0)
				throw new java.io.IOException("portInFloat->portIn_8=" + w);
			v = v | (w << shift);
		}
		v = v | (((e1 << 8) | e0) << 23);
		if (s != 0)
			v = v | (-1 << 31);
		float r = Float.intBitsToFloat(v);
		return r;
	}

	int portFlush() {
		// override in AGDirectLinkDebug
		if (endpos > 0)
			try {
				outStream.write(buffer, 0, endpos);
				outStream.flush();
			} catch (Exception e) {
				return ERR_FLUSH_IO;
			}
		endpos = 0;
		return 0;
	}

	int portReserveSpace(int size) {
		int rc = 0;
		if ((endpos + size) > buffer.length)
			rc = portFlush();
		if (rc < 0)
			return rc;
		return endpos + size;
	}

	int portOutTag(int tag) {
		// override in AGDirectLinkDebug
		int rc = portReserveSpace(1);
		if (rc < 0)
			return rc;
		rc = bufferOut_8(tag);
		return rc;
	}

	int portOut(int tag, long v) {
		int rc = 0;
		if (0 < tag)
			rc = portOutTag(tag);
		if (!(rc < 0))
			rc = portOutInteger(v);
		return rc;
	}

	int portOut ( boolean x ) {
		if ( x )
			return portOutTag(TAG_TRUE);
		return portOutTag(TAG_FALSE);
	}
	
	int portOut(byte x) {
		// override in AGDirectLinkDebug
		return portOut(TAG_BYTE, x);
	}

	int portOut(char x) {
		// override in AGDirectLinkDebug
		return portOut(TAG_CHAR, x);
	}

	int portOut(short x) {
		// override in AGDirectLinkDebug
		return portOut(TAG_SHORT, x);
	}

	int portOut(int x) {
		// override in AGDirectLinkDebug
		return portOut(TAG_INT, x);
	}

	int portOut(long x) {
		// override in AGDirectLinkDebug
		return portOut(0, x);
	}

	int portOutSeqHead(String from, int len, int tag) {
		// override in AGDirectLinkDebug
		if ( from=="" ) from = "";   // to suppress warning
		int rc = portReserveSpace(1);
		if (rc < 0) return rc;
		bufferOut_8(TAG_SEQ);
		rc = portOutInteger(len);
		if (rc < 0) return rc;
		rc = portReserveSpace(1);
		if (rc < 0) return rc;
		bufferOut_8(tag);
		return endpos;
	}

	int portOut ( byte[] x ) {
//		 override in AGDirectLinkDebug
		int rc = portReserveSpace(1);
		if (rc < 0) return rc;
		bufferOut_8(TAG_BYTES);
		rc = portOutInteger(x.length);
		if (rc < 0) return rc;
		for (int i = 0; i < x.length; i++)
		{
			rc = portReserveSpace(1);
			if (rc < 0) return rc;
			bufferOut_8(x[i]);
			
		}
		return endpos;
	}
	
	int portOut ( UPI x ) { return portOut((UPIImpl)x); }
	
	int portOut ( UPIImpl x ) {
//		 override in AGDirectLinkDebug
		if ( null==x.getUpi() ) return portOut ( x.getCode() );
		int rc = portReserveSpace(1);
		if (rc < 0) return rc;
		bufferOut_8(TAG_UPI);
		int b = 0;  int i = 0;
		while ( b>-1 ) 
		{
			b = x.getByte(i++);
			if ( b>-1 )
			{
				rc = portReserveSpace(1);
				if (rc < 0) return rc;
				bufferOut_8(b);
			}
		}
		return endpos;
	}
	
	int portOut(UPI[] x) {
		portOutSeqHead("UPI", x.length, TAG_UPI);
		for (int i = 0; i < x.length; i++)
			portOut(x[i]);
		return endpos;
	}

	int portOut(short[] x) {
		portOutSeqHead("short", x.length, TAG_SHORT);
		for (int i = 0; i < x.length; i++)
			portOutInteger(x[i]);
		return endpos;
	}

	int portOut(int[] x) {
		portOutSeqHead("int", x.length, TAG_INT);
		for (int i = 0; i < x.length; i++)
			portOutInteger(x[i]);
		return endpos;
	}

	int portOut(long[] x) {
		portOutSeqHead("long", x.length, TAG_LONG);
		for (int i = 0; i < x.length; i++)
			portOutInteger(x[i]);
		return endpos;
	}

	int portOut(float[] x) {
		portOutSeqHead("float", x.length, TAG_FLOAT);
		for (int i = 0; i < x.length; i++)
			portOut(x[i]);
		return endpos;
	}

	int portOut(double[] x) {
		portOutSeqHead("double", x.length, TAG_DOUBLE);
		for (int i = 0; i < x.length; i++)
			portOut(x[i]);
		return endpos;
	}

	int portOut(CharSequence[] x) {
		portOutSeqHead("String", x.length, TAG_STRING);
		for (int i = 0; i < x.length; i++)
			portOut(x[i]);
		return endpos;
	}

	int portOut(float x) {
		// override in AGDirectLinkDebug
		int rc = portReserveSpace(7);
		if (rc < 0)
			return rc;
		int b = Float.floatToRawIntBits(x);
		int s = 0;
		if (b < 0) {
			b = b ^ (-1 << 31);
			s = 1;
		}
		int e = (b >> 23);
		b = b | (0xff << 23);
		b = b ^ (0xff << 23);
		bufferOut_8(TAG_FLOAT);
		bufferOut_8(s);
		bufferOut_16(e);
		for (int shift = 0; shift < 23; shift += 8)
			bufferOut_8((int) (0xffL & (b >> shift)));
		return endpos;
	}

	int portOut(double x) {
		// override in AGDirectLinkDebug
		int rc = portReserveSpace(11);
		if (rc < 0)
			return rc;
		long b = Double.doubleToRawLongBits(x);
		int s = 0;
		if (b < 0) {
			b = b ^ (-1L << 63);
			s = 1;
		}
		int e = (int) (b >> 52);
		b = b | (0x7ffL << 52);
		b = b ^ (0x7ffL << 52);
		bufferOut_8(TAG_DOUBLE);
		bufferOut_8(s);
		bufferOut_16(e);
		for (int shift = 0; shift < 52; shift += 8)
			bufferOut_8((int) (0xffL & (b >> shift)));
		return endpos;
	}

	int portOut(CharSequence x) {
		// override in AGDirectLinkDebug
		if ( x==null ) return portOutNull();
		int rc;
		int len = x.length();
		if (len < TAG_SSTR_MAX) {
			// Short string - TAG_SSTR_START + length

			rc = portReserveSpace(1);
			if (rc < 0)
				return rc;
			bufferOut_8(TAG_SSTR_START + len);
			for (int i = 0; i < len; i++)
				//JAVA5 portOutInteger(x.codePointAt(i));
				portOutInteger(x.charAt(i));
		} else {
			// Long string - TAG_LSTR

			rc = portReserveSpace(1);
			if (rc < 0)
				return rc;
			bufferOut_8(TAG_LSTR);
			rc = portOutInteger(len);
			if (rc < 0)
				return rc;
			int run = 0;
			int runChar = 0;
			for (int i = 0; i < len; i++) {
				//JAVA5 int c = x.codePointAt(i);
				int c = x.charAt(i);
				if (run == 0) {
					run = 1;
					runChar = c;
				} else if (runChar == c)
					run++;
				else {
					rc = portOutFragment(run, runChar);
					if (rc < 0)
						return rc;
					run = 1;
					runChar = c;
				}
			}
			rc = portOutFragment(run, runChar);
		}
		return rc;
	}

	int portOutFragment(int run, int runChar) {
		int rc = endpos;
		if (run < TAG_FRAG_MIN)
			for (int j = 0; j < run; j++) {
				rc = portOutInteger(runChar);
				if (rc < 0)
					return rc;
			}
		else {
			rc = portReserveSpace(1);
			if (!(rc < 0))
				bufferOut_8(TAG_FRAG);
			if (!(rc < 0))
				rc = portOutInteger(run);
			if (!(rc < 0))
				rc = portOutInteger(runChar);
		}
		return rc;
	}
	
	int portOutNull () { return portOutTag(TAG_NULL); }

	int portOutUnwrapped(Object arg) throws IOException {
		if (arg == null ) return portOutNull();
		if ( arg instanceof Boolean )
			return portOut( ( (Boolean)arg ).booleanValue() );
		if (arg instanceof Byte)
			return portOut( ( (Byte)arg ).byteValue() );
					//(((Byte) arg).byteValue());
		if (arg instanceof Short)
			return portOut(((Short) arg).shortValue());
		if (arg instanceof Integer)
			return portOut(((Integer) arg).intValue());
		if (arg instanceof Long)
			return portOut(((Long) arg).longValue());
		if (arg instanceof Float)
			return portOut(((Float) arg).floatValue());
		if (arg instanceof Double)
			return portOut(((Double) arg).doubleValue());
		if (arg instanceof byte[])
			return portOut((byte[]) arg);
		if (arg instanceof UPIImpl)
			return portOut((UPIImpl)arg);
		if (arg instanceof UPI[])
			return portOut((UPI[])arg);
		if (arg instanceof short[])
			return portOut((short[]) arg);
		if (arg instanceof int[])
			return portOut((int[]) arg);
		if (arg instanceof long[])
			return portOut((long[]) arg);
		if (arg instanceof float[])
			return portOut((float[]) arg);
		if (arg instanceof double[])
			return portOut((double[]) arg);
		if (arg instanceof String)
			return portOut((String) arg);
		if (arg instanceof String[])
			return portOut((String[]) arg);
		throw new IOException("Cannot unwrap " + arg);
	}

	int bufferOut_8(int x) {
		// override in AGDirectLinkDebug
		buffer[endpos] = (byte) (0xff & x);
		endpos++;
		return endpos;
	}

	int bufferOut_16(int x) {
		// override in AGDirectLinkDebug
		buffer[endpos] = (byte) (0xff & x);
		endpos++;
		buffer[endpos] = (byte) (0xff & (x >> 8));
		endpos++;
		return endpos;
	}

}

