/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.io.Closeable;
import java.util.concurrent.ScheduledExecutorService;

/**
 *
 */
public interface AGAbstractRepository extends Repository, Closeable {

    String getSpec();

    AGValueFactory getValueFactory();

    AGRepositoryConnection getConnection() throws RepositoryException;

    AGRepositoryConnection getConnection(ScheduledExecutorService executor)
            throws RepositoryException;

    AGCatalog getCatalog();

    AGServer getServer();

}
