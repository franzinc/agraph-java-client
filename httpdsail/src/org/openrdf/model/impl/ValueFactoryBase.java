/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * Abstract base class for {@link ValueFactory} implementations that implements
 * the utility methods for creating literals for basic types by calling the
 * generic {@link ValueFactory#createLiteral(String, URI)} with the appropriate
 * value and datatype.
 * 
 * @author Arjohn Kampman
 */
public abstract class ValueFactoryBase implements ValueFactory {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The ID for the next bnode that is created.
	 */
	private int nextBNodeID;

	/**
	 * The prefix for any new bnode IDs.
	 */
	private String bnodePrefix;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ValueFactoryBase() {
		initBNodeParams();
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Generates a new bnode prefix based on <tt>currentTimeMillis()</tt> and
	 * resets <tt>_nextBNodeID</tt> to <tt>1</tt>.
	 */
	protected void initBNodeParams() {
		// BNode prefix is based on currentTimeMillis(). Combined with a
		// sequential number per session, this gives a unique identifier.
		bnodePrefix = "node" + Long.toString(System.currentTimeMillis(), 32) + "x";
		nextBNodeID = 1;
	}

	public BNode createBNode() {
		if (nextBNodeID == Integer.MAX_VALUE) {
			// Start with a new bnode prefix
			initBNodeParams();
		}

		return createBNode(bnodePrefix + nextBNodeID++);
	}

	/**
	 * Calls {@link ValueFactory#createLiteral(String, URI)} with the
	 * String-value of the supplied value and {@link XMLSchema#BOOLEAN} as
	 * parameters.
	 */
	public Literal createLiteral(boolean b) {
		return createLiteral(Boolean.toString(b), XMLSchema.BOOLEAN);
	}

	/**
	 * Calls {@link #createIntegerLiteral(long, URI)} with the supplied value and
	 * {@link XMLSchema#BYTE} as parameters.
	 */
	public Literal createLiteral(byte value) {
		return createIntegerLiteral(value, XMLSchema.BYTE);
	}

	/**
	 * Calls {@link #createIntegerLiteral(long, URI)} with the supplied value and
	 * {@link XMLSchema#SHORT} as parameters.
	 */
	public Literal createLiteral(short value) {
		return createIntegerLiteral(value, XMLSchema.SHORT);
	}

	/**
	 * Calls {@link #createIntegerLiteral(long, URI)} with the supplied value and
	 * {@link XMLSchema#INT} as parameters.
	 */
	public Literal createLiteral(int value) {
		return createIntegerLiteral(value, XMLSchema.INT);
	}

	/**
	 * Calls {@link #createIntegerLiteral(long, URI)} with the supplied value and
	 * {@link XMLSchema#LONG} as parameters.
	 */
	public Literal createLiteral(long value) {
		return createIntegerLiteral(value, XMLSchema.LONG);
	}

	/**
	 * Calls {@link #createNumericLiteral(Number, URI)} with the supplied value
	 * and datatype as parameters.
	 */
	protected Literal createIntegerLiteral(Number value, URI datatype) {
		return createNumericLiteral(value, datatype);
	}

	/**
	 * Calls {@link #createFPLiteral(Number, URI)} with the supplied value and
	 * {@link XMLSchema#FLOAT} as parameters.
	 */
	public Literal createLiteral(float value) {
		return createFPLiteral(value, XMLSchema.FLOAT);
	}

	/**
	 * Calls {@link #createFPLiteral(Number, URI)} with the supplied value and
	 * {@link XMLSchema#DOUBLE} as parameters.
	 */
	public Literal createLiteral(double value) {
		return createFPLiteral(value, XMLSchema.DOUBLE);
	}

	/**
	 * Calls {@link #createNumericLiteral(Number, URI)} with the supplied value
	 * and datatype as parameters.
	 */
	protected Literal createFPLiteral(Number value, URI datatype) {
		return createNumericLiteral(value, datatype);
	}

	/**
	 * Calls {@link ValueFactory#createLiteral(String, URI)} with the
	 * String-value of the supplied number and the supplied datatype as
	 * parameters.
	 */
	protected Literal createNumericLiteral(Number number, URI datatype) {
		return createLiteral(number.toString(), datatype);
	}

	/**
	 * Calls {@link ValueFactory#createLiteral(String, URI)} with the
	 * String-value of the supplied calendar and the appropriate datatype as
	 * parameters.
	 * 
	 * @see XMLGregorianCalendar#toXMLFormat()
	 * @see XMLGregorianCalendar#getXMLSchemaType()
	 * @see XMLDatatypeUtil#qnameToURI(javax.xml.namespace.QName)
	 */
	public Literal createLiteral(XMLGregorianCalendar calendar) {
		return createLiteral(calendar.toXMLFormat(), XMLDatatypeUtil.qnameToURI(calendar.getXMLSchemaType()));
	}
}
