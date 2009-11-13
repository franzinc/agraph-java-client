/*  Sesame - Storage and Querying architecture for RDF and RDF Schema
 *  Copyright (C) 2001-2006 Aduna
 *
 *  Contact: 
 *  	Aduna
 *  	Prinses Julianaplein 14 b
 *  	3817 CS Amersfoort
 *  	The Netherlands
 *  	tel. +33 (0)33 465 99 87
 *  	fax. +33 (0)33 465 99 87
 *
 *  	http://aduna-software.com/
 *  	http://www.openrdf.org/
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.franz.agbase;

/**
 * Defines constants for the standard XML Schema data types.
 **/
public class XmlSchema {

/*
 * The XML Schema namespace
 */

	/** The XML Schema namespace (<tt>http://www.w3.org/2001/XMLSchema#</tt>). **/
	public static final String NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

/*
 * Primitive datatypes
 */

	/** <tt>http://www.w3.org/2001/XMLSchema#duration</tt> **/
	public static final String DURATION = NAMESPACE + "duration";

	/** <tt>http://www.w3.org/2001/XMLSchema#dateTime</tt> **/
	public static final String DATETIME = NAMESPACE + "dateTime";

	/** <tt>http://www.w3.org/2001/XMLSchema#time</tt> **/
	public static final String TIME = NAMESPACE + "time";

	/** <tt>http://www.w3.org/2001/XMLSchema#date</tt> **/
	public static final String DATE = NAMESPACE + "date";

	/** <tt>http://www.w3.org/2001/XMLSchema#gYearMonth</tt> **/
	public static final String GYEARMONTH = NAMESPACE + "gYearMonth";

	/** <tt>http://www.w3.org/2001/XMLSchema#gYear</tt> **/
	public static final String GYEAR = NAMESPACE + "gYear";

	/** <tt>http://www.w3.org/2001/XMLSchema#gMonthDay</tt> **/
	public static final String GMONTHDAY = NAMESPACE + "gMonthDay";

	/** <tt>http://www.w3.org/2001/XMLSchema#gDay</tt> **/
	public static final String GDAY = NAMESPACE + "gDay";

	/** <tt>http://www.w3.org/2001/XMLSchema#gMonth</tt> **/
	public static final String GMONTH = NAMESPACE + "gMonth";

	/** <tt>http://www.w3.org/2001/XMLSchema#string</tt> **/
	public static final String STRING = NAMESPACE + "string";

	/** <tt>http://www.w3.org/2001/XMLSchema#boolean</tt> **/
	public static final String BOOLEAN = NAMESPACE + "boolean";

	/** <tt>http://www.w3.org/2001/XMLSchema#base64Binary</tt> **/
	public static final String BASE64BINARY = NAMESPACE + "base64Binary";

	/** <tt>http://www.w3.org/2001/XMLSchema#hexBinary</tt> **/
	public static final String HEXBINARY = NAMESPACE + "hexBinary";

	/** <tt>http://www.w3.org/2001/XMLSchema#float</tt> **/
	public static final String FLOAT = NAMESPACE + "float";

	/** <tt>http://www.w3.org/2001/XMLSchema#decimal</tt> **/
	public static final String DECIMAL = NAMESPACE + "decimal";

	/** <tt>http://www.w3.org/2001/XMLSchema#double</tt> **/
	public static final String DOUBLE = NAMESPACE + "double";

	/** <tt>http://www.w3.org/2001/XMLSchema#anyURI</tt> **/
	public static final String ANYURI = NAMESPACE + "anyURI";

	/** <tt>http://www.w3.org/2001/XMLSchema#QName</tt> **/
	public static final String QNAME = NAMESPACE + "QName";

	/** <tt>http://www.w3.org/2001/XMLSchema#NOTATION</tt> **/
	public static final String NOTATION = NAMESPACE + "NOTATION";

/*
 * Derived datatypes
 */

