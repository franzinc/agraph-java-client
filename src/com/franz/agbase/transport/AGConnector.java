
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

/*
 * Created on Jun 29, 2006
 *
 */
package com.franz.agbase.transport;

import java.io.IOException;

import com.franz.agbase.AllegroGraphException;
import com.franz.ag.UPI;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.impl.UPIImpl;
import com.franz.agbase.util.AGBase;
import com.franz.agbase.util.AGC;

/**
 * Instances of the concrete subclasses are only created by createConnectot().
 * Instances are never re-used; caller discards the instance after calling disable().
 * 
 * @author mm
 *
 * 
 */
public abstract class AGConnector extends AGC {
	
	boolean connected = false;

	int port = 3456;

	int jport = -3457;
	
	String host = "";

	int pollCount = 3;

	int pollInterval = 1000;
	int timeout = 5000;
	int debug = 0;
	
	int currentServerLevel = -1; 
	Object[] serverVersions = null;
	
	public abstract String getVersion () throws AllegroGraphException;

	public void init(int port, int port2, String host, int pollCount, int pollInterval,
			int debug, int timeout) {
		this.port = port;
		if ( port2<0 ) this.jport = port2;
		else if ( port2>0 ) this.jport = -port2;
		this.host = host;
		this.pollCount = pollCount;
		this.pollInterval = pollInterval;
		this.debug = debug;
		this.timeout = timeout;
	}
	
	public static AGConnector createConnector ( String mode ) {
		if ( mode==null ) mode = "";
		if ( "direct".equalsIgnoreCase(mode) ) 
			return new AGDirectConnector();
		// mode must be "jlinker"
		// return new AGJLinkerConnector();
		throw new IllegalStateException("Unknown mode: " + mode);	
	}

	static void copy ( long[] a, int[] b ) {
		for (int i=0; i<a.length; i++) b[i] = (int)(a[i]);
	}
	
	static void copy ( int[] a, int[] b ) {
		for (int i=0; i<a.length; i++) b[i] = a[i];
	}
	
	static void copy ( String[] a, String[] b ) {
		for (int i=0; i<a.length; i++) b[i] = a[i];
	}
	
	static void testIndex ( AGBase ag ) {
		if ( ag.tsx<0 )
			throw new IllegalStateException
				("Attempting to access closed AllegroGraph database.");
	}
	
	
	//ALL DEFS BELOW used to be interface AGCallInterface

	//abstract void init(int port, int jport, String host, int pollCount, int pollInterval);
	//abstract void init(int port, int jport, String host, int pollCount, int pollInterval, boolean debug);

	/**
	 * Query the version number of the transport layer.
	 */
	public abstract int transportVersion ();
	
	/**
	 * Enable a connection to the AllegroGraph server.
	 * 
	 * @throws IOException 
	 *             if a connection cannot be made.
	 * 
	 */
	public abstract void enable() throws IOException;

	/**
	 * Disable the connection to the AllegroGraph server.
	 * <p>
	 * All databases accessed through this connection become obsolete in the
	 * Java application. The persistend data may be closed if the server is
	 * still running.
	 */
	public abstract void disable();

	/**
	 * Query the state of a connection to the AllegroGraph server.
	 * 
	 * @return 1 if busy, 0 if idle, -1 if not connected
	 */
	public abstract int query();

	abstract int access(String createCode, String name, String dir)
			throws AllegroGraphException;
	
	public abstract int access(String createCode, String name, String dir, Object[] more)
		throws AllegroGraphException;

	public abstract boolean exists(String name, String directory) throws AllegroGraphException;


	/**
	 * Sync a database.
	 */
	public abstract void syncTripleStore(AGBase ag) throws AllegroGraphException;

	/**
	 * Close a database.
	 */
	public abstract boolean closeTripleStore(AGBase ag, boolean doClose) throws AllegroGraphException;

	/**
	 * Load a file of triple declarations.
	 * 
	 * @param name a pathname string, or an array of strings.
	 *           The file(s) must be in the ntriples format
	 *            expected by the AllegroGraph server.
	 * @param context a string in ntriples format, a Value instance, or a UPI instance
	 *        that specifies the default context for the new triples.
	 *        Null specifies the default context of the triple store.
	 * @param from a string containing ntriples definitions.  If specified,
	 *        the name argument is ignored.
	 * @param place If given, and an Object array of at least one element,
	 *        the default context of the operation is stored there.
	 * @param save null for server default, Boolean(true) to request save-string-literals,
	 *        Boolean(false) to suppress save-string-literals.
	 * @param ext null or the name of an external-format in the server.
	 * @return the number of triples added to the triple store.
	 */
	public abstract long loadNTriples(AGBase ag, Object name, Object context,
			String from, Object[] place, Boolean save, String ext)
			throws AllegroGraphException;
	
