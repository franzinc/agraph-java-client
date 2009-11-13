
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

import java.io.IOException;
import java.util.prefs.Preferences;

import com.franz.agbase.impl.TriplesIteratorImpl;
import com.franz.agbase.transport.AGConnector;
import com.franz.agbase.util.AGBase;
import com.franz.agbase.util.AGC;
import com.franz.agbase.util.AGConnInternals;

/**
 * This class implements access to AllegroGraph triple stores through a
 * server interface.
 * <p>
 * One instance of this class can support access to several open
 * triple stores, each with its own AllegroGraph instance.  A Java
 * application may connect to multiple servers by creating multiple
 * instances of this class.
 * @author mm
 *
 */
public class AllegroGraphConnection extends AGConnInternals {
	

	/**
	 * Query the current initial namespace registry.
	 * This namespace registry is copied into every new AllegroGraphConnection instance.
	 * @return the current instance.
	 */
	public static NamespaceRegistry getInitialNamespaceRegistry() {
		return initialns.promote();
	}
	
	/**
	 * Set the current initial namespace registry.
	 * This namespace registry is copied into every new AllegroGraphConnection instance.
	 * @param ns the instance to set.  If this is set to null, then there is no
	 *   default namespace registry.
	 *   <p>
	 *   The initial value is NamespaceRegistry.RDFandOwl.
	 */
	public static void setInitialNamespaceRegistry ( NamespaceRegistry ns ) {
		initialns = new NamespaceRegistry(ns);
	}
	
	/**
	 * Retrieve the static default value of the poll count parameter.
	 * The built-in initial value is 1.
	 * @return an integer that determines the number of connection attempts
	 */
	public static int getDefaultPollCount () { return defaultPollCount; }
	
	/**
	 * Retrieve the static default value of the poll interval parameter.
	 * The built-in initial value is 500.
	 * @return an integer number that determines the interval (in milliseconds) between 
	 *     attempts to connect
	 * 
	 */
	public static int getDefaultPollInterval () { return defaultPollInterval; }
	
	/**
	 * Retrieve the current value of the poll count parameter.
	 * @return an integer that determines the number of connection attempts
	 */
	public int getPollCount () { return pollCount; }
	
	/**
	 * Retrieve the current value of the poll interval parameter.
	 * @return an integer that determines the interval between 
	 *     attempts to connect
	 * 
	 */
	public int getPollInterval () { return pollInterval; }
	
	/**
	 * Set the static default values of the poll count and poll interval 
	 * parameters.
	 * @param count an integer that determines the number of connection attempts
	 * @param interval an integer that determines the interval between 
	 *     attempts to connect
	 *     
	 *     <p>A positive value greater than zero is used to set the
	 *     corresponding parameter.  If a zero or negative value is
	 *     specified, the corresponding parameter is unchanged.
	 *    <p>The initial static defaults are 2 for poll count and 500
	 *    for poll interval.  
	 *    
	 */
	public static void setDefaultPolling ( int count, int interval ) {
		if ( count>0 ) defaultPollCount = count;
		if ( interval>0 ) defaultPollInterval = interval;
	}
	
	/**
	 * Set the current values of the poll count and poll interval 
	 * parameters.
	 * @param count an integer that determines the number of connection attempts
	 * @param interval an integer that determines the interval between 
	 *     attempts to connect
	 *     
	 *     <p>A positive value greater than zero is used to set the
	 *     corresponding parameter.
	 *     <p> If a zero value is specified, set the current value of the 
	 *     parameter from the static default value.
	 *     <p>If a negative value is
	 *     specified, the corresponding parameter is unchanged.
	 *     
	 *     <p>The initial values are set from the static default values.
	 *     <p>If an application encounters timeout 
	 *    exceptions when attempting a connection one or both of these
	 *    parameters should be given a larger value. 
	 */
	public void setPolling ( int count, int interval ) {
		if ( !(count<0) ) pollCount = ( count>0 )?count:defaultPollCount;
		if ( !(interval<0) ) pollInterval = ( interval>0 )?interval:defaultPollInterval;
	}
	
	
	protected AGConnector getAgc ( AllegroGraphConnection ags ) {
		return ags.agc;
	}

	
	/**
	 * Retrieve the static default connection mode. 
	 * @return The string "direct".
	 */
	public static String getDefaultMode () { return defaultMode; }
	
	/**
	 * Retrieve the connection mode. 
	 * @return The string "direct" or "jlinker".
	 */
	public String getMode () { return mode; }
	
	/**
	 * Set the static default connection mode to use direct socket 
	 * connection for communicating with the AllegroGraph server.
	 * If the static default is not set, the built-in default is to use
	 * the direct mode.
	 * <p>
	 * Direct mode is optimized for fast transfer of information between a Java
	 * application and the AllegroGraph server.
	 * <p>
	 * <strong>The mode setting in the Java application must
	 * match the mode setting in the AllegroGraph server. </strong>
	 */
//	public static void setDefaultDirect() {
//		defaultMode = "direct";
//	}
	
	/**
	 * Set the connection mode to use direct socket 
	 * connection for communicating with the AllegroGraph server.
	 * If the connection mode is not set explicitly, the static default
	 * connection mode is used.
	 * <p>
	 * Direct mode is optimized for fast transfer of information between a Java
	 * application and the AllegroGraph server.
	 * <p>
	 * <strong>The mode setting in the Java application must
	 * match the mode setting in the AllegroGraph server. </strong>
	 */
//	public void setDirect() {
//		mode = "direct";
//	}

	/**
	 * Set the static default connection mode to use ACL Jlinker for 
	 * communicating with the AllegroGraph server.
	 * If the static default is not set, the built-in default is to use
	 * the direct mode.
	 * <p>
	 * In Jlinker mode, object Ids are limited to Java int values instead of
	 * Java long values.
	 * <p>
	 * Jlinker mode is somewhat slower than Direct mode, but allows calls from
	 * Lisp to Java. This may be useful in some applications and in debugging
	 * situations.
	 * <p>
	 * <strong>The mode setting in the Java application must
	 * match the mode setting in the AllegroGraph server. </strong>
	 */
//	public static void setDefaultJLinker() {
//		defaultMode = "jlinker";
//	}
	
	/**
	 * Set the connection mode to use ACL Jlinker for 
	 * communicating with the AllegroGraph server.
	 * If the connection mode is not set explicitly, the static default
	 * connection mode is used.
	 * <p>
	 * In Jlinker mode, object Ids are limited to Java int values instead of
	 * Java long values.
	 * <p>
	 * Jlinker mode is somewhat slower than Direct mode, but allows calls from
	 * Lisp to Java. This may be useful in some applications and in debugging
	 * situations.
	 * <p>
	 * <strong>The mode setting in the Java application must
	 * match the mode setting in the AllegroGraph server. </strong>
	 */
//	public void setJLinker() {
//		mode = "jlinker";
//	}

	/**
	 * Retrieve the static default primary port number. 
	 * The built-in initial value is 4567.
	 * @return The default primary port number.
	 * 
	 */
	public static int getDefaultPort() { return defaultPort; }
	
