/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.pool;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.openrdf.repository.RepositoryException;

import com.franz.util.Closeable;

/**
 * JNDI factory for {@link AGConnPropFactory} and {@link GenericObjectPool}.
 * 
 * <p>The Pool library is required to use this package:
 * <a href="http://commons.apache.org/pool/">Apache Commons Pool, commons-pool-1.5.5.jar</a>
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
 * </p>
 * 
 * <p>
 * Example Tomcat JNDI configuration, based on
 * <a href="http://tomcat.apache.org/tomcat-7.0-doc/jndi-resources-howto.html#Adding_Custom_Resource_Factories"
 * >Tomcat HOW-TO create custom resource factories</a>:
 * In /WEB-INF/web.xml:
 * <code><pre>
 * <resource-env-ref>
 *   <description>AllegroGraph connection pool</description>
 *   <resource-env-ref-name>connection-pool/agraph</resource-env-ref-name>
 *   <resource-env-ref-type>com.franz.agraph.pool.AGConnPool</resource-env-ref-type>
 * </resource-env-ref>
 * </pre></code>
 * Your code:
 * <code><pre>
 * Context initCtx = new InitialContext();
 * Context envCtx = (Context) initCtx.lookup("java:comp/env");
 * AGConnPool pool = (AGConnPool) envCtx.lookup("connection-pool/agraph");
 * AGRepositoryConnection conn = pool.borrowConnection();
 * try {
 *   ...
 *   conn.commit();
 * } finally{
 *   conn.close();
 * }
 * </pre></code>
 * Tomcat's resource factory:
 * <code><pre>
 * <Context ...>
 *   ...
 *   <Resource name="connection-pool/agraph"
 *             auth="Container"
 *             type="com.franz.agraph.pool.AGConnPool"
 *             factory="com.franz.agraph.pool.AGConnPoolJndiFactory"
 *             username="test"
 *             password="xyzzy"
 *             url="http://localhost:10035"
 *             catalog="/"
 *             repository="my_repo"
 *             session="TX"
 *             min_idle="5"
 *             max_idle="10"
 *             max_active="40"
 *             max_wait="60000"/>
 *   ...
 * </Context>
 * </pre></code>
 * </p>
 * 
 * @since v4.3
 */
public class AGConnPoolJndiFactory implements ObjectFactory {
	
	/**
         * Property names for {@link Config}.
	 * @see Config
	 */
	public enum PoolProp {
		MIN_IDLE,
		MAX_IDLE,
		MAX_ACTIVE,
		MAX_WAIT
		// TODO WHEN_EXHAUSTED_ACTION, others
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
	 * @param connProps keys are the toStrings of {@link AGConnPropFactory.Prop}
	 * @param poolProps keys are the toStrings of {@link PoolProp}
	 */
	public AGConnPool createPool(Map<String, String> connProps, Map<String, String> poolProps) throws RepositoryException {
		AGConnPropFactory fact = new AGConnPropFactory(connProps);
		AGConnPool pool = new AGConnPool<GenericObjectPool>(
				new GenericObjectPool(
						fact,
						createGenericConfig(poolProps)));
		pool.closeLater(fact);
		return pool;
	}

	/**
	 * @param values enum values
	 * @return map suitable for {@link #createPool(Map, Map)}
	 */
	protected static Map<String, String> refToMap(Reference ref, Object[] values) {
		Map<String, String> props = new HashMap<String, String>();
		for (Object prop : values) {
			RefAddr ra = ref.get(prop.toString());
			if (ra == null) {
				ra = ref.get(prop.toString().toLowerCase());
			}
			if (ra != null) {
				String propertyValue = ra.getContent().toString();
				props.put(prop.toString(), propertyValue);
			}
		}
		return props;
	}

	private static String prop(Map<String, String> props, PoolProp p) {
		return props.get(p.toString());
	}
	
	public static Config createGenericConfig(Map<String, String> props) {
		Config config = new Config();
		if (prop(props, PoolProp.MAX_IDLE) != null) {
			config.maxIdle = Integer.parseInt(prop(props, PoolProp.MAX_IDLE));
		}
		if (prop(props, PoolProp.MIN_IDLE) != null) {
			config.minIdle = Integer.parseInt(prop(props, PoolProp.MIN_IDLE));
		}
		if (prop(props, PoolProp.MAX_ACTIVE) != null) {
			config.maxActive = Integer.parseInt(prop(props, PoolProp.MAX_ACTIVE));
		}
		if (prop(props, PoolProp.MAX_WAIT) != null) {
			config.maxWait = Long.parseLong(prop(props, PoolProp.MAX_WAIT));
		}
//		if (prop(props, PoolProp.WHEN_EXHAUSTED_ACTION) != null) {
//			config.whenExhaustedAction = Integer.parseInt(prop(props, PoolProp.WHEN_EXHAUSTED_ACTION));
//		}
		return config;
	}

}
