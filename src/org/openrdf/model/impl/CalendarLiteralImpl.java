/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.datatypes.XMLDatatypeUtil;

/**
 * An extension of {@link LiteralImpl} that stores a calendar value to avoid
 * parsing.
 * 
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class CalendarLiteralImpl extends LiteralImpl {

	private static final long serialVersionUID = -8959671333074894312L;

	private final XMLGregorianCalendar calendar;

	/**
	 * Creates a literal for the specified calendar using a datatype appropriate
	 * for the value indicated by {@link XMLGregorianCalendar#getXMLSchemaType()}.
	 */
	public CalendarLiteralImpl(XMLGregorianCalendar calendar) {
		super(calendar.toXMLFormat(), XMLDatatypeUtil.qnameToURI(calendar.getXMLSchemaType()));
		this.calendar = calendar;
	}

	@Override
	public XMLGregorianCalendar calendarValue()
	{
		return calendar;
	}
}
