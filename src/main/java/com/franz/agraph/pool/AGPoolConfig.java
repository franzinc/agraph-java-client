/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.pool;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.Map;

/**
 * Extension to {@link GenericObjectPoolConfig} to add more properties.
 *
 * @see AGPoolProp
 * @since v4.3.3
 */
public class AGPoolConfig extends GenericObjectPoolConfig {

    public static final int DEFAULT_INITIAL_SIZE = 0;
    public static final boolean DEFAULT_SHUTDOWN_HOOK = false;
    /**
     * @see AGPoolProp#initialSize
     * @see #DEFAULT_INITIAL_SIZE
     */
    public final int initialSize;
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
            setMaxIdle(Integer.parseInt(props.get(AGPoolProp.maxIdle)));
        }
        if (props.containsKey(AGPoolProp.minIdle)) {
            setMinIdle(Integer.parseInt(props.get(AGPoolProp.minIdle)));
        }
        // Note: renamed from active to total in cp2
        if (props.containsKey(AGPoolProp.maxActive)) {
            setMaxTotal(Integer.parseInt(props.get(AGPoolProp.maxActive)));
        }
        // Note: renamed from wait to waitMillis in cp2
        if (props.containsKey(AGPoolProp.maxWait)) {
            setMaxWaitMillis(Long.parseLong(props.get(AGPoolProp.maxWait)));
        }
        if (props.containsKey(AGPoolProp.testOnBorrow)) {
            setTestOnBorrow(Boolean.valueOf(props.get(AGPoolProp.testOnBorrow)));
        }
        if (props.containsKey(AGPoolProp.testOnReturn)) {
            setTestOnReturn(Boolean.valueOf(props.get(AGPoolProp.testOnReturn)));
        }
        if (props.containsKey(AGPoolProp.timeBetweenEvictionRunsMillis)) {
            setTimeBetweenEvictionRunsMillis(
                    Long.parseLong(props.get(AGPoolProp.timeBetweenEvictionRunsMillis)));
        }
        if (props.containsKey(AGPoolProp.minEvictableIdleTimeMillis)) {
            setMinEvictableIdleTimeMillis(
                    Long.parseLong(props.get(AGPoolProp.minEvictableIdleTimeMillis)));
        }
        if (props.containsKey(AGPoolProp.testWhileIdle)) {
            setTestWhileIdle(Boolean.valueOf(props.get(AGPoolProp.testWhileIdle)));
        }
        if (props.containsKey(AGPoolProp.softMinEvictableIdleTimeMillis)) {
            setSoftMinEvictableIdleTimeMillis(
                    Long.parseLong(props.get(AGPoolProp.softMinEvictableIdleTimeMillis)));
        }
        if (props.containsKey(AGPoolProp.numTestsPerEvictionRun)) {
            setNumTestsPerEvictionRun(
                    Integer.parseInt(props.get(AGPoolProp.numTestsPerEvictionRun)));
        }
    }

}