	/**
	 * Set the static default primary port number. 
	 * If the static default primary port number is not set
	 * explicitly, the built-in initial value is 4567.
	 * @param port A port number.
	 */
	public static void setDefaultPort(int port) {
		defaultPort = port;
	}
	
	/**
	 * Retrieve the primary port number. 
	 * @return The primary port number.
	 *    The built-in initial value is 4567.
	 */
	public int getPort() { return port; }
	
	/**
	 * Set the primary port number. 
	 * If the primary port number is not set explicitly, the static default
	 * is used.
	 * @param p A port number.
	 */
	public void setPort(int p) {
		port = p;
	}

	/**
	 * Retrieve the static default secondary port number. 
	 * The built-in initial value is 4568.
	 * @return The default secondary port number.
	 *    
	 */
	public static int getDefaultPort2() { return defaultPort2; }
	
	/**
	 * Set the static default secondary port number. 
	 * If the static default secondary port number is not set
	 * explicitly, the built-in initial value is 4568.
	 * @param port A port number.
	 */
	public static void setDefaultPort2(int port) {
		defaultPort2 = port;
	}
	
	/**
	 * Retrieve the secondary port number. 
	 * @return The default secondary port number.
	 *    The built-in initial value is 4568.
	 */
	public int getPort2() { return port2; }
	
	/**
	 * Set the secondary port number. 
	 * If the secondary port number is not set explicitly, the static default
	 * is used.
	 * @param p A port number.
	 */
	public void setPort2(int p) {
		port2 = p;
	}

	/**
	 * Retrieve the static default host name. 
	 * The built-in initial value is "" which denotes "localhost".
	 * @return The default host name.
	 *    
	 */
	public static String getDefaultHost() { return defaultHost; }
	
	/**
	 * Set the static default host name. 
	 * If the static default host name is not set explicitly, 
	 * the built-in initial value is "" which denotes "localhost".
	 * @param host A host name string.  The empty string denotes the
	 *     loopback host "localhost".
	 */
	public static void setDefaultHost(String host) {
		defaultHost = host;
	}
	
	/**
	 * Retrieve the host name. 
	 * @return The host name used for a connection to the AllegroGraph server.
	 *    The built-in initial value is "" which denotes "localhost".
	 */
	public String getHost() { return host; }
	
	/**
	 * Set the host name. 
	 * If the host name is not set explicitly, 
	 * the static default host name is used.
	 * @param newhost A host name string.  The empty string denotes the
	 *     loopback host "localhost".
	 */
	public void setHost(String newhost) {
		host = newhost;
	}
	
	/**
	 * Retrieve the static default debug flag. 
	 * The built-in default value is zero.
	 * @return The default debug flag.
	 */
	public static int getDefaultDebug() { return defaultDebug; }
	
	/**
	 * Set the static default debug flag. 
	 * If the static default debug flag is not set explicitly,
	 * the built-in initial value is zero.
	 * @param db When true, new instances of AllegroGraphConnection are
	 *    created in debug mode and produce additional progress messages.
	 *    A zero value suppresses all output. Values 1 to 4 produce
	 *    increasing amounts of output.
	 */
	public static void setDefaultDebug ( int db ) {
		defaultDebug = db;
	}
	
	/**
	 * Retrieve the debug flag. 
	 * @return The debug flag.
	 *    The built-in initial value is zero.
	 */
	public int getDebug() { return debug; }
	
	/**
	 * Set the debug flag. 
	 * If the debug flag is not set explicitly,
	 * the static default debug flag is used.
	 * @param db When >0, new instances of AllegroGraphConnection are
	 *    created in debug mode and produce additional progress messages.
	 */
	public void setDebug ( int db ) {
		debug = db;
	}
	
	/**
	 * Retrieve the static default command used to start an AllegroGraph
	 * server.
	 * The built-in default value is the empty string.
	 * @return a command string.  An empty string indicates that the command
	 *     is unknown and hence not usable.
	 *  
	 */
	public static String getDefaultCommand () { return defaultLispCommand; }
	
	/**
	 * Set the static default command used to start an AllegroGraph
	 * server.
	 *
	 * @param cmd A command string that will start the AllegroGraph
	 * server process in the local machine.
	 *
	 * <p>
	 * If the static default command is not set explicitly,
	 * it is looked up in the Java System property "com.franz.ag.exec".
	 * If it set to " ", the string containing one space. then
	 * a value is obtained from
	 *   <ul>
	 *     <li>from a User Preference
	 *     <li>from a system Preference.
	 *    </ul>
	 *       The Preference values may be initialized once per
	 *  installation by calling the main() method of this class.  The 
	 *  Preferences values are saved in the registry on Windows machines
	 *  but are not reliable on some Linux or Unix machines.
	 */
	public static void setDefaultCommand ( String cmd ) {
		if ( cmd==null )
			throw new IllegalArgumentException("command cannot be null");
		defaultLispCommand = cmd;
	}
	
	/**
	 * Retrieve the command used to start an AllegroGraph
	 * server.
	 * 
	 * @return a command string.  An empty string indicates that the command
	 *     is unknown and hence not usable.
	 *  
	 */
	public String getCommand () { return lispCommand; }
	
	/**
	 * Set the  command used to start an AllegroGraph
	 * server.
	 * If the command is not set explicitly, the static default command is used.
	 * @param cmd A command string that will start the AllegroGraph
	 * server process in the local machine.
	 */
	public void setCommand ( String cmd ) {
		if ( cmd==null )
			throw new IllegalArgumentException("command cannot be null");
		lispCommand = cmd;
	}
	
	/**
	 * Retrieve the static default value of the Server Keep flag.
	 * The built-in initial value is false.
	 * @return true true or false
	 * 
	 */
	public static boolean getDefaultServerKeep () { return defaultServerKeep; }
	
	/**
	 * Set the static default value of the Server Keep flag.
	 * <p>
	 * The Server Keep flag determines the behavior when an instance
	 * of AllegroGraphConnection is discarded.
	 * If the instance called startServer() and the  Server Keep flag
	 * is false, then the server process is terminated.  Otherwise
	 * the server process is left running.
	 * @param v
	 */
	public void setDefaultServerKeep ( boolean v ) { defaultServerKeep = v; }
	
	/**
	 * Retrieve the value of the Server Keep flag.
	 * @return true or false
	 * @see #getDefaultServerKeep()
	 */
	public boolean getServerKeep () { return keep; }
	
	/**
	 * Set the value of the Server Keep flag.
	 * If the flag is not set explicitly, the static default value
	 * is used.
	 * <p>
	 * The Server Keep flag determines the behavior when an instance
	 * of AllegroGraphConnection is discarded.
	 * If the instance called startServer() and the  Server Keep flag
	 * is false, then the server process is terminated.  Otherwise
	 * the server process is left running.
	 * @param v
	 */
	public void setServerKeep ( boolean v ) { keep = v; }
	
	
	/**
	 * Create an instance of a connection to an AllegroGraph Server. 
	 * The instance
	 * is a placeholder for parameters until the enable() method
	 * is called.
	 *
	 */
	public AllegroGraphConnection() {
		super();
	}
	