	public abstract long loadNTriples(AGBase ag, Object name, Object context,
			String from, Object[] place, Boolean save, String ext, boolean unzip)
			throws AllegroGraphException;


	/**
	 * Parse an XML file of RDF triple definitions and add the triples to the
	 * database.
	 * 
	 * @param filePath
	 *            A string that points to the file. The file must be in the RDF
	 *            format expected by the AllegroGraph server.
	 */
	public abstract long loadRDF(AGBase ag, Object filePath, Object context, 
			String baseURI, Object useRapper) throws AllegroGraphException;
	
	public abstract long loadRDF(AGBase ag, Object filePath, Object context, 
			String baseURI, Object useRapper, String from, Boolean save) throws AllegroGraphException;

	/**
	 * Query the number of triples in the database.
	 * 
	 * @return the number of triples in the database.
	 */
	public abstract long numberOfTriples(AGBase ag) throws AllegroGraphException;

	/**
	 * Index new triples.
	 */
	public abstract void indexTriples(AGBase ag, boolean wait) throws AllegroGraphException;

	/**
	 * Index all the triples in the database.
	 */
	public abstract void indexAll(AGBase ag, boolean wait) throws AllegroGraphException;
	
	/**
	 * Request indexing operation
	 * @param ag the store
	 * @param wait if true, wait until done
	 * @param all if true, index all triples, if false, index new triples,
	 *    if null merge small index chunks
	 * @throws AllegroGraphException
	 */
	public abstract void indexStore ( AGBase ag, boolean wait, Boolean all ) throws AllegroGraphException;

	/**
	 * Retrieve a Cursor instance for the matches to the arguments.
	 * 
	 * @param s A String or UPI specifying a subject;
	 *            null or "" denote a wild card.
	 * @param p A String or UPI specifying a predicate;
	 *            null or "" denote a wild card.
	 * @param o A String or UPI specifying an object; 
	 *            null or "" denote a wild card.
	 * @param c A String or UPI specifying a context;
	 *            null denotes a wild card, "" denotes the null context.
	 * @param lh An integer specifying a look-ahead value.
	 * @return a Cursor instance or null if there are no matches at all. All the
	 *         string arguments must be in ntriples syntax.
	 *         <p>
	 *         The look-ahead value specifies how many triples should be
	 *         returned immediately from Lisp to Java. A zero denotes the
	 *         default defined in the Cursor class.
	 */
	public abstract TriplesIterator getTriples(AllegroGraph ag, Object s, Object p, Object o, Object c, int lh)
			throws AllegroGraphException;
	
	public abstract TriplesIterator getTriples(AllegroGraph ag, Object s, Object p, Object o, Object c,
			Object subend, Object predend, Object obend, Object cxend, int lh)
		throws AllegroGraphException;
	
	public abstract Object getTriples(AllegroGraph ag, Object s, Object p, Object o, Object c,
			Object subend, Object predend, Object obend, Object cxend,
			int lh, Object[] options)
		throws AllegroGraphException;
	
	public abstract TriplesIterator getInfTriples(AllegroGraph ag, Object s, Object p, Object o, Object c, int lh, boolean infer)
		throws AllegroGraphException;
	
	public abstract TriplesIterator getInfTriples(AllegroGraph ag, Object s, Object p, Object o, Object c,
			Object subend, Object predend, Object obend, Object cxend, int lh, boolean infer)
			throws AllegroGraphException;

	public abstract TriplesIterator getFreetextStatements ( AllegroGraph ag, String pattern, int lh )
		throws AllegroGraphException;

	public abstract boolean hasTriple(AGBase ag, Object s, Object p, Object o, Object c )
		throws AllegroGraphException;
	
	public abstract boolean hasInfTriple(AGBase ag, Object s, Object p, Object o, Object c, boolean infer )
		throws AllegroGraphException;

	/**
	 * Add a new triple to the database.
	 * 
	 * @param s A String or UPI specifying a subject.
	 * @param p A String or UPI specifying a predicate.
	 * @param o A String or UPI specifying an object.
	 * @param c A String or UPI specifying a context, or null.
	 * @return the array of 5 values { triple_id, subject_UPI, predicate_UPI,
	 *                                 object_UPI, context_UPI }
	 */
	public abstract Object[] addTriple(AGBase ag, Object s, Object p, Object o, Object c )
			throws AllegroGraphException;

