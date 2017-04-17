/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.agraph.repository;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import com.franz.util.Closeable;

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

}