	/**
	 * Create an instance of a connection to an AllegroGraph Server.
	 * The new instance
	 * has copies of all the user settable value.
	 * Any per-client properties in the server will be shared among connections
	 * created in this way.
	 * @param a
	 */
	// this does not make much sense at this time...
//	public AllegroGraphConnection ( AllegroGraphConnection a ) {
//		super();
//		clientId = a.clientId;
//		port = a.port;
//		host = a.host;
//		geoSubs = a.geoSubs;
//		keep = a.keep;
//		nsregs = a.nsregs;
//		pollCount = a.pollCount;
//		pollInterval = a.pollInterval;
//		port2 = a.port2;
//		timeout = a.timeout;
//	}


	/**
	 * 
	 * Enable the interface by connecting to an AllegroGraph server.
	 * The connection is determined by the following values:
	 * <ul>
	 *      <li>The integer port number where the AllegroGraph server is
	 *            listening is queried with getPort() and modified with 
	 *            setPort().
	 *      <li> The name of the host where the AllegroGraph server is
	 *            listening is queried with getHost() and modified with 
	 *            setHost(). This can normally be "localhost" or "".
	 *      <li> The connection mode is queried with getMode() and
	 *           modified with setDirect() or setJLinker().
	 *      <li> The integer port number for the second socket in the
	 *            AllegroGraph/Java connection. Some implementations of the
	 *            AllegroGraph server use two sockets. This argument is ignored
	 *            if the server uses only one socket.
	 *  </ul>
	 * @throws IOException
	 *             if the interface cannot be enabled.
	 *  <p>
	 *  Only one instance with mode "jlinker" can be enabled at any one time
	 *  in each running Java application.
	 *  <ul>
	 *     <li>Direct Mode Exceptions:
	 *         <ul>
	 *         <li>"Too many connections." -- The AllegroGraph Version 1.2
	 *                  server allows only one connection at a time.
	 *         <li>"Connection rejected." -- The AllegrGraph server 
	 *                  application filter function rejected the connection.
	 *         <li>"Connected but timed out."  -- The expected server
	 *                  acknowledgment byte did not arrive in the expected
	 *                  time interval.
	 *         <li>"Unexpected initial reply "  -- A byte arrived from the
	 *                  server but it was unexpected.
	 *         <li>other socket error
	 *         </ul>
	 *      <li>JLinker Mode Exceptions:
	 *          <ul>
	 *          <li>"Only one enabled JLinker instance allowed."
	 *          <li>"Connection failed with null explanation."
	 *          <li>"Connection failed with empty explanation."
	 *          <li>other jlinker error
	 *          <li>other socket error
	 *          </ul>
	 * 
	 * 
	 * @return true if the connection was not enabled and now is.
	 *     Return false if the connection was already enabled.
	 * @throws AllegroGraphException
	 */
	public boolean enable() throws IOException, AllegroGraphException {
		if ( agc==null )
			agc = AGConnector.createConnector(mode);
		else if (-1<agc.query())
			return false;
		agc.init(port, port2, host, pollCount, pollInterval, debug, timeout);
		agc.enable();
		if ( debug>0 ) serverTrace(true);
		
		Object[] v = null;  int db = 0; Object w = null;
		try { w = agc.serverOptionOne("client-debug", "");
		} catch (Exception e) {}
		if ( v!=null ) db = AGConnector.toInt(w);
		
		int l = 0;
		try { w = agc.serverOptionOne("client-batch", "");
		} catch (Exception e) {}
		if ( w!=null ) l = AGConnector.toInt(w);
		if ( l>0 ) TriplesIteratorImpl.defaultLookAhead = l;
		
		int pr = 0;
		try { w = agc.serverOptionOne("server-level", 
									// Send the expected server-level value.
									9);
		} catch (Exception e) {}
		if ( w!=null ) pr = AGConnector.toInt(w);
		if ( pr<AGC.AGU_PROTOCOL_LEVEL )
			throw new AllegroGraphException
			   ("AllegroGraph server is out of date: " + pr + "<" + AGC.AGU_PROTOCOL_LEVEL);
		else if ( pr>AGC.AGU_PROTOCOL_LEVEL )
			throw new AllegroGraphException
			   ("Library agraph.jar is out of date: " + pr + ">" + AGC.AGU_PROTOCOL_LEVEL);
		
		try { agc.serverOptionOne("client-level", AGC.AGU_PROTOCOL_LEVEL);
		} catch (Exception e) {}
		
		debugMask = db | 0x40000000;
		serverId = agc.serverId();
		if ( serverId<101 ) 
			throw new AllegroGraphException
			   ("AllegroGraph server is out of date serverId: " + serverId);
		
		// this does not make much sense without a better client id def
		// clientId = agc.clientId(clientId);
		freshState();
		return true;
	}
	
//	long clientId = -100;
	
	/**
	 * Attempt to interrupt an operation in the AllegroGraph server.
	 * @return the integer 1 if an operation was actually interrupted;
	 *    0 if the connection specified by the argument was idle;  
	 *    or a negative integer
	 *    if the operation failed in some way because the connection
	 *    was not in a running or idle state.
	 * @throws AllegroGraphException
	 * @throws IOException
	 */
	public int interrupt ( )
		throws AllegroGraphException, IOException {
		long id = serverId;
		if ( id<0 ) return -10;
		if ( id==0 ) return -9;
		if ( id<101 ) return -8;
		if ( !isEnabled() ) return -7;
		int r = 0;
		if ( isBusy() )
		{
			AllegroGraphConnection temp = new AllegroGraphConnection();
			temp.setHost(getHost());
			temp.setPort(getPort());
			temp.enable();
			r = temp.agc.interruptServer(id);
			temp.disable(false);
		}
		return r;
	}
	
	/**
	 * Disable the interface. All remote references to AllegroGraph data are
	 * invalidated by this call. Any open triple stores are closed.
	 */
	public void disable() {
		disable(true);
	}

	/**
	 * Disable the interface.
	 * 
	 * @param close
	 *            If true, close any open triple stores before disabling the
	 *            interface.
	 *            <p>
	 *            All remote references to AllegroGraph data are invalidated by
	 *            this call.
	 *            <p>
	 *            If called with a false arguments, any open triple stores will
	 *            remain open for a while, at most until the AllegroGraph server
	 *            is shut down.  This option should be used only if the 
	 *            application is such that any open AllegroGraph instances will
	 *            never be used again.
	 * 
	 */
	public synchronized void disable(boolean close)  {
		if ( agc==null ) return;    // already disabled [bug18223]
		Object[] all = arrayOfTS();
		if (close)
			for (int i = 0; i < all.length; i++) {

				AGBase s = (AGBase) (all[i]);
				if (!(s.tsx < 0))
					try {
						agc.closeTripleStore(s, true);
						s.tsx = -1;
					} catch (Exception e) {
					}
			}
		if ( (agc!=null) && (-1<agc.query()) ) agc.disable();
		reset();
	}

	protected void finalize () throws AllegroGraphException, IOException {
		disable(true);
		if ( !keep ) stopServer();
	}
	

	/**
	 * Query the state of the AllegroGraph/Java connection.
	 * 
	 * @return true if the interface is enabled.
	 */
	public boolean isEnabled() {
		if (agc == null)
			return false;
		return -1<agc.query();
	}
	