	/** <tt>http://www.w3.org/2001/XMLSchema#normalizedString</tt> **/
	public static final String NORMALIZEDSTRING = NAMESPACE + "normalizedString";

	/** <tt>http://www.w3.org/2001/XMLSchema#token</tt> **/
	public static final String TOKEN = NAMESPACE + "token";

	/** <tt>http://www.w3.org/2001/XMLSchema#language</tt> **/
	public static final String LANGUAGE = NAMESPACE + "language";

	/** <tt>http://www.w3.org/2001/XMLSchema#NMTOKEN</tt> **/
	public static final String NMTOKEN = NAMESPACE + "NMTOKEN";

	/** <tt>http://www.w3.org/2001/XMLSchema#NMTOKENS</tt> **/
	public static final String NMTOKENS = NAMESPACE + "NMTOKENS";

	/** <tt>http://www.w3.org/2001/XMLSchema#Name</tt> **/
	public static final String NAME = NAMESPACE + "Name";

	/** <tt>http://www.w3.org/2001/XMLSchema#NCName</tt> **/
	public static final String NCNAME = NAMESPACE + "NCName";

	/** <tt>http://www.w3.org/2001/XMLSchema#ID</tt> **/
	public static final String ID = NAMESPACE + "ID";

	/** <tt>http://www.w3.org/2001/XMLSchema#IDREF</tt> **/
	public static final String IDREF = NAMESPACE + "IDREF";

	/** <tt>http://www.w3.org/2001/XMLSchema#IDREFS</tt> **/
	public static final String IDREFS = NAMESPACE + "IDREFS";

	/** <tt>http://www.w3.org/2001/XMLSchema#ENTITY</tt> **/
	public static final String ENTITY = NAMESPACE + "ENTITY";

	/** <tt>http://www.w3.org/2001/XMLSchema#ENTITIES</tt> **/
	public static final String ENTITIES = NAMESPACE + "ENTITIES";

	/** <tt>http://www.w3.org/2001/XMLSchema#integer</tt> **/
	public static final String INTEGER = NAMESPACE + "integer";

	/** <tt>http://www.w3.org/2001/XMLSchema#long</tt> **/
	public static final String LONG = NAMESPACE + "long";

	/** <tt>http://www.w3.org/2001/XMLSchema#int</tt> **/
	public static final String INT = NAMESPACE + "int";

	/** <tt>http://www.w3.org/2001/XMLSchema#short</tt> **/
	public static final String SHORT = NAMESPACE + "short";

	/** <tt>http://www.w3.org/2001/XMLSchema#byte</tt> **/
	public static final String BYTE = NAMESPACE + "byte";

	/** <tt>http://www.w3.org/2001/XMLSchema#nonPositiveInteger</tt> **/
	public static final String NON_POSITIVE_INTEGER = NAMESPACE + "nonPositiveInteger";

	/** <tt>http://www.w3.org/2001/XMLSchema#negativeInteger</tt> **/
	public static final String NEGATIVE_INTEGER = NAMESPACE + "negativeInteger";

	/** <tt>http://www.w3.org/2001/XMLSchema#nonNegativeInteger</tt> **/
	public static final String NON_NEGATIVE_INTEGER = NAMESPACE + "nonNegativeInteger";

	/** <tt>http://www.w3.org/2001/XMLSchema#positiveInteger</tt> **/
	public static final String POSITIVE_INTEGER = NAMESPACE + "positiveInteger";

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedLong</tt> **/
	public static final String UNSIGNED_LONG = NAMESPACE + "unsignedLong";

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedInt</tt> **/
	public static final String UNSIGNED_INT = NAMESPACE + "unsignedInt";

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedShort</tt> **/
	public static final String UNSIGNED_SHORT = NAMESPACE + "unsignedShort";

	/** <tt>http://www.w3.org/2001/XMLSchema#unsignedByte</tt> **/
	public static final String UNSIGNED_BYTE = NAMESPACE + "unsignedByte";
}
