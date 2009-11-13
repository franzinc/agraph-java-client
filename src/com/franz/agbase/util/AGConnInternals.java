package com.franz.agbase.util;

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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.GeospatialSubtype;
import com.franz.agbase.NamespaceRegistry;
import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.transport.AGConnector;

public class AGConnInternals {
	

	protected static final String agPrefPath = "franz.com/allegrograph/java";

	protected static String defaultMode = "direct";

	protected static String defaultHost = "";

	protected static int defaultPort = 4567;

	protected static int defaultPort2 = 4568;

	protected static int defaultDebug = 0;

	protected static String defaultLispCommand = "";

	protected static boolean defaultServerKeep = false;

	protected static int defaultPollCount = 1;

	protected static int defaultPollInterval = 500;

	protected static int defaultTimeout = 5000;
	
	protected static final String agLispKey = "lispcommand";

	// Declared in package com.franz.ag to allow dual AG APIs
	protected static com.franz.ag.NamespaceRegistry initialns = new com.franz.agbase.NamespaceRegistry(NamespaceRegistry.RDFandOwl);

	
	protected int pollCount = defaultPollCount;

	protected int pollInterval = defaultPollInterval;

	protected int timeout = defaultTimeout;


	

	protected String mode = defaultMode;

	protected String host = defaultHost;

	protected int port = defaultPort;

	protected int port2 = defaultPort2;

	protected int debug = defaultDebug;

	protected String lispCommand = defaultLispCommand;

	public boolean keep = defaultServerKeep;

	public AGConnector agc = null;
	
	public Vector<Object> oldTokens = new Vector<Object>(100);
	
	private ArrayList<Object> allTS = new ArrayList<Object>();
	
	public synchronized void addTS(AGBase ts) {
		allTS.add(ts);
	}
	public synchronized void dropTS (AGBase ts ) {
		int ix = allTS.indexOf(ts);
		allTS.remove(ix);
	}
	
	public synchronized Object[] arrayOfTS () { return allTS.toArray(); }
	
	
	/**
	 *  Replace all values made invalid by new connection.
	 *
	 */
	public void freshState () {
		geoSubs = new GeospatialSubtype[256];
		oldTokens = new Vector<Object>(100);
		allTS = new ArrayList<Object>();
		valueMap = Collections.synchronizedMap(new WeakHashMap<Object, Object>());
	}

	public int oldBatch = 10;

	public GeospatialSubtype[] geoSubs = new GeospatialSubtype[256];

	

	public int debugMask = 0;

	public Map<Object, Object> valueMap = Collections.synchronizedMap(new WeakHashMap<Object, Object>());

	

	public NamespaceRegistry nsregs = new NamespaceRegistry(initialns);
	{
		for (int i = 0; i < geoSubs.length; i++) {
			geoSubs[i] = null;
		}
	}
	
	
	

	public long serverId = -1;
	public Object[] selectPlannerOptions = null;
	protected void reset () {
		agc = null;
		serverId = 0;
		selectPlannerOptions = null;
	}
	

	/**
	 * Display some details about the server instance.
	 */
	public String toString() {
		return getClass().getName() + "<" + serverId + " " + mode + ">";
	}
	public AGConnector getServer() {
		if (agc == null)
			throw new IllegalStateException("Server is not enabled.");
		return agc;
	}

	// MADE THIS PUBLIC - RMM
	public boolean ifDebug(int index) {
		int mask = 0;
		if ( index>0 ) mask = 1<<(index-1);
		if ( (debugMask&mask)>0 ) return true;
		return false;
	}
	
	
	public static Object serverOption ( AllegroGraphConnection agc, Object... more ) throws AllegroGraphException {
		return agc.getServer().serverOptionOne(more);
	}
	
}
