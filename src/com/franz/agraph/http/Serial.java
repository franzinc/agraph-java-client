/******************************************************************************
 ** Copyright (c) 2008-2010 Franz Inc.
 ** All rights reserved. This program and the accompanying materials
 ** are made available under the terms of the Eclipse Public License v1.0
 ** which accompanies this distribution, and is available at
 ** http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package com.franz.agraph.http;

import static com.franz.agraph.http.SerialConstants.SO_END_OF_ITEMS;
import static com.franz.agraph.http.SerialConstants.SO_NULL;
import static com.franz.agraph.http.SerialConstants.SO_STRING;
import static com.franz.agraph.http.SerialConstants.SO_VECTOR;

/**
 * given a array of arrays and strings convert to a byte array
 */
public class Serial {
	
	private ByteArray barr;
	
	public static String serialize(Object data) {
		Serial o = new Serial();
		o.serializex(data);
		return AGEncoder.encode(o.finish());
	}
	
	public Serial() {
		// start the serialization process
		barr = new ByteArray();
	}
	
	public byte[] finish() {
		// when all objects are serialized, this returns the byte
		// array with the serialized data
		
		barr.addbyte(SO_END_OF_ITEMS);
		
		byte[] retv = barr.extract();
		barr = null; // to be gc'ed
		
		return retv;
	}
	
	public Serial serializex(Object obj) {
		if (obj instanceof String) {
			String str = (String) obj;
			barr.addbyte(SO_STRING);
			serializeInteger(str.length());
			for (int i = 0; i < str.length(); i++) {
				
				// deal with unicode cvt via utf-8 here
				barr.addbyte((byte) (str.codePointAt(i) & 0xff));
			}
		} else if (obj instanceof Object[]) {
			Object[] vec = (Object[]) obj;
			
			barr.addbyte(SO_VECTOR);
			serializeInteger(vec.length);
			for (int i = 0; i < vec.length; i++) {
				serializex(vec[i]);
			}
		} else if (obj == null) {
			barr.addbyte(SO_NULL);
		} else {
			throw new RuntimeException("cannot serialize object " + obj);
		}
		
		return this;
		
	}
	
	void serializeInteger(int i) {
		// i is non negative
		while (true) {
			byte lower = (byte) (i & 0x7f);
			int rest = i >> 7;
			
			if (rest != 0) {
				lower |= 0x80;
				
			}
			barr.addbyte(lower);
			
			if (rest == 0) {
				break;
			}
			i = rest;
		}
		
		return;
		
	}
}
