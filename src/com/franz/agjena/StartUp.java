
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

package com.franz.agjena;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.AllegroGraph;
import com.franz.agjena.exceptions.NiceException;

/**
 * 
 */
public class StartUp  {
	

	private static int debug = 0;
	private static boolean quiet = false;
	
    //------------------------------------------------------------------------------------
    // 
    //------------------------------------------------------------------------------------
    
    // THIS ENUM OUGHT TO APPEAR IN THE CORE AllegroGraph CODE:
	public static enum AccessTripleStore {
		ACCESS("access"), RENEW("renew"), OPEN("open"), CREATE("create"), REPLACE("replace");
		
		private String command;
		AccessTripleStore (String command) {
			this.command = command;
		}
		public String getCommand() {return this.command;}
	}
	
	private static Map<String, String> DEFAULT_AGSERVER_LOCATION_MAP = new HashMap<String, String>();
	
	// TODO: ADD OTHER DEFAULT LOCATIONS FOR OTHER OS's:
	static {
		DEFAULT_AGSERVER_LOCATION_MAP.put("Mac OS X", "/Applications/AllegroGraph/AllegroGraphJavaServer");
	 }
	
	/**
	 * Start up the AllegroGraph Java server.  If that fails with the provided parameters,
	 * possibly try a second time using a default path to the startup application. 
	 */
	private static void startServer(AllegroGraphConnection agConn, String agJavaServerPath) throws IOException {
		if (!quiet) System.out.println("Starting server ...");
		try {
			agConn.startServer();
		} catch (ConnectException ex) {
			String thisOS = System.getProperty("os.name");
			String defaultPath = DEFAULT_AGSERVER_LOCATION_MAP.get(thisOS);
			if (defaultPath != null) {
				File theStartupFile = new File(defaultPath);
				if (!theStartupFile.exists()) defaultPath = null;
			}
			if ((defaultPath != null) && !defaultPath.equals(agJavaServerPath)) {
				System.out.print("Failed to start AllegroGraph server, possibly because ");
				if (agJavaServerPath == null)
					System.out.println("no path provided to location of AllegroGraphJavaServer\n");
				else
					System.out.println("incorrect path '" + agJavaServerPath + "'provided to location of AllegroGraphJavaServer\n");
				System.out.println("  Retrying at location '" + defaultPath + "'");
				startServer(agConn, defaultPath);
				return;
			}
			if (agJavaServerPath != null) {
				throw new NiceException("Failed to start AllegroGraph server at location " + agJavaServerPath, ex);				
			} else {
				throw ex;
			}				
		}
		if (!quiet) System.out.println("  started.");
	}
    
