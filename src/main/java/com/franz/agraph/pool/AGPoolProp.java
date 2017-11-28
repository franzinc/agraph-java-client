/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.pool;

import com.franz.agraph.repository.AGRepositoryConnection;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Property names for {@link AGPoolConfig}.
 * <p>
 * Property names to open a {@link AGRepositoryConnection}.
 * <p>TODO: {@link AGRepositoryConnection#setSessionLoadInitFile(boolean)}</p>
 * <p>TODO: {@link AGRepositoryConnection#addSessionLoadScript(String)}</p>
 * <p>
 * Many of these properties are specified and used by {@link GenericObjectPool}.
 *
 * @see GenericObjectPool
 * @see GenericObjectPoolConfig
 */
public enum AGPoolProp {

    /**
     * When the pool is created, this many connections will be
     * initialized, then returned to the pool.
     *
     * @see AGPoolConfig#initialSize
     */
    initialSize,

    /**
     * When the pool is created, if this is true (default is false),
     * a hook will be registered to close the pool.
     * Connections will be closed whether idle or not.
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
     *
     * @see GenericObjectPool#setMaxTotal(int)
     */
    maxActive,

    /**
     * milliseconds to wait to borrow before throwing {@link java.util.NoSuchElementException}
     *
     * @see GenericObjectPool#setMaxWaitMillis(long)
     */
    maxWait,

    /**
     * Calls {@link AGRepositoryConnection#size(org.eclipse.rdf4j.model.Resource...)}.
     * <p>
     * Redundant because {@link AGConnFactory#activateObject(PooledObject)}
     * always calls {@link AGRepositoryConnection#rollback()}.
     *
     * @see GenericObjectPool#setTestOnBorrow(boolean)
     * @see AGConnFactory#validateObject(PooledObject)
     */
    testOnBorrow,

    /**
     * Calls {@link AGRepositoryConnection#size(org.eclipse.rdf4j.model.Resource...)}.
     *
     * @see GenericObjectPool#setTestOnReturn(boolean)
     * @see AGConnFactory#validateObject(PooledObject)
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