	/**
	 * Add new triples to the database.
	 * 
	 * @param s An array of UPI or String instances, or UPI or String
	 * @param p An array of UPI or String instances, or UPI or String
	 * @param o An array of UPI or String instances, or UPI or String
	 * @param c An array of UPI or String instances, or UPI or String
	 * @return an array of 5 arrays.
	 *    The first array is triple id numbers.
	 *    The second array is subject node UPIs.
	 *    The third array is predicate node UPIs.
	 *    The fourth array is object node UPIs.
	 *    The fifth array is context node UPIs.
	 *    All the string arguments must be
	 *         in ntriples syntax.
	 */
	public abstract Object[] addTriples(AGBase ag, Object s, Object p, Object o, Object c )
			throws AllegroGraphException;

	public abstract Object[] getParts(AGBase ag, UPI id) throws AllegroGraphException;
	
	public abstract void getParts(AGBase ag, UPI[] ids, int[] types, String[] vals,
			String[] mods) throws AllegroGraphException;

	public abstract String getTextPart(AGBase ag, UPI id) throws AllegroGraphException;

	public abstract String getTypePart(AGBase ag, UPI id) throws AllegroGraphException;

	public abstract String getLangPart(AGBase ag, UPI id) throws AllegroGraphException;
	
	/**
	 * 
	 * @param ag
	 * @param id
	 * @return null if not a triple id, otherwise array of subject, predicate, object ids
	 * @throws AllegroGraphException 
	 */
	public abstract UPIImpl[] getTripleParts(AGBase ag, long id) throws AllegroGraphException;

	/**
	 * Create a new blank Node.
	 * 
	 * @return a Node instance.
	 */
	public abstract UPIImpl newBlankNode(AGBase ag) throws AllegroGraphException;

	/**
	 * Create a new blank Node.
	 * 
	 * @param name
	 *            a string to replace the default id string.
	 * @return a UPI instance for the new Blank Node.
	 */
	public abstract UPIImpl newBlankNode(AGBase ag, String name)
			throws AllegroGraphException;
	
	public abstract UPIImpl[] newBlankNodes(AGBase ag, int n) throws AllegroGraphException;

	/**
	 * Create a Node instance from a URI string.
	 * 
	 * @param uri
	 *            A URI string.
	 * @return a Node instance.
	 */
	public abstract UPIImpl newResource(AGBase ag, String uri) throws AllegroGraphException;
	
	public abstract UPIImpl[] newResources(AGBase ag, String[] uri) throws AllegroGraphException;

	/**
	 * Allocate a literal id for a string.
	 * 
	 * @param text
	 *            A string.
	 * @return a Literal instance.
	 */
	public abstract UPIImpl newLiteral(AGBase ag, String text, String type, String lang)
			throws AllegroGraphException;
	
	public abstract UPIImpl newLiteral(AGBase ag, String text, UPI type, String lang)
	throws AllegroGraphException;

	public abstract UPIImpl[] newLiteral(AGBase ag, String[] text, String[] type, String[] lang)
	throws AllegroGraphException;

	/**
	 * Advance a Cursor instance.
	 * 
	 * @author mm
	 * 
	 *  
	 */
	public abstract UPIImpl[] nextCursor(AGBase ag, Object source, int lh)
			throws AllegroGraphException;
	
	/**
	 * Return two arrays: 
	 *     An array of long id numbers + more marker
	 *     An array of encoded triple part strings.
	 */
	public abstract Object[] nextCursorAndParts(AGBase ag, Object source, int lh)
		throws AllegroGraphException;	
	
	/**
	 * 
	 * @param ag
	 * @param source string token of :select-values cursor
	 * @param lh
	 * @return Same result as selectValues() --- 
	 *         Object[8]{ UPI[], int[] types, String[] labels, String[] mods,
	 *                   more, width, token, plimit }
	 * @throws AllegroGraphException
	 */
	public abstract Object[] nextValuesArray ( AGBase ag, Object source, int lh )
		throws AllegroGraphException;	
	
	
	public abstract void delete(AGBase ag, Object s, Object p, Object o, Object c, boolean wildOk )
		throws AllegroGraphException;
	
	public abstract void discardCursor(AGBase ag, Object ref)
		throws AllegroGraphException;
	
