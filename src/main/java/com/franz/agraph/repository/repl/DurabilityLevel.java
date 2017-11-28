/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository.repl;

/**
 * Special values for the durability repl setting.
 */
public enum DurabilityLevel {
    /**
     * Also known as one.
     */
    MIN,
    /**
     * Number of instances in the cluster.
     */
    MAX,
    /**
     * the next integer greater than half the number of instances.
     */
    QUORUM,
    /**
     * Use value from server's config file.
     */
    DEFAULT
}
