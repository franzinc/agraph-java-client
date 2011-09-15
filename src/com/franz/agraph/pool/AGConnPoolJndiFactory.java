/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.servlet.ServletContextListener;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.pool.AGConnPropFactory.Prop;
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
 * with the method {@link #createPool(Map, Map)} or {@link #createPool(Object...)}.
 * </p>
 * 
 * <p>Closing the connection pool is important because server sessions will
 * stay active until {@link Prop#sessionLifetime}.
 * The option to use a Runtime shutdownHook is built-in with {@link PoolProp#shutdownHook}.
 * Another option is to use {@link ServletContextListener} - this is appropriate if the
 * agraph jar is deployed within your webapp and not with the webserver.
 * With tomcat, a <a href="http://tomcat.apache.org/tomcat-6.0-doc/config/context.html#Lifecycle_Listeners"
 * >Lifecycle Listener</a> can be configured, but the implementation to do this
 * is not included in this library.
 * </p>
 * 
 * @since v4.3.3
 */
public class AGConnPoolJndiFactory implements ObjectFactory {
	
	public static class AGConfig extends Config {
		public static final int DEFAULT_INITIAL_SIZE = 0;
		public int initialSize = DEFAULT_INITIAL_SIZE;
		
		public static final boolean DEFAULT_SHUTDOWN_HOOK = false;
		public boolean shutdownHook = DEFAULT_SHUTDOWN_HOOK;
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
		 * @see AGConfig#initialSize
		 */
		initialSize,
		
		/**
		 * When the pool is created, if this is true (default is false),
		 * a hook will be registered to close the pool.
		 * Connections will be closed whether idle or not.
		 * @see AGConfig#shutdownHook
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
		Map<AGConnPropFactory.Prop, String> connProps = (Map<AGConnPropFactory.Prop, String>) refToMap(ref, AGConnPropFactory.Prop.values());
		Map<PoolProp, String> poolProps = (Map<PoolProp, String>) refToMap(ref, PoolProp.values());
		return createPool(connProps, poolProps);
	}

	/**
	 * Create a pool from configuration properties.
	 * @param keyValuePairs alternating key/value pairs where keys are {@link Prop} and {@link PoolProp}
	 */
	public AGConnPool createPool(Object...keyValuePairs) throws RepositoryException {
		Map<AGConnPropFactory.Prop, String> connProps = (Map<AGConnPropFactory.Prop, String>) toMap(keyValuePairs, EnumSet.allOf(Prop.class));
		Map<PoolProp, String> poolProps = (Map<PoolProp, String>) toMap(keyValuePairs, EnumSet.allOf(PoolProp.class));
		return createPool(connProps, poolProps);
	}

	/**
	 * Create a pool from configuration properties.
	 * @param connProps keys are {@link AGConnPropFactory.Prop}
	 * @param poolProps keys are {@link PoolProp}
	 */
	public AGConnPool createPool(Map<AGConnPropFactory.Prop, String> connProps, Map<PoolProp, String> poolProps) throws RepositoryException {
		AGConnPropFactory fact = new AGConnPropFactory(connProps);
		AGConfig config = createConfig(poolProps);
		final AGConnPool pool = AGConnPool.create(fact, config);
		pool.closeLater(fact);
		return pool;
	}
	
	/**
	 * @param values enum values
	 * @return map suitable for {@link #createPool(Map, Map)}
	 */
	protected static Map<? extends Enum, String> refToMap(Reference ref, Enum[] values) {
		Map<Enum, String> props = new HashMap<Enum, String>();
		for (Enum prop : values) {
			RefAddr ra = ref.get(prop.name());
			if (ra == null) {
				ra = ref.get(prop.name().toLowerCase());
			}
			if (ra != null) {
				String propertyValue = ra.getContent().toString();
				props.put(prop, propertyValue);
			}
		}
		return props;
	}
	
    protected static Map<? extends Enum, String> toMap(Object[] keyValuePairs, EnumSet<? extends Enum> enumSet) {
    	Map<Enum, String> map = new HashMap<Enum, String>();
    	for (int i = 0; i < keyValuePairs.length; i=i+2) {
    		Enum key = (Enum) keyValuePairs[i];
    		if (enumSet.contains(key)) {
        		Object val = keyValuePairs[i+1];
    			map.put(key, val==null ? null : val.toString());
    		}
		}
    	return map;
    }
    
	private static String prop(Map<PoolProp, String> props, PoolProp p) {
		return props.get(p);
	}
	
	public static AGConfig createConfig(Map<PoolProp, String> props) {
		AGConfig config = new AGConfig();
		if (prop(props, PoolProp.initialSize) != null) {
			config.initialSize = Integer.parseInt(prop(props, PoolProp.initialSize));
		}
		if (prop(props, PoolProp.shutdownHook) != null) {
			config.shutdownHook = Boolean.valueOf(prop(props, PoolProp.shutdownHook));
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
