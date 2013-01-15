/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import org.apache.commons.pool.impl.GenericKeyedObjectPool.Config;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.franz.agraph.repository.AGRepositoryConnection;

/**
 * Property names for {@link AGPoolConfig}.
 * 
 * Property names to open a {@link AGRepositoryConnection}.
 * 
 * <p>TODO: {@link AGRepositoryConnection#setSessionLoadInitFile(boolean)}</p>
 * <p>TODO: {@link AGRepositoryConnection#addSessionLoadScript(String)}</p>
 * 
 * Many of these properties are specified and used by {@link GenericObjectPool}.
 * 
 * @see GenericObjectPool
 * @see Config
 */
public enum AGPoolProp {
	
	/**
	 * When the pool is created, this many connections will be
	 * initialized, then returned to the pool.
	 * @see AGPoolConfig#initialSize
	 */
	initialSize,
	
	/**
	 * When the pool is created, if this is true (default is false),
	 * a hook will be registered to close the pool.
	 * Connections will be closed whether idle or not.
	 * 
	 * <p>When the pool is closed, from outside of the hook, the
	 * hook will be {@link Runtime#removeShutdownHook(Thread) removed}
	 * so it is not leaked in the list of hooks.</p>
	 * 
	 * @see AGPoolConfig#shutdownHook
	 * @see Runtime#addShutdownHook(Thread)
	 */
	shutdownHook,
	
	/**
	 * @see GenericObjectPool#setMinIdle(int)
	 */
	minIdle,
	
	/**
	 * @see GenericObjectPool#setMaxIdle(int)
	 */
	maxIdle,
	
	/**
	 * Max number of connections that can be allocated by the pool.
	 * If multiple clients (or different pools), are using the same
	 * AllegroGraph Server, this value should be set to something
	 * less than the SessionPorts.
	 * See <a href="http://www.franz.com/agraph/support/documentation/current/server-installation.html#sessionport"
	 * target="_top">Session Port Setup</a>.
	 * @see GenericObjectPool#setMaxActive(int)
	 */
	maxActive,
	
	/**
	 * milliseconds to wait to borrow before throwing {@link java.util.NoSuchElementException}
	 * @see GenericObjectPool#setMaxWait(long)
	 */
	maxWait,
	
	/**
	 * Calls {@link AGRepositoryConnection#size(org.openrdf.model.Resource...)}.
	 * 
	 * Redundant because {@link AGConnFactory#activateObject(Object)}
	 * always calls {@link AGRepositoryConnection#rollback()}.
	 * 
	 * @see GenericObjectPool#setTestOnBorrow(boolean)
	 * @see AGConnFactory#validateObject(Object)
	 */
	testOnBorrow,
	
	/**
	 * Calls {@link AGRepositoryConnection#size(org.openrdf.model.Resource...)}.
	 * @see GenericObjectPool#setTestOnReturn(boolean)
	 * @see AGConnFactory#validateObject(Object)
	 */
	testOnReturn,
	
	/**
	 * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis(long)
	 */
	timeBetweenEvictionRunsMillis,
	
	/**
	 * @see GenericObjectPool#setMinEvictableIdleTimeMillis(long)
	 */
	minEvictableIdleTimeMillis,
	
	/**
	 * @see GenericObjectPool#setTestWhileIdle(boolean)
	 */
	testWhileIdle,
	
	/**
	 * @see GenericObjectPool#setSoftMinEvictableIdleTimeMillis(long)
	 */
	softMinEvictableIdleTimeMillis,
	
	/**
	 * @see GenericObjectPool#setNumTestsPerEvictionRun(int)
	 */
	numTestsPerEvictionRun,
	
	// TODO whenExhaustedAction
	
}
