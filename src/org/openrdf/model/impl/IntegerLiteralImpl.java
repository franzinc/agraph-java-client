/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * An extension of {@link LiteralImpl} that stores an integer value using a
 * {@link BigInteger} object.
 * 
 * @author Arjohn Kampman
 */
public class IntegerLiteralImpl extends LiteralImpl {

	private static final long serialVersionUID = 4199641304079427245L;

	private final BigInteger value;

	/**
	 * Creates an xsd:integer literal with the specified value.
	 */
	public IntegerLiteralImpl(BigInteger value) {
		this(value, XMLSchema.INTEGER);
	}

	/**
	 * Creates a literal with the specified value and datatype.
	 */
	public IntegerLiteralImpl(BigInteger value, URI datatype) {
		// TODO: maybe IntegerLiteralImpl should not extend LiteralImpl?
		super(value.toString(), datatype);
		this.value = value;
	}

	@Override
	public byte byteValue()
	{
		return value.byteValue();
	}

	@Override
	public short shortValue()
	{
		return value.shortValue();
	}

	@Override
	public int intValue()
	{
		return value.intValue();
	}

	@Override
	public long longValue()
	{
		return value.longValue();
	}

	@Override
	public float floatValue()
	{
		return value.floatValue();
	}

	@Override
	public double doubleValue()
	{
		return value.doubleValue();
	}

	@Override
	public BigInteger integerValue()
	{
		return value;
	}

	@Override
	public BigDecimal decimalValue()
	{
		return new BigDecimal(value);
	}
}
