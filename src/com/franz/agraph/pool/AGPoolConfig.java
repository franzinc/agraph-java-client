/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import java.util.Map;

import org.apache.commons.pool.impl.GenericObjectPool.Config;

/**
 * Extension to {@link Config} to add more properties.
 * 
 * @see AGPoolProp
 * @since v4.3.3
 */
public class AGPoolConfig extends Config {

	public static final int DEFAULT_INITIAL_SIZE = 0;
	
	/**
	 * @see AGPoolProp#initialSize
	 * @see #DEFAULT_INITIAL_SIZE
	 */
	public final int initialSize;
	
	public static final boolean DEFAULT_SHUTDOWN_HOOK = false;
	
	/**
	 * @see AGPoolProp#shutdownHook
	 * @see #DEFAULT_SHUTDOWN_HOOK
	 */
	public final boolean shutdownHook;
	
	public AGPoolConfig(Map<AGPoolProp, String> props) {
		if (props.containsKey(AGPoolProp.initialSize)) {
			initialSize = Integer.parseInt(props.get(AGPoolProp.initialSize));
		} else {
			initialSize = DEFAULT_INITIAL_SIZE;
		}
		if (props.containsKey(AGPoolProp.shutdownHook)) {
			shutdownHook = Boolean.valueOf(props.get(AGPoolProp.shutdownHook));
		} else {
			shutdownHook = DEFAULT_SHUTDOWN_HOOK;
		}
		if (props.containsKey(AGPoolProp.maxIdle)) {
			maxIdle = Integer.parseInt(props.get(AGPoolProp.maxIdle));
		}
		if (props.containsKey(AGPoolProp.minIdle)) {
			minIdle = Integer.parseInt(props.get(AGPoolProp.minIdle));
		}
		if (props.containsKey(AGPoolProp.maxActive)) {
			maxActive = Integer.parseInt(props.get(AGPoolProp.maxActive));
		}
		if (props.containsKey(AGPoolProp.maxWait)) {
			maxWait = Long.parseLong(props.get(AGPoolProp.maxWait));
		}
		if (props.containsKey(AGPoolProp.testOnBorrow)) {
			testOnBorrow = Boolean.valueOf(props.get(AGPoolProp.testOnBorrow));
		}
		if (props.containsKey(AGPoolProp.testOnReturn)) {
			testOnReturn = Boolean.valueOf(props.get(AGPoolProp.testOnReturn));
		}
		if (props.containsKey(AGPoolProp.timeBetweenEvictionRunsMillis)) {
			timeBetweenEvictionRunsMillis = Long.parseLong(props.get(AGPoolProp.timeBetweenEvictionRunsMillis));
		}
		if (props.containsKey(AGPoolProp.minEvictableIdleTimeMillis)) {
			minEvictableIdleTimeMillis = Long.parseLong(props.get(AGPoolProp.minEvictableIdleTimeMillis));
		}
		if (props.containsKey(AGPoolProp.testWhileIdle)) {
			testWhileIdle = Boolean.valueOf(props.get(AGPoolProp.testWhileIdle));
		}
		if (props.containsKey(AGPoolProp.softMinEvictableIdleTimeMillis)) {
			softMinEvictableIdleTimeMillis = Long.parseLong(props.get(AGPoolProp.softMinEvictableIdleTimeMillis));
		}
		if (props.containsKey(AGPoolProp.numTestsPerEvictionRun)) {
			numTestsPerEvictionRun = Integer.parseInt(props.get(AGPoolProp.numTestsPerEvictionRun));
		}
	}
			
}
