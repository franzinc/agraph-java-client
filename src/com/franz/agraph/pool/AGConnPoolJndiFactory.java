/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.util.Closeable;

/**
 * JNDI factory for {@link AGConnPropFactory} and {@link GenericObjectPool}.
 * 
 * <p>The Pool library is required to use this package:
 * <a href="http://commons.apache.org/pool/">Apache Commons Pool, commons-pool-1.5.6.jar</a>.
 * Note, this jar along with the agraph-java-client jar and all of its dependencies
 * must be in the webserver's library for it to be able to load.
 * </p>
 * 
 * <p>The properties supported for the connections are specified
 * by {@link AGConnPropFactory.Prop}.
 * </p>
 * 
 * <p>The properties supported for the pooling are specified
 * by {@link PoolProp}.
 * </p>
 * 
 * <p>Note, when {@link Closeable#close()} is called
 * on an {@link AGConnPool},
 * connections will be closed whether they are
 * idle (have been returned) or not.
 * This is different from {@link GenericObjectPool#close()}.
 * Also note, when a {@link AGRepositoryConnection} from the pool is closed,
 * the {@link AGRepository} and {@link AGServer} will also be closed
 * since these are not shared with other {@link AGRepositoryConnection}s.
 * </p>
 * 
 * <p>
 * Example Tomcat JNDI configuration, based on
 * <a href="http://tomcat.apache.org/tomcat-7.0-doc/jndi-resources-howto.html#Adding_Custom_Resource_Factories"
 * >Tomcat HOW-TO create custom resource factories</a>:
 * In /WEB-INF/web.xml:
 * <code><pre>
 * &lt;resource-env-ref&gt;
 *     &lt;description&gt;AllegroGraph connection pool&lt;/description&gt;
 *     &lt;resource-env-ref-name&gt;connection-pool/agraph&lt;/resource-env-ref-name&gt;
 *     &lt;resource-env-ref-type&gt;com.franz.agraph.pool.AGConnPool&lt;/resource-env-ref-type&gt;
 * &lt;/resource-env-ref&gt;
 * </pre></code>
 * Your code:
 * <code><pre>
 * Context initCtx = new InitialContext();
 * Context envCtx = (Context) initCtx.lookup("java:comp/env");
 * AGConnPool pool = (AGConnPool) envCtx.lookup("connection-pool/agraph");
 * AGRepositoryConnection conn = pool.borrowConnection();
 * try {
 *     ...
 *     conn.commit();
 * } finally {
 *     conn.close();
 *     // or equivalently
 *     pool.returnObject(conn);
 * }
 * </pre></code>
 * Tomcat's resource factory:
 * <code><pre>
 * &lt;Context ...&gt;
 *     ...
 *     &lt;Resource name="connection-pool/agraph"
 *               auth="Container"
 *               type="com.franz.agraph.pool.AGConnPool"
 *               factory="com.franz.agraph.pool.AGConnPoolJndiFactory"
 *               username="test"
 *               password="xyzzy"
 *               url="http://localhost:10035"
 *               catalog="/"
 *               repository="my_repo"
 *               session="TX"
 *               initialSize="5"
 *               maxIdle="10"
 *               maxActive="40"
 *               maxWait="60000"/&gt;
 *     ...
 * &lt;/Context&gt;
 * </pre></code>
 * </p>
 * 
 * <p>It is also possible to use this class directly without JNDI,
 * with the method {@link #createPool(Map, Map)}.
 * </p>
 * 
 * @since v4.3.3
 */
public class AGConnPoolJndiFactory implements ObjectFactory {
	
	public static class AGConfig extends Config {
		public static final int DEFAULT_INITIAL_SIZE = 0;
		public int initialSize = DEFAULT_INITIAL_SIZE;
	}
	
	/**
	 * Property names for {@link AGConfig}.
	 * Most of these properties are specified and used by {@link GenericObjectPool}.
	 * 
	 * @see GenericObjectPool
	 * @see GenericObjectPool.Config
	 */
	public enum PoolProp {
		/**
		 * When the pool is created, this many connections will be
		 * initialized, then returned to the pool.
		 */
		initialSize,
		