	public abstract void discardCursors(AGBase ag, Object[] ref)
		throws AllegroGraphException;
	
	public abstract TriplesIterator selectTriples(AllegroGraph ag, String query, Object preset, String pvar, 
			int limit, boolean infer, boolean distinct )
		throws AllegroGraphException;
	
	public abstract TriplesIterator selectTriples(AllegroGraph ag, String query, Object[] presets, String pvars, 
			int limit, boolean infer, boolean distinct )
		throws AllegroGraphException;
	
	/**
	 * 
	 * @param ag
	 * @param Query
	 * @param presets
	 * @param pvars
	 * @return Object[8]{ UPI[], int[] types, String[] labels, String[] mods,
	 *                   more, width, token, plimit }
	 * @throws AllegroGraphException
	 */
	public abstract Object[] selectValues( AGBase ag, String Query, Object[] presets, String pvars, boolean infer, boolean distinct )
		throws AllegroGraphException;
	
	public abstract Object[] selectValues( AGBase ag, String Query, Object[] presets, String pvars, boolean infer, boolean distinct, Object[] more )
	throws AllegroGraphException;
	
	public abstract Object[] getFreetextSubjects ( AGBase ag, String pattern, int limit )
		throws AllegroGraphException;
	
	public abstract String[] freetextPredicates ( AGBase ag, Object defs )
		throws AllegroGraphException;
	
	public abstract void serverTrace ( boolean onoff, String outFile ) 
		throws AllegroGraphException;
	
	public abstract boolean serverLevel ( int level) 
		throws AllegroGraphException;
	
	public abstract void serverTrace( AGBase ag, boolean onoff, String outFile ) 
		throws AllegroGraphException;
	
	public abstract Object[] evalInServer( AGBase ag, String expression )
		throws AllegroGraphException;
	
	public abstract Object[] evalInServer( String expression )
		throws AllegroGraphException;
	
	public abstract long serverId () throws AllegroGraphException;
	
	//abstract long clientId ( long id ) throws AllegroGraphException;
	
	public abstract int interruptServer ( long id ) throws AllegroGraphException;
	
	public abstract boolean twinqlAsk ( AGBase ag, String query, boolean infer )
		throws AllegroGraphException;
	public abstract boolean twinqlAsk ( AGBase ag, String query, boolean infer, Object[] more )
	throws AllegroGraphException;
	
	public abstract Object[] twinqlSelect ( AGBase ag, String query, String vars, int limit,
			int offset, int slimit, boolean infer )
		throws AllegroGraphException;
	public abstract Object[] twinqlSelect ( AGBase ag, String query, String vars, int limit,
			int offset, int slimit, boolean infer, Object[] more )
		throws AllegroGraphException;
	
	public abstract TriplesIterator twinqlFind ( AllegroGraph ag, String query, int limit, int offset, int slimit, boolean infer )
		throws AllegroGraphException;
	public abstract TriplesIterator twinqlFind ( AllegroGraph ag, String query, int limit, 
			int offset, int slimit, boolean infer, Object[] more )
	throws AllegroGraphException;
	
	public abstract TriplesIterator twinqlConstruct ( AllegroGraph ag, String query, int limit, 
			int offset, int slimit, boolean infer, Object[] more )
	throws AllegroGraphException;
	
	public abstract TriplesIterator twinqlDescribe ( AllegroGraph ag, String query, int limit, 
			int offset, int slimit, boolean infer, Object[] more )
	throws AllegroGraphException;
	
	public abstract String twinqlQuery ( AGBase ag, String query, String format, int limit, int offset, boolean infer )
		throws AllegroGraphException;
	public abstract String twinqlQuery ( AGBase ag, String query, String format, 
			int limit, int offset, boolean infer, Object[] more )
	throws AllegroGraphException;
	
	public abstract Object indexing ( AGBase ag, int mode, int value, String[] flavors ) 
		throws AllegroGraphException;
	
	public abstract Object mapping ( AGBase ag, int mode, String[] map )
		throws AllegroGraphException;
	
	public abstract String[] namespaces ( AGBase ag, String[] map )
	throws AllegroGraphException;
	
	public abstract Object[] addPart ( AGBase ag, String part )
	throws AllegroGraphException;

	public static int toInt ( Object v ) {
		if ( v instanceof Integer ) return ((Integer)v).intValue();
		else if ( v instanceof Long ) return ((Long)v).intValue();
		else if ( v instanceof Short ) return ((Short)v).intValue();
		else if ( v instanceof Byte ) return ((Byte)v).intValue();
		return 0;
	}

