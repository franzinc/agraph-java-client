/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
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
	
	public final String serverUrl;
	public final String username;
	public final String password;
	public final String catalog;
	public final String repository;
	public final Session session;
	public final Integer sessionLifetime;
	
	public AGConnConfig(Map<AGConnProp, String> props) {
		if (props.containsKey(AGConnProp.serverUrl)) {
			serverUrl = props.get(AGConnProp.serverUrl);
		} else {
			throw new IllegalArgumentException("Property required for AGConn: " + AGConnProp.serverUrl);
		}
		if (props.containsKey(AGConnProp.username)) {
			username = props.get(AGConnProp.username);
		} else {
			throw new IllegalArgumentException("Property required for AGConn: " + AGConnProp.username);
		}
		if (props.containsKey(AGConnProp.password)) {
			password = props.get(AGConnProp.password);
		} else {
			throw new IllegalArgumentException("Property required for AGConn: " + AGConnProp.password);
		}
		catalog = props.get(AGConnProp.catalog);
		if (props.containsKey(AGConnProp.repository)) {
			repository = props.get(AGConnProp.repository);
		} else {
			throw new IllegalArgumentException("Property required for AGConn: " + AGConnProp.repository);
		}
		session = Session.valueOfCaseInsensitive(props.get(AGConnProp.session), Session.SHARED);
		if (props.containsKey(AGConnProp.sessionLifetime)) {
			sessionLifetime = Integer.parseInt( props.get(AGConnProp.sessionLifetime));
		} else {
			sessionLifetime = null;
		}
	}
	
}
