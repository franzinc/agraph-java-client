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
 * 
 */
public class Deserial {
	
	/* data to process */
	private byte[] data;
	
	private int pos;
	
	private int max;
	
	private boolean finished = false;
	
	public static Object deserialize(String data) {
		Deserial o = new Deserial( AGDecoder.decode(data) );
		return o.deserialize();
	}
	
	public Deserial(byte[] givendata) {
		data = givendata;
		pos = 0;
		max = givendata.length;
	}
	
	byte nextbyte() {
		if (pos >= max) {
			throw new RuntimeException("ran off the end");
		}
		pos++;
		return data[pos - 1];
	}
	
	int posInteger() {
		int result = 0;
		int shift = 0;
		
		while (true) {
			int val = nextbyte();
			int masked;
			
			masked = val & 0x7f;
			result = result + (masked << shift);
			if ((val & 0x80) == 0)
				break;
			shift += 7;
		}
		
		return result;
		
	}
	
	public Object deserialize() {
		byte val = nextbyte();
		int length;
		
		switch (val) {
		case SO_VECTOR: {
			length = posInteger();
			Object[] res = new Object[length];
			
			for (int i = 0; i < length; i++) {
				res[i] = deserialize();
			}
			return res;
		}
			
		case SO_STRING: {
			length = posInteger();
			
			StringBuilder res = new StringBuilder();
			for (int i = 0; i < length; i++) {
				res.append((char) nextbyte());
			}
			return res.toString();
		}
			
		case SO_NULL:
			return null;
			
		case SO_END_OF_ITEMS:
			finished = true;
			return null;
			
		default:
			throw new RuntimeException("bad code found by deserializer: " + val);
			
		}
	}

}