	/**
	 * Query the state of the AllegroGraph/Java connection.
	 * 
	 * @return true if the interface is enabled and busy.
	 */
	public boolean isBusy() {
		if (agc == null)
			return false;
		return 0<agc.query();
	}

	/**
	 * Create an AllegroGraph instance that accesses a new
	 * empty AllegroGraph triple store.
	 * <p>
	 * triple stores are created with the methods access(), create(), open(), 
	 *  renew(), and replace().
	 *  <p> 
	 * @param name the name of the triple store or a pathname string that specifies 
	 *    the name and location of the triple store.  If the name argument is a
	 *    pathname string then the directory argument must be null.
	 * @param directory the directory where the data base does or will reside.
	 *    If this argument is a non-null string, then the name argument must
	 *    be a simple name.
	 *  
	 * @throws AllegroGraphException if the triple store exists already,
	 *   or if the new triple store cannot be created for any other reason.
	 */
	public AllegroGraph create(String name, String directory)
			throws AllegroGraphException {
		return new AllegroGraph(this, "create", name, directory);
	}
	
	/**
	 * Connect an AllegroGraph instance to a triple store.
	 * The triple store is created where one did not exist.
	 * <p>
	 * This form is useful when additional attributes must be set 
	 * with {@link AllegroGraph#setAttribute(String, Object)}
	 * before connecting to the triple store. 
	 * @param ag An unconnected AllegroGraph instance.
	 * @return the AllegroGraph instance argument, updated and connected.
	 * @throws AllegroGraphException
	 */
	public AllegroGraph create ( AllegroGraph ag ) throws AllegroGraphException {
		ag.connect(this, "create");
		return ag;
	}

	/**
	 * Create an instance that accesses an existing AllegroGraph triple store.
	 * <p>
	 * triple stores are created with the methods access(), create(), open(), 
	 *  renew(), and replace().
	 *  <p> 
	 * @param name the name of the triple store or a pathname string that specifies 
	 *    the name and location of the triple store.  If the name argument is a
	 *    pathname string then the directory argument must be null.
	 * @param directory the directory where the data base does or will reside.
	 *    If this argument is a non-null string, then the name argument must
	 *    be a simple name.
	 *  
	 * @throws AllegroGraphException if the triple store does not exist
	 *    or if it cannot be opened for any other reason.
	 */
	public AllegroGraph open(String name, String directory)
			throws AllegroGraphException {
		return new AllegroGraph(this, "open", name, directory);
	}
	
	/**
	 * Connect an AllegroGraph instance to a triple store.
	 * The triple store must exists; connect to it.
	 * <p>
	 * This form is useful when additional attributes must be set 
	 * with {@link AllegroGraph#setAttribute(String, Object)}
	 * before connecting to the triple store. 
	 * @param ag An unconnected AllegroGraph instance.
	 * @return the AllegroGraph instance argument, updated and connected.
	 * @throws AllegroGraphException
	 */
	public AllegroGraph open ( AllegroGraph ag ) throws AllegroGraphException {
		ag.connect(this, "open");
		return ag;
	}
	
	
	
	
	/**
	 * Determine if an AllegroGraph triple store exists.
	 * 
	 * @param name the name of the triple store or a pathname string that specifies 
	 *    the name and location of the triple store.  If the name argument is a
	 *    pathname string then the directory argument must be null.
	 * @param directory the directory where the data base does or will reside.
	 *    If this argument is a non-null string, then the name argument must
	 *    be a simple name.
	 * @return true if the triple store exist, false otherwise.
	 * @throws AllegroGraphException
	 *             if the call fails for some reason.
	 *             <p>
	 *             If this method returns true, then creating an AllegroGraph
	 *             instance with the open() method will succeed.
	 */
	public boolean exists(String name, String directory)
			throws AllegroGraphException {
		return getServer().exists(name, directory);
	}

	/**
	 * Create an instance that accesses an existing AllegroGraph triple store
	 *   or a freshly created new empty triple store.
	 *   If the triple store exists, it is opened. If the triple store does not exist,
	 *   it is created.
	 *   <p>
	 * Triple stores are created with the methods access(), create(), open(), 
	 *  renew(), and replace().
	 *  <p> 
	 * @param name the name of the triple store or a pathname string that specifies 
	 *    the name and location of the triple store.  If the name argument is a
	 *    pathname string then the directory argument must be null.
	 * @param directory the directory where the data base does or will reside.
	 *    If this argument is a non-null string, then the name argument must
	 *    be a simple name.
	 *          
	 * @throws AllegroGraphException if the triple store cannot be opened or created.
	 */
	public AllegroGraph access(String name, String directory)
			throws AllegroGraphException {
		return new AllegroGraph(this, "access", name, directory);
	}
	
	/**
	 * Connect an AllegroGraph instance to a triple store.
	 * If the triple store exists, connect to it;  if it does not
	 * exist, create it.
	 * <p>
	 * This form is useful when additional attributes must be set 
	 * with {@link AllegroGraph#setAttribute(String, Object)}
	 * before connecting to the triple store. 
	 * @param ag An unconnected AllegroGraph instance.
	 * @return the AllegroGraph instance argument, updated and connected.
	 * @throws AllegroGraphException
	 */
	public AllegroGraph access ( AllegroGraph ag ) throws AllegroGraphException {
		ag.connect(this, "access");
		return ag;
	}

	/**
	 * Create an instance that accesses an AllegroGraph triple store.  
	 * If the triple store exists, it is deleted and replaced with a new
	 * empty triple store.
	 * <p>
	 * Triple stores are created with the methods access(), create(), open(), 
	 *  renew(), and replace().
	 *  <p> 
	 * @param name the name of the triple store or a pathname string that specifies 
	 *    the name and location of the triple store.  If the name argument is a
	 *    pathname string then the directory argument must be null.
	 * @param directory the directory where the data base does or will reside.
	 *    If this argument is a non-null string, then the name argument must
	 *    be a simple name.
	 *            <p>
	 *            If the operation implies the deletion of a triple store, the
	 *            entire directory is deleted.
	 * @throws AllegroGraphException if the triple store cannot be accessed or
	 *   created.
	 */
	public AllegroGraph renew(String name, String directory)
			throws AllegroGraphException {
		return new AllegroGraph(this, "renew", name, directory);
	}
	
	/**
	 * Connect an AllegroGraph instance to a triple store.
	 * If the triple store exists, delete it and replace it
	 * with a new empty one;  if it does not
	 * exist, create it.
	 * <p>
	 * This form is useful when additional attributes must be set 
	 * with {@link AllegroGraph#setAttribute(String, Object)}
	 * before connecting to the triple store. 
	 * @param ag An unconnected AllegroGraph instance.
	 * @return the AllegroGraph instance argument, updated and connected.
	 * @throws AllegroGraphException
	 */
	public AllegroGraph renew ( AllegroGraph ag ) throws AllegroGraphException {
		ag.connect(this, "renew");
		return ag;
	}

