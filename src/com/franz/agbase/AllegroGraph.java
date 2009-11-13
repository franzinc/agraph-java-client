 
//***** BEGIN LICENSE BLOCK *****
//Version: MPL 1.1
//
//The contents of this file are subject to the Mozilla Public License Version
//1.1 (the "License"); you may not use this file except in compliance with
//the License. You may obtain a copy of the License at
//http://www.mozilla.org/MPL/
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
//for the specific language governing rights and limitations under the
//License.
//
//The Original Code is the AllegroGraph Java Client interface.
//
//The Original Code was written by Franz Inc.
//Copyright (C) 2006 Franz Inc.  All Rights Reserved.
//
//***** END LICENSE BLOCK *****

package com.franz.agbase;


import java.util.ArrayList;

import com.franz.agbase.impl.AGFactory;
import com.franz.agbase.impl.LiteralNodeImpl;
import com.franz.agbase.impl.TripleImpl;
import com.franz.agbase.impl.UPIImpl;
import com.franz.agbase.impl.URINodeImpl;
import com.franz.agbase.impl.ValueSetIteratorImpl;
import com.franz.agbase.transport.AGConnector;
import com.franz.agbase.util.AGC;
import com.franz.agbase.util.AGInternals;
  

/**
 * Each instance of this class implements access to one AllegroGraph triple store.
 * <p>
 * There is no public constructor.  Instances of this class are
 * created by calls to methods in the {@link AllegroGraphConnection} class:
 *  <ul>
 *     <li>{@link AllegroGraphConnection#open(String, String)} to open an existing triple store.
 *     <li>{@link AllegroGraphConnection#create(String, String)} to create a new triple store when
 *        one did not exist before.
 *     <li>{@link AllegroGraphConnection#access(String, String)} to open or create a triple store.
 *     <li>{@link AllegroGraphConnection#renew(String, String)}) to open a new triple store or
 *         replace an existing one with a new empty triple store.
 *     <li>{@link AllegroGraphConnection#replace(String, String)} to replace an existing triple store
 *        with a new empty one.
 *     <li>{@link AllegroGraphConnection#federate(String, AllegroGraph[], boolean)} to create a federated store composed of several
 *        open triple stores.
 *  </ul>
 *  If attributes need to be set to control the behavior of the store, the application creates an 
 *  AllegroGraph instance, sets attributes with {@link #setAttribute(StoreAttribute, Object)},
 *  and then accesses the store with  {@link AllegroGraphConnection#access(AllegroGraph)} or the other 
 *  single-argument forms of the above methods.
 * <h4>Triple Component Specification</h4>
 * The components of triples in update and search methods are usually 
 * declared as Object.  In that case the argument may be:
 *   <ul>
 *     <li>A string in NTriples notation or a !-notation.
 *         <br><code>"&lt;http://franz.com/example#label&gt;"</code>
 *         <br><code>"\"an explicit literal\""</code>
 *         <br><code>"!franz:label"</code>
 *     <li>An instance of a class that implements the {@link ValueNode} interface.
 *         <br><code>ag.createLiteral("some label")</code>
 *     <li>A {@link UPI} instance.
 *     <li>In some operations, a null value or an empty string may be allowed.
 *   </ul>
 *   When a method argument may be an array, it must be an array of one
 *   of the above types.
 *  
 * <h4>The Context or Graph Component</h4>
 * AllegroGraph is a triple store implementations that allows triples to be
 * identified by an additional component that may be 
 * a literal, a URI or a blank node.
 * This component is sometimes called a context and sometimes a graph
 * specifier.  If this component is not specified, triples are added to the
 * null context, or the default graph. 
 * The terms <i>null context</i> and <i>default graph</i> are used interchangeably
 * in the method descriptions.
 * <p>
 * In search operations, if the context is not specified, then the search
 * examines only the null context.  If the context is specified,
 * then a null value is wild and matches any context, the empty string
 * denotes the null context, any other value must be a valid Value reference. 
 * 
 * 
 * <h4>AllegroGraph Jena and OpenRDF Sesame implementations</h4>
 * AllegroGraph is internally a quad store that accepts any AllegroGraph part value 
 * in any one of the 4 parts of a quad.  This definition is more liberal than the 
 * RDF definitions implemented in Jena and in Sesame 1.2 and 2. 
 * <p>
 * We provide a complete Sesame 2 Sail implementation in {@link com.knowledgereefsystems.agsail.AllegroSail}
 * and a Repository implementation in {@link com.franz.ag.repository.AGRepository}.
 * <p>
 * We provide a Jena adapter in class {@link com.franz.agjena.AllegroGraphModel} and friends.
 * 
 * @author mm@Franz.com
 */

