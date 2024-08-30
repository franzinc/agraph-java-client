package com.franz.agraph.repository;

import com.franz.agraph.http.AGHttpRepoClient;

public enum AGTransactionState {

    /**
     * The default state of a new AGRepositoryConnection
     * {@link AGHttpRepoClient#isAutoCommit()} is true, but {@link AGRepositoryConnection#isActive()}) returns false
     */
    AUTOCOMMIT_DEFAULT,

    /**
     * {@link AGRepositoryConnection#setAutoCommit}(true) was explicitly called
     */
    AUTOCOMMIT_ENABLED,

    /**
     * State after {@link AGRepositoryConnection#begin} is called
     */
    ACTIVE,

    /**
     * State after {@link AGRepositoryConnection#commit} has completed
     */
    COMMIT_SUCCESSFUL,

    /**
     * State after {@link AGRepositoryConnection#rollback} has completed
     */
    ROLLBACK_SUCCESSFUL
}