		/**
		 * @see GenericObjectPool#setMinIdle(int)
		 */
		minIdle,
		/**
		 * @see GenericObjectPool#setMaxIdle(int)
		 */
		maxIdle,
		/**
		 * @see GenericObjectPool#setMaxActive(int)
		 */
		maxActive,
		/**
		 * @see GenericObjectPool#setMaxWait(long)
		 */
		maxWait,
		/**
		 * @see GenericObjectPool#setTestOnBorrow(boolean)
		 */
		testOnBorrow,
		/**
		 * @see GenericObjectPool#setTestOnReturn(boolean)
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
	
	/**
	 * From the obj {@link Reference}, gets the {@link RefAddr}
	 * names and values, converts to Maps and
	 * returns {@link #createPool(Map, Map)}.
	 */
	@Override
	public Object getObjectInstance(Object obj,
			Name name,
			Context nameCtx,
			Hashtable<?,?> environment)
	throws Exception {
		if (!(obj instanceof Reference)) {
			return null;
		}
		Reference ref = (Reference) obj;
		if (! AGConnPool.class.getName().equals(ref.getClassName())) {
			return null;
		}
		return createPool(refToMap(ref, AGConnPropFactory.Prop.values()),
				refToMap(ref, PoolProp.values()));
	}
	
	/**
	 * Create a pool from configuration properties.
	 * @param connProps keys are the names of {@link AGConnPropFactory.Prop}
	 * @param poolProps keys are the names of {@link PoolProp}
	 */
	public AGConnPool createPool(Map<String, String> connProps, Map<String, String> poolProps) throws RepositoryException {
		AGConnPropFactory fact = new AGConnPropFactory(connProps);
		AGConfig config = createConfig(poolProps);
		AGConnPool pool = new AGConnPool( new GenericObjectPool(fact, config));
		pool.closeLater(fact);
		
		if (config.initialSize > 0) {
			List<AGRepositoryConnection> conns = new ArrayList<AGRepositoryConnection>(config.initialSize);
			for (int i = 0; i < config.initialSize; i++) {
				conns.add(pool.borrowConnection());
			}
			for (AGRepositoryConnection conn : conns) {
				// returns it to the pool
				conn.close();
			}
		}
		
		return pool;
	}
	
	/**
	 * @param values enum values
	 * @return map suitable for {@link #createPool(Map, Map)}
	 */
	protected static Map<String, String> refToMap(Reference ref, Enum[] values) {
		Map<String, String> props = new HashMap<String, String>();
		for (Enum prop : values) {
			RefAddr ra = ref.get(prop.name());
			if (ra == null) {
				ra = ref.get(prop.name().toLowerCase());
			}
			if (ra != null) {
				String propertyValue = ra.getContent().toString();
				props.put(prop.toString(), propertyValue);
			}
		}
		return props;
	}
	
	private static String prop(Map<String, String> props, PoolProp p) {
		return props.get(p.name());
	}
	
	public static AGConfig createConfig(Map<String, String> props) {
		AGConfig config = new AGConfig();
		if (prop(props, PoolProp.initialSize) != null) {
			config.initialSize = Integer.parseInt(prop(props, PoolProp.initialSize));
		}
		if (prop(props, PoolProp.maxIdle) != null) {
			config.maxIdle = Integer.parseInt(prop(props, PoolProp.maxIdle));
		}
		if (prop(props, PoolProp.minIdle) != null) {
			config.minIdle = Integer.parseInt(prop(props, PoolProp.minIdle));
		}
		if (prop(props, PoolProp.maxActive) != null) {
			config.maxActive = Integer.parseInt(prop(props, PoolProp.maxActive));
		}
		if (prop(props, PoolProp.maxWait) != null) {
			config.maxWait = Long.parseLong(prop(props, PoolProp.maxWait));
		}
		if (prop(props, PoolProp.testOnBorrow) != null) {
			config.testOnBorrow = Boolean.valueOf(prop(props, PoolProp.testOnBorrow));
		}
		if (prop(props, PoolProp.testOnReturn) != null) {
			config.testOnReturn = Boolean.valueOf(prop(props, PoolProp.testOnReturn));
		}
		if (prop(props, PoolProp.timeBetweenEvictionRunsMillis) != null) {
			config.timeBetweenEvictionRunsMillis = Long.parseLong(prop(props, PoolProp.timeBetweenEvictionRunsMillis));
		}
		if (prop(props, PoolProp.minEvictableIdleTimeMillis) != null) {
			config.minEvictableIdleTimeMillis = Long.parseLong(prop(props, PoolProp.minEvictableIdleTimeMillis));
		}
		if (prop(props, PoolProp.testWhileIdle) != null) {
			config.testWhileIdle = Boolean.valueOf(prop(props, PoolProp.testWhileIdle));
		}
		if (prop(props, PoolProp.softMinEvictableIdleTimeMillis) != null) {
			config.softMinEvictableIdleTimeMillis = Long.parseLong(prop(props, PoolProp.softMinEvictableIdleTimeMillis));
		}
		if (prop(props, PoolProp.numTestsPerEvictionRun) != null) {
			config.numTestsPerEvictionRun = Integer.parseInt(prop(props, PoolProp.numTestsPerEvictionRun));
		}
		return config;
	}
	
}
