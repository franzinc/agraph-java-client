/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.datatypes.XMLDatatypeUtil;

/**
 * An implementation of the {@link Literal} interface.
 * 
 * @author Arjohn Kampman
 * @author David Huynh
 */
public class LiteralImpl implements Literal {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -1649571784782592271L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The literal's label.
	 */
	private String label;

	/**
	 * The literal's language tag (null if not applicable).
	 */
	private String language;

	/**
	 * The literal's datatype (null if not applicable).
	 */
	private URI datatype;

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected LiteralImpl() {
	}

	/**
	 * Creates a new plain literal with the supplied label.
	 * 
	 * @param label
	 *        The label for the literal, must not be <tt>null</tt>.
	 */
	public LiteralImpl(String label) {
		this(label, null, null);
	}

	/**
	 * Creates a new plain literal with the supplied label and language tag.
	 * 
	 * @param label
	 *        The label for the literal, must not be <tt>null</tt>.
	 * @param language
	 *        The language tag for the literal.
	 */
	public LiteralImpl(String label, String language) {
		this(label, language, null);
	}

	/**
	 * Creates a new datyped literal with the supplied label and datatype.
	 * 
	 * @param label
	 *        The label for the literal, must not be <tt>null</tt>.
	 * @param datatype
	 *        The datatype for the literal.
	 */
	public LiteralImpl(String label, URI datatype) {
		this(label, null, datatype);
	}

	/**
	 * Creates a new Literal object, initializing the variables with the supplied
	 * parameters.
	 */
	private LiteralImpl(String label, String language, URI datatype) {
		assert label != null;

		setLabel(label);
		if (language != null) {
			setLanguage(language.toLowerCase());
		}
		if (datatype != null) {
			setDatatype(datatype);
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	protected void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	protected void setLanguage(String language) {
		this.language = language;
	}

	public String getLanguage() {
		return language;
	}

	protected void setDatatype(URI datatype) {
		this.datatype = datatype;
	}

	public URI getDatatype() {
		return datatype;
	}

	// Overrides Object.equals(Object), implements Literal.equals(Object)
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof Literal) {
			Literal other = (Literal)o;

			// Compare labels
			if (!label.equals(other.getLabel())) {
				return false;
			}

			// Compare datatypes
			if (datatype == null) {
				if (other.getDatatype() != null) {
					return false;
				}
			}
			else {
				if (!datatype.equals(other.getDatatype())) {
					return false;
				}
			}

			// Compare language tags
			if (language == null) {
				if (other.getLanguage() != null) {
					return false;
				}
			}
			else {
				if (!language.equals(other.getLanguage())) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	// overrides Object.hashCode(), implements hashCode()
	@Override
	public int hashCode() {
		return label.hashCode();
	}

	/**
	 * Returns the label of the literal.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(label.length() * 2);

		sb.append('"');
		sb.append(label);
		sb.append('"');

		if (language != null) {
			sb.append('@');
			sb.append(language);
		}

		if (datatype != null) {
			sb.append("^^");
			sb.append(datatype.toString());
		}

		return sb.toString();
	}

	public String stringValue() {
		return label;
	}

	public boolean booleanValue() {
		return XMLDatatypeUtil.parseBoolean(getLabel());
	}

	public byte byteValue() {
		return XMLDatatypeUtil.parseByte(getLabel());
	}

	public short shortValue() {
		return XMLDatatypeUtil.parseShort(getLabel());
	}

	public int intValue() {
		return XMLDatatypeUtil.parseInt(getLabel());
	}

	public long longValue() {
		return XMLDatatypeUtil.parseLong(getLabel());
	}

	public float floatValue() {
		return XMLDatatypeUtil.parseFloat(getLabel());
	}

	public double doubleValue() {
		return XMLDatatypeUtil.parseDouble(getLabel());
	}

	public BigInteger integerValue() {
		return XMLDatatypeUtil.parseInteger(getLabel());
	}

	public BigDecimal decimalValue() {
		return XMLDatatypeUtil.parseDecimal(getLabel());
	}

	public XMLGregorianCalendar calendarValue() {
		return XMLDatatypeUtil.parseCalendar(getLabel());
	}
}