	/**
	 * Create an instance that accesses an AllegroGraph triple store.
	 * The triple store must already exist, and it will
	 *            be replace with a new empty one.
	 *  <p>
	 * Triple stores are created with the methods access(), create(), open(), 
	 *  renew(), and replace().
	 *  <p> 
	 * @param name the name of the triple store or a pathname string that specifies 
	 *    the name and location of the triple store.  If the name argument is a
	 *    pathname string then the directory argument must be null.
	 * @param directory the directory where the data base does or will reside.
	 *    If this argument is a non-null string, then the name argument must
	 *    be a simple name.
	 *            <p>
	 *            Since the operation implies the deletion of a triple store, the
	 *            entire directory is deleted.
	 * @throws AllegroGraphException if the triple store does not exist,
	 *   or if it cannot be replaced for any reason.
	 */
	public AllegroGraph replace(String name, String directory)
			throws AllegroGraphException {
		return new AllegroGraph(this, "replace", name, directory);

	}
	
	/**
	 * Connect an AllegroGraph instance to a triple store.
	 * The triple store must exist; delete it and replace it with a new empty one.
	 * <p>
	 * This form is useful when additional attributes must be set 
	 * with {@link AllegroGraph#setAttribute(String, Object)}
	 * before connecting to the triple store. 
	 * @param ag An unconnected AllegroGraph instance.
	 * @return the AllegroGraph instance argument, updated and connected.
	 * @throws AllegroGraphException
	 */
	public AllegroGraph replace ( AllegroGraph ag ) throws AllegroGraphException {
		ag.connect(this, "replace");
		return ag;
	}
	
	/**
	 * Query the indexing chunk size in the AllegroGraph server.
	 * 
	 * @return the indexing chunk size.
	 * @throws AllegroGraphException
	 */
	public long getChunkSize() throws AllegroGraphException {
		Object w = getServer().serverOptionOne("chunk-size", -1);
		if ( w!=null ) return AGConnector.longValue(w);
		throw new AllegroGraphException("Cannot get chunk size.");
	}
	
	/**
	 * Set the indexing chunk size in the AllegroGraph server.
	 * See the discussion of chunk size at AllegroGraph.indexTriples().
	 * @param s the desired chunk size
	 * <p>
	 * The built-in default value is 1,048,576 (2 to the 20th) on 32-bit platforms 
	 * and 2,097,152 (2 to the 21st) on 64-bit platforms. 
	 * <p>Note that this is a global value that affects all the 
	 * triple stores in a server.
	 * @throws AllegroGraphException
	 */
	public void setChunkSize( long s ) throws AllegroGraphException {
		if ( s<1 ) throw new IllegalArgumentException( "ChunkSize must be positive.");
		getServer().serverOptionOne("chunk-size", s);
	}
	
	/**
	 * Query the indexing chunk count warning limit in the AllegroGraph server.
	 * 
	 * @return the indexing chunk count warning limit.
	 * @throws AllegroGraphException
	 */
	public long getChunkCountWarningLimit() throws AllegroGraphException {
		Object w = getServer().serverOptionOne("chunk-limit", -1);
		if ( w!=null ) return AGConnector.longValue(w);
		throw new AllegroGraphException("Cannot get chunk count warning limit.");
	}
	
	/**
	 * Set the indexing chunk count warning limit in the AllegroGraph server.
	 * See the discussion of chunk size at AllegroGraph.indexTriples().
	 * @param s the desired chunk size
	 * <p>
	 * The built-in default value is 500. 
	 * <p>Note that this is a global value that affects all the 
	 * triple stores in a server.
	 * @throws AllegroGraphException
	 */
	public void setChunkCountWarningLimit( long s ) throws AllegroGraphException {
		if ( s<1 ) throw new IllegalArgumentException( "Chunk count warning limit must be positive.");
		getServer().serverOptionOne("chunk-limit", s);
	}
	
	/**
	 * Get the current server default value for the expected number of
	 * unique resources in a new triple store.
	 *   
	 * @return the current value of this server parameter.
	 * @throws AllegroGraphException
	 */
	public long getDefaultExpectedResources() throws AllegroGraphException {
		Object w = getServer().serverOptionOne("expected", -1);
		if ( w!=null ) return AGConnector.longValue(w);
		throw new AllegroGraphException("Cannot get expected resource number.");
	}
	
	/**
	 * Set the current server default value for the expected number of
	 * unique resources in a new triple store.
	 * The new value will affect any triple stores created after this call.
	 * This number is the expected number of distinct URIs and literals
	 * in the triple store.  
	 * <p>
	 * If the number is too small, performance may suffer during
	 * triple store creation.  A rough rule of thumb is to specify a number
	 * that is one third of the number of triples.
	 * The built-in default is 100000.
	 * @param s the new value.
	 * @throws AllegroGraphException
	 */
	public void setDefaultExpectedResources( long s ) throws AllegroGraphException {
		if ( s<1 ) throw new IllegalArgumentException( "ExpectedResources must be positive.");
		getServer().serverOptionOne("expected", s);		
	}
	
	
	/**
	 * Evaluate a Lisp expression in the AllegroGraph server.
	 * The expression is evaluated in the default environment
	 * associated with this AllegroGraphConnection instance.
	 * @param expression A string containing a well-formed Lisp expression.
	 * @return An array of Object instances representing the 
	 *    values computed by the call.  The
	 *    values returned by the call must be coercible to simple Java types:
	 *    number, string, vector of numbers, vector of strings, UPI instance,
	 *    vector of UPIs, or null.
	 * 
	 * @throws AllegroGraphException
	 * * <p>
	 * <strong>Usage:</strong>
	 * <p>
	 * This method allows a Java application to run programs in 
	 * the AllegroGraph server.  Using this method can change the state
	 * of the server and the content of triple stores.
	 * It is provided as a diagnostic aid, and as a tool for
	 * advanced users.
	 * <p>
	 * By default, this method is disabled in the server.  It must be
	 * enabled by starting the server with a file of permitted expression
	 * patterns.  See the server reference for details.
	 * <p>
	 * Most uses of this method will require some knowledge of the
	 * Lisp programming language, and an understanding of the 
	 * AllegroGraph implementation.
	 * <p>
	 *  The evaluation environment contains
	 *  bindings for *package*, *readtable*.  Each AllegroGraphConnection
	 *  instance defines a distinct default environment.
	 *  <p>
	 *  The initial value of *package* is the db.agraph.user package.
	 *  It uses :cl, :excl, :db.agraph, and :prolog.  If the value of
	 *  *package* is modified, the change persists for subsequent calls
	 *  to evalInServer on the same connection.
	 *  <p>
	 *  The initial value of *readtable* is a copy of the standard Common
	 *  Lisp readtable.  If the value of *readtable* is modified, the
	 *  change persists for subsequent calls to evalInServer on the same connection.
	 *  <p>
	 *  Side-effects on the environment persist as long as the connection
	 *  is maintained.
	 *  <p>
	 *  If a Java application needs to mention symbols in a fixed application
	 *  package (maybe mentioned in a source file), it may be advisable
	 *  to evaluate an in-package form in the environment.
	 *  <p>
	 *  The called code must not touch any triple stores that may be accessed by this or by other
	 *  client applications.  If the code needs to access a triple store,
	 *  the AllegroGraph.evalInServer method must be called on a suitable instance.
	 *  <p>
	 *  The variable *db* is bound if AllegroGraph.evalInServer is called
	 *  but if modified, the change will not persist.
	 *  
	 */
	public Object[] evalInServer ( String expression ) throws AllegroGraphException {
		return getServer().evalInServer(expression);
	}

	
	
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
     * The trace state is global for all connections to the server.
     * @return -1 if the call failed for some reason, 0 otherwise.
     */
    public int serverTrace ( boolean onoff ) {
    	try {
			getServer().serverTrace(onoff, null);
			return 0;
		} catch (AllegroGraphException e) {
			return -1;
		}
    }
    
