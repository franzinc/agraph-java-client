/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;

/**
 * Property names to open a {@link AGRepositoryConnection}.
 * 
 * <p>TODO: {@link AGRepositoryConnection#setSessionLoadInitFile(boolean)}</p>
 * <p>TODO: {@link AGRepositoryConnection#addSessionLoadScript(String)}</p>
 */
public enum AGConnProp {
	
	/**
	 * @see AGServer#AGServer(String, String, String)
	 */
	serverUrl,
	
	/**
	 * @see AGServer#AGServer(String, String, String)
	 */
	username,
	
	/**
	 * @see AGServer#AGServer(String, String, String)
	 */
	password,
	
	/**
	 * Catalog name - "/" or no value for {@link AGServer#getRootCatalog()}.
	 * @see AGServer#getCatalog(String)
	 */
	catalog,
	
	/**
	 * @see AGCatalog#openRepository(String)
	 */
	repository,
	
	/**
	 * Value must be one of {@link Session}.
	 */
	session,
	
	/**
	 * Sets the 'lifetime' (in seconds) for a dedicated session.
	 * Seconds a session can be idle before being collected.
	 * @see AGRepositoryConnection#setSessionLifetime(int)
	 */
	sessionLifetime,
	
	/**
	 * Socket timeout (SO_TIMEOUT) in milliseconds to be used when executing the method.
	 * A timeout value of zero is interpreted as an infinite timeout.
	 * 
	 * <p>WARNING: this may break long queries.</p>
	 * 
	 * @see HttpConnectionParams#setSoTimeout(int)
	 * @see HttpMethodParams#SO_TIMEOUT
	 * @since v4.4
	 */
	httpSocketTimeout;

	/**
	 * Property values for {@link AGConnProp#session}.
	 */
	public static enum Session {
		
		/**
		 * No dedicated session, and autoCommit is true
		 * (that is, {@link AGRepositoryConnection#setAutoCommit(boolean)} is not called).
		 * 
		 * <p>Warning: if the borrowed connections are changed to
		 * dedicated or shared, the connections will remain dedicated
		 * when borrowed again, but autoCommit will be reset to true.</p>
		 */
		SHARED,
		
		/**
		 * Calls {@link AGRepositoryConnection#setAutoCommit(boolean)} with true.
		 */
		DEDICATED,
		
		/**
		 * Calls {@link AGRepositoryConnection#setAutoCommit(boolean)} with false.
		 */
		TX;
		
		static Session valueOfCaseInsensitive(String name, Session defaultVal) {
			Session s = Session.valueOf(name);
			if (s == null) {
				s = Session.valueOf(name.toUpperCase());
			}
			if (s == null) {
				return defaultVal;
			} else {
				return s;
			}
		}
	}
	
}
