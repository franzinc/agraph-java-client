package com.franz.agraph.repository;

/**
 * Provides detailed configuration for the warmup operation.
 *
 * Config objects are immutable. New instances can be created
 * using a fluid interface:
 *
 * <pre>{@code
 *   WarmupConfig.create().includeStrings(true).includeTriples(false);
 * }</pre>
 *
 * The config stores values for a set of parameters. These currently include:
 * <dl>
 *     <dt>includeStrings (boolean, default: true)</dt>
 *     <dd>Should the string table be included in the operation?</dd>
 *     <dt>includeTriples (boolean, default: true)</dt>
 *     <dd>Should triple indices be included in the operation?</dd>
 * </dl>
 *
 * @see AGRepositoryConnection#warmup(WarmupConfig)
 */
public final class WarmupConfig {
    private boolean includeStrings;
    private boolean includeTriples;

    /**
     * Creates a fresh config with default values.
     *
     * @return a new config object.
     */
    public static WarmupConfig create() {
        return new WarmupConfig();
    }

    /**
     * Creates a fresh config with default values.
     */
    private WarmupConfig() {
        includeStrings = true;
        includeTriples = true;
    }

    /**
     * Clones another config object.
     *
     * @param other Object to be cloned.
     */
    private WarmupConfig(final WarmupConfig other) {
        includeStrings = other.includeStrings;
        includeTriples = other.includeTriples;
    }

    /**
     * Checks if the string table should be included.
     * @return Value of the includeStrings parameter.
     */
    public boolean getIncludeStrings() {
        return includeStrings;
    }

    /**
     * Checks if triple indices should be included.
     * @return Value of the includeTriples parameter.
     */
    public boolean getIncludeTriples() {
        return includeTriples;
    }

    /**
     * Modifies the includeStrings parameter.
     *
     * @param includeStrings New value.
     */
    private void setIncludeStrings(final boolean includeStrings) {
        this.includeStrings = includeStrings;
    }

    /**
     * Modifies the includeTriples parameter.
     *
     * @param includeTriples New value.
     */
    private void setIncludeTriples(final boolean includeTriples) {
        this.includeTriples = includeTriples;
    }

    /**
     * Creates a config with a modified includeStrings value.
     *
     * @param includeStrings New value.
     * @return A fresh config object with the new value.
     */
    public WarmupConfig includeStrings(final boolean includeStrings) {
        final WarmupConfig result = new WarmupConfig(this);
        result.setIncludeStrings(includeStrings);
        return result;
    }

    /**
     * Creates a config with a modified includeTriples value.
     *
     * @param includeTriples New value.
     * @return A fresh config object with the new value.
     */
    public WarmupConfig includeTriples(final boolean includeTriples) {
        final WarmupConfig result = new WarmupConfig(this);
        result.setIncludeTriples(includeTriples);
        return result;
    }

    /**
     * Creates a modified config that causes the string table
     * to be included in the warmup operation.
     *
     * @return A new, modified config.
     */
    public WarmupConfig includeStrings() {
        return includeStrings(true);
    }

    /**
     * Creates a modified config that causes the string table
     * to be excluded from the warmup operation.
     *
     * @return A new, modified config.
     */
    public WarmupConfig includeTriples() {
        return includeTriples(true);
    }

    /**
     * Creates a modified config that causes triple indices
     * to be included in the warmup operation.
     *
     * @return A new, modified config.
     */
    public WarmupConfig excludeStrings() {
        return includeStrings(false);
    }

    /**
     * Creates a modified config that causes triple indices
     * to be excluded from the warmup operation.
     *
     * @return A new, modified config.
     */
    public WarmupConfig excludeTriples() {
        return includeTriples(false);
    }
}