    /**
     * Query the implementation level of the AllegroGraph server.
     * @param level A positive integer.
     * @return True if the server is at or above the specified level.
     * @throws AllegroGraphException
     */
    public boolean serverLevel ( int level ) throws AllegroGraphException {
    	return getServer().serverLevel(level);
    }
    
    /**
     * Query version information from the AllegroGraph server
     * @return
     * @throws AllegroGraphException
     */
    public String getServerVersion () throws AllegroGraphException {
    	return getServer().getVersion();
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
     * The trace state is global for all connections to the server.
     * @return -1 if the call failed for some reason, 0 otherwise.
     */
    public int serverTrace ( String outFile ) {
    	try {
			getServer().serverTrace(true, outFile);
			return 0;
		} catch (AllegroGraphException e) {
			return -1;
		}
    }
    
    /**
     * Manage cumulative timers for all clients.
     * @param onOff
     * @param perStore if true, collect per-store times for each active store instance
     *     in addition to cumulative times. 
     * @throws AllegroGraphException
     */
    public void serverTimers ( boolean onOff, boolean perStore ) throws AllegroGraphException {
    	getServer().serverTimers(null, onOff, perStore);
    }
    
    /**
     * Get the cumulative timers from the server and reset them.
     * @return An array of 5 numbers.
     * @throws AllegroGraphException
     */
    public long[] getTimers () throws AllegroGraphException {
    	return getServer().getTimers(null);
    }
    
    /**
     * Query the namespace definitions in the AllegroGraph server.
     * There is one set of namespace definitions in the AllegroGraph server.
     * The definitions are copied to each triple store created from this
     * connection instance.  Therefore, changes to this table 
     * do not affect any triple stores that are already open. 
     * The definitions are not persistent.
     * @return An array of strings.  The even numbered elements are prefixes
     *    and the following odd numbered element is the full namespace text.
     */
    public String[] getNamespaces () {
    	if ( nsregs==null ) return null;
    	return nsregs.toArray();
    }
    
    public NamespaceRegistry getNamespaceRegistry () {
    	if ( null==nsregs ) return null;
    	return new NamespaceRegistry(nsregs);
    }
    
    
    
    private NamespaceRegistry nsregsInit() {
    	if ( nsregs!=null ) return nsregs;
    	if ( initialns==null ) throw new IllegalStateException
    		("The default NamespaceRegistry is suppressed in" + 
    			" this instance of AllegroGraphConnection");
    	nsregs = new NamespaceRegistry(initialns);
    	return nsregs;
    }
    
    /**
     * Register one namespace definition.
     * The definition is added to the registry used to initialize new
     * AllegroGraph instances.
     * The definition overrides an existing definition.
     * @param prefix the prefix
     * @param full The full namespace text.  If the full text is null 
     *    or "", then the prefix is removed from the table.
     * @throws AllegroGraphException
     */
    public void registerNamespace ( String prefix, String full ) {
    	nsregsInit().register(prefix, full);
    }
    
    /**
     * Register several namespace definitions.
     * The definitions are added to the current table and override
     * any existing definitions.
     * @param defs An array of strings.  The even numbered elements are prefixes
     *    and the following odd numbered element is the full namespace text.
     *    If the full text is null or "", then the prefix is removed
     *    from the table.
     */
    public void registerNamespaces ( String[] defs ) {
    	nsregsInit();
    	for (int i = 0; i < defs.length; i=i+2) {
    		nsregs.register(defs[i], defs[i+1]);
		}
    }
    
 // Declared in package com.franz.ag to allow dual AG APIs
    public void registerNamespaces ( com.franz.ag.NamespaceRegistry ns ) {
    	nsregsInit().register(ns);
    }
    
    /**
     * Register several namespace definitions.
     * The definitions replace any definitions that were in place.
     * @param ns the source of the definitions.
     */
    public void setNamespaceRegistry ( NamespaceRegistry ns ) {
    	if ( ns==null ) 
    		nsregs = null;
    	else
    		nsregs = new NamespaceRegistry(ns);
    }
    
    
	
	/**
	 * Display some details about the server instance.
	 */
	public String toString() {
		return getClass().getName() + "<" + serverId + " " + mode + ">";
	}
	

	
	


	private Process agp;

	/**
	 * Start a system process running an AllegroGraph server.
	 * Equivalent to <code>startServer(null)</code>.
	 * @throws IOException
	 */
	public void startServer () throws IOException { 
		startServer(null); 
		}
	

	/**
	 * Start a system process running an AllegroGraph server.
	 * 
	 * @param log The name of a log file or null.  If non-null, the AG server
	 *           writes a log to this file in the home directory.
	 * @throws IOException from Runtime.exec().
	 * 
	 * <p>
	 * If the command was specified with {@link #setCommand(String)}
	 * or with {@link #setDefaultCommand(String)} then the command
	 * specified by the program is used.
	 * <p>
	 * If the command is specified as "" or " " and the system property 
	 * "com.franz.ag.exec" is set then the property value is used.
	 * <p>
	 * If the command is specified " " then system and user Preferences
	 * are examined.  The Preferences values may be set by executing the main()
	 * method in this class.
	 * 
	 */
	public void startServer( String log ) throws IOException
	{
		if ( lispCommand==null ) lispCommand = "";
		
		String c = System.getProperty("com.franz.ag.exec", lispCommand);
		if ( "".equals(lispCommand) )
			lispCommand = c;
		if ( " ".equals(lispCommand) )
		{
			Preferences p, ag;
			if ( c.equals("") )
			{
				p = Preferences.userRoot();
				ag = p.node(agPrefPath);
				c = ag.get(agLispKey, "");
			}
			if ( c.equals("") )
			{
				p = Preferences.systemRoot();
				ag = p.node(agPrefPath);
				c = ag.get(agLispKey, "");
			}
			defaultLispCommand = c;
			lispCommand = c;
		}
		if ( agp!=null ) throw new IllegalStateException("Already started");
        if ( null==lispCommand || "".equals(lispCommand) || " ".equals(lispCommand) )
        	throw new IllegalStateException("ACL command must be set");
        String options1 = "", options2 = "";  
        if ( log!=null ) { options1 = "--log-file"; options2 = log; }
//		ProcessBuilder agb = new ProcessBuilder(
//										lispCommand										
//										);
//		agb.directory(home);
//		agp = agb.start();
//        if ( (home!=null) && (0<home.length()) )
//        	throw new UnsupportedOperationException( "StartServer does not support home argument.");
        
        String[] cmd = new String[]{
        		lispCommand, 
        		"--port", ""+port, options1, options2,
        };
        if ( debug>0 ) {
        	String m = "";
        	for (int i = 0; i < cmd.length; i++) {
				m = m + " " + cmd[i];
			}
        	System.out.println( "startServer command:" + m );	
        }
       
        agp = Runtime.getRuntime().exec(cmd);
        try { Thread.sleep(1000);  // Give it time to get started...
		} catch (InterruptedException e) {	}
	}
	
	/**
	 * Stop an AllegroGraph server process that was 
	 * started by this Java application.
	 * @return 0 if all went smoothly, -1 if there was no process to
	 *     terminate, or a positive number of error responses.
	 * @throws IOException
	 */
	public int stopServer ( ) throws IOException {
		return stopServer(false);
	}
	
	/**
	 * Stop an AllegroGraph server process.
	 * @param killNotStarted if false, stop the server if it was started
	 *     by this application;  if true, stop the server even if it was
	 *     started some other way.
	 * @return 0 if all went smoothly, -1 if there was no process to
	 *     terminate, or a positive number of error responses.
	 * @throws IOException
	 */
	public int stopServer ( boolean killNotStarted)
		throws IOException {
		int rc = 0;
		if ( debug>0 ) System.out.println( "stopServer: " + agp + " " + agc );
		if ( agp==null ) 
		{
			if ( !killNotStarted ) return -1;
		}	
		if ( agc==null )
		{
			// try to connect to make a clean exit
			try { 
				if ( debug>0 )
					System.out.println( "stopServer calling enable");
				boolean en = enable(); 
				if ( debug>0 )
					System.out.println( "stopServer enable returned " + en );
			}
			catch (Exception e) {
				if ( debug>0 ) System.out.println( "connect " + e );
				agc = null; rc++;
				}
		}
		if ( agc!=null ) {

			if ( 0>agc.query() )
			{
				if ( debug>0 )
					System.out.println( "stopServer calling re-enable");
				agc.enable();
			}
			if ( -1<agc.query() ) 
			{
				if ( debug>0 )
					System.out.println( "stopServer calling stop-agj-application");
				String r = ""; Object rv;
				try {
					rv = agc.serverOptionOne("stop-app", "");
				} catch (Exception e) {
					rv = "ERROR: " + e ; rc++;
				}
				if ( rv instanceof String ) r = (String)rv;
				else if ( null!=rv ) { r = "ERROR: odd type " + rv; rc++; }
				if ( debug>0 )
					System.out.println( "stopServer stop-agj-ap => " + r);
				if ( debug>0 )
					System.out.println( "stopServer calling disable");
				disable();
				try { Thread.sleep(3000); } catch (InterruptedException e) {}
			}
			else
			{
				if ( debug>0 )
					System.out.println( "stopServer re-enable failed");
			}
		}
		if (agp!=null ) 
		{
			if ( debug>0 )
				System.out.println( "stopServer calling destroy");
			agp.destroy();
			try { agp.waitFor(); } catch (InterruptedException e) {}
		}
		
		return rc;
	}
	
	
	/**
	 * Run this method to
	 * set the default AllegroGraph server command once per
	 * installation.
	 * 
	 * @param args  The arguments to the JVM invocation.
	 * 
	 *  <pre>
	 *  java AllegroGraphConnection [-system command] [-user command]
	 *  </pre>
	 *  If the <code>-system</code> argument is supplied, set the 
	 *  default for all users.
	 *  <p>
	 *  If the <code>-user</code> argument is supplied, set the 
	 *  default for the current user only.
	 *  <p>
	 *   The command argument is the absolute pathname to the AllegroGraphJavaServer
	 *   executable.
	 * 
	 */
	public static void main(String[] args) {
		String sys = "", usr = "";
		for (int i = 0; i < args.length; i++) {
			String string = args[i];
			if ( string.equalsIgnoreCase("-system") )
				sys = args[++i];
			else if ( string.equalsIgnoreCase("-user") )
				usr = args[++i];			
		}
		Preferences ps = Preferences.systemRoot();
		Preferences ags = ps.node(agPrefPath);
		String agcs = ags.get(agLispKey, "");
		String pms = "System value of " + agPrefPath + "/" + agLispKey;
		if ( agcs.equals("") )
			System.out.println( "Current " + pms + " is unset." );
		else
			System.out.println( "Current " + pms + " is " + agcs );
		Preferences pu = Preferences.userRoot();
		Preferences agu = pu.node(agPrefPath);
		String agcu = agu.get(agLispKey, "");
		String pmu = "User value of "+ agPrefPath + "/" + agLispKey;
		if ( agcu.equals("") )
			System.out.println( "Current " + pmu + " is unset." );
		else
			System.out.println( "Current " + pmu + " is " + agcu );
		
		if ( !sys.equals("") )
		{
			ags.put(agLispKey, sys);
			System.out.println( "Modified " + pms + " to " + sys );
		}
		if ( !usr.equals("") )
		{
			agu.put(agLispKey, usr);
			System.out.println( "Modified " + pmu + " to " + usr );
		}
	
	}
	


	// FEDERATION ADDITIONS

	/**
	 * Combine several open triple stores into one federated triple store.
	 * @param name A name for the federation.  It is best to use a unique name.
	 * @param parts The triple stores that will be federated.
	 *    All the compoent triple stores must be open and must be connected through 
	 *    the same connection.
	 * @param supersede When false, throw an exception if a triple store with the
	 *    same name is already open.  When true, and an open triple store with the
	 *    same name is found, then close the triple store before making the 
	 *    federation.  If the existing triple store was a federated store, it is
	 *    destroyed by closing it;  If the existing triple store is a persistent
	 *    store, then closing it simply makes it unavailable until reopened.
	 *    
	 *  <p>
	 *  <h4>The Default Graph in a Federated Triple Store</h4>
	 *  The default graph of a federated triple store is distinct from the default
	 *  graph of the component stores.  The default graph of any triple store is
	 *  distinct from the default graph of any other triple store.  As a result,
	 *  the default graph is not a useful concept in federated stores.
	 *  If an application needs to view some triples in several different
	 *  stores of a federation as members of the same graph, then the triples
	 *  must be created with a common graph marker.
	 */
	public AllegroGraph federate ( String name, AllegroGraph[] parts, boolean supersede )
	 throws AllegroGraphException {
		return new AllegroGraph(this, name, parts, supersede);
	}
	
	/**
	 * Find an open triple store with the given name.
	 * @param name The name of a triple store.  This can be a simple name 
	 *    or a pathname string.
	 * @return A new AllegroGraph instance for the triple store.
	 * @throws AllegroGraphException if the name is not found or ambiguous.
	 */
	public AllegroGraph findStore ( String name )
	throws AllegroGraphException  {
	return new AllegroGraph(this, name, (String)null);
	}
	
	/**
	 * Find an open triple store with the given name.
	 * @param name The name of a triple store.
	 * @param directory The directory where the triple store is filed.  This
	 *   argument can be used to identify one of several triple stores with
	 *   the same name.  If this argument is a non-null string, then the name
	 *   argument must be a simple name.
	 * @return A new AllegroGraph instance for the triple store.
	 * @throws AllegroGraphException  if the name is not found or ambiguous.
	 */
	public AllegroGraph findStore ( String name, String directory )
		throws AllegroGraphException  {
		return new AllegroGraph(this, name, directory);
	}
	
	/**
	 * Delete the triple store with the given name.
	 * @param name The name of a triple store.  This can be a simple name 
	 *    or a pathname string.
	 * @throws AllegroGraphException if the name is not found or ambiguous.
	 */
	public void deleteStore ( String name ) throws AllegroGraphException { deleteStore(name, null); }
	
	/**
	 * Delete the triple store with the given name.
	 * @param name The name of a triple store.
	 * @param directory The directory where the triple store is filed.  This
	 *   argument can be used to identify one of several triple stores with
	 *   the same name.  If this argument is a non-null string, then the name
	 *   argument must be a simple name.
	 * @throws AllegroGraphException  if the name is not found or ambiguous.
	 */
	public void deleteStore ( String name, String directory ) throws AllegroGraphException {
		getServer().findStore(name, directory, "delete");
	}

	/**
	 * Retrieve the static default value of the connection timeout.
	 * The built-in initial value is 5000. 
	 * @return the defaultTimeout in milliseconds.
	 * See also {@link #getTimeout()}.
	 */
	public static int getDefaultTimeout() {
		return defaultTimeout;
	}

	/**
	 * Set the default timeout value for connection attempts.
	 * @param defaultTimeout the defaultTimeout to set in milliseconds.
	 * See also {@link #setTimeout(int)}.
	 */
	public static void setDefaultTimeout(int defaultTimeout) {
		AllegroGraphConnection.defaultTimeout = defaultTimeout;
	}

	/**
	 * Retrieve the current timeout value for this connection.
	 * The default initial value is queried with {@link #getDefaultTimeout()}.
	 * The value is modified with {@link #setTimeout(int)}.
	 * @return the timeout in milliseconds.
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Set the timeout value for connection attempts.
	 * This value is used when connecting to a server.  If the server
	 * does not connect within this interval, the connection is aborted.
	 * The server must also send an identifying byte within 10 times
	 * this interval, or the connection is aborted.
	 * @param timeout the timeout to set in milliseconds.
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	/**
	 * Query the default size of the SPOGI cache in the current server.
	 * @return the default size of the SPOGI cache.
	 * @throws AllegroGraphException
	 */
	public long getDefaultSPOGICacheSize () throws AllegroGraphException {
		Object v = serverOption(this, "spogi-cache-size", -1);
		return AGConnector.longValue(v);
	}
	
	/**
	 * Modify the default size of the SPOGI cache in the current server.
	 * @param size must be positive.
	 * 
	 * <p><i>Note:</i>  The default size affects all triple stores created on this server
	 * by any application, and any user connected to this server.  
	 * This method may be removed in future versions with more strict access controls.
	 * @throws AllegroGraphException
	 */
	public void setDefaultSPOGICacheSize ( long size ) throws AllegroGraphException {
		serverOption(this, "spogi-cache-size", size);
	}
	
	/**
	 * Return a list of all the known Prolog functors.
	 * @return Each array element contains the name of one Prolog functor.
	 *    Each name is displayed relative to the :db.agraph.user package.
	 * @throws AllegroGraphException
	 */
	public String[] getPrologFunctors () throws AllegroGraphException {
		return (String[]) serverOption(this, "prolog", "functors");
	}
	
	/**
	 * Return a list of all the known Prolog functors that match a pattern.
	 * @param regExp a regular expression 
	 * @return Each array element contains the name of one Prolog functor
	 *    whose name matches the regExp.
	 *    Each name is displayed relative to the :db.agraph.user package.
	 * @throws AllegroGraphException
	 */
	public String[] getPrologFunctors ( String regExp ) throws AllegroGraphException {
		return (String[]) serverOption(this, "prolog", "functors", regExp);
	}
	
	/**
	 * Return a listing of all the known Prolog rules.
	 * This list may contain many obscure rules known only to the AllegroGrapg internals.
	 * @return Each array element contains the rules pertaining to one functor.
	 * @throws AllegroGraphException
	 */
	public String[] getPrologRules () throws AllegroGraphException {
		return (String[]) serverOption(this, "prolog", "rules");
	}
	
	/**
	 * Return a listing of the rules pertaining to one Prolog functor.
	 * @param functorName
	 * @return a listing of the rules pertaining to this functor
	 * @throws AllegroGraphException
	 */
	public String getPrologRules ( String functorName ) throws AllegroGraphException {
		return (String) serverOption(this, "prolog", "rules", functorName);
	}
	
	/**
	 * Return a listing of the rules pertaining to a set of Prolog functors.
	 * @param functorNames
	 * @return each array element is a listing of the rules pertaining to the corresponding functor.
	 *     If there are no rules, the array element will be null.
	 * @throws AllegroGraphException
	 */
	public String[] getPrologRules ( String[] functorNames ) throws AllegroGraphException {
		return (String[]) serverOption(this, "prolog", "rules", functorNames);
	}
	
	/**
	 * Add one Prolog rule to the server.
	 * @param ruleInString the text of one rule.
	 * @throws AllegroGraphException
	 * <strong>Note that Prolog rules are global to the AllegroGraph server and therefore
	 * affect all users simultaneously.
	 * </strong>
	 */
	public void addPrologRule(String ruleInString) throws AllegroGraphException {
		serverOption(this, "prolog", "add", ruleInString);
	}
	
	/**
	 * Add some Prolog rules to the server from a file visible to the server.
	 * The file can only contain Prolog rules, in-package forms and defpackage forms.
	 * @param nameOfFile the name of a file
	 * @throws AllegroGraphException
	 * <strong>Note that Prolog rules are global to the AllegroGraph server and therefore
	 * affect all users simultaneously.
	 * </strong>
	 */
	public void addPrologFile( String nameOfFile ) throws AllegroGraphException {
		serverOption(this, "prolog", "add", "file", nameOfFile);
	}
	
	/**
	 * Add some Prolog rules to the server from an array of rules.
	 * The array can only contain Prolog rules, in-package forms and defpackage forms.
	 * @param arrayOfRules
	 * @throws AllegroGraphException
	 * <strong>Note that Prolog rules are global to the AllegroGraph server and therefore
	 * affect all users simultaneously.
	 * </strong>
	 */
	public void addPrologRules ( String[] arrayOfRules ) throws AllegroGraphException {
		serverOption(this, "prolog", "add", arrayOfRules);
	}
	
	/**
	 * Return a string containing memory usage information from the server.
	 * @param to if null, return a string containing the information;
	 *    if the empty string, print the information to the server log file;
	 *    any other string should contain the pathname of a file where the information
	 *    will be written.
	 * @param tempDir if null, use the server default temporary directory;
	 *    a string specifies a directory where the server can write a temporary file,
	 *    or the full name of the file.
	 * @return a string, the contents may be useful to Franz customer support,
	 *    or null if a file was specified.
	 * @throws AllegroGraphException
	 */
	public String reportMemoryUsage ( String to, String tempDir ) throws AllegroGraphException {
		return (String) serverOption(this, "memory", to, tempDir);
	}
	
	
	
	
}
