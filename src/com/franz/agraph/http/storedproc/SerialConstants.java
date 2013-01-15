/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http.storedproc;

/**
 *
 */
interface SerialConstants {

	byte SO_VECTOR = 1;
	byte SO_STRING = 5;
	byte SO_NULL = 7;
	byte SO_LIST = 8;
	byte SO_POS_INTEGER = 9;
	byte SO_END_OF_ITEMS = 10;
	byte SO_NEG_INTEGER = 11;
	byte SO_BYTEVECTOR = 15; // usb8 vector
	
}