	public static long longValue ( Object x ){
		if ( x==null ) return 0;
		if ( x instanceof Long )    return ((Long)x).longValue();
		if ( x instanceof Integer ) return ((Integer)x).intValue();
		if ( x instanceof Short )   return ((Short)x).shortValue();
		if ( x instanceof Byte )    return ((Byte)x).byteValue();
		if ( (x instanceof UPIImpl) && !(((UPIImpl)x).withLabel()) )
			return ((UPIImpl)x).getCode();
		throw new IllegalArgumentException
		            ("Cannot get a long value from " + x);
	}
	
	public static boolean hasLongValue ( Object x ){
		if ( x==null ) return true;
		if ( x instanceof Long )    return true;
		if ( x instanceof Integer ) return true;
		if ( x instanceof Short )   return true;
		if ( x instanceof Byte )    return true;
		if ( (x instanceof UPIImpl) && !(((UPIImpl)x).withLabel()) )
			return true;
		return false;
	}
	
	
	public static double doubleValue ( Object x ) {
		if ( x==null ) return 0;
		if ( x instanceof Double ) return ((Double)x).doubleValue();
		if ( x instanceof Float ) return ((Float)x).doubleValue();
		throw new IllegalArgumentException
        	("Cannot get a double value from " + x);
	}
	
	// FEDERATION ADDITIONS
	
	static boolean isInteger(Object x) {
		if ( x==null ) return true;
		if ( x instanceof Long )    return true;
		if ( x instanceof Integer ) return true;
		if ( x instanceof Short )   return true;
		if ( x instanceof Byte )    return true;
		return false;
	}

	static boolean canBeUPI(Object x) {
		if ( x==null ) return false;
		if ( isInteger(x) ) return true;
		if ( x instanceof UPIImpl ) return true;
		return false;
	}

	protected static UPIImpl toUPI(Object x) {
		if ( x==null ) return null;
		if ( isInteger(x) ) return new UPIImpl(longValue(x));
		if ( x instanceof UPIImpl ) return (UPIImpl)x;
		throw new IllegalArgumentException
	    	("Cannot get a UPI value from " + x);
	}

	public static UPIImpl[] toUPIArray(Object x) {
		if ( x==null ) return new UPIImpl[0];
		if ( canBeUPI(x) ) return new UPIImpl[] { toUPI(x) };
		//System.out.println("
		if ( x instanceof UPIImpl[] ) return (UPIImpl[])x;
		if ( x.getClass().isArray() ) {
			int ln = java.lang.reflect.Array.getLength(x);
			UPIImpl[] r = new UPIImpl[ln];
			for (int i=0; i<ln; i++) {
				Object e = java.lang.reflect.Array.get(x, i);
				if ( e==null ) r[i] = null;
				else r[i] = toUPI(e);
			}
			return r;
		}
		throw new IllegalArgumentException
			("Cannot get a UPI[] value from " + x);
	}
	
	public static int[] intArray(Object x) {
		if (x==null ) return new int[0];	
		if ( x instanceof int[] ) return (int[])x;
		if ( x instanceof long[] ) return copyToIntArray((long[])x);
		if ( x instanceof short[] ) return copyToIntArray((short[])x);
		if ( x instanceof byte[] ) return copyToIntArray((byte[])x);
		if ( x instanceof Object[] ) return copyToIntArray((Object[])x);
		throw new IllegalArgumentException
	    ("Cannot get a int[] value from " + x);
	}

	public static long[] longArray(Object x) {
		if (x==null ) return new long[0];	
		if ( x instanceof long[] ) return (long[])x;
		if ( x instanceof int[] ) return copyToLongArray((int[])x);
		if ( x instanceof short[] ) return copyToLongArray((short[])x);
		if ( x instanceof byte[] ) return copyToLongArray((byte[])x);
		if ( x instanceof Object[] ) return copyToLongArray((Object[])x);
		throw new IllegalArgumentException
	    ("Cannot get a long[] value from " + x);
	}

	static int[] copyToIntArray(long[] x) {
		if ( x==null ) return new int[0];
		int[] y = new int[x.length];
		for (int i=0; i<x.length; i++) y[i] = (int) x[i];
		return y;
	}
	
	static int[] copyToIntArray(short[] x) {
		if ( x==null ) return new int[0];
		int[] y = new int[x.length];
		for (int i=0; i<x.length; i++) y[i] = x[i];
		return y;
	}
	
