package com.franz.agraph.repository;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import com.franz.util.Closeable;

public interface AGAbstractRepository extends Repository, Closeable {
	public String getSpec();
	public AGValueFactory getValueFactory();
	public AGRepositoryConnection getConnection() throws RepositoryException;
	public AGCatalog getCatalog();
}
