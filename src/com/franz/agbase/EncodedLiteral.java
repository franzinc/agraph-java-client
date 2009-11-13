package com.franz.agbase;

import com.franz.agbase.AllegroGraphException;



/**
 * This interface defines Literal instances that hold encoded
 * AllegroGraph values.
 * Encoded values allow queries that match a range of values;
 * they can also be used to store data in a more compact form.
 * <p>
 * Encoded literals are discussed in more detail in the AllegroGraph 
 * Introduction.
 * <p>
 *  Encoded literals are created by calling the methods
 *     {@link AllegroGraph#createEncodedLiteral(String, String)},
 *     {@link AllegroGraph#createEncodedLiteral(long, String)}, or
 *     {@link AllegroGraph#createEncodedLiteral(double, String)}. 
 *  <p>
 *  Encoded literals are also created when bulk loading from an N-Triples
 *  file if datatype or predicate mappings are defined in the triple store.
 *  See {@link AllegroGraph#setDataMapping(String[])} and friends.
 *  <p>
 *  <table border=1>
 *   <tr><th>Encoding name</th><th>What is encoded and how</th></tr>
 *   <tr><td><code>"triple-id"</code></td><td>the long integer returned by Triple.queryAGId() is 
 *      encoded as a value suitable for abbreviated reification. 
 *   </td></tr><tr>
 *   <td><code>"single-float"</code></td><td>a float or double value is encoded as a range-searchable
 *       literal. 
 *   </td></tr><tr>
 *   <td><code>"double-float"</code></td><td>a float or double value is encoded as a range-searchable
 *       literal. 
 *   </td></tr><tr>
 *   <td><code>"gyear"</code></td><td>a string value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "gYear". 
 *   </td></tr><tr>
 *   <td><code>"time"</code></td><td>a string value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "time". 
 *   </td></tr><tr>
 *   <td><code>"date"</code></td><td>a string value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "date". 
 *   </td></tr><tr>
 *   <td><code>"long"</code></td><td>an integer value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "long". 
 *   </td></tr><tr>
 *   <td><code>"short"</code></td><td>an integer value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "short". 
 *   </td></tr><tr>
 *   <td><code>"int"</code></td><td>an integer value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "int". 
 *   </td></tr><tr>
 *   <td><code>"byte"</code></td><td>an integer value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "byte". 
 *   </td></tr><tr>
 *   <td><code>"unsigned-long"</code></td><td>an integer value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "unsignedLong". 
 *   </td></tr><tr>
 *   <td><code>"unsigned-short"</code></td><td>an integer value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "unsignedShort". 
 *   </td></tr>
 *   <tr><td><code>"unsigned-int"</code></td><td>an integer value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "unsignedInt". 
 *   </td></tr><tr><td><code>"unsigned-byte"</code></td><td>an integer value is encoded as a range-searchable
 *       literal; the value should conform to the XML Schema type "unsignedByte". 
 *   </td></tr>
 *   <tr><td><code>"date-time"</code></td><td>a containing an ISO 8601 date-time value is encoded
 *       as a range-searchable
 *       literal; the value should conform to the XML Schema type "dateTime". 
 *       </td></tr>
 *  </table>
 */
public interface EncodedLiteral extends LiteralNode {

	/**
	 * Get the encoding used for this literal value.
	 * @return the string that names the encoding
	 */
	public String getEncoding();

	/**
	 * Get the object that represents the encoded value of the literal.
	 * @return a String, Long, or Double instance
	 */
	public Object getValue();

	public void add() throws AllegroGraphException;

	public String toString();

	/**
	 * This method overrides the method in the Literal class, and returns null.
	 */
	public String getLanguage();

}