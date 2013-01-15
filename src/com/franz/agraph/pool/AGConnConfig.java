/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import java.util.Map;

import com.franz.agraph.pool.AGConnProp.Session;

/**
 * @since v4.3.3
 */
public class AGConnConfig {
	
	
	/**
	 * @see AGConnProp#serverUrl
	 */
	public final String serverUrl;
	
	/**
	 * @see AGConnProp#username
	 */
	public final String username;
	
	/**
	 * @see AGConnProp#password
	 */
	public final String password;
	
	/**
	 * @see AGConnProp#catalog
	 */
	public final String catalog;
	
	/**
	 * @see AGConnProp#repository
	 */
	public final String repository;
	
	/**
	 * @see AGConnProp#session
	 */
	public final Session session;
	
	/**
	 * @see AGConnProp#sessionLifetime
	 */
	public final Integer sessionLifetime;
	
	/**
	 * @see AGConnProp#httpSocketTimeout
	 * @since v4.4
	 */
	public final Integer httpSocketTimeout;
	
	public AGConnConfig(Map<AGConnProp, String> props) {
		serverUrl = getStringRequired(props, AGConnProp.serverUrl);
		username = getStringRequired(props, AGConnProp.username);
		password = getStringRequired(props, AGConnProp.password);
		catalog = props.get(AGConnProp.catalog);
		repository = getStringRequired(props, AGConnProp.repository);
		session = Session.valueOfCaseInsensitive(props.get(AGConnProp.session), Session.SHARED);
		sessionLifetime = getInt(props, AGConnProp.sessionLifetime);
		httpSocketTimeout = getInt(props, AGConnProp.httpSocketTimeout);
	}
	
	private Integer getInt(Map<AGConnProp, String> props, AGConnProp prop) {
		if (props.containsKey(prop)) {
			return Integer.parseInt( props.get(prop));
		} else {
			return null;
		}
	}
	
	private String getStringRequired(Map<AGConnProp, String> props, AGConnProp prop) {
		if (props.containsKey(prop)) {
			return props.get(prop);
		} else {
			throw new IllegalArgumentException("Property required for AGConn: " + prop);
		}
	}
	
}
