package com.franz.ag.repository;

import java.io.File;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.SailException;

import com.franz.agbase.AllegroGraph;
import com.franz.agsail.util.AGSInternal;
import com.knowledgereefsystems.agsail.AllegroSail;

/**
 * An implementation of the Repository interface that stores triples in an AllegroGraph 
 * triple store.  The Java application communicates with the AllegroGraph server
 * through a socket.
 * <p>
 * 
 * @author mm
 *
 */
public class AGRepository implements Repository {
	
	/**
	 * The name of this class identifies the version of the AllegroGraph
	 * Java implementation.
	 * This name is also visible in the list of members in a jar file
	 * when it is inspected with Emacs or WinZip.
	 */
	public static class V3_2Jan27 {}
	
	/**
	 * Query the current AGRepository version.
	 * @return a version string.
	 */
	@SuppressWarnings("unchecked")
	public static String version () { 
		Class thisClass = AGRepository.class;
		Class[] mems = thisClass.getDeclaredClasses();
		String home = thisClass.getName();
		String s = "";
		home = home + "$V";
		for (int i = 0; i < mems.length; i++) {
			String sub = mems[i].getName();
			if ( sub.startsWith(home) )
				s = sub;
		}
		return s; }

	
	AllegroGraph store = null;
	AGSInternal ags = null;
	AllegroSail sail = null;
	
	/**
	 * Create a new Repository instance to access an AllegroGraph triple store.
	 * @param ts An {@link AllegroGraph} instance created by the application.
	 * The application code is responsible for closing the store instance and 
	 * disconnecting from the server.
	 */
	public AGRepository(AllegroGraph ts) {
		store = ts;
		sail = new AllegroSail(ts);
	}
	
	/**
	 * Specify whether indexing should be done at each commit.
	 * @param roc When true, index the triples added up to the commit call.
	 */
	public void setReindexOnCommit ( boolean roc ) {
		sail.setReindexOnCommit(roc);
	}
	
	/**
	 * Control the  indexing strategy of this access to the store.
	 * @param roc When true, reindex the entire store at each commit.
	 *   Otherwise, index only the new triples.
	 */
	public void setIndexAllOnCommit ( boolean roc ) {
		sail.setIndexAllOnCommit(roc);
	}
	
	/**
	 * Control the  indexing mode of this access to the store.
	 * @param bi When true, index triples asynchronously.
	 */
	public void setBackgroundIndexing ( boolean bi ) {
		sail.setBackgroundIndexing(bi);
	}
	
	public AllegroGraph getAllegroGraph() {
		return store;		
	}
	
	// Repository API below
	
	private AGRepositoryConnection repoConn = null;
	
	/**
	 * Opens a connection to this repository that can be used for querying and 
	 * updating the contents of the repository. 
	 * Created connections need to be closed to make sure that any resources they 
	 * keep hold of are released. 
	 * The best way to do this is to use a try-finally-block as follows: 
	 *   <pre>

 RepositoryConnection con = repository.getConnection();
 try {
        // perform operations on the connection
 }
 finally {
        con.close();
 }
 </pre>
     * @return A connection that allows operations on this repository.
     *    Only <strong>one</strong> connection instance is created for each Repository
     *    instance.
     * @throws RepositoryException - If something went wrong during the creation of the Connection.
	 */
	public RepositoryConnection getConnection() throws RepositoryException {
		if ( repoConn==null )
			repoConn = new AGRepositoryConnection(this);
		return repoConn;
	}

	public File getDataDir() {
		return sail.getDataDir();
	}

	public ValueFactory getValueFactory() {
		return sail.getValueFactory();
	}

	public void initialize() throws RepositoryException {
		try {
			sail.initialize();
			ags = sail.getAGSInternal();
		} catch (SailException e) {
			throw new RepositoryException(e.getLocalizedMessage());
		}	
	}

	public boolean isWritable() throws RepositoryException {
		try {
			return sail.isWritable();
		} catch (SailException e) {
			throw new RepositoryException(e.getLocalizedMessage());
		}
	}

	public void setDataDir(File arg0) {
		sail.setDataDir(arg0);
	}

	public void shutDown() throws RepositoryException {
		try {
			if ( repoConn!=null )
			{
				repoConn.close();
				repoConn = null;
			}
			sail.shutDown();
			sail = null;
		} catch (SailException e) {
			throw new RepositoryException(e.getLocalizedMessage());
		}
	}

}