public class AllegroGraph extends AGInternals
	{
	
	/**
	 * The name of this class identifies the version of the AllegroGraph
	 * Java implementation.
	 * This name is also visible in the list of members in a jar file
	 * when it is inspected with Emacs or WinZip.
	 */
	public static class V3_2BaseSep04 {	}

	/**
	 * Query the current AllegroGraph version.
	 * @return a version string.
	 */
	@SuppressWarnings("unchecked")
	public static String version () { 
		Class[] mems = AllegroGraph.class.getDeclaredClasses();
		String ag = AllegroGraph.class.getName();
		String s = "";
		ag = ag + "$V";
		for (int i = 0; i < mems.length; i++) {
			String sub = mems[i].getName();
			if ( sub.startsWith(ag) )
				s = sub;
		}
		return s; }
	
	/**
	 * Query the current AllegroGraph and component versions.
	 * @return An array of strings for the AllegroGraph version, 
	 *    and the server protocol level.
	 */
	public static String[] versions () {
		return new String[] { version(), 
							  "" + AGC.AGU_PROTOCOL_LEVEL,
		   					  };
	}
	
	/**
	 * Print AllegroGraph version information.
	 *
	 */
	public static void main ( String[] args ) {
		String[] v = versions();
		
		for (int i = 0; i < v.length; i++) {
			System.out.println(versionLabels[i] + v[i]);
		}
	}
	private static String[] versionLabels = new String[] {
			"AllegroGraph Version ",
			"      protocol level ",
			};
	
	
	
	/**
	 * Set the look-ahead value for subsequent Statement search operations.
	 * @param n an integer look-ahead value - must be positive.
	 *    A zero value specifies that the defaultLookAhead value in the 
	 *    TriplesIterator class should be used.
	 * <p>
	 * The look-ahead value determines how many Statements will be
	 * pre-loaded into a TriplesIterator instance when it is created or advanced.
	 * The pre-loaded Statements in the TriplesIterator are retrieved 
	 * immediately in Java without a round-trip to the 
	 * AllegroGraph server. 
	 * @see TriplesIterator#setLookAhead(int)
	 */
	public void setLookAhead (int n) { 
		if (n>-1) defaultLookAhead = n;
		else throw new IllegalArgumentException
					("setLookAhead cannot be negative " + n);
	}
	
	/**
	 * Query the look-ahead value.
	 * @return an integer
	 * @see #setLookAhead(int)
	 */
	public int getLookAhead () { return defaultLookAhead; }


		
	protected AllegroGraph(AllegroGraphConnection sv, String access, String name, String directory)
			throws AllegroGraphException {
		ags = sv; storeName = name;   storeDirectory = directory;
		if ( "".equals(access) ) 
			return;
		connect(access);
	}

	
	
	
	
	/**
	 * Enumerate the attributes that can be set with
	 * {@link AllegroGraph#setAttribute(com.franz.agbase.AllegroGraph.StoreAttribute, Object)}.
	 * @author mm
	 *
	 */
	public enum StoreAttribute {
		
		/**
		 * Long --  The initial size of the resource table in a new triple store.
		 */
		EXPECTED_UNIQUE_RESOURCES("expected-unique-resources"),
		
		/**
		 * String[] -- The desired indices for a new triple store.
		 */
		WITH_INDICES("with-indices"),
		
		/**
		 * Boolean -- When true, create some commonly used RDF and Owl resources in 
		 *           a new triple store.
		 */
		INCLUDE_STANDARD_PARTS("include-standard-parts"),
		
		/**
		 * Boolean --  When true, open an existing triple store in read-only mode.
		 * If an application wants to create a UPI map on a read-only store, it must specify an
		 * alternate (writable) directory for the map with
		 * {@link AllegroGraph#setUPIMapDirectory(String)}.
		 */
		READ_ONLY("read-only"),
		
		/**
		 * String --  A hostname where the server will find a remote triple store.
		 *        When an INDIRECT_HOST is specified, the name
		 *        and directory arguments are relative to the location of the remote host.
		 */
		INDIRECT_HOST("indirect-host"),
		
		/**
		 * Integer --  The port number where the INDIRECT_HOST is listening.
		 */
		INDIRECT_PORT("indirect-port")
		;
		
		private String value;
		private StoreAttribute ( String value ) { this.value = value; }
		private String value () { return value; }
		
	}
	
	/**
	 * Set attributes that affect the creation of new triple stores 
	 * or the accessing of existing ones.  
	 * These attributes must be set before one of the connection 
	 * methods is called since they only take effect during the creation 
	 * or opening of a triple store.
	 * @param name The name of the attribute.
	 * @param value The value of the attribute.
	 * 
	 */
	public synchronized void setAttribute ( StoreAttribute name, Object value ) {
		if ( tsx>-1 ) throw new IllegalStateException ("Allready connected");
		if ( tsx<-1 ) throw new IllegalStateException("Closed triple store");
		accOpts.setAttribute(name.value(), value);
	}
	
	/**
	 * @deprecated Use {@link #setAttribute(com.franz.agbase.AllegroGraph.StoreAttribute, Object)}
	 */
	public synchronized void setAttribute ( String name, Object value ) {
		if ( tsx>-1 ) throw new IllegalStateException ("Allready connected");
		if ( tsx<-1 ) throw new IllegalStateException("Closed triple store");
		accOpts.setAttribute(name, value);
	}


	/**
	 * Sync an AllegroGraph triple store.
	 */
	public void syncTripleStore() throws AllegroGraphException {
		verifyEnabled().syncTripleStore(this);
	}

	
	/**
	 * Close an AllegroGraph instance.
	 * Since many other users may be using this same triple store, closing
	 * the AllegroGraph instance does not actually close the triple store
	 * but  only synchronizes the store and invalidates this access instance
	 * unless this is the only instance accessing the store. 
	 */
	public synchronized boolean closeTripleStore () throws AllegroGraphException {
		return closeTripleStore(true);
	}
	
	/**
	 * Close an AllegroGraph instance and optionally close the 
	 * triple store if this is the only reference.
	 * @param doClose when true, close the triple-store if this is the
	 *     only reference.  
	 * @return true if the triple store is known to be closed.
	 * @throws AllegroGraphException
	 */
	public synchronized boolean closeTripleStore ( boolean doClose ) throws AllegroGraphException {
		if (tsx < 1)
			return false;
		boolean r = verifyEnabled().closeTripleStore(this, doClose);
		tsx = -2;
		ags.dropTS(this);
		return r;
	}

	/**
	 * Load a file of triple declarations.
	 * The triples are added in the null context of the triple store.
	 * 
	 * @param name A string that specifies the source of the triple declarations.
	 *      This can be a file pathname or a file URI or a web URI.
	 *      The data must be in the Ntriples format
	 *            expected by the AllegroGraph server.
	 *            All file pathnames are relative to the pathname defaults in the server.
	 *            The file environment of the client application is irrelevant.
	 *            <p>
	 *  If a file is very large, the operation may take a long time.
	 *  During that time, the calling thread, and the AllegroGraphConnection
	 *  instance are blocked.  On Windows, it is possible to see
	 *  progress messages in the server console window which is normally
	 *  hidden.  On Unix, the server progress messages may be seen if the 
	 *  server is running with a log file; in that case the log file
	 *  will grow as progress messages are added.
	 *  <p>
	 *  If other AllegroGraph instances of different stores are connected
	 *  through different AllegroGraphConnection instances, they may continue
	 *  to make queries or add triples.
	 */
	public long loadNTriples(String name) throws AllegroGraphException {
		return verifyEnabled().loadNTriples(this, name, null, null, null, null, null);
	}
	
	/**
	 * Load a file of triple declarations.
	 * The triples are added in the specified context.
	 * @param name a string pointing to the source file.
	 * @param context a triple part reference for the graph component of the new triples.
	 *      If the context is specified
	 *          as "source", a URI is constructed from the source file path.
	 * <p>
	 *  See the note about progress messages in the description of 
	 *  @see #loadNTriples(String)
	 */
	public long loadNTriples(String name, Object context) throws AllegroGraphException {
		return verifyEnabled().loadNTriples(this, name, ntripleContext(context), null, null, null, null);
	}
	

	/**
	 * Load several files of triple declarations.
	 * The triples are added in the null context of the triple store.
	 * 
	 * @param names an array of strings specifying several data sources.
	 *  <p>
	 *  See the notes about source specifiers and progress messages in the description of 
	 *  @see #loadNTriples(String)
	 */
	public long loadNTriples(String[] names) throws AllegroGraphException {
		return verifyEnabled().loadNTriples(this, names, null, null, null, null, null);
	}
	
	/**
	 * Load several files of triple declarations.
	 * The triples are added in the specified context.
	 * 
	 * @param names an array of strings specifying several data sources.
	 *  @param context a triple part reference for the graph component of the new triples.
	 *      If the context is specified
	 *          as "source", a URI is constructed from the source file path.
	 *  <p>
	 *  See the notes about source specifiers and progress messages in the description of 
	 *  @see #loadNTriples(String)
	 */
	public long loadNTriples(String[] names, Object context) throws AllegroGraphException {
		return verifyEnabled().loadNTriples(this, names, ntripleContext(context), null, null, null, null);
	}
	
	/**
	 * Load one or more files of triple declarations.
	 * The triples are added in the specified context.
	 * 
	 * @param names A string pointing to one file, or 
	 *            an array of such strings. The files must be in the Ntriples
	 *            format expected by the AllegroGraph server.
	 *  @param context A triple part reference for the graph component of the new triples.
	 *      If the context is specified
	 *          as "source", a URI is constructed from the source file path.
	 *  @param save This argument controls how encoded literals are stored in the store.
	 *      When true, if a triple triggers an encoding of a literal, two triples are added 
	 *      to the store, one with the encoded literal and one with the string literal.
	 *      When false, only the encoded triple is added.
	 *      When null, allow the default behavior (currently to add both).
	 *  @param ext A string that names the external-format used to translate octets into
	 *     characters.  The server default is determined by the locale of the server.
	 *     Valid values are 
	 *     <pre>
	 *     "1250" "1251" "1252" "1253" "1254" "1255" "1256" "1257" "1258" 
	 *     "874"  "932"  "936"  "949"  "950"  "big5" "emacs-mule" 
	 *     "euc"  "euc-jp"      "fat"  "fat-le"      "gb2312"      "gbk" 
	 *     "iso-2022-jp"        "iso8859-1"          "iso8859-14"  "iso8859-15"  
	 *     "iso8859-2"          "iso8859-3"          "iso8859-4"   "iso8859-5"  
	 *     "iso8859-6"          "iso8859-7"          "iso8859-8"   "iso8859-9"  
	 *     "jis"                "jvm-utf8"           "koi8-r"      "latin-14"  
	 *     "latin-15"           "latin-2"            "latin-3"     "latin-4"  
	 *     "latin-5"            "latin-6"            "latin-7"     "latin-8"  
	 *     "latin-9"            "latin1"             "latin14"     "latin15"  
	 *     "latin2"             "latin3"             "latin4"      "latin5"  
	 *     "latin6"             "latin7"             "latin8"      "latin9"  
	 *     "shiftjis"           "ujis"               "unicode"     "utf8"
	 *      </pre>
	 *      The numeric strings refer to Windows Code Page encodings.
	 *  @param place When non-null, must be an array of at least one Object.  The array element 
	 *     zero is updated with the UPI of the graph component of all the triples added
	 *     by the bulk load.
	 *  <p>
	 *  See the note about progress messages in the description of 
	 *  @see #loadNTriples(String)
	 */
	public long loadNTriples(Object names, Object context, Boolean save, String ext, Object[] place) throws AllegroGraphException {
		return verifyEnabled().loadNTriples(this, names, ntripleContext(context), null, place, save, ext);
	}
	
	/**
	 * Load one compressed file of triple declarations.
	 * The triples are added in the specified context.
	 * @param name
	 * @param context
	 * @param save
	 * @param ext
	 * @param place
	 * @param unzip When true, the input file is expected to be in gzip compression normally
	 *          with a .gz extension.
	 * @return
	 * @throws AllegroGraphException
	 */
	public long loadNTriples(String name, Object context, Boolean save, String ext, Object[] place, boolean unzip) throws AllegroGraphException {
		return verifyEnabled().loadNTriples(this, name, ntripleContext(context), null, place, save, ext, unzip);
	}
	
	
	/**
	 * Parse a string and add triples to the null context of triple store. 
	 * The string must be in the Ntriples format expected by the AllegroGraph server.
	 * @param from a string in Ntriples format
	 * @throws AllegroGraphException
	 */
	public long parseNTriples ( String from ) throws AllegroGraphException {
		return verifyEnabled().loadNTriples(this, null, null, from, null, null, null);
	}
		
	/**
	 * Parse a string and add triples to the specified context of the triple store. 
	 * The string must be in the Ntriples format expected by the AllegroGraph server.
	 * 
	 * @param from a string in Ntriples format
	 * @param context a String or instance that identifies a triple part to be used
	 *          as the context marker for all the triples.  If the context is specified
	 *          as "source", a URI is constructed from the source file path.
	 * @throws AllegroGraphException
	 */
	public long parseNTriples ( String from, Object context ) throws AllegroGraphException {
		return verifyEnabled().loadNTriples(this, null,
				ntripleContext(context), from, null, null, null);	
	}
	
	/**
	 * Parse a string and add triples to the specified context of the triple store. 
	 * The string must be in the Ntriples format expected by the AllegroGraph server.
	 * 
	 * @param from from a string in Ntriples format
	 * @param context a String or instance that identifies a triple part to be used
	 *          as the context marker for all the triples.  If the context is specified
	 *          as "source", a URI is constructed from the source file path.
	 * @param save This argument controls how encoded literals are stored in the store.
	 *      When true, if a triple triggers an encoding of a literal, two triples are added 
	 *      to the store, one with the encoded literal and one with the string literal.
	 *      When false, only the encoded triple is added.
	 *      When null, allow the default behavior (currently to add both).
	 * @param place null or an Object array of length at least one.
	 *    The array (if present) element zero is updated with the UPI of the actual
	 *    context marker used.
	 * @return the number of triples added.
	 * @throws AllegroGraphException
	 */
	public long parseNTriples ( String from, Object context, boolean save, Object[] place) throws AllegroGraphException {
		return verifyEnabled().loadNTriples(this, null,
				ntripleContext(context), from, place, save, null);	
	}
	
	

	/**
	 * Parse a file of triple definitions in RDF/XML notation and add the triples to the
	 * null context of the triple store.
	 * 
	 * @param filePath A string that specifies the source of the triple declarations.
	 *      This can be a file pathname or a file URI or a web URI.
	 *      The data must be in the RDF/XML format
	 *            expected by the AllegroGraph server.
	 *            All file pathnames are relative to the pathname defaults in the server.
	 *            The file environment of the client application is irrelevant.
	 * @return the number of triples added to the store           
	 *  <p>
	 *  See the note about progress messages in the description
	 *  of {@see #loadNTriples(String)}.
	 *  
	 */
	public long loadRDFXML(String filePath) throws AllegroGraphException {
		return verifyEnabled().loadRDF(this, filePath, null, null, null);
	}
	
	/**
	 * Parse a file of triple definitions in RDF/XML notation and add the triples to the
	 * specified context of the triple store.
	 * 
	 * @param filePath A string that specifies the source of the triple declarations.
	 *      This can be a file pathname or a file URI or a web URI.
	 *      The data must be in the RDF/XML format
	 *            expected by the AllegroGraph server.
	 *            All file pathnames are relative to the pathname defaults in the server.
	 *            The file environment of the client application is irrelevant.
	 * @param context a String or instance that identifies a triple part to be used
	 *          as the context marker for all the triples.  If the context is specified
	 *          as "source", a URI is constructed from the source file path.
	 * @return the number of triples added to the store
	 *  <p>
	 *  See the note about progress messages in the description
	 *  of {@see #loadNTriples(String)}.
	 *  
	 */
	public long loadRDFXML(String filePath, Object context) throws AllegroGraphException {
		return verifyEnabled().loadRDF(this, filePath, ntripleContext(context), null, null);
	}
	
	
	/**
	 * Parse a file of triple definitions in RDF/XML notation and add the triples to the
	 * specified context of the triple store.
	 * 
	 * @param filePath A string that specifies the source of the triple declarations.
	 *      This can be a file pathname or a file URI or a web URI.
	 *      The data must be in the RDF/XML format
	 *            expected by the AllegroGraph server.
	 *            All file pathnames are relative to the pathname defaults in the server.
	 *            The file environment of the client application is irrelevant.
	 * @param context a String or instance that identifies a triple part to be used
	 *          as the context marker for all the triples.  If the context is specified
	 *          as "source", a URI is constructed from the source file path.
	 * @param baseURI a string that specifies a base URI for the file.
	 * @return the number of triples added to the store
	 *  <p>
	 *  See the note about progress messages in the description
	 *  of {@see #loadNTriples(String)}.
	 *  
	 */
	public long loadRDFXML(String filePath, Object context, String baseURI) throws AllegroGraphException {
		return verifyEnabled().loadRDF(this, filePath, ntripleContext(context), baseURI, false, null, null);
	}
	
	/**
	 * Parse a file of triple definitions in RDF/XML notation and add the triples to the
	 * specified context of the triple store.
	 * @param filePath A string or array of strings.
	 *     Each string can be a pathname in the server's file system, or a file or web URL
	 *     visible to the server.
	 * @param context a String or instance that identifies a triple part to be used
	 *          as the context marker for all the triples.  If the context is specified
	 *          as "source", a URI is constructed from the source file path.
	 * @param baseURI a string that specifies a base URI for the file.
	 * @param useRapper if not null or false, then the RDF/XML file will be
     *     piped through the open source tool rapper.  
     *     A string can specify the path to the rapper command.
     *     All the source arguments must specify files visible to the server.
	 * @param save This argument controls how encoded literals are stored in the store.
	 *      When true, if a triple triggers an encoding of a literal, two triples are added 
	 *      to the store, one with the encoded literal and one with the string literal.
	 *      When false, only the encoded triple is added.
	 *      When null, allow the default behavior (currently to add both).
	 * @return
	 * @throws AllegroGraphException
	 */
	public long loadRDFXML(Object filePath, Object context, String baseURI, Object useRapper, Boolean save) throws AllegroGraphException {
		return verifyEnabled().loadRDF(this, filePath, ntripleContext(context), baseURI, useRapper, null, save);
	}
	
	
	/**
	 * Parse files of triple definitions in RDF/XML notation and add the triples to the
	 * null context of the triple store.
	 * @param filePaths an array of file specifier strings.  The files are loaded in the
	 *    order specified.
	 * @return the number of triples added to the store
	 * @throws AllegroGraphException
	 */
	public long loadRDFXML(String[] filePaths) throws AllegroGraphException {
		return verifyEnabled().loadRDF(this, filePaths, null, null, null);
	}
	
	
	
	/**
	 * Parse a string containing RDF/XML definitions of triples and add the triples to the
	 * specified context of the triple store.
	 * @param data  the string
	 * @param context a String or instance that identifies a triple part to be used
	 *          as the context marker for all the triples.  If the context is specified
	 *          as "source", a URI is constructed from the source file path.
	 * @param baseURI a string that specifies a base URI for the file.
	 * @return
	 * @throws AllegroGraphException
	 */
	public long parseRDFXML(String data, Object context, String baseURI) throws AllegroGraphException {
		return verifyEnabled().loadRDF(this, null, ntripleContext(context), baseURI, null, data, null);
	}
	
	/**
	 * Parse a string containing RDF/XML definitions of triples and add the triples to the
	 * specified context of the triple store.
	 * @param data the string
	 * @param context a String or instance that identifies a triple part to be used
	 *          as the context marker for all the triples.  If the context is specified
	 *          as "source", a URI is constructed from the source file path.
	 * @param baseURI a string that specifies a base URI for the file.
	 * @param save This argument controls how encoded literals are stored in the store.
	 *      When true, if a triple triggers an encoding of a literal, two triples are added 
	 *      to the store, one with the encoded literal and one with the string literal.
	 *      When false, only the encoded triple is added.
	 *      When null, allow the default behavior (currently to add both).
	 * @return
	 * @throws AllegroGraphException
	 */
	public long parseRDFXML(String data, Object context, String baseURI, Boolean save) throws AllegroGraphException {
		return verifyEnabled().loadRDF(this, null, ntripleContext(context), baseURI, null, data, save);
	}
	
	

	/**
	 * Query the number of triples in the triple store.
	 * 
	 * @return the number of triples in the triple store.
	 */
	public long numberOfTriples() throws AllegroGraphException {
		return verifyEnabled().numberOfTriples(this);
	}
	



	/**
	 * Index new triples.
	 * 
	 * @throws AllegroGraphException if an error occurs during indexing.
	 * <p>
	 * If the current indexing chunk size is too small, it can cause
	 * too many files to be opened during indexing; in that case the 
	 * AllegroGraphException exception with the string
	 * 
	 *    "too-many-index-chunks-error:  will create nnn files"
	 *    
	 * is thrown; nnn is the number of chunks.  At this point,
	 * the triple store has not been indexed, but the server is in
	 * a stable state.  The Application can adjust the chunk size
	 * with AllegroGraphConnection.setChunkSize() and try the indexing call again.
	 * <p>
	 * If the current indexing chunk size is too large, the server may run out
	 * of memory during indexing; in that case, the thrown exception will
	 * depend on where and when the out-of-memory condition occurred.
	 * The triple store may be in a partially indexed state.
	 */
	public void indexNewTriples() throws AllegroGraphException {
		try {
			verifyEnabled().indexTriples(this, true);
		} catch (IllegalArgumentException e) {
			throwIndexError(e);
			throw e;
		}
	}
	
	/**
	 * Index new triples immediately or in the background.
	 * @param wait If true, then return only after indexing is completed.
	 *    If false, schedule an indexing task to run in the background
	 *    and return immediately.
	 * @throws AllegroGraphException
	 */
	public void indexNewTriples( boolean wait ) throws AllegroGraphException {
		try {
			verifyEnabled().indexTriples(this, wait);
		} catch (IllegalArgumentException e) {
			throwIndexError(e);
			throw e;
		}
	}
	
	public void mergeNewTriples ( boolean wait ) throws AllegroGraphException {
		verifyEnabled().indexStore(this, wait, null);
	}
	
	void throwIndexError( Throwable e ) throws AllegroGraphException {
		String m = e.toString();
		if (-1==m.indexOf("too-many-index-chunks-error ")) return;
		String w = " will create ";
		int ch = m.indexOf(w);
		if ( ch==-1 ) return;
		int ch2 = m.indexOf(" ", ch+w.length());
		if ( ch2==-1 ) return;
		throw new AllegroGraphException
		  ("too-many-index-chunks-error: " + m.substring(ch, ch2) + " files");
	}


	/**
	 * Index all the triples in the triple store.
	 * @throws AllegroGraphException if an error occurs during indexing.
	 * <p>
	 * See the discussion of chunk size at indexNewTriples().
	 */
	public void indexAllTriples() throws AllegroGraphException {
		try {
			verifyEnabled().indexAll(this, true);
		} catch (IllegalArgumentException e) {
			throwIndexError(e);
			throw e;
		}
	}
	
	/**
	 * Index all triples immediately or in the background.
	 * @param wait If true, then return only after indexing is completed.
	 *    If false, schedule an indexing task to run in the background
	 *    and return immediately.
	 * @throws AllegroGraphException
	 */
	public void indexAllTriples( boolean wait ) throws AllegroGraphException {
		try {
			verifyEnabled().indexAll(this, wait);
		} catch (IllegalArgumentException e) {
			throwIndexError(e);
			throw e;
		}
	}

	

	private String stringElt(String[] a, int i) {
		if ( a==null ) return null;
		if (i < a.length)
			return a[i];
		return a[(a.length) - 1];
	}

	
	/**
	 * Convert a UPI instance into a ValueObject instance.
	 * @param id A UPI instance that denotes a Resource or Literal object
	 * @return The Resource or Literal object instance.
	 * @throws IllegalStateException if the object could not be created.
	 */
	public ValueObject newValue(UPI id) {
		ValueObject v = null;
		if ( !UPIImpl.canReference(id) )
			throw
			new IllegalStateException
					("AllegroGraph Id cannot be registered:" + id);
		if ( UPIImpl.isNullContext(id) )
			return this.getDefaultGraph(id);
		try {
			Object[] r = verifyEnabled().getParts(this, id);
			v = newValue(id,
					((Integer)r[0]).intValue(), 
					(String)r[1], 
					(String)r[2]);
		} catch (AllegroGraphException e) { failCreate("ValueObject", e);}
		if ( v == null ) failCreate("ValueObject", null);
		return v;
	}
	
	void failCreate( String what, Throwable e ) {
		throw new IllegalStateException("Cannot create " + what + " -- " + e);
	}

	
	/**
    * Retrieve the string and type information associated with a triple store part.
    *
    * @param id A UPI instance that identifies a triple store part.
    * @return An array of three strings:
    *     <ul><li>A string that identifies the type of the object:
    *             <ul><li>"anon" - A blank node
    *                 <li>"node" - A resource node
    *                 <li>"literal" - A literal node
    *                 <li>"literal/lang" - A literal node with a language tag
    *                 <li>"typed-literal" - A literal node with a type tag
    *                 <li>"unknown" - The id is not in the triple store
    *                 </ul>
    *          <li>A string that contains the URI or literal data 
    *                  associated with the object
    *          <li>A string that contains additional data, ie a language or
    *                    a type tag
    *         </ul>
    * If the object does not have the component, then the array element is null.
    *  
    */
	public String[] getParts(UPI id) throws AllegroGraphException {
		Object[] r = verifyEnabled().getParts(this, id);
		return new String[]
						  { typeToString(((Integer)r[0]).intValue()),
							(String)r[1], (String)r[2] };
	}
	
	/**
	 * Return an array of 4 UPI instances for subject, predicate, object, and context.
	 * @throws AllegroGraphException 
	 */
	public UPIImpl[] getTripleParts ( long id ) throws AllegroGraphException {
		UPIImpl[] v = verifyEnabled().getTripleParts(this, id);
		if ( v==null ) throw new AllegroGraphException("Id is not a triple: " + id);
		return v;
	}
	
	/**
	 * Like getParts(UPI) but operates on an array of UPI instances.
	 * 
	 * @param ids an array of UPI instances
	 * @param types a String array of the same length as ids.
	 * @param vals a String array of the same length as ids.
	 * @param mods a String array of the same length as ids.
	 * <p>
	 * The arrays types, vals, mods are modified to hold the
	 * string values corresponding to the id number in the ids array.
	 * @throws AllegroGraphException
	 */
	public void getParts(UPI[] ids, String[] types, String[] vals, String[] mods)
			throws AllegroGraphException {
		int[] tnums = new int[ids.length];
		verifyEnabled().getParts(this, ids, tnums, vals, mods);
		for (int i=0; i<ids.length; i++)
			types[i] = typeToString(tnums[i]);
	}
	
	

	
/**
 * Get the current value of the select limit parameter.
 * @return an integer value. 
 * <p>
 *  This number determines the number of values transmitted
 *  in one set of results from the AllegroGraph server to the 
 *  Java client.   If the number of expected results is very large,
 *  but the application is interested in only a few, then a small number
 *  may improve performance.  If the number is very large, there may
 *  be a long delay as the large array of results is prepared
 *  and transmitted to the client.
 *    The total number of actual values
 * returned depends on the number of free variables in the query. 
 * <p>
 * The built-in initial value is 1000.
 */
	public int getSelectLimit() { return selectLimit; }
	
	/**
	 * Set the value of the select limit parameter.
	 * @param v A positive integer.
	 * @see #getSelectLimit()
	 */
	public void setSelectLimit( int v ) {
		if ( v<0 ) throw new IllegalArgumentException("setSelectLimit argument must be non-negative.");
		selectLimit = v;
	}
	

	 
	
	
	


	/**
	 * Create a BlankNode instance.
	 * @return the BlankNode
	 * @throws IllegalStateException if the creation fails.
	 * <p>
	 * It does not make sense to create a BlankNode that is not registered
	 * in the triple store because two instances of apparently identical
	 * unregistered BlankNodes would map to distinct BlankNodes in the
	 * triple store.
	 */
	public BlankNode createBNode() {
		return createBNode(null);
	}
	
	private ArrayList<UPI> blanks = new ArrayList<UPI>();
	
	private UPI fetchBlankUPI () {
		if ( blanks.isEmpty() )
			try { 
				UPI[] supply = createBNodeIds(100);
				for (UPI upi : supply) { blanks.add(upi); }
			} catch (AllegroGraphException e) {
				throw new IllegalStateException(e);
			}
		return blanks.remove(0);
	}
	
	

	/**
	 * Create a BlankNode instance.
	 * @return the BlankNode
	 * @throws IllegalStateException if the creation fails.
	 * <p>
	 * See note on createBNode().
	 */
	public BlankNode createBNode(java.lang.String nodeId) {
		BlankNode b = null; Exception ee = null;
		if ( nodeId==null ) nodeId = "";  // server does not like a null here.
		try { b = AGFactory.makeBlankNode(this, fetchBlankUPI(), nodeId);
		} catch (Exception e) {	ee = e; }
		if ( b==null ) failCreate("BlankNode", ee);
		return b;
	}
	

	
	/**
	 * Allocate a set of BlankNodes.
	 * @param n the number of nodes to allocate.
	 * @return an array of BlankNode instances.
	 * @throws AllegroGraphException if there is a problem during creation.
	 * <p>
	 * See note on createBNode().
	 */
	public BlankNode[] createBNodes(int n) throws AllegroGraphException{
		UPIImpl[] v = verifyEnabled().newBlankNodes(this, n);
		BlankNode[] r = new BlankNode[n];
		for (int i=0; i<n; i++) r[i] = AGFactory.makeBlankNode(this, v[i], null);
		return r;
	}
	
	

	/**
	 * Create an AllegroGraph EncodedLiteral instance from a Java long value.
	 * @param v The Java long value.
	 * @param encoding A string that specifies the AllegroGraph encoding desired.
	 *    The available encodings are described in the AllegroGraph Introduction document.
	 * @return an EncodedLiteral instance that can be used  in query ir update operations.
	 */
	public EncodedLiteral createEncodedLiteral( long v, String encoding ) {
		return AGFactory.makeEncodedLiteral(this, v, encoding);
	}
	
	/**
	 * Create an AllegroGraph EncodedLiteral instance from a Java double value.
	 * @param v The Java double value.
	 * @param encoding A string that specifies the AllegroGraph encoding desired.
	 *    The available encodings are described in the AllegroGraph Introduction document.
	 * @return an EncodedLiteral instance that can be used  in query ir update operations.
	 */
	public EncodedLiteral createEncodedLiteral( double v, String encoding ) {
		return AGFactory.makeEncodedLiteral(this, v, encoding);
	}
	
	/**
	 * Create an AllegroGraph EncodedLiteral instance from a Java string value.
	 * @param v The Java string value.
	 * @param encoding A string that specifies the AllegroGraph encoding desired.
	 *    The available encodings are described in the AllegroGraph Introduction document.
	 * @return an EncodedLiteral instance that can be used  in query ir update operations.
	 */
	public EncodedLiteral createEncodedLiteral( String v, String encoding ) {
		return AGFactory.makeEncodedLiteral(this, v, encoding);
	}

	
	/**
	 * Create a LiteralNode instance without updating the triple store.
	 * @return a LiteralNode instance.
	 * <p>
	 * The literal instance will have a null UPI.
	 * 
	 */
	public LiteralNode createLiteral(java.lang.String value) {
		return AGFactory.makeLiteral(this, null, value, null, null, LiteralNodeImpl.LANG_NONE, null);
	}

	/**
	 * Create a literal instance 
	 * and add the LiteralNode to the triple store registry.
	 * 
	 * @throws IllegalStateException if the creation fails.
	 */
	public LiteralNode addLiteral(java.lang.String value) {
		LiteralNode b = null; AllegroGraphException ee = null;
		UPI tupi = null;
		try { UPIImpl id = verifyEnabled().newLiteral(this, value, tupi, null);
			b = AGFactory.makeLiteral(this, id, value, null, null, LiteralNodeImpl.LANG_NONE, null);
		} catch (AllegroGraphException e) {	ee = e; }
		if ( b==null ) failCreate("Literal", ee);
		return b;
	}


	/**
	 * Create a LiteralNode instance with a language tag
	 * but do not modify the triple store.
	 * @return a LiteralNode instance.
	 * <p>
	 * The literal instance will have a null UPI.
	 */
	public LiteralNode createLiteral(java.lang.String value,
			java.lang.String language) {
		return AGFactory.makeLiteral(this, null, value, null, null, LiteralNodeImpl.LANG_KNOWN, language);
	}
	
	/**
	 * Create a literal instance with a language tag 
	 * and add the LiteralNode to the triple store registry.
	 * 
	 * @return a LiteralNode instance.
	 * @throws IllegalStateException if the creation fails.
	 */
	public LiteralNode addLiteral(java.lang.String value,
			java.lang.String language) {
		LiteralNode b = null; AllegroGraphException ee = null;
		String type = null;
		try { UPIImpl id = verifyEnabled().newLiteral(this, value, type, language);
		b = AGFactory.makeLiteral(this, id, value, null, null, LiteralNodeImpl.LANG_KNOWN, language);
		} catch (AllegroGraphException e) {	ee = e; }
		if ( b==null ) failCreate("languageLiteral", ee);
		return b;
	}
	
	
	/**
	 * Create a typed Literal instance from a string
	 * but do not modify the triple store.
	 * 
	 * @param text A string URI.
	 * @param type A string URI that denotes the type.
	 * @return a Literal instance.
	 */
	public LiteralNode createTypedLiteral(String text, String type) {
		return AGFactory.makeLiteral(this, null, text, null, type, LiteralNodeImpl.LANG_NONE, null);
	}
	
	/**
	 * Create a typed Literal instance from a string
	 * and add the Literal to the triple store registry.
	 * 
	 * @param text A string URI.
	 * @param type A string URI that denotes the type.
	 * @return a Literal instance.
	 * @throws IllegalStateException if the creation fails.
	 */
	public LiteralNode addTypedLiteral(String text, String type) {
		LiteralNode b = null; AllegroGraphException ee = null;
		try {
			UPIImpl id = verifyEnabled().newLiteral(this, text, type, null);
			b = AGFactory.makeLiteral(this, id, text, null, type, LiteralNodeImpl.LANG_NONE, null);
		} catch (AllegroGraphException e) {	ee = e; }
		if ( b==null ) failCreate("typedLiteral", ee);
		return b;
	}
	
	/**
	 * Create a typed Literal instance from a string
	 * but do not modify the triple store.
	 * 
	 * @param text A string.
	 * @param type A UPI instance that identifies a ValueNode
	 * @return a Literal instance.
	 */
	public LiteralNode createTypedLiteral(String text, UPI type) {
		return AGFactory.makeLiteral(this, null, text, type, null, LiteralNodeImpl.LANG_NONE, null);
	}
	
	/**
	 * Create a typed Literal instance from a string and add the literal
	 * to the triple store registry.
	 * 
	 * @param text A string.
	 * @param type A UPI instance that identifies the datatype resource.
	 * @return a Literal instance.
	 * @throws IllegalStateException if the creation fails.
	 */
	public LiteralNode addTypedLiteral(String text, UPI type) {
		LiteralNode b = null; AllegroGraphException ee = null;
		try {
			UPIImpl id = verifyEnabled().newLiteral(this, text, type, null);
			b = AGFactory.makeLiteral(this, id, text, type, null, LiteralNodeImpl.LANG_NONE, null);
		} catch (AllegroGraphException e) {	ee = e; }
		if ( b==null ) failCreate("typedLiteral", ee);
		return b;
	}

	/**
	 * Create a typed Literal instance from a string and add the literal
	 * to the triple store registry.
	 * 
	 * @param text A string.
	 * @param type A URI instance that identifies the datatype resource.
	 * @return a Literal instance.
	 * @throws IllegalStateException if the creation fails.
	 */
	public LiteralNode addTypedLiteral ( String text, URINode type ) {
		if ( type instanceof URINodeImpl )
		{
			UPI id = ((URINodeImpl) type).queryAGId();
			if ( id!=null ) return addTypedLiteral(text, id);
		}	
		return addTypedLiteral(text, type.toString());
	}
	
	

	/**
	 * Create a LiteralNode instance with a datatype tag
	 * but do not modify the triple store.
	 * @return a LiteralNode instance.
	 * <p>
	 * The literal instance will have a null UPI.
	 */
	public LiteralNode createLiteral(java.lang.String value,
			URINode datatype) {
		if ( datatype instanceof URINodeImpl ) {
			URINodeImpl nd = (URINodeImpl)datatype;
			UPI nid = nd.queryAGId();
			if ( UPIImpl.canReference(nid) )
				return AGFactory.makeLiteral(this, null, value, nid, nd.queryURI(),
						LiteralNodeImpl.LANG_NONE, null);
		}
		return AGFactory.makeLiteral(this, null, value, null, datatype.toString(),
				LiteralNodeImpl.LANG_NONE, null);
	}
	
	/**
	 * Create a literal instance with a datatype tag 
	 * and add the LiteralNode to the triple store registry.
	 * 
	 * @return a LiteralNode instance.
	 * @throws IllegalStateException if the creation fails.
	 */
	public LiteralNode addLiteral(java.lang.String value,
			URINode datatype) throws AllegroGraphException {
		if ( datatype instanceof URINodeImpl ) {
			URINodeImpl nd = (URINodeImpl)datatype;
			UPI nid = nd.queryAGId();
			nid = nd.getAGId();
			if ( UPIImpl.canReference(nid) ) 
				{
				LiteralNode v = addTypedLiteral(value, nid);
				((LiteralNodeImpl)v).type = datatype.toString();
				return v;
				}
		}
		return addTypedLiteral(value, datatype.toString());
	}
	
	
	
	/**
	 * Create a set of Literal instances and add the
	 * literals to the triple store registry.
	 * 
	 * @param values An array of string literal values (of length n).
	 * @param datatypes null, or an array of datatype URIs.  If null, none of 
	 *    the new Literal instances will have a datatype qualifier.  If an array,
	 *    the first n elements will be used as datatype labels for the 
	 *    corresponding Literal.  A null element specifies a Literal without
	 *    a datatype label.  If the length of the array is less than n, the
	 *    last element is repeated as often as necessary.  If a non-null value
	 *    is specified in an array element, the corresponding element in the 
	 *    languages array is ignored.
	 * @param languages null, or an array of language labels.  If null, none of 
	 *    the new Literal instances will have a language qualifier.  If an array,
	 *    the first n elements will be used as language labels for the 
	 *    corresponding Literal.  A null element specifies a Literal without
	 *    a language label.  If the length of the array is less than n, the
	 *    last element is repeated as often as necessary.
	 * @return An array of Literal instances of the same size as the first
	 *     argument array. 
	 */
	public LiteralNode[] addLiterals(String[] values, 
									String[] datatypes,
									String[] languages)
	throws AllegroGraphException 
	{
		LiteralNode[] v;
		UPIImpl[] ids = verifyEnabled().newLiteral(this, values, datatypes, languages);	
		v = new LiteralNode[values.length];
		for (int i=0; i<values.length; i++) {
			String ts = stringElt(datatypes, i);
			String ls = stringElt(languages, i);
			v[i] = AGFactory.makeLiteral(this, ids[i], values[i], null, ts, LiteralNodeImpl.LANG_KNOWN, ls);
		}
		return v;
	}


	/**
	 * Create a resource node with a URI label
	 * but do not modify the triple store.
	 * @param uri
	 * @return a URINode instance
	 */
	public URINode createURI(java.lang.String uri) {
		return AGFactory.makeNode(this, null, uri);
	}
		
	/**
	 * Create a resource node with a URI label
	 * and add the resource to the triple store registry.
	 * @param uri
	 * @return a Node instance.
	 * @throws AllegroGraphException if the creation fails.
	 */
	public URINode addURI (java.lang.String uri)
		throws AllegroGraphException {
		return AGFactory.makeNode(this, verifyEnabled().newResource(this, uri), uri);
	}
	
	/**
	 * Create a set of resource nodes in the triple store registry.
	 * 
	 * @param uri An array of URI strings.
	 * @return an array of UPIs for the new nodes.
	 * @throws AllegroGraphException
	 */
	public UPI[] addURIIds(String[] uri) throws AllegroGraphException{
		return verifyEnabled().newResources(this, uri);
	}
	
	/**
	 * Create a set of resource Node instances
	 * and add the resources to the triple store registry.
	 * 
	 * @param uri An array of URI strings.
	 * @return an array of Node instances.
	 * @throws AllegroGraphException
	 */
	public URINode[] addURIs (String[] uri) throws AllegroGraphException{
		URINode[] v = new URINode[uri.length];
		UPIImpl[] ids = verifyEnabled().newResources(this, uri);
		for (int i=0; i<uri.length; i++) v[i] = AGFactory.makeNode(this, ids[i], uri[i]);
		return v;
	}


	/**
	 * Create a resource node with a URI label
	 * but do not modify the triple store.
	 * 
	 * @return a URINode instance 
	 * @throws IllegalStateException if the creation fails.
	 */
	public URINode createURI(java.lang.String namespace,
			java.lang.String localName) {
			return createURI(namespace + localName);
	}


	/**
	 * Retrieve null context statements from the triple store.
	 * 
	 * @return a TriplesIterator instance
	 * @throws AllegroGraphException if there is
	 *    a problem.
	 * 
	 */
	public TriplesIterator getStatements(Object subject, Object predicate,
			Object object) throws AllegroGraphException {
		return verifyEnabled().getTriples(this,
				validRefOrWild(subject), 
				validRefOrWild(predicate),
				validRefOrWild(object),
				UPIImpl.nullUPI(),
				defaultLookAhead);
	}
	
	/**
	 * Retrieve null context statements from the triple store.
	 * 
	 * @param includeInferred if true, include inferred triples in the result.
	 *    If false, return only triples that are actually present in the 
	 *    triple store. 
	 * @return a TriplesIterator instance
	 * @throws AllegroGraphException if there is
	 *    a problem.
	 * 
	 */
	public TriplesIterator getStatements(boolean includeInferred, Object subject, Object predicate,
			Object object) throws AllegroGraphException {
		return verifyEnabled().getInfTriples(this,
				validRefOrWild(subject), 
				validRefOrWild(predicate),
				validRefOrWild(object),
				UPIImpl.nullUPI(),
				defaultLookAhead, includeInferred);
	}
	
	/**
	 * Retrieve statements from the triple store .
	 * 
	 * @return a TriplesIterator instance
	 * @throws AllegroGraphException if there is
	 *    a problem.
	 * 
	 */
	public TriplesIterator getStatements(Object subject, Object predicate,
			Object object, Object context) throws AllegroGraphException {
		return verifyEnabled().getTriples(this,
				validRefOrWild(subject), 
				validRefOrWild(predicate),
				validRefOrWild(object),
				anyContextRef(context, 3),
				defaultLookAhead);
	}
	
	/**
	 * Retrieve statements from the triple store.
	 * 
	 * @param includeInferred if true, include inferred triples in the result.
	 *    If false, return only triples that are actually present in the 
	 *    triple store. 
	 * @param context Specifies the context or graph of the statements.  Null is wild
	 *    and matches any context; the empty string denotes the Null Context.
	 * @return a TriplesIterator instance
	 * @throws AllegroGraphException if there is
	 *    a problem.
	 * 
	 */
	public TriplesIterator getStatements(boolean includeInferred, Object subject, Object predicate,
			Object object, Object context) throws AllegroGraphException {
		return verifyEnabled().getInfTriples(this,
				validRefOrWild(subject), 
				validRefOrWild(predicate),
				validRefOrWild(object),
				anyContextRef(context, 3),
				defaultLookAhead, includeInferred);
	}
	
	/**
	 * Retrieve statements from the triple store and specify a range of
	 *  values for the object and/or context components of the triples.
	 *  
	 * @param subject
	 * @param predicate 
	 * @param object
	 * @param obEnd a non-null value specifies the end of a range of values
	 *   for the object component of the triples.  A null value specifies
	 *   that only the object argument will be used to match triples.
	 * @param context a null value is a wild card, the empty string specifies 
	 *   the null context, any other value must specify a resource that will 
	 *   match the context component.
	 * @param contextEnd a non-null value specifies the end of a range of values
	 *   for the context component of the triples.  A null value specifies
	 *   that only the context argument will be used to match triples.
	 * @return a TriplesIterator instance
	 * @throws AllegroGraphException if there is
	 *    a problem.
	 * 
	 */
	public TriplesIterator getStatements(Object subject, Object predicate,
			Object object, Object obEnd, Object context, Object contextEnd)
		throws AllegroGraphException {
		return verifyEnabled().getTriples(this,
				validRangeRef(subject), 
				validRangeRef(predicate),
				validRangeRef(object),
				anyContextRef(context, 5),
				null, null,
				validRangeRef(obEnd),
				anyContextRef(contextEnd, 6),
				defaultLookAhead);
	}
	
	
	
	/**
	 * Retrieve statements from the triple store and specify a range of
	 *  values for the object and/or context components of the triples.
	 * 
	 * @param includeInferred if true, include inferred triples in the result.
	 *    If false, return only triples that are actually present in the 
	 *    triple store.
	 * @param subject
	 * @param predicate 
	 * @param object
	 * @param obEnd a non-null value specifies the end of a range of values
	 *   for the object component of the triples.  A null value specifies
	 *   that only the object argument will be used to match triples.
	 * @param context a null value is a wild card, the empty string specifies 
	 *   the null context, any other value must specify a resource that will 
	 *   match the context component.
	 * @param contextEnd a non-null value specifies the end of a range of values
	 *   for the context component of the triples.  A null value specifies
	 *   that only the context argument will be used to match triples.
	 * @return a TriplesIterator instance
	 * @throws AllegroGraphException if there is
	 *    a problem.
	 * 
	 */
	public TriplesIterator getStatements(boolean includeInferred, Object subject, Object predicate,
			Object object, Object obEnd, Object context, Object contextEnd)
		throws AllegroGraphException {
		return verifyEnabled().getInfTriples(this,
				validRangeRef(subject), 
				validRangeRef(predicate),
				validRangeRef(object),
				anyContextRef(context, 5),
				null, null,
				validRangeRef(obEnd),
				anyContextRef(contextEnd, 6),
				defaultLookAhead, includeInferred);
	}
	
	/**
	 * Retrieve statements from the triple store and specify a range of
	 *  values for any of the components of the triples.
	 *  @see #getStatements(boolean, Object, Object, Object, Object, Object, Object)
	 * @param includeInferred
	 * @param subject
	 * @param subEnd
	 * @param predicate
	 * @param predEnd
	 * @param object
	 * @param obEnd
	 * @param context
	 * @param contextEnd
	 * @return
	 * @throws AllegroGraphException
	 */
	public TriplesIterator getStatements(boolean includeInferred,
			Object subject, Object subEnd, Object predicate, Object predEnd,
			Object object, Object obEnd, Object context, Object contextEnd)
		throws AllegroGraphException {
		return verifyEnabled().getInfTriples(this,
				validRangeRef(subject), 
				validRangeRef(predicate),
				validRangeRef(object),
				anyContextRef(context, 5),
				validRangeRef(subEnd),
				validRangeRef(predEnd),
				validRangeRef(obEnd),
				anyContextRef(contextEnd, 6),
				defaultLookAhead, includeInferred);
	}


	

	/** 
	 * Find out if null-context statements matching the arguments are in the
	 * triple store.
	 * 
	 */
	public boolean hasStatement(Object subject, Object predicate, Object object)
	throws AllegroGraphException {
		return hasStatement(subject, predicate, object, URINodeImpl.nullContext);
	}
	
	/** 
	 * Find out if null-context statements matching the arguments are in the
	 * triple store.
	 * @param includeInferred If false, only ground triples (triples directly stored 
	 *    in the triple store) are included in the search.  If true, the search 
	 *    includes triples derived by reasoning.
	 * 
	 */
	public boolean hasStatement(boolean includeInferred, Object subject, Object predicate, Object object)
	throws AllegroGraphException {
		return hasStatement(includeInferred, subject, predicate, object, URINodeImpl.nullContext);
	}
	
	/** 
	 * Find out if any statements matching the arguments are in the
	 * triple store.
	 * 
	 */
	public boolean hasStatement(Object subject, Object predicate, Object object, Object context)
			throws AllegroGraphException {
		return verifyEnabled().hasTriple(this, validRefOrWild(subject),
				validRefOrWild(predicate),
				validRefOrWild(object),
				anyContextRef(context, 3)
				);
	}
	
	/** 
	 * Find out if any statements matching the arguments are in the
	 * triple store.
	 * @param includeInferred If false, only ground triples (triples directly stored 
	 *    in the triple store) are included in the search.  If true, the search 
	 *    includes triples derived by reasoning.
	 * 
	 */
	public boolean hasStatement(boolean includeInferred, Object subject, Object predicate, Object object, Object context)
	throws AllegroGraphException {
		return verifyEnabled().hasInfTriple(this, validRefOrWild(subject),
				validRefOrWild(predicate),
				validRefOrWild(object),
				anyContextRef(context, 3), includeInferred
		);
	}
	

	/**
	 * Add a Statement in the null context to the triple store.
	 * 
	 */
	public void addStatement(Object subject, Object predicate, Object object)
		throws AllegroGraphException {
		addStatement(subject, predicate, object, null);
	}
	
	/**
	 * Add a Statement to the triple store.
	 * 
	 */
	public void addStatement(Object subject, Object predicate, Object object, Object context)
		throws AllegroGraphException {
		verifyEnabled().addTriple(this, validRef(subject), validRef(predicate),
				validRef(object), anyContextRef(context, 1));
	}

	

	
	
	/**
	 * Add several null context Statements to the triple store.
	 * 
	 * @param subject An array of Resource instances.
	 * @param predicate An array of URI instances.
	 * @param object An array of Value instances.
	 * <p>
	 * The number of Statements added is determined by the longest
	 * array argument.  The last element in short arrays is extended
	 * to the required length.  A single (not array) argument is treated as an 
	 * array of one element.
	 * @throws AllegroGraphException
	 */
	public void addStatements ( Object subject, Object predicate, Object object )
		throws AllegroGraphException {
		addStatements(subject, predicate, object, null);
	}
	
	/**
	 * Add several Statements to the triple store.
	 * 
	 * @param subject An array of Resource instances.
	 * @param predicate An array of URI instances.
	 * @param object An array of Value instances.
	 * @param contexts An array of URI instances.
	 * <p>
	 * The number of Statements added is determined by the longest
	 * array argument.  The last element in short arrays is extended
	 * to the required length.  A single (not array) argument is treated as an 
	 * array of one element.
	 * @throws AllegroGraphException
	 */
	public void addStatements ( Object subject, Object predicate, Object object, Object contexts )
		throws AllegroGraphException {
		try {
			verifyEnabled().addTriples(this, validRefs(subject),
										validRefs(predicate),
										validRefs(object), 
										anyContextRefs(contexts, 1)
										);
		} catch (AllegroGraphException e) {
			throw new AllegroGraphException(e);
		}
	}
	

	/**
	 * Delete all the statements in the triple store (one-by-one).
	 * 
	 */
	public void clear() throws AllegroGraphException {
		try {
			verifyEnabled().delete( this,
									UPIImpl.wildUPI(),
									UPIImpl.wildUPI(),
									UPIImpl.wildUPI(),
									UPIImpl.wildUPI(),
									true
									);
		} catch (AllegroGraphException e) {
			throw new AllegroGraphException(e);
		}
	}

	

	
	private static String xsiPrefix = com.franz.agbase.XmlSchema.NAMESPACE;
	
	// This table cannot be static because the UPIs are registered with
	//  each triple store in a separate tstring table.
	private URINode[] xsiCache = new URINode[]{
		                              null,   // 0
		                              null,   // 0
		                              null,   // 0
		                              null,   // 0
		                              null,   // 0
		                              null,   // 0
									  null   // 0
	                                  };
	private static final int XS_BOOLEAN = 0;
	private static final int XS_BYTE = 1;
	private static final int XS_SHORT = 2;
	private static final int XS_INT = 3;
	private static final int XS_LONG = 4;
	private static final int XS_FLOAT = 5;
	private static final int XS_DOUBLE = 6;
	private synchronized URINode getDataType ( int index ) {
		if ( xsiCache[index]==null ) {
			String type = "";
			switch (index) {
			case XS_BOOLEAN: type = "boolean"; break;
			case XS_BYTE: type = "byte"; break;
			case XS_SHORT: type = "short"; break;
			case XS_INT: type = "int"; break;
			case XS_LONG: type = "long"; break;
			case XS_FLOAT: type = "float"; break;
			case XS_DOUBLE: type = "double"; break;
			default: throw new IllegalArgumentException("bad Schema type index");
			}
			xsiCache[index] = createURI(xsiPrefix, type);
		}
			
		return xsiCache[index];
	}
	
	/**
	 * Create a typed LiteralNode instance from a Java boolean value
	 * but do not modify the triple store.
	 * @return a LiteralNode instance.
	 * <p>
	 * The LiteralNode instance will have a null UPI.
	 */
	public LiteralNode createLiteral(boolean value) {
		return createLiteral(Boolean.toString(value), getDataType(XS_BOOLEAN));
	}
	/**
	 * Create a typed LiteralNode instance from a Java boolean value
	 * and add the LiteralNode to the triple store registry.
	 * @return a LiteralNode
	 */
	public LiteralNode addLiteral(boolean value)
		throws AllegroGraphException {
		return addLiteral(Boolean.toString(value), getDataType(XS_BOOLEAN));
	}
	
	/**
	 * Create a typed LiteralNode instance from a Java long value
	 * but do not modify the triple store.
	 * @return a LiteralNode instance.
	 * <p>
	 * The LiteralNode instance will have a null UPI.
	 */
	public LiteralNode createLiteral(long value) {
		return createLiteral(Long.toString(value), getDataType(XS_LONG));
	}
	/**
	 * Create a typed LiteralNode instance from a Java long value
	 * and add the LiteralNode to the triple store registry.
	 * @return a LiteralNode
	 */
	public LiteralNode addLiteral(long value)
		throws AllegroGraphException {
		return addLiteral(Long.toString(value), getDataType(XS_LONG));
	}
	
	/**
	 * Create a typed LiteralNode instance from a Java int value
	 * but do not modify the triple store.
	 * @return a LiteralNode
	 * <p>
	 * The literal instance will have a null UPI.
	 */
	public LiteralNode createLiteral(int value) {
		return createLiteral(Integer.toString(value), getDataType(XS_INT));
	}
	/**
	 * Create a typed LiteralNode instance from a Java int value
	 * and add the LiteralNode to the triple store registry.
	 * @return a LiteralNode
	 */
	public LiteralNode addLiteral(int value)
		throws AllegroGraphException {
		return addLiteral(Integer.toString(value), getDataType(XS_INT));
	}
	
	/**
	 * Create a typed LiteralNode instance from a Java short value
	 * but do not modify the triple store.
	 * @return a LiteralNode instance.
	 * <p>
	 * The LiteralNode instance will have a null UPI.
	 */
	public LiteralNode createLiteral(short value) {
		return createLiteral(Short.toString(value), getDataType(XS_SHORT));
	}
	/**
	 * Create a typed LiteralNode instance from a Java short value
	 * and add the LiteralNode to the triple store registry.
	 * @return a LiteralNode instance.
	 */
	public LiteralNode addLiteral(short value)
		throws AllegroGraphException {
		return addLiteral(Short.toString(value), getDataType(XS_SHORT));
	}
	
	/**
	 * Create a typed LiteralNode instance from a Java byte value
	 * but do not modify the triple store.
	 * @return a LiteralNode instance.
	 * <p>
	 * The LiteralNode instance will have a null UPI.
	 */
	public LiteralNode createLiteral(byte value) {
		return createLiteral(Byte.toString(value), getDataType(XS_BYTE));
	}
	/**
	 * Create a typed LiteralNode instance from a Java byte value
	 * and add the LiteralNode to the triple store registry.
	 * @return a LiteralNode
	 */
	public LiteralNode addLiteral(byte value)
		throws AllegroGraphException {
		return addLiteral(Byte.toString(value), getDataType(XS_BYTE));
	}
	
	/**
	 * Create a typed LiteralNode instance from a Java double value
	 * but do not modify the triple store.
	 * @return a LiteralNode instance
	 * <p>
	 * The LiteralNode instance will have a null UPI.
	 */
	public LiteralNode createLiteral(double value) {
		return createLiteral(Double.toString(value), getDataType(XS_DOUBLE));
	}
	/**
	 * Create a typed LiteralNode instance from a Java double value
	 * and add the LiteralNode to the triple store registry.
	 * @return a LiteralNode
	 */
	public LiteralNode addLiteral(double value)
		throws AllegroGraphException {
		return addLiteral(Double.toString(value), getDataType(XS_DOUBLE));
	}
	
	/**
	 * Create a typed LiteralNode instance from a Java float value
	 * but do not modify the triple store.
	 * @return a LiteralNode instance.
	 * <p>
	 * The LiteralNode instance will have a null UPI.
	 */
	public LiteralNode createLiteral(float value) {
		return createLiteral(Float.toString(value), getDataType(XS_FLOAT));
	}
	/**
	 * Create a typed LiteralNode instance from a Java float value
	 * and add the LiteralNode to the triple store registry.
	 * @return a LiteralNode
	 */
	public LiteralNode addLiteral(float value)
		throws AllegroGraphException {
		return addLiteral(Float.toString(value), getDataType(XS_FLOAT));
	}
	
	
	/**
	 * Create a Triple instance but do not update the triple store.
	 * @return a Triple instance.
	 */
	public Triple createStatement(ResourceNode subject, URINode predicate, ValueNode object, ResourceNode context ) {
		
		UPI s = queryAGId(subject);
		UPI p = queryAGId(predicate);
		UPI o = queryAGId(object);
		UPI c = UPIImpl.nullUPI();
		if ( context instanceof URINodeImpl )
			c = queryAGId(context);
		TripleImpl b = (TripleImpl) AGFactory.makeTriple(this, s, p, o, c);
		b.subjInstance = subject;
		b.predInstance = predicate;
		b.objInstance = object;
		return b;
	}
	
	
	
	
	/**
	 * Add a new triple to the triple store.
	 * 
	 * @param s A String, UPI, or Value specifying a subject.
	 * @param p A String, UPI, or Value specifying a predicate.
	 * @param o A String, UPI, or Value specifying an object.
	 * @return a Triple instance for the new triple. All the string arguments
	 *         must be in Ntriples syntax.
	 */
	public Triple newTriple(Object s, Object p, Object o)
	throws AllegroGraphException {
		return newTriple(s, p, o, null);
	}
	
	/**
	 * Add a new triple to the triple store.
	 * 
	 * @param s A String, UPI, or Value specifying a subject.
	 * @param p A String, UPI, or Value specifying a predicate.
	 * @param o A String, UPI, or Value specifying an object.
	 * @return the integer id of the new triple
	 */
	public long newTripleId(Object s, Object p, Object o)
	throws AllegroGraphException {
		return newTripleId(s, p, o, null);
	}
	
	/**
	 * Add a new triple to the triple store.
	 * 
	 * @param s A String, UPI, or Value specifying a subject.
	 * @param p A String, UPI, or Value specifying a predicate.
	 * @param o A String, UPI, or Value specifying an object.
	 * @param c A String, UPI, or Value specifying a context.
	 * @return a Triple instance for the new triple. All the string arguments
	 *         must be in Ntriples syntax.
	 */
	public Triple newTriple(Object s, Object p, Object o, Object c)
			throws AllegroGraphException {
		Object[] tra = verifyEnabled().addTriple(this,
							validRef(s), validRef(p), validRef(o),
							anyContextRef(c, 1));
		Triple tr = AGFactory.makeTriple(this,
								((Long)tra[0]).longValue(),
								(UPIImpl)tra[1], 
								(UPIImpl)tra[2], 
								(UPIImpl)tra[3], 
								(UPIImpl)tra[4]								         
								);
		return tr;
	}
	
	/**
	 * Add a new triple to the triple store.
	 * 
	 * @param s A String, UPI, or Value specifying a subject.
	 * @param p A String, UPI, or Value specifying a predicate.
	 * @param o A String, UPI, or Value specifying an object.
	 * @param c A String, UPI, or Value specifying a context.
	 * @return the integer id of the new triple
	 */
	public long newTripleId(Object s, Object p, Object o, Object c)
		throws AllegroGraphException {
		Object[] tra = verifyEnabled().addTriple(this,
				validRef(s), validRef(p), validRef(o),
				anyContextRef(c, 1));
		return ((Long)tra[0]).longValue();
	}
	
	/**
	 * Add new triples to the triple store.
	 * 
	 * @param s An array of Strings UPIs or Value instances specifying subjects.
	 * @param p An array of Strings UPIs or Value instances specifying predicates.
	 * @param o An array of Strings UPIs or Value instances specifying objects.
	 * @return an array of Triple instances. All the string arguments must be in
	 *         Ntriples syntax.
	 *         <p>
	 * The number of triples added is determined by the longest
	 * array argument.  The last element in short arrays is extended
	 * to the required length.
	 */
	public Triple[] newTriples(Object s, Object p, Object o)
		throws AllegroGraphException {
		return newTriples(s, p, o, null);
	}
	
	/**
	 * Add new triples to the triple store.
	 * 
	 * @param s An array of Strings UPIs or Value instances specifying subjects.
	 * @param p An array of Strings UPIs or Value instances specifying predicates.
	 * @param o An array of Strings UPIs or Value instances specifying objects.
	 * @param c An array of Strings UPIs or Value instances specifying contexts.
	 * @return an array of Triple instances. All the string arguments must be in
	 *         Ntriples syntax.
	 *         <p>
	 * The number of triples added is determined by the longest
	 * array argument.  The last element in short arrays is extended
	 * to the required length.
	 */
	public Triple[] newTriples(Object s, Object p, Object o, Object c)
			throws AllegroGraphException {
		Object[] r = verifyEnabled().addTriples(this, validRefs(s), validRefs(p),
				validRefs(o), anyContextRefs(c, 1));
		long[] ri = (long[]) r[0];
		UPI[] rs = (UPI[]) r[1];
		UPI[] rp = (UPI[]) r[2];
		UPI[] ro = (UPI[]) r[3];
		Triple[] rt = new Triple[ri.length];
		for (int i = 0; i < ri.length; i++) {
			Triple tr = AGFactory.makeTriple(this, ri[i], rs[i], rp[i], ro[i]);
			//tr.subject = stringElt(s, i);
			//tr.predicate = stringElt(p, i);
			//tr.object = stringElt(o, i);
			// tr.context = AG_NULL_STRING;
			rt[i] = tr;
		}
		return rt;
	}
	
	/**
	 * Add new triples to the triple store.
	 * 
	 * @param s An array of Strings UPIs or Value instances specifying subjects.
	 * @param p An array of Strings UPIs or Value instances specifying predicates.
	 * @param o An array of Strings UPIs or Value instances specifying objects.
	 * @return an array of Triple id integers
	 *         <p>
	 * The number of triples added is determined by the longest
	 * array argument.  The last element in short arrays is extended
	 * to the required length.
	 */
	public long[] newTripleIds(Object s, Object p, Object o)
	throws AllegroGraphException {
	return newTripleIds(s, p, o, null);
}

	/**
	 * Add new triples to the triple store.
	 * 
	 * @param s An array of Strings UPIs or Value instances specifying subjects.
	 * @param p An array of Strings UPIs or Value instances specifying predicates.
	 * @param o An array of Strings UPIs or Value instances specifying objects.
	 * @param c An array of Strings UPIs or Value instances specifying contexts.
	 * @return an array of Triple id integers
	 *         <p>
	 * The number of triples added is determined by the longest
	 * array argument.  The last element in short arrays is extended
	 * to the required length.
	 */
	public long[] newTripleIds(Object s, Object p, Object o, Object c)
	throws AllegroGraphException {
		Object[] r = verifyEnabled().addTriples(this,
				validRefs(s), validRefs(p), validRefs(o),
				anyContextRefs(c, 1));
		return (long[]) r[0];
	}


	/**
	 * Remove all statement that match a pattern in the bull context of the triple store.
	 * 
	 * @param s the subject  pattern (Value, UPI, string, or array of same)
	 * @param p the predicate  pattern  (Value, UPI, string, or array of same)
	 * @param o the object pattern (Value, UPI, string, or array of same)
	 * <p>
	 * The s, p, or o, arguments may also be null or an empty string
	 * to denote a wild card.
	 * @throws AllegroGraphException
	 */
	public void removeStatements(Object s, Object p, Object o )
		throws AllegroGraphException
		{ removeStatements(s, p, o, UPIImpl.nullUPI()); }
	
	/**
	 * Remove all statement that match a pattern from the triple store.
	 * 
	 * @param s the subject  pattern (Value, UPI, string, or array of same)
	 * @param p the predicate  pattern  (Value, UPI, string, or array of same)
	 * @param o the object pattern (Value, UPI, string, or array of same)
	 * @param c the context pattern (Value, UPI, string, or array of same); null denotes all contexts,
	 *     the empty string denotes the null context.
	 * <p>
	 * The s, p, or o, arguments may also be null or an empty string
	 * to denote a wild card.
	 * @throws AllegroGraphException
	 */
	public void removeStatements(Object s, Object p, Object o, Object c )
	throws AllegroGraphException
	{ verifyEnabled().delete(this, validRefsOrWild(s), validRefsOrWild(p),
			validRefsOrWild(o), anyContextRefs(c, 3), true
			); }
	



	
	/**
	 * Allocate a set of BlankNode ID numbers.
	 * @param n the number of nodes to allocate.
	 * @return an array of intege ID numbers
	 * @throws AllegroGraphException if there is a problem during creation.
	 */
	public UPI[] createBNodeIds(int n) throws AllegroGraphException{
		return verifyEnabled().newBlankNodes(this, n);		
	}
	
	/**
	 * Remove one null-context statement from the triple store.
	 * 
	 * @param s A Resource, a string in triples format, or a UPI
	 * @param p A Node, a string in triples format, or a UPI
	 * @param o A Value, a string in triples format, or a UPI
	
	 * @throws AllegroGraphException
	 */
	public void removeStatement(Object s, Object p, Object o )
		throws AllegroGraphException {
		removeStatement(s, p, o, UPIImpl.nullUPI());
	}
	
	/**
	 * Remove one statement from the triple store.
	 * 
	 * @param s the subject of the Statement in question
	 * @param p the predicate of the Statement in question
	 * @param o the object of the Statement in question
	 * @param c the context (or graph) of the Statement in question
	 * <p>
	 * Each of the parameters may be a Value instance, or a UPI instance,
	 * or a string containing an ntriple format reference.
	 * <p>
	 * The context argument may be null to denote
	 * the Null Context (Default Graph).
	 * @throws AllegroGraphException
	 */
	public void removeStatement(Object s, Object p, Object o, Object c )
		throws AllegroGraphException {
	verifyEnabled().delete(this, validRef(s), validRef(p), validRef(o), anyContextRef(c, 1), false);
	}
	
	
	/**
	 * Add a set of literal instances to the triple store registry.
	 * @param values an array of string literal values.
	 * @param datatypes an array of string URIs for datatypes.  If an array element is
	 *      null, the corresponding literal does not have a datatype qualifier.  If the
	 *      array argument is null, then none of the literals have datatype qualifiers.
	 * @param languages an array of strings language qualifiers. If an array element is
	 *      null, the corresponding literal does not have a language qualifier.  If 
	 *      the array argument is null, then none of the literals have language
	 *      qualifiers.
	 *      <p>
	 *      If an element of the datatypes array is not null, the corresponding
	 *      element in the languages array is ignored. 
	 * @return an array of UPIs for the new literals.
	 * @throws AllegroGraphException
	 * <p>
	 * The result array is the same length as the values array.  The other two argument
	 * arrays may be shorter.  If shorter, the last element in each array is repeated
	 * as often as necessary.
	 */
	public UPI[] addLiteralIds(String[] values,
								  String[] datatypes,
								  String[] languages)
		throws AllegroGraphException 
	{
		return verifyEnabled().newLiteral(this, values, datatypes, languages);	
	}
	
    
    /**
     * Collect the unique subjects from triples that match a text pattern.
     * Search the triple store for triples where the object was indexed as freetext
     * and the text matches the specified pattern.
     * @param pattern A text pattern as described in {@link #getFreetextStatements(String)}
     * @return An iterator that will generate all the subjects.  Each result set is a single
     *      subject.
     * @throws AllegroGraphException
     */
    public ValueSetIterator getFreetextUniqueSubjects ( String pattern )
    	throws AllegroGraphException {
    	Object[] v = verifyEnabled().getFreetextSubjects(this, pattern, selectLimit);
    	//return (ValueObject[])selectValuesArray(true, v, false);
    	return new ValueSetIteratorImpl(this, v);
    }
    
    /**
     * Find triples that match a text pattern.
     * Search the triple store for triples where the object was indexed as freetext
     * and the text matches the specified pattern.
     * @param pattern A text pattern as described below.
     * @return A TriplesIterator instance that iterates over the results.
     * @throws AllegroGraphException
     * <p>
     * The text pattern is a string containing a well-formed pattern expression
     * defines as follows:
     * <pre>
     * <i>pattern</i> := <i>atomic_pattern</i> | <i>composite_pattern</i>
     * <i>composite_pattern</i> := <b>( and</b>  <i>pattern</i>* <b>)</b> | <b>( or</b>  <i>pattern</i>* <b>)</b>
     * <i>atomic_pattern</i> := <i>word_pattern</i> | <i>phrase_pattern</i>
     * <i>word_pattern</i> := <b>'</b><i>char</i>*<b>'</b>
     * <i>phrase pattern</i> := <b>[</b><i>char</i>*<b>]</b>
     * <i>char</i> := <b>/(</b>      denotes the character [
     * <i>char</i> := <b>/)</b>      denotes the character ]
     * <i>char</i> := <b>/!</b>      denotes the character '
     * <i>char</i> := <b>//</b>     denotes the character /
     * <i>char</i> := <b>/#</b>      denotes the character "
     * <i>char</i> := <b>?</b>       denotes a wild card that matches any single character
     * <i>char</i> := <b>*</b>       denotes a wild card that matches any sequence of characters
     * <i>char</i> := <i>any</i>     any other character denotes itself
     * </pre>
     */
    public TriplesIterator getFreetextStatements ( String pattern )
    	throws AllegroGraphException {
    	return verifyEnabled().getFreetextStatements(this, pattern, defaultLookAhead);
    }
    
    /**
     * Retrieve the URI labels of the registered freetext predicates.
     * @return an array of strings - these are the URI labels of the registered predicates.
     * @throws AllegroGraphException
     */
    public String[] getFreetextPredicates () throws AllegroGraphException {
    	return verifyEnabled().freetextPredicates(this, null);
    }
    
    /**
     * Register a predicate for free text indexing.
     * The text in literal objects of triples with registered predicates are 
     * indexed so that they can be searched with text patterns
     * with {@link #getFreetextStatements(String)} or with
     * {@link #getFreetextUniqueSubjects(String)}.
     * Free text indexing predicates must be registered before any triples 
     * are added to the triple store.
     * @param predicate The predicate may be specified as a string in Ntriples syntax 
     *    or !-notation, a UPI instance, or a URI instance.
     * @throws AllegroGraphException
     */
    public void registerFreetextPredicate ( Object predicate )
    	throws AllegroGraphException {
    	verifyEnabled().freetextPredicates(this, validRefs(predicate));
    }
    
    Object selectValuesArray ( boolean one, Object[] v, boolean nullOk )
    	throws AllegroGraphException {
    	if ( v==null ) {
    		if (one) return new ValueObject[0];
    		return new ValueObject[0][0];
    	}
    	UPIImpl[] ids = (UPIImpl[])v[0];
    	if ( ids==null ) {
    		if (one) return new ValueObject[0];
    		return new ValueObject[0][0];
    	}
    	int[] types = (int[])v[1];
    	String[] labels = (String[]) v[2];
    	String[] mods = (String[])v[3];
    	int more = ((Integer)v[4]).intValue();
    	int width = ((Integer)v[5]).intValue();
    	if ( one && width!=1 )
    		throw new IllegalArgumentException
    			("Asked for single result but received " + width);
    	Object token = v[6];
    	int plimit = ((Integer)v[7]).intValue();
    	int all = ids.length;
    	Object sv = null;
    	if ( 8<v.length ) sv = v[8];
    	if (one)
    	{
    		ValueObject[] s = new ValueObject[all];
        		for (int i=0; i<all; i++) 
        			s[i] = newSelectValue(nullOk, ids[i], types[i], labels[i], mods[i]);
        	return registerValues(s, token, more, plimit, sv, nullOk);
    	}
    	ValueObject[][] r;
    	if ( width==0 )
    	{
    		r = new ValueObject[all][0];
    		return registerValues(r, token, more, plimit, sv, nullOk);
    	}	
    	int n = all/width;
    	int i = 0; int j = 0; r = new ValueObject[n][width];
    	while (i<all) {
    		for ( int k=0; k<width; k++ ) {
    			r[j][k] = newSelectValue(nullOk, ids[i], types[i], labels[i], mods[i]);
    			i++;
    		}
    		j++;
    	}
    	return registerValues(r, token, more, plimit, sv, nullOk);
    }
    
    Object registerValues ( Object r, Object token, int more, int plimit, Object sv, boolean nullOk)
    	throws AllegroGraphException {
    	if ( more>0 || sv!=null )
    		ags.valueMap.put(r, new valueMapEntry(this, token, more, plimit, sv, nullOk));
    	discardOldTokens(false);
    	//System.out.println("registerValues" + r);
    	return r;
    }
    
    
    static class valueMapEntry {
    	Object savedToken; int savedMore; AGInternals savedAG; Object savedVal = null;
    	int savedPlimit;  boolean savedNullOk;
    	public valueMapEntry ( AGInternals ag, Object token, int more, int pl, Object sv, boolean nullOk ) {
			savedToken = token; savedMore = more; 
			savedAG = ag; savedPlimit = pl; savedVal = sv;
			savedNullOk = nullOk;
		}
    	protected synchronized void finalize() throws Throwable {
    		if ( (savedAG.ags)==null ) return;
    		if ( null!=savedToken ) savedAG.ags.oldTokens.add(savedToken);
    	}
    }
    	
   
	/**
	 * Create a Null Context Statement instance but do not update the triple store.
	 * @return a Triple instance.
	 */
	public Triple createStatement(ResourceNode subject, URINode predicate, ValueNode object) {
		return createStatement(subject, predicate, object, null);
	}
	
	
	
	private int intResult ( Object r ) { return AGConnector.toInt(r); }
	
	/**
	 * Query the number of unmerged index chunks in the triple store.
	 * The indexTriples() method builds a new index chunk each time it
	 * is called.  It takes less time to build a partial index, but when
	 * too many index chunks are present, query performance suffers.
	 * The indexAll() method builds a single unified index for the entire 
	 * triple store.
	 * @return the number of index chunks in the triple store.
	 * @throws AllegroGraphException
	 */
	public int getUnmergedCount() throws AllegroGraphException {
		Object r = verifyEnabled().indexing(this, AGC.AGU_IQ_CHUNKS, 0, null);
		return intResult(r);
	}
	
	/**
	 * Query the number of unindexed triples in the triple store.
	 * Indexed triples can be searched more efficiently.
	 * @return the number of unindexed triples in the triple store.
	 * @throws AllegroGraphException
	 */
	public int getUnindexedTripleCount() throws AllegroGraphException {
		Object r = verifyEnabled().indexing(this, AGC.AGU_IQ_COUNT, 0, null);
		return intResult(r);
	}
	
	/**
	 * Query the threshold for automatic indexing of the triple store.
	 * The built-in default value is zero to suppress automatic indexing.
	 * @return the integer value of the threshold for automatic indexing
	 *    of the triple store.
	 * @throws AllegroGraphException
	 */
	public int getUnindexedThreshold() throws AllegroGraphException {
		Object r = verifyEnabled().indexing(this, AGC.AGU_IQ_UNTHRESH, 0, null);
		return intResult(r);
	}
	
	/**
	 * Set the threshold for automatic indexing of the triple store.
	 * When the number of unindexed triples in the triple store exceeds this
	 * number, then the new triples are automatically indexed into a new
	 * index chunk.  A value of zero suppresses automatic indexing.
	 * @param val The new value for this parameter.
	 * @throws AllegroGraphException
	 */
	public void setUnindexedThreshold( int val ) throws AllegroGraphException {
		verifyEnabled().indexing(this, AGC.AGU_IS_UNTHRESH, val, null);
	}
	
	/**
	 * Query the threshold for automatic re-indexing of the triple store.
	 * The built-in default value is zero to suppress automatic re-indexing.
	 * @return the integer value of the threshold for automatic re-indexing
	 *    of the triple store.
	 * @throws AllegroGraphException
	 */
	public int getUnmergedThreshold() throws AllegroGraphException {
		Object r = verifyEnabled().indexing(this, AGC.AGU_IQ_CHTHRESH, 0, null);
		return intResult(r);
	}
	
	/**
	 * Set the threshold for automatic re-indexing of the triple store.
	 * When the number of index chunks in the triple store exceeds this
	 * number, then the new triples are automatically re-indexed into a new
	 * single unified index.  A value of zero suppresses automatic re-indexing.
	 * @param val The new value for this parameter.
	 * @throws AllegroGraphException
	 */
	public void setUnmergedThreshold( int val ) throws AllegroGraphException {
		verifyEnabled().indexing(this, AGC.AGU_IS_CHTHRESH, val, null);
	}
	
	/**
	 * Query the range of indexing that is applied to this triple store.
	 * @return an array of strings that describe the indexing flavors.
	 * @throws AllegroGraphException
	 */
	public String[] getIndexFlavors() throws AllegroGraphException {
		Object r = verifyEnabled().indexing(this, AGC.AGU_IQ_FLAVORS, 0, null);
		if ( r==null ) return new String[0]; // [rfe7522] always return an array
		return (String[])(r);
	}
	
	/**
	 * Replace the set of indexing flavors in the triple store.
	 * The changes take effect the next time an indexing operation
	 * is initiated.
	 * @param flavors An array of strings specifying the desired indexing
	 *    flavors.  Any existing flavors are discarded.
	 *    Each string is a permutation of the letters "spog".
	 * @throws AllegroGraphException
	 * <p>
	 * <b>Multi-user Note:</b> The set of index flavors is a persistent property of 
	 * the persistent triple store.  If several users are accessing the same triple 
	 * store, any changes to the index flavors are seen by all users.
	 */
	public void setIndexFlavors( String[] flavors ) throws AllegroGraphException {
		verifyEnabled().indexing(this, AGC.AGU_IS_FLAVORS, 0, flavors);
	}
	
	/**
	 * Add some index flavors to the triple store.
	 * The changes take effect the next time an indexing operation
	 * is initiated.
	 * @param flavors An array of strings specifying the desired indexing
	 *    flavors.  These flavors are added to the existing flavors.
	 *    Each string is a permutation of the letters "spogi".
	 * @throws AllegroGraphException
	 * <p>
	 * <b>Multi-user Note:</b> The set of index flavors is a persistent property of 
	 * the persistent triple store.  If several users are accessing the same triple 
	 * store, any changes to the index flavors are seen by all users.
	 */
	public void addIndexFlavors( String[] flavors ) throws AllegroGraphException {
		verifyEnabled().indexing(this, AGC.AGU_IA_FLAVORS, 0, flavors);
	}
	
	/**
	 * Delete some indexing flavors from the triple store.
	 * The changes take effect the next time an indexing operation
	 * is initiated.
	 * @param flavors An array of strings specifying the  indexing
	 *    flavors to delete.  These flavors are deleted from the existing flavors.
	 *    Each string is a permutation of the letters "spog".
	 * @throws AllegroGraphException
	 * <p>
	 * <b>Multi-user Note:</b> The set of index flavors is a persistent property of 
	 * the persistent triple store.  If several users are accessing the same triple 
	 * store, any changes to the index flavors are seen by all users.
	 */
	public void dropIndexFlavors( String[] flavors ) throws AllegroGraphException {
		verifyEnabled().indexing(this, AGC.AGU_ID_FLAVORS, 0, flavors);
	}
	
	/**
	 * Add to the table of datatype and property mappings in the triple store.
	 * These mappings are in effect only during bulk loading with loadNtriple()
	 * loadNtriples() or loadRDFXML().
	 * Data converted to internal data types can be queried with range
	 * queries.
	 * @param map An array of strings of a length that is a multiple of 3.
	 *   Each group of 3 consecutive elements defines one mapping:
	 *   the first element is string URI of a datatype or predicate;
	 *   the second element is a string naming an internal representation type
	 *   of AllegroGraph (the valid types are listed in the AllegroGraph
	 *   introduction); the third element is the string "datatype" or 
	 *   "predicate" to identify the kind of mapping.
	 *   <p>
	 *    The entries in the
	 *    array are added to the table in the triple store.  An existing
	 *    entry is superseded by the new definition. 
	 *    <p>
	 *    When a triple is created with a predicate matching a defined URI,
	 *    then the object of the triple is parsed and converted to the 
	 *    internal data type.
	 *    <p>
	 *    When a literal is created with a datatype modifier that matches a
	 *    defined URI, then the content of the literal is parsed and converted to the 
	 *    internal data type.
	 * @throws AllegroGraphException
	 */
	public void addDataMapping ( String[] map ) throws AllegroGraphException {
		verifyEnabled().mapping(this, AGC.AGU_MAP_ADD, map);
	}
	
	/**
	 * Replace the current table of datatype and property mappings in the triple store.
	 * These mappings are in effect only during bulk loading with 
	 * loadNtriples().
	 * @param map An array of strings similar to the array returned by getDataMapping().  
	 * @see #addDataMapping(String[])
	 * @see #getDataMapping()
	 * @throws AllegroGraphException
	 */
	public void setDataMapping ( String[] map ) throws AllegroGraphException {
		verifyEnabled().mapping(this, AGC.AGU_MAP_REP, map);
	}
	
	/**
	 * Query the current table of datatype and property mappings in the triple store.
	 * These mappings are in effect only during bulk loading with loadNtriple()
	 * loadNtriples() or loadRDFXML().
	 * @return An array of strings that describe the mappings.
	 *    The length of the array is a multiple of 3.
	 *    The contents of the array is a sequence of strings
	 *    u1, d1, k1, u2, d2, k2, ...
	 *    where ui is a URI string that identifies a predicate
	 *   or Schema type, di is a string that identifies an internal 
	 *   AllegroGraph data type of the mapped value, and ki is
	 *   the string "predicate" or "datatype" to identify the kind of mapping.
	 * 
	 * @throws AllegroGraphException
	 * @see #addDataMapping(String[])
	 */
	public String[] getDataMapping () throws AllegroGraphException {
		Object r = verifyEnabled().mapping(this, AGC.AGU_MAP_QUERY, null);
		if ( r==null ) return new String[0];
		if ( r instanceof String[] ) return (String[])r;
		return new String[0];
	}
	
	/**
	 * Evaluate a Lisp expression in the AllegroGraph server.
	 * During the evaluation, the default triple store is the one
	 * associated with this AllegroGraph instance.
	 * The access to this triple store will be through the same interlocks
	 * used by other client operations.
	 * @param expression The expression.
	 * @return An array of values returned by the call.
	 * @throws AllegroGraphException
	 * <p>
	 * <b>The proper usage of this method is fully described in</b>
	 * <br>
	 * @see com.franz.agbase.AllegroGraphConnection#evalInServer(String)
	 */
	public Object[] evalInServer ( String expression ) throws AllegroGraphException {
		return verifyEnabled().evalInServer(this, expression);
	}
	
	/**
	 * Evaluate a Lisp expression in the AllegroGraph server.
	 * During the evaluation, the default triple store is the one
	 * associated with this AllegroGraph instance.
	 * @param expression The expression.
	 * @param environment
	 * @return An array of values returned by the call.
	 * @throws AllegroGraphException
	 * <p>
	 * <b>The proper usage of this method is fully described in</b>
	 * <br>
	 * @see com.franz.agbase.AllegroGraphConnection#evalInServer(String, String)
	 */
//	public Object[] evalInServer ( String expression, String environment ) 
//		throws AllegroGraphException {
//		return verifyEnabled().evalInServer(this, expression, environment);
//	}
//	
	/**
     * Change the trace state in the AllegroGraph server.
     *
     * @param onoff true to turn on tracing.
     * <p>
     * When tracing is turned on in the AllegroGraph server, every call
     * from Java to the server causes some output on the server console.
     * This output may be useful in diagnosing some problems.
     * Since tracing may produce large quantities of output it should
     * be used with caution and only around critical sections of code.
     * <p>
     * The trace state applies only to operations on this triple store.
     * @return -1 if the call failed for some reason, 0 otherwise.
     */
    public int serverTrace ( boolean onoff ) {
    	try {
			verifyEnabled().serverTrace(this, onoff, null);
			return 0;
		} catch (AllegroGraphException e) {
			return -1;
		}
    }
    
    /**
     * Start tracing calls from Java in the AllegroGraph server.
     *
     * @param outFile the name and path for a file where the trace output will go.
     * <p>
     * When tracing is turned on in the AllegroGraph server, every call
     * from Java to the server causes some output on the server console.
     * This output may be useful in diagnosing some problems.
     * Since tracing may produce large quantities of output it should
     * be used with caution and only around critical sections of code.
     * <p>
     * The trace state applies only to operations on this triple store.
     * @return -1 if the call failed for some reason, 0 otherwise.
     */
    public int serverTrace ( String outFile ) {
    	try {
			verifyEnabled().serverTrace(this, true, outFile);
			return 0;
		} catch (AllegroGraphException e) {
			return -1;
		}
    }
    
    
    /**
     * Query the namespace definitions specific to this triple store.
     * The definitions are not persistent, and apply only to this
     * AllegroGraph instance.
     * @return An array of strings.  The even numbered elements are prefixes
     *    and the following odd numbered element is the full namespace text.
     */
    public String[] getNamespaces () {
    	if ( nsregs==null ) return new String[0];
    	return nsregs.toArray();
    }
    
    /**
     * Query the namespace definitions specific to this triple store.
     * The definitions are not persistent, and apply only to this
     * AllegroGraph instance.
     * @return the current definitions.
     */
    public NamespaceRegistry getNamespaceRegistry () {
    	if ( null==nsregs ) return null;
    	return new NamespaceRegistry(nsregs);
    }
    
    /**
     * Register one namespace definition for this triple store.
     * 
     * The new definition overrides an existing definition.
     * @param prefix the prefix
     * @param full The full namespace text.  If the full text is null 
     *    or "", then the prefix is removed from the table.
     * @throws AllegroGraphException
     */
    public void registerNamespace ( String prefix, String full )
    	throws AllegroGraphException {
    	nsregsInit().register(prefix, full);
    	verifyEnabled().namespaces(this, nsregs.toArray() );
    }
    
    /**
     * Register several namespace definitions for this triple store.
     * 
     * The new definitions overrides any existing definitions.
     * @param defs An array of strings.  The even numbered elements are prefixes
     *    and the following odd numbered element is the full namespace text.
     *    If the full text is null or "", then the prefix is removed
     *    from the table.
     * @throws AllegroGraphException
     * 
     */
    public void registerNamespaces ( String[] defs )
    	throws AllegroGraphException {
    	if ( defs==null ) return;
    	if ( 0==defs.length ) return;
    	nsregsInit();
    	for (int i = 0; i < defs.length; i=i+2) {
			nsregs.register(defs[i], defs[i+1]);
		}
    	verifyEnabled().namespaces(this, nsregs.toArray());
    }
    
    /**
     * Register several namespace definitions for this triple store.
     * 
     * The new definitions overrides any existing definitions.
     * @param ns the new definitions.
     * @throws AllegroGraphException
     */
    public void registerNamespaces ( NamespaceRegistry ns ) throws AllegroGraphException {
    	nsregsInit().register(ns);
    	verifyEnabled().namespaces(this, nsregs.toArray());
    }
    
 // Declared in package com.franz.ag to allow dual AG APIs
    public void registerNamespaces ( com.franz.ag.NamespaceRegistry ns ) throws AllegroGraphException {
    	nsregsInit().register(ns);
    	verifyEnabled().namespaces(this, nsregs.toArray());
    }
    
    /**
     * Register several namespace definitions for this triple store.
     * The new definitions replace any existing definitions entirely.
     * @param ns the new definitions.
     * @throws AllegroGraphException
     */
    public void setNamespaceRegistry ( NamespaceRegistry ns ) throws AllegroGraphException {
    	nsregs = new NamespaceRegistry(ns);
    	verifyEnabled().namespaces(this, (nsregs==null)?null:nsregs.toArray());
    }
    
 // Declared in package com.franz.ag to allow dual AG APIs
    public void setNamespaceRegistry ( com.franz.ag.NamespaceRegistry ns ) throws AllegroGraphException {
    	nsregs = new NamespaceRegistry(ns);
    	verifyEnabled().namespaces(this, (nsregs==null)?null:nsregs.toArray());
    }
    
    /**
     * Reset the namespace definition in this AllefroGraph instance to the
     * initial default.
     * @throws AllegroGraphException
     */
    public void setNamespaceRegistry () throws AllegroGraphException {
    	if ( ags == null )
    		nsregs = null;
    	else if ( null==ags.nsregs )
    		nsregs = null;
    	else
    		nsregs = new NamespaceRegistry(ags.nsregs);
    	verifyEnabled().namespaces(this, (nsregs==null)?null:nsregs.toArray());
    }
    
    
    /**
     * Add a Literal, Node or BlankNode to the triple store.
     * @param part A string in Ntriples syntax or !-notation that denotes
     *     a Literal, a Node, or a blank node.
     *     <p>
     *     NOTE: mentioning a blank node twice with the same string will still 
     *     create two distinct blank node instances.
     * @return a ValueObject instance that represents the data.
     * @throws AllegroGraphException
     */
    public ValueNode addPart ( String part ) throws AllegroGraphException {
    	if ( part.startsWith("_:") ) 
    		return createBNode(part);
    	Object[] r = verifyEnabled().addPart(this, refNtripleString(part));
    	UPIImpl upi = (r[0] instanceof UPIImpl)?(UPIImpl)r[0]:null;
    	int type = 0;
    	if ( (1<r.length) )
    		type = (int) AGConnector.longValue(r[1]);
    	String val = null;  String mod = null;
    	if ( (2<r.length) ) val = (String) r[2];
    	if ( (3<r.length) ) val = (String) r[3];
    	return (ValueNode) newValue(upi, type, val, mod);
    }
    


	// FEDERATION ADDITIONS
    
    protected AllegroGraph ( AllegroGraphConnection sv, String name, AGInternals[] parts, boolean supersede)
    	throws AllegroGraphException {
    	ags = sv; storeName = name;   storeDirectory = null;
    	int[] iparts = new int[parts.length];
    	for (int i = 0; i < iparts.length; i++) {
    		if ( (parts[i].ags)!=sv )
    			throw new AllegroGraphException
    			( "Component of federated triple store must be opened on same connection.");
    		if ( 0>parts[i].tsx )
    			throw new AllegroGraphException
    			( "Components of federated triple store must be open triple stores.");
			iparts[i] = parts[i].tsx;
		}
    	tsx = verifyEnabled().federate(name, iparts, supersede);
    	ags.addTS(this);
    	this.initNamespaces();
    }

    protected AllegroGraph ( AllegroGraphConnection sv, String name, String directory )
		throws AllegroGraphException {
    	ags = sv; storeName = name;   storeDirectory = directory;
    	Object[] ra = verifyEnabled().findStore(name, directory, null);
    	if ( null==ra ) throw new AllegroGraphException("Could not find triple store.");
    	tsx = ((Integer)ra[0]).intValue();
    	storeName = (String) ra[1];
    	storeDirectory = (String) ra[2];
    	ags.addTS(this);
    }
    
    /**
     * Create an unconnected AllegroGraph instance.
     * This constructor is useful if attributes need to be set before the
     * triple store is opened or created.
     * @see #setAttribute(String, Object)
     * @param name the pathname string for the triple store.
     */
    public AllegroGraph ( String name ) {
    	storeName = name; 
    }
    
    
    protected AllegroGraph ( AGInternals from, int ix, String name, String directory ) {
    	ags = from.ags; storeName = name;   storeDirectory = directory;
    	tsx = ix;
    	ags.addTS(this);
    }
    
    /**
     * Retrieve the components of a federated triple store.
     * @return null if this is not a federated triple store;
     *    otherwise, return an array of the component stores.
     *    The result array is composed of new AllegroGraph instances, 
     *    one for each component.
     * @throws AllegroGraphException
     */
    public AllegroGraph[] getStores () throws AllegroGraphException {
    	Object[] r = verifyEnabled().getStores(this);
    	if ( null==r ) return null;
    	AllegroGraph[] ra = new AllegroGraph[(r.length)/3];
    	for (int i = 0; i < ra.length; i++) {
			ra[i] = new AllegroGraph(this, ((Integer)r[i*3]).intValue(),
								(String)r[1+i*3], (String)r[2+i*3]);
		}
    	return ra;
    }
    
//    /**
//     * Retrieve a named component of a federated triple store.
//     * @param name the name of the component.
//     * @return null if this is not a federated triple store, 
//     *     or if the component is not found;
//     *     otherwise, return a new AllegroGraph instance.
//     * @throws AllegroGraphException
//     */
//    public AllegroGraph getStore ( String name )
//		throws AllegroGraphException {
//    	return getStore(name, null);
//    }	
    
//    /**
//     * Retrieve a named component of a federated triple store.
//     * @param name the name of the component.
//     * @param directory a string containing the directory path of the 
//     *    desired component.  This argument is needed if the name
//     *    is ambiguous.
//     * @return null if this is not a federated triple store, 
//     *     or if the component is not found;
//     *     otherwise, return a new AllegroGraph instance.
//     * @throws AllegroGraphException
//     */
//    public AllegroGraph getStore ( String name, String directory )
//    	throws AllegroGraphException {
//    	if ( null==name ) throw new IllegalArgumentException("Name must be non-null.");
//    	Object[] r = verifyEnabled().getStores(this, name, directory);
//    	if ( null==r ) return null;
//    	return new AllegroGraph(this, ((Integer)r[0]).intValue(),
//				(String)r[1], (String)r[2]);
//    }
    
    /**
	 * Query the synchronization mode for this AllegroGraph instance.
	 * @return true if this AllegroGraph instance is configured to synchronize
	 * automatically  after every update call from Java.  Return false if it the
	 * responsibility of the application to synchronize.
	 */
	public boolean getSyncEveryTime () { return sync; }
	
	/**
	 * Set the synchronization mode for this AllegroGraph instance.
	 * By default, each addStatement() or addStatements() call
	 * triggers a synchronization to 
	 * update the persistent store.  Some applications can save time by 
	 * synchronizing explicitly at suitable times.
	 * 
	 * @param s if true, the triple store will be synchronized after every
	 *    update call from Java.  If false, the application must call 
	 *    {@link #syncTripleStore(AllegroGraph)} every now and then
	 *    in order to force a persistent store update.
	 *    
	 *  NOTE: If the synchronization mode is false, and the application
	 *  terminates abruptly, any updates since the last synchronization
	 *  call will be lost.
	 */
	public void setSyncEveryTime ( boolean s ) { sync = s; } 

    
	/**
	 * Display some details about the AllegroGraph instance.
	 */
	public String toString() {
		return getClass().getName() + "<" + tsx + " " + storeName + ">";
	}
	
	private SNAExtension sna = null;
	
	/**
	 * Get the SNAExtension instance of this AllegroGraph instance.
	 * @return the {@link SNAExtension} instance
	 */
	public SNAExtension getSNAExtension () {
		if ( null==sna ) sna = new SNAExtension(this);
		return sna;
	}
	
	private GeoExtension geo = null;
	
	/**
	 * Get the GeoExtension instance of this AllegroGraph instance.
	 * @return the {@link GeoExtension} instance
	 */
	public GeoExtension getGeoExtension () {
		if ( null==geo ) geo = new GeoExtension(this);
		return geo;
	}
	
	private Object ntripleContext(Object c) throws AllegroGraphException {
		if ( c instanceof String && "source".equalsIgnoreCase((String)c) )
			return "source";
		return anyContextRef(c, 1);
	}
	
	/**
	 * Query the state of SPOGI caching in this triple store.
	 * @return true if caching is enabled.
	 * @throws AllegroGraphException
	 */
	public boolean isSPOGICacheEnabled () throws AllegroGraphException {
		Object[] v = verifyEnabled().clientOption(this, "spogi-cache", "state", -1);
		if ( v==null ) return false;
		if ( 0==v.length ) return false;
		return 1==AGConnector.longValue(v[0]);
	}
	
	/**
	 * Modify the state of SPOGI caching in this triple store.
	 * @param onoff if true, enable caching; if false, disable.
	 * @throws AllegroGraphException
	 * <p><i>Note:</i>  This state is visible to and affects all applications
	 *  and all users connected to this triple store. 
	 */
	public void setSPOGICacheEnabled ( boolean onoff ) throws AllegroGraphException {
		verifyEnabled().clientOption(this, "spogi-cache", "state", onoff?1:0);
	}

	/**
	 * Query the size of the SPOGI cach on this triple store.
	 * @return the size of the cache.
	 * @throws AllegroGraphException
	 */
	public long getSPOGICacheSize () throws AllegroGraphException {
		Object[] v = verifyEnabled().clientOption(this, "spogi-cache", "size", -1);
		return AGConnector.longValue(v[0]);
	}
	
	/**
	 * Modify the size of the SPOGI cach on this triple store.
	 * @param size must be positive.
	 * @throws AllegroGraphException
	 * <p><i>Note:</i>  This value is visible to and affects all applications
	 *  and all users connected to this triple store. 
	 */
	public void setSPOGICacheSize ( long size ) throws AllegroGraphException {
		verifyEnabled().clientOption(this, "spogi-cache", "size", size);
	}
	
	
	/**
	 * Query the directory where AllegroGraph
	 * will search for UPI maps for this triple-store.
	 * @return a string containing the pathname of the directory, or null if 
	 *    this property is not set.
	 * @throws AllegroGraphException
	 */
	public String getUPIMapDirectory () throws AllegroGraphException {
		Object[] r = verifyEnabled().clientOption(this, "upi-map");
		if ( r==null ) return null;
		if ( 1>r.length ) return null;
		return (String)r[0];
	}
	
	/**
	 * Specify a directory where AllegroGraph
	 * will search for UPI maps for this triple-store.
	 * A non-null value can be used to allow maps to be created for a read-only
	 * triple store.
	 * 
	 * @param dir a string containing the pathname of the directory where the maps 
	 *     will be stored, or null to look in the triple store directory.
	 * @throws AllegroGraphException
	 */
	public void setUPIMapDirectory ( String dir ) throws AllegroGraphException {
		verifyEnabled().clientOption(this, "upi-map", "directory", dir);
	}
	
}

