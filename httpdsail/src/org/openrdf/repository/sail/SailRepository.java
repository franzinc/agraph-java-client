/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository.sail;

import java.io.File;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailException;

/**
 * An implementation of the {@link Repository} interface that operates on a
 * (stack of) {@link Sail Sail} object(s). The behaviour of the repository is
 * determined by the Sail stack that it operates on; for example, the repository
 * will only support RDF Schema or OWL semantics if the Sail stack includes an
 * inferencer for this.
 * <p>
 * Creating a repository object of this type is very easy. For example, the
 * following code creates and initializes a main-memory store with RDF Schema
 * semantics:
 * 
 * <pre>
 * Repository repository = new RepositoryImpl(new ForwardChainingRDFSInferencer(new MemoryStore()));
 * repository.initialize();
 * </pre>
 * 
 * Or, alternatively:
 * 
 * <pre>
 * Sail sailStack = new MemoryStore();
 * sailStack = new ForwardChainingRDFSInferencer(sailStack);
 * 
 * Repository repository = new Repository(sailStack);
 * repository.initialize();
 * </pre>
 * 
 * @author Arjohn Kampman
 */
public class SailRepository implements Repository {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final Sail sail;
	
	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new repository object that operates on the supplied Sail.
	 * 
	 * @param sail
	 *        A Sail object.
	 */
	public SailRepository(Sail sail) {
		this.sail = sail;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public File getDataDir() {
		return sail.getDataDir();
	}

	public void setDataDir(File dataDir) {
		sail.setDataDir(dataDir);
	}

	public void initialize()
		throws RepositoryException
	{
		try {
			sail.initialize();
		}
		catch (SailException e) {
			throw new RepositoryException(e.getMessage(), e);
		}
	}

	public void shutDown()
		throws RepositoryException
	{
		try {
			sail.shutDown();
		}
		catch (SailException e) {
			throw new RepositoryException("Unable to shutdown Sail", e);
		}
	}

	/**
	 * Gets the Sail object that is on top of the Sail stack that this repository
	 * operates on.
	 * 
	 * @return A Sail object.
	 */
	public Sail getSail() {
		return sail;
	}

	public boolean isWritable()
		throws RepositoryException
	{
		try {
			return sail.isWritable();
		}
		catch (SailException e) {
			throw new RepositoryException("Unable to determine writable status of Sail", e);
		}
	}

	public ValueFactory getValueFactory() {
		return sail.getValueFactory();
	}

	public SailRepositoryConnection getConnection()
		throws RepositoryException
	{
		try {
			return new SailRepositoryConnection(this, sail.getConnection());
		}
		catch (SailException e) {
			throw new RepositoryException(e);
		}
	}
}