	static int[] copyToIntArray(byte[] x) {
		if ( x==null ) return new int[0];
		int[] y = new int[x.length];
		for (int i=0; i<x.length; i++) y[i] = x[i];
		return y;
	}
	
	static long[] copyToLongArray(int[] x) {
		if ( x==null ) return new long[0];
		long[] y = new long[x.length];
		for (int i=0; i<x.length; i++) y[i] = x[i];
		return y;
	}

	static long[] copyToLongArray(short[] x) {
		if ( x==null ) return new long[0];
		long[] y = new long[x.length];
		for (int i=0; i<x.length; i++) y[i] = x[i];
		return y;
	}

	static long[] copyToLongArray(byte[] x) {
		if ( x==null ) return new long[0];
		long[] y = new long[x.length];
		for (int i=0; i<x.length; i++) y[i] = x[i];
		return y;
	}

	static long[] copyToLongArray(Object[] x) {
		if ( x==null ) return new long[0];
		long[] y = new long[x.length];
		for (int i=0; i<x.length; i++) y[i] = AGConnector.longValue(x[i]);
		return y;
	}
	
	static int[] copyToIntArray(Object[] x) {
		if ( x==null ) return new int[0];
		int[] y = new int[x.length];
		for (int i=0; i<x.length; i++) y[i] = (int) AGConnector.longValue(x[i]);
		return y;
	}

	public abstract int federate ( String name, int[] parts, boolean supersede )
	throws AllegroGraphException;
	
	/**
	 * 
	 * @param ag AlegroGraph instance
	 * @return null if not a federated store, or 
	 *     array of 3*n elements { index, name, dir, ... }
	 * @throws AllegroGraphException
	 */
	public abstract Object[] getStores ( AGBase ag )
	throws AllegroGraphException;
	
	/**
	 * 
	 * @param name of desired store
	 * @param directory null or dir to disambiguate name
	 * @param delete the string "delete" to request deletion
	 * @return array of 3 elements { index, name, dir }
	 * @throws AllegroGraphException
	 */
	public abstract Object[] findStore ( String name, String directory, Object delete )
	throws AllegroGraphException;
	
	public abstract Object serializeTriples ( AGBase ag, Object[] args )
	throws AllegroGraphException;
	
	public abstract Object[] applyAGFn ( AGBase ag, String fn, Object[] args )
	throws AllegroGraphException;
	
	public abstract Object[] applyFn ( String fn, Object[] args )
	throws AllegroGraphException;
	
	/**
	 * Send one request and get back a single value.
	 * @param args name of option, value
	 * @return one value
	 * @throws AllegroGraphException
	 */
	public abstract Object serverOptionOne ( Object... args ) throws AllegroGraphException;
	
	/**
	 * Send more request and get all the values.
	 * @param args name of option, value, ...
	 * @return an array of values
	 * @throws AllegroGraphException
	 */
	public abstract Object serverOptionAll ( Object... args ) throws AllegroGraphException;
	
	/**
	 * Send more arguments and get all the values.
	 * @param name
	 * @param val the first argument
	 * @param more additional arguments
	 * @return an array of values
	 * @throws AllegroGraphException
	 */
	public abstract Object[] clientOption ( AGBase ag, Object... more )
	throws AllegroGraphException;
	
	/**
	 * Manager timers on the server.
	 * @param ag if null, aplies to cumulative timers for all stores.
	 *           If not null, applies only to calls on this instance.
	 * @param onoff 
	 * @param perStore
	 * @throws AllegroGraphException
	 */
	public abstract void  serverTimers ( AGBase ag, boolean onoff, boolean perStore )
	throws AllegroGraphException;
	
	/**
	 * Get the timers from the server and reset them to zero.
	 * @param ag if null, get the cumulative timers.  Otherwise, get the timers for the
	 *     specified store.
	 * @return an array of 5 numbers:
	 *     <ul>The total real time spent in server calls.
	 *     <li>The number of server calls included.
	 *     <li>The total real time spent in triple store operations.
	 *     <li>The total cpu time spent in server calls.
	 *     <li>The total cpu time spent in triple store operations.
	 *     <li>
	 *     </ul>
	 *     All times are in milliseconds.  Only selected triple store operations 
	 *     are measured.
	 * @throws AllegroGraphException
	 */
	public abstract long[] getTimers ( AGBase ag )
	throws AllegroGraphException;
}
