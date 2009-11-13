
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

import com.franz.agbase.impl.UPIImpl;


class AGDirectLinkDebug extends AGDirectLink {
	
//	 0 none, 1 top level, 2 tagged part, 3 any part, 5 byte level.
//   static int debugClient = 0;


	void dp(int level, String text) {
		if (!(level > debugClient)) {
			String head = "";
			for (int i=0; i<level; i++) head = head + "  ";
			System.out.println( head + text );
	}}

	AGDirectLinkDebug() {
	} // dummy just for the sake of making instance

	AGDirectLinkDebug(String host, int port, int pc, int pi) 
		throws java.io.IOException {
		super(host, port, pc, pi);
		dp(1, "new " + host + ":" + port + " " + pc + "/" + pi);
	}
	
	AGDirectLinkDebug(String host, int port, int pc, int pi, int timeout) 
	throws java.io.IOException {
		super(host, port, pc, pi, timeout);
		dp(1, "new " + host + ":" + port + " " + pc + "/" + pi + 
				" timout " + timeout);
	}
	
	int sendOpHeader(String op, int style, int argCount) throws IOException {
		dp(1, "opHeader: " + op + " " + style + " " + argCount);
		return super.sendOpHeader(op, style, argCount);
	}

	Object sendOpTail(String op, int opix, int style, int rx) throws IOException {
		dp(1, "opTail: " + op + " " + opix);
		return super.sendOpTail(op, opix, style, rx);
	}

	Object opResIn(String op, int opix, int rx) throws IOException {
		Object r = super.opResIn(op, opix, rx);
		dp(1, "opRes=" + op + " " + opix + " " + r);
		return r;
	}

	Object streamInValue(int tag) throws java.io.IOException {
		Object w = super.streamInValue(tag);
		dp(2, "streamIn=" + tag + ": " + w);
		return w;
	}

	Object portInSeqBody(int tag, int len, int sub) throws java.io.IOException {
		dp(2, "portInSequence:" + tag + "(" + len + ")" + sub);
		Object w = super.portInSeqBody(tag, len, sub);
		dp(2, "portInSequence=" + w);
		return w;
	}

	Object[] portInOp(int tag) throws java.io.IOException {
		dp(2, "portInOp:0x" + Integer.toHexString(tag));
		return super.portInOp(tag);
	}

	int portIn_8() throws IOException {
		int res = super.portIn_8();
		dp(5, "portIn_8=" + res + "  0x" +
				Integer.toHexString(res)
	              + "  0b" + Integer.toBinaryString(res)
					);	
		return res;
	}

	int portOutInteger(long x) {
		dp(4, "portOutInteger: " + x);
		return super.portOutInteger(x);
	}

	long portInLong(int tag) throws java.io.IOException {
		dp(2, "portInLong: tag=0x" + Integer.toHexString(tag));
		long r = super.portInLong(tag);
		dp(2, "portInLong= " + r);
		return r;
	}

	CharSequence portInString(int tag) throws java.io.IOException {
		dp(2, "portInString: tag 0x" + Integer.toHexString(tag));
		CharSequence v = super.portInString(tag);
		dp(2, "portInString= " + v);
		return v;
	}

	double portInDouble(int tag) throws java.io.IOException {
		double r = super.portInDouble(tag);
		dp(2, "portInDouble= " + r);
		return r;
	}

	float portInFloat(int tag) throws java.io.IOException {
		float r = super.portInFloat(tag);
		dp(2, "portInFloat= " + r);
		return r;
	}
	
	byte[] portInBytes ( int tag ) throws IOException {
		byte[] b = super.portInBytes(tag);
		dp(2, "portInBytes= " + b.length);
		return b;
	}
	
	UPIImpl portInUPI ( int tag ) throws IOException {
		UPIImpl b = super.portInUPI(tag);
		dp(2, "portInUPI= " + b);
		return b;
	}

	int portFlush() {
		if (endpos > 0) 
			dp(4, "portFlush: " + endpos + " bytes: "
				+ buffer[0] + " " + buffer[1] + "...");
		return super.portFlush();
	}

	int portOutTag(int tag) {
		int rc = super.portOutTag(tag);
		dp(4, "portOut tag= " + tag);
		return rc;
	}

	int portOut(byte x) {
		dp(2, "portOut byte: " + x);
		return super.portOut(x);
	}

	int portOut(char x) {
		dp(2, "portOut char: " + x);
		return super.portOut(x);
	}

	int portOut(short x) {
		dp(2, "portOut short: " + x);
		return super.portOut(x);
	}

	int portOut(int x) {
		dp(2, "portOut int: " + x);
		return super.portOut(x);
	}
	
	int portOut(long x) {
		dp(2, "portOut long: " + x);
		return super.portOut(x);
	}

	int portOutSeqHead( String from, int len, int tag ) {
		dp(2, "portOut seq: of " + from + "[" + len + "] tag=" + Integer.toHexString(tag));
		return super.portOutSeqHead(from, len, tag);
	}

	int portOut(float x) {
		dp(2, "portOut float: " + x);
		return super.portOut(x);
	}

	int portOut(double x) {
		dp(2, "portOut double: " + x);
		return super.portOut(x);
	}

	int portOut(CharSequence x) {
		dp(2, "portOut String: " + x);
		return super.portOut(x);
	}
	
	int portOut ( UPIImpl x ) {
		dp(2, "portOut UPI: " + x);
		return super.portOut(x);
	}
	
	

	int bufferOut_8(int x) {
		dp(5, "bufferOut_8: " + x + "  0x" + Integer.toHexString(x)
					              + "  0b" + Integer.toBinaryString(x)
									);	
		return super.bufferOut_8(x);
	}

	int bufferOut_16(int x) {
		dp(5, "bufferOut_16: " + x);
		return super.bufferOut_16(x);
	}

}

