/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import org.openrdf.model.vocabulary.XMLSchema;

/**
 * An extension of {@link LiteralImpl} that stores a boolean value to avoid
 * parsing.
 * 
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class BooleanLiteralImpl extends LiteralImpl {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -3610638093719366795L;

	public static final BooleanLiteralImpl TRUE = new BooleanLiteralImpl(true);

	public static final BooleanLiteralImpl FALSE = new BooleanLiteralImpl(false);

	/*-----------*
	 * Variables *
	 *-----------*/

	private boolean value;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates an xsd:boolean typed literal with the specified value.
	 */
	public BooleanLiteralImpl(boolean value) {
		super(Boolean.toString(value), XMLSchema.BOOLEAN);
		this.value = value;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean booleanValue()
	{
		return value;
	}

	/**
	 * Returns a {@link BooleanLiteralImpl} for the specified value. This method
	 * uses the constants {@link #TRUE} and {@link #FALSE} as result values,
	 * preventing the often unnecessary creation of new
	 * {@link BooleanLiteralImpl} objects.
	 */
	public static BooleanLiteralImpl valueOf(boolean value) {
		return value ? TRUE : FALSE;
	}
}
