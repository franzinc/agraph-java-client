package com.franz.agraph.repository.repl;

/**
 * A container for all settings related to commit behavior in
 * multi-master replication clusters.
 * <p>
 * Instances of this class are immutable. To change a setting
 * use the corresponding 'withXXXX()' method, which will return a fresh
 * object with updated settings.
 */
public class TransactionSettings {
    // Number of instances, null means 'use symbolic value'.
    private Integer durability;
    // Symbolic value (or DEFAULT), must not be null.
    private DurabilityLevel symbolicDurability;
    // Timeout in seconds or null.
    private Integer distributedTransactionTimeout;
    // Number of comits or null.
    private Integer transactionLatencyCount;
    // Timeout in seconds or null.
    private Integer transactionLatencyTimeout;

    /**
     * Create a configuration object where all settings use the default
     * values configured on the server.
     */
    public TransactionSettings() {
        this.durability = null;
        this.symbolicDurability = DurabilityLevel.DEFAULT;
        this.distributedTransactionTimeout = null;
        this.transactionLatencyCount = null;
        this.transactionLatencyTimeout = null;
    }

    /**
     * Copy another settings object.
     *
     * @param other Object to be cloned.
     */
    public TransactionSettings(final TransactionSettings other) {
        this.durability = other.durability;
        this.symbolicDurability = other.symbolicDurability;
        this.distributedTransactionTimeout = other.distributedTransactionTimeout;
        this.transactionLatencyCount = other.transactionLatencyCount;
        this.transactionLatencyTimeout = other.transactionLatencyTimeout;
    }

    /**
     * Retrieves the durability level (see {@link #withDurability(Integer)}).
     * <p>
     * This goes through a visitor interface since durability can be either
     * an integer or a symbolic {@link DurabilityLevel}.
     *
     * @param <T>     Type of values returned by the visitor.
     * @param visitor Object that will consume the durability value.
     * @return Whatever the visitor returns.
     */
    public <T> T visitDurability(final DurabilityVisitor<T> visitor) {
        if (durability == null) {
            return visitor.visitDurabilityLevel(symbolicDurability);
        } else {
            return visitor.visitInteger(durability);
        }
    }

    /**
     * Retrieves the transaction timeout
     * (see {@link #withDistributedTransactionTimeout(Integer)}).
     *
     * @return Number of seconds to wait or {@code null}, meaning "use the default
     * value configured on the server".
     */
    public Integer getDistributedTransactionTimeout() {
        return distributedTransactionTimeout;
    }

    /**
     * Retrieve the latency count ({@link #withTransactionLatencyCount(Integer)}.
     *
     * @return Max number of pending commits or {@code null}, meaning "use the default
     * value configured on the server".
     */
    public Integer getTransactionLatencyCount() {
        return transactionLatencyCount;
    }

    /**
     * Retrieve the latency timeout ({@link #withTransactionLatencyTimeout(Integer)}.
     *
     * @return Number of seconds to wait for Transaction Latency Count
     * to be satisfied or {@code null}, meaning "use the
     * default value configured on the server".
     */
    public Integer getTransactionLatencyTimeout() {
        return transactionLatencyTimeout;
    }

    /**
     * Sets the durability level to a given number of instances.
     * <p>
     * The durability is a positive integer value that specifies how many instances must
     * have a commit ingested in order for that commit to be considered durable. The count
     * includes the instance that made the commit.
     * <p>
     * A durability setting of 1 means that when an instance makes a commit that
     * commit is immediately considered durable before even being sent to any other
     * instance (the commit will still be sent to the other instances after
     * it's considered durable).
     * <p>
     * A value that equals the total number of nodes in the cluster  means that every
     * instance must have ingested the commit before it's considered durable.
     * If one or more instances are stopped at the moment then the commit will not
     * become durable until the stopped instances are restarted.
     *
     * @param durability Number of instances or {@code null}, meaning "use the default
     *                   value configured on the server".
     * @return A fresh config instance with updated settings.
     */
    public TransactionSettings withDurability(final Integer durability) {
        final TransactionSettings result = new TransactionSettings(this);
        result.durability = durability;
        result.symbolicDurability = DurabilityLevel.DEFAULT;
        return result;
    }

    /**
     * Sets the durability level (see {@link #withDurability(Integer)}) to a symbolic value.
     *
     * @param durability Durability level name or {@code null}, meaning "use the default
     *                   value configured on the server".
     * @return A fresh config instance with updated settings.
     */
    public TransactionSettings withDurability(final DurabilityLevel durability) {
        final TransactionSettings result = new TransactionSettings(this);
        result.durability = null;
        result.symbolicDurability = durability == null ? DurabilityLevel.DEFAULT : durability;
        return result;
    }

    /**
     * Sets the distributed transaction timeout.
     * <p>
     * Use this setting to specify how long a commit call will wait for the commit
     * to become durable. It's a non-negative integer (number of seconds).
     * <p>
     * If the durability is greater than one then the committing process has
     * to wait for acknowledgements from the other servers that the transaction
     * was committed.  The committing process returns to the caller
     * when the durability has been reached or the distributed transaction timeout
     * seconds has passed, whichever comes first.
     * <p>
     * When the commit returns the caller does not know if durability has been
     * reached.
     *
     * @param distributedTransactionTimeout Timeout in seconds or {@code null}.
     *                                      {@code null} means 'use server config').
     * @return A fresh config instance with updated settings.
     */
    public TransactionSettings withDistributedTransactionTimeout(
            final Integer distributedTransactionTimeout) {
        final TransactionSettings result = new TransactionSettings(this);
        result.distributedTransactionTimeout = distributedTransactionTimeout;
        return result;
    }

    /**
     * Sets the distributed transaction latency count.
     * <p>
     * Use this setting to limit the number of non-durable (pending) commits
     * that can be active on the cluster. If this limit is reached all new
     * commits will signal an error (and have to be retried).
     * <p>
     * When a commit is done the committing process tries to wait until the
     * commit is durable but if that takes too long
     * (see {@link #withDistributedTransactionTimeout(Integer)}) then commit will return
     * with the system still working on making that transaction durable.
     * <p>
     * If the latency count is 4 then even if the last four commits are not
     * yet durable it is possible to do one more commit. But if there are
     * five pending transactions then any attempt to commit will result in
     * an error.
     * <p>
     * Another example: If you set the latency count to zero then
     * each commit must be durable before the next commit can be done.
     *
     * @param transactionLatencyCount number of commits or {@code null} (use server default).
     * @return A fresh config instance with updated settings.
     */
    public TransactionSettings withTransactionLatencyCount(
            final Integer transactionLatencyCount) {
        final TransactionSettings result = new TransactionSettings(this);
        result.transactionLatencyCount = transactionLatencyCount;
        return result;
    }

    /**
     * Sets the distributed transaction latency timeout.
     * <p>
     * Use this setting to specify how long a commit operation should
     * wait for the Transaction Latency Count to be satisfied before
     * throwing an error.
     *
     * @param transactionLatencyTimeout number of seconds or {@code null} (use server default).
     * @return A fresh config instance with updated settings.
     */
    public TransactionSettings withTransactionLatencyTimeout(
            final Integer transactionLatencyTimeout) {
        final TransactionSettings result = new TransactionSettings(this);
        result.transactionLatencyTimeout = transactionLatencyTimeout;
        return result;
    }
}
