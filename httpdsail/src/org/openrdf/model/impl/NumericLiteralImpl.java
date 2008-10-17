/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * An extension of {@link LiteralImpl} that stores a numeric value to avoid
 * parsing.
 * 
 * @author David Huynh
 */
public class NumericLiteralImpl extends LiteralImpl {

	private static final long serialVersionUID = 3004497457768807919L;

	private final Number number;

	/**
	 * Creates a literal with the specified value and datatype.
	 */
	public NumericLiteralImpl(Number number, URI datatype) {
		super(number.toString(), datatype);
		this.number = number;
	}

	/**
	 * Creates an xsd:byte typed litral with the specified value.
	 */
	public NumericLiteralImpl(byte number) {
		this(number, XMLSchema.BYTE);
	}

	/**
	 * Creates an xsd:short typed litral with the specified value.
	 */
	public NumericLiteralImpl(short number) {
		this(number, XMLSchema.SHORT);
	}

	/**
	 * Creates an xsd:int typed litral with the specified value.
	 */
	public NumericLiteralImpl(int number) {
		this(number, XMLSchema.INT);
	}

	/**
	 * Creates an xsd:long typed litral with the specified value.
	 */
	public NumericLiteralImpl(long n) {
		this(n, XMLSchema.LONG);
	}

	/**
	 * Creates an xsd:float typed litral with the specified value.
	 */
	public NumericLiteralImpl(float n) {
		this(n, XMLSchema.FLOAT);
	}

	/**
	 * Creates an xsd:double typed litral with the specified value.
	 */
	public NumericLiteralImpl(double n) {
		this(n, XMLSchema.DOUBLE);
	}

	@Override
	public byte byteValue()
	{
		return number.byteValue();
	}

	@Override
	public short shortValue()
	{
		return number.shortValue();
	}

	@Override
	public int intValue()
	{
		return number.intValue();
	}

	@Override
	public long longValue()
	{
		return number.longValue();
	}

	@Override
	public float floatValue()
	{
		return number.floatValue();
	}

	@Override
	public double doubleValue()
	{
		return number.doubleValue();
	}
}