	/**
	 * Parse the configuration arguments, start up the
	 * @param hostName
	 * @param databaseName
	 * @param dbDirectory
	 * @param args
	 * @return  a running AllegroGraph server
	 * @throws AllegroGraphException
	 * @throws IOException
	 */
	public static AllegroGraph helpStartUpTripleStore (AccessTripleStore accessOption,
			String host, String dbName, String dbDirectory, 
			String[] args) throws AllegroGraphException, IOException {
		int port = 4567; // where server is listening
		String tripleFile = ""; // a source file for database
		String agJavaServerPath = null;  // location of AllegroGraphJavaServer launcher
		int exitWait = 0;
		boolean startServer = (agJavaServerPath != null);
	
		// Scan startup parameters
		for (int i = 0; i < args.length;) {
			String flag = (args[i]);
			if (flag.equals("-p"))
				port = Integer.parseInt(args[++i]);
			else if (flag.equals("-h"))
				host = args[++i];
			else if (flag.equals("-d"))
				dbDirectory = args[++i];
			else if (flag.equals("-n"))
				dbName = args[++i];
		//	else if (flag.equals("-r"))
		//		rdfFile = args[++i];
			else if (flag.equals("-t"))
				tripleFile = args[++i];
			else if (flag.equals("-w"))
				exitWait = Integer.parseInt(args[++i]);
			else if (flag.equals("-z"))
				debug = 1;
			else if (flag.equals("-zz"))
				debug = 2;
			else if (flag.equals("-x"))
				startServer = true;
			else if (flag.equals("-q"))
				quiet = true;
			else if (flag.equals("-l"))  // 'l' for launch server
				agJavaServerPath = args[++i];
			i++;
		}
	
		if (Utils.isNullString(host)) {
			throw new NiceException("Name of host (server machine) is required");
		}
		if (Utils.isNullString(dbName)) {
			throw new NiceException("Database name is required");
		}
		if (Utils.isNullString(dbDirectory)) {
			System.out.print("Database folder argument (-d) is required.");
			System.exit(1); // not sure where this came from  - RMM
		}
		System.out.println("port=" + port + "  dbDirectory=" + dbDirectory + "  dbName=" + dbName);
	
		// Connect to the server.
		AllegroGraphConnection agConn = new AllegroGraphConnection();
		if (agJavaServerPath != null) {
			AllegroGraphConnection.setDefaultCommand(agJavaServerPath);
			startServer = true;
		}
		agConn.setPort(port);
		agConn.setHost(host);
		agConn.setDebug(debug);
		if ( startServer ) {
			startServer(agConn, agJavaServerPath);
		}
		if (!quiet) System.out.println("Enabling connection ...");
		try {
			Thread.sleep(2000);  // wait 2 sec. for server to start up
			agConn.enable();
		} catch (Exception ex) {
			if (false && !startServer) {
				// maybe we should have tried 'startServer' after all.  Let's see:
				startServer(agConn, null);
				agConn.enable();
			} else {
				if (ex instanceof RuntimeException) throw (RuntimeException)ex;
				else if (ex instanceof AllegroGraphException) throw (AllegroGraphException)ex;
				else if (ex instanceof IOException) throw (IOException)ex;
				else throw new NiceException(ex);
			}
		}
		if (!quiet) System.out.println("Connected to " + agConn);
		// Create a AllegroGraph database instance.
		AllegroGraph agStore = null;
		try {
			switch (accessOption) {
			case RENEW: agStore = agConn.renew(dbName, dbDirectory);
						break;
			case ACCESS:agStore = agConn.access(dbName, dbDirectory);
						break;
			case OPEN:agStore = agConn.open(dbName, dbDirectory);
					break;
			case REPLACE:agStore = agConn.replace(dbName, dbDirectory);
						break;
			case CREATE:agStore = agConn.create(dbName, dbDirectory);
						break;
			}
		} catch (Exception ex) {
			if (!quiet) System.out.println("Caught exception while renewing triple store:\n" + ex.getMessage());
			if (!quiet) System.out.println("Stopping server ...");
			int result = agConn.stopServer(false);
			if (!quiet) {
				switch(result) {
				case 0: System.out.println("OK");
				case 1: System.out.println("Nothing to terminate.");
				default : System.out.println("Oh oh.");
				}
			}
			return null;
		}
		System.out.println("Connected database " + dbName + " contains " + agStore.numberOfTriples() + " triples");
		// if we reach here without an exception, I guess we succeeded:
		return agStore;
	}
	
	/**
	 * Parse the configuration arguments, start up the
	 * @param hostName
	 * @param databaseName
	 * @param dbDirectory
	 * @param args
	 * @return  a running AllegroGraph server
	 * @throws AllegroGraphException
	 * @throws IOException
	 */
	public static AllegroGraph startUpTripleStore (AccessTripleStore accessOption,
			String host, String dbName, String dbDirectory, 
			String[] args) {
		try {
			return helpStartUpTripleStore(accessOption, host, dbName, dbDirectory, args);
		} catch (Exception ex) {
			if (ex instanceof RuntimeException) throw (RuntimeException)ex;
			else {
				throw new NiceException("Failure starting up AllegroGraph server.", ex);
			}
		}
	}

	/**
	 * Close the triple store and shut down the AllegroGraph Java server.
	 * @throws AllegroGraphException
	 * @throws IOException
	 */
	public static void shutDownTripleStore (AllegroGraph agStore) {
		if (agStore == null) return;
		if (!quiet) System.out.println("All done.  Closing triple store.");
		try {
			agStore.closeTripleStore();
			if (!quiet) System.out.println("Stopping the server.");
			new AllegroGraphConnection().stopServer(false);
			agStore.getConnection().stopServer(false); 
		} catch (Exception ex) {
			if (ex instanceof RuntimeException) throw (RuntimeException)ex;
			else throw new NiceException("Failure shutting down AllegroGraph server.", ex);
		}
	}
	
    //------------------------------------------------------------------------------------
    // Testing
    //------------------------------------------------------------------------------------
	
	public static void main(String[] args) throws AllegroGraphException, IOException {
		String hostName = "localhost";
		String databaseName = "/tmp/test";
		String dbDirectory = "/Users/bmacgregor/Desktop/AGFolder";
		
		AllegroGraph agStore = StartUp.startUpTripleStore(AccessTripleStore.RENEW, hostName, databaseName, dbDirectory, args);
		StartUp.shutDownTripleStore(agStore);
	}

}
        
    
    
