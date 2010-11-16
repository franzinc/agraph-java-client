/******************************************************************************
 ** Copyright (c) 2008-2010 Franz Inc.
 ** All rights reserved. This program and the accompanying materials
 ** are made available under the terms of the Eclipse Public License v1.0
 ** which accompanies this distribution, and is available at
 ** http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package com.franz.agraph.http;

import java.util.List;

/**
 * Given a array of arrays and strings convert to a byte array.
 * 
 * @since v4.2
 * @deprecated The stored proc feature and API are experimental, and subject to change in a future release.
 */
public class AGSerializer {
	
	private ByteArray barr;
	
	/**
	 * @param data Strings, Integers, null, byte[], Object[], and {@link List} of these.
	 * @return byte-encoded and serialized to a string
	 */
	public static String serializeAndEncode(Object[] data) {
		AGSerializer o = new AGSerializer();
		o.serializex(data);
		return AGEncoder.encode(o.finish());
	}
	
	public AGSerializer() {
		// start the serialization process
		barr = new ByteArray();
	}
	
	public byte[] finish() {
		// when all objects are serialized, this returns the byte
		// array with the serialized data
		
		barr.addbyte(SerialConstants.SO_END_OF_ITEMS);
		
		byte[] retv = barr.extract();
		barr = null; // to be gc'ed
		
		return retv;
	}
	
	public AGSerializer serializex(Object obj) {
		if (obj instanceof String) {
			String str = (String) obj;
			barr.addbyte(SerialConstants.SO_STRING);
			serializeInteger(str.length());
			for (int i = 0; i < str.length(); i++) {
				
				// deal with unicode cvt via utf-8 here
				barr.addbyte((byte) (str.codePointAt(i) & 0xff));
			}
		} else if (obj instanceof Integer) {
			int i = (Integer) obj;
			if (i >= 0) {
				barr.addbyte(SerialConstants.SO_POS_INTEGER);
				serializeInteger(i);
			} else {
				barr.addbyte(SerialConstants.SO_NEG_INTEGER);
				serializeInteger(-i);
			}
		} else if (obj instanceof Object[]) {
			Object[] vec = (Object[]) obj;
			barr.addbyte(SerialConstants.SO_VECTOR);
			serializeInteger(vec.length);
			for (int i = 0; i < vec.length; i++) {
				serializex(vec[i]);
			}
		} else if (obj instanceof byte[]) {
			byte[] vec = (byte[]) obj;
			barr.addbyte(SerialConstants.SO_BYTEVECTOR);
			serializeInteger(vec.length);
			for (int i = 0; i < vec.length; i++) {
				barr.addbyte(vec[i]);
			}
		} else if (obj instanceof List) {
			List vec = (List) obj;
			barr.addbyte(SerialConstants.SO_LIST);
			serializeInteger(vec.size());
			for (int i = 0; i < vec.size(); i++) {
				serializex(vec.get(i));
			}
			// TODO: extra null needed by lisp side, bug in lisp?
			barr.addbyte(SerialConstants.SO_NULL);
		} else if (obj == null) {
			barr.addbyte(SerialConstants.SO_NULL);
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
