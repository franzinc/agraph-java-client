/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
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
import javax.servlet.ServletContextListener;

import org.apache.commons.pool.impl.GenericObjectPool;

import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.util.Closeable;

/**
 * JNDI factory for {@link AGConnPool}.
 * 
 * <p>The Commons-Pool library is required to use this package:
 * <a href="http://commons.apache.org/pool/">Apache Commons Pool, commons-pool-1.5.6.jar</a>.
 * Note, this jar along with the agraph-java-client jar and all of its dependencies
 * must be in the webserver's library for it to be able to load.
 * </p>
 * 
 * <p>The properties supported for the connections are specified
 * by {@link AGConnProp}.
 * </p>
 * 
 * <p>The properties supported for the pooling are specified
 * by {@link AGPoolProp}.
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
 *               serverUrl="http://localhost:10035"
 *               catalog="/"
 *               repository="my_repo"
 *               session="TX"
 *               testOnBorrow="true"
 *               initialSize="5"
 *               maxIdle="10"
 *               maxActive="40"
 *               maxWait="60000"/&gt;
 *     ...
 * &lt;/Context&gt;
 * </pre></code>
 * </p>
 * 
 * <p>Closing the connection pool is important because server sessions will
 * stay active until {@link AGConnProp#sessionLifetime}.
 * The option to use a Runtime shutdownHook is built-in with {@link AGPoolProp#shutdownHook}.
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
	
	/**
	 * From the obj {@link Reference}, gets the {@link RefAddr}
	 * names and values, converts to Maps and
	 * returns {@link AGConnPool#create(Object...)}.
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
		Map<AGConnProp, String> connProps = (Map<AGConnProp, String>) refToMap(ref, AGConnProp.values());
		Map<AGPoolProp, String> poolProps = (Map<AGPoolProp, String>) refToMap(ref, AGPoolProp.values());
		return AGConnPool.create(connProps, poolProps);
	}

	/**
	 * @param values enum values
	 * @return map suitable for {@link #createPool(Map, Map)}
	 */
	private static Map<? extends Enum, String> refToMap(Reference ref, Enum[] values) {
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
	
}
