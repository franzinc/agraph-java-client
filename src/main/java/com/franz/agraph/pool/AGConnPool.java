/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.pool;

import com.franz.agraph.repository.AGRepositoryConnection;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pooling for {@link AGRepositoryConnection}s.
 * The recommended way to create a pool is by {@link #create(Object...)}
 * or by configuring a {@link AGConnPoolJndiFactory} with your appserver.
 *
 * <pre>{@code
 *     AGConnPool pool = AGConnPool.create(
 *         AGConnProp.serverUrl, "http://localhost:10035",
 *         AGConnProp.username, "test",
 *         AGConnProp.password, "xyzzy",
 *         AGConnProp.catalog, "/",
 *         AGConnProp.repository, "my_repo",
 *         AGConnProp.session, AGConnProp.Session.DEDICATED,
 *         AGPoolProp.shutdownHook, true,
 *         AGPoolProp.maxActive, 10,
 *         AGPoolProp.initialSize, 2);
 *     AGRepositoryConnection conn = pool.borrowConnection();
 *     try {
 *         ...
 *         conn.commit();
 *     } finally {
 *         conn.close();
 *         // or equivalently
 *         pool.returnObject(conn);
 *     }
 * }</pre>
 *
 * <p>This pool delegates the pooling implementation to another
 * pool (a {@link GenericObjectPool}).
 * When {@link AGRepositoryConnection Connections} are {@link #borrowObject() borrowed},
 * they are wrapped so that {@link AutoCloseable#close()}
 * will call {@link #returnObject(Object)} instead of actually closing.
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
 * <p>Note, when {@link AGConnPool#close()} is called
 * on a {@link AGConnPool}, connections that have not been returned to the pool
 * will *not* be closed. Such connections will be closed immediately when
 * returned to the pool.</p>
 *
 * @since v4.3.3
 */
public class AGConnPool implements ObjectPool<AGRepositoryConnection>, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AGConnPool.class);

    private final ObjectPool<AGRepositoryConnection> delegate;
    private final Thread shutdownHook;

    /**
     * @see #create(Object...)
     */
    private AGConnPool(PooledObjectFactory<AGRepositoryConnection> factory,
                       AGPoolConfig poolConfig) {
        delegate = new GenericObjectPool<>(factory, poolConfig);

        if (poolConfig.initialSize > 0) {
            List<AGRepositoryConnection> conns = new ArrayList<>(poolConfig.initialSize);
            try {
                for (int i = 0; i < poolConfig.initialSize; i++) {
                    conns.add(borrowObject());
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
            // return them to the pool
            for (AGRepositoryConnection conn : conns) {
                conn.close();
            }
        }
        if (poolConfig.shutdownHook) {
            shutdownHook = new Thread(this::close);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } else {
            shutdownHook = null;
        }
    }

    private AGConnPool(AGPoolConfig poolConfig, AGConnConfig connConfig) {
        this(new AGConnFactory(connConfig), poolConfig);
    }

    /**
     * Creates a pool using a custom connection factory.
     *
     * @param factory    a connection factory.
     * @param poolConfig an {@link AGPoolConfig}
     * @return AGConnPool the connection pool object
     */
    public static AGConnPool create(PooledObjectFactory<AGRepositoryConnection> factory,
                                    AGPoolConfig poolConfig) {
        return new AGConnPool(factory, poolConfig);
    }

    /**
     * Create a pool from configuration properties.
     *
     * @param connProps keys are {@link AGConnProp}
     * @param poolProps keys are {@link AGPoolProp}
     * @return AGConnPool  the connection pool object
     * @throws RepositoryException if an error occurs during pool creation
     */
    public static AGConnPool create(Map<AGConnProp, String> connProps,
                                    Map<AGPoolProp, String> poolProps)
            throws RepositoryException {
        AGPoolConfig poolConfig = new AGPoolConfig(poolProps);
        AGConnConfig connConfig = new AGConnConfig(connProps);
        return new AGConnPool(poolConfig, connConfig);
    }

    /**
     * Create a pool from configuration properties.
     *
     * @param keyValuePairs alternating key/value pairs where keys are {@link AGConnProp} and {@link AGPoolProp}
     * @return {@link AGConnPool}  the connection pool object
     * @throws RepositoryException if an error occurs during pool creation
     */
    public static AGConnPool create(Object... keyValuePairs) throws RepositoryException {
        Map<AGConnProp, String> connProps = (Map<AGConnProp, String>) toMap(keyValuePairs, EnumSet.allOf(AGConnProp.class));
        Map<AGPoolProp, String> poolProps = (Map<AGPoolProp, String>) toMap(keyValuePairs, EnumSet.allOf(AGPoolProp.class));
        return create(connProps, poolProps);
    }

    private static Map<? extends Enum, String> toMap(Object[] keyValuePairs, EnumSet<? extends Enum> enumSet) {
        Map<Enum, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i = i + 2) {
            Enum key = (Enum) keyValuePairs[i];
            if (enumSet.contains(key)) {
                Object val = keyValuePairs[i + 1];
                map.put(key, val == null ? null : val.toString());
            }
        }
        return map;
    }

    @Override
    public void addObject() throws Exception {
        delegate.addObject();
    }

    @Override
    public AGRepositoryConnection borrowObject() throws RepositoryException {
        final AGRepositoryConnection conn;
        try {
            conn = delegate.borrowObject();
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
        // Make sure 'close' will return the connection to the pool
        // instead of really closing it.
        conn.setPool(this);
        return conn;
    }

    /**
     * Same as {@link #borrowObject()}.
     *
     * @return A connection.
     * @deprecated Use {@link #borrowObject()} instead.
     */
    public AGRepositoryConnection borrowConnection() {
        return borrowObject();
    }

    @Override
    public void clear() throws Exception {
        delegate.clear();
    }

    @Override
    public int getNumActive() throws UnsupportedOperationException {
        return delegate.getNumActive();
    }

    @Override
    public int getNumIdle() throws UnsupportedOperationException {
        return delegate.getNumIdle();
    }

    @Override
    public void invalidateObject(AGRepositoryConnection conn) throws Exception {
        delegate.invalidateObject(conn);
    }

    @Override
    public void returnObject(AGRepositoryConnection conn) throws Exception {
        // Make sure 'close' will really close and not try to
        // return the connection to the pool again.
        conn.setPool(null);
        delegate.returnObject(conn);
    }

    public void ensureIdle(int n) throws Exception {
        if (delegate instanceof GenericObjectPool) {
            GenericObjectPool gop = (GenericObjectPool) delegate;
            gop.setMinIdle(n);
            if (gop.getMaxIdle() < n) {
                gop.setMaxIdle(n);
            }
        }
        for (int i = 0; i < n; i++) {
            addObject();
        }
    }

    @Override
    public void close() {
        if (log.isDebugEnabled()) {
            log.debug("close " + this);
        }
        delegate.close();
        if (shutdownHook != null) {
            // It would be safe to close a pool multiple times,
            // but if we don't delete the hook it will keep a
            // reference to the closed pool and waste memory.
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
    }

    protected void finalize() throws Throwable {
        if (getNumActive() > 0) {
            close();
            log.warn("Finalizing with open connections, please close the pool properly. " + this);
        }
        super.finalize();
    }

    @Override
    public String toString() {
        return "{AGConnPool"
                + " active=" + getNumActive()
                + " idle=" + getNumIdle()
                + " delegate=" + delegate
                + " this=" + super.toString()
                + "}";
    }

}
