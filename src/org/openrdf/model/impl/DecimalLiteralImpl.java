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
 * {@link BigDecimal} object.
 * 
 * @author Arjohn Kampman
 */
public class DecimalLiteralImpl extends LiteralImpl {

	private static final long serialVersionUID = -3310213093222314380L;
	
	private final BigDecimal value;

	/**
	 * Creates an xsd:decimal literal with the specified value.
	 */
	public DecimalLiteralImpl(BigDecimal value) {
		this(value, XMLSchema.DECIMAL);
	}

	/**
	 * Creates a literal with the specified value and datatype.
	 */
	public DecimalLiteralImpl(BigDecimal value, URI datatype) {
		// TODO: maybe DecimalLiteralImpl should not extend LiteralImpl?
		super(value.toPlainString(), datatype);
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
		return value.toBigInteger();
	}

	@Override
	public BigDecimal decimalValue()
	{
		return value;
	}
}
