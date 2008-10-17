/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository.sail;

import java.io.File;

import miniclient.Catalog;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailException;

import franz.exceptions.ServerException;
import franz.exceptions.UnimplementedMethodException;

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
public class AllegroRepository {

	private String accessVerb = null;
	private Catalog miniCatalog = null;
	private miniclient.Repository miniRepository;
	private String repositoryName = null; 
	private SailRepository sailRepository = null;
	private SailRepositoryConnection repositoryConnection = null;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Create a new Sail that operates on the triple store that 'miniRepository'
	 * connects to.
	 */
	public AllegroRepository(Catalog catalog, String repositoryName, String accessVerb) {
		this.miniCatalog = catalog;
		this.repositoryName = repositoryName;
		this.accessVerb = accessVerb;		
	}

	/*---------*
	 * Methods *
	 *---------*/

	public File getDataDir() {
		throw new UnimplementedMethodException("getDataDir");
	}

	public void setDataDir(File dataDir) {
		throw new UnimplementedMethodException("setDataDir");
	}

	public void initialize() throws RepositoryException {
        System.out.println("ATTACH" + this.accessVerb + " " + this.miniCatalog.listTripleStores());
        boolean clearIt = false;
        String repName = this.repositoryName;
        Catalog conn = this.miniCatalog;
        if (this.accessVerb == SailRepository.RENEW) {
            if (conn.listTripleStores().contains(repName)) {
                // not nice, since someone else probably has it open:
                clearIt = true;
            } else {
                conn.createTripleStore(repName);
            }
        } else if (this.accessVerb == SailRepository.CREATE) {
            if (conn.listTripleStores().contains(repName)) {
                throw new ServerException(
                    "Can't create triple store named '" + repName + "' because a store with that name already exists.");
            }
            conn.createTripleStore(repName);
        } else if (this.accessVerb == SailRepository.OPEN) {
            if (!conn.listTripleStores().contains(repName)) {
                throw new ServerException(
                    "Can't open a triple store named '" + repName + "' because there is none.");
            }
        } else if(this.accessVerb == SailRepository.ACCESS) {
            if (!conn.listTripleStores().contains(repName)) {
                conn.createTripleStore(repName) ;
            }
        }
        this.miniRepository = conn.getRepository(repName);
        // we are done unless a RENEW requires us to clear the store
        if (clearIt) {
            this.miniRepository.deleteMatchingStatements(null, null, null, null);
        }
	}

	public void shutDown() {
		this.miniCatalog = null;
		this.miniRepository = null;
	}


	public boolean isWritable() {
		return this.miniRepository.isWriteable();
	}

	public ValueFactory getValueFactory() {
		return sail.getValueFactory();
	}

	public SailRepositoryConnection getConnection() {
		this.verify();
		if (this.repositoryConnection == null) {
			this.repositoryConnection = new SailRepositoryConnection(this.sailRepository);
		}
		return this.repositoryConnection;
	}
}
