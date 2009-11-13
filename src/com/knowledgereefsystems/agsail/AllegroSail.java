/* Copyright (C) 2008 Knowledge Reef Systems.  All rights reserved. */

package com.knowledgereefsystems.agsail;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.AllegroGraph;
import com.franz.agsail.AGForSail;
import com.franz.agsail.util.AGSInternal;

import org.openrdf.model.ValueFactory;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailChangedListener;
import org.openrdf.sail.SailException;
import org.openrdf.sail.memory.model.MemValueFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * This class implements a Sesame 2.x Sail interface to an AllegroGraph
 * triple store. 
 * <p>
 * The constructor is the only unique implementation in this class.
 * <p>
 * The methods are described in the Sesame documentation.
 */
public class AllegroSail implements Sail {
    private static final boolean DEBUG = false;

    private final Set<SailChangedListener> listeners = new HashSet<SailChangedListener>();

    private final String host;
    private final int port;
    private final boolean start;
    private final String name;
    private final File directory;
    private boolean reindexOnCommit = false;
    private boolean indexAllOnCommit = false;
    private boolean backgroundIndexing = false;
    private final boolean syncOnInsert;

    private int lookahead = -1;
    private int selectLimit = -1;

    // Persists for the lifetime of the Sail.
    private AllegroGraphConnection agConnection = null;

    // A single AllegroGraph object (the basis of an AllegroSailConnection)
    // which also serves as a ValueFactory.
    private AGForSail aGraph = null;
    private AllegroGraph directInstance = null;

    private ValueFactory valueFactory;

	private AllegroGraph agpreset = null;

    /**
     * Create a Sail instance to an AllegroGaph triple store.
     * Each instance is serviced by a separate connection to the server.
     * 
     * @param host        network host serving AllegroGraph
     * @param port        AllegroGraph port
     * @param start       whether to start a new server
     * @param name        name of the triple store
     * @param directory   parent directory of the triple store
     * @param lookahead   value for search operations. If less than
     *                    1, a default value will be used
     * @param selectLimit the value of the AllegroGraph's select limit
     *                    parameter. If less than 1, a default value will be used.
     * @param reindexOnCommit when true, re-index the store after each commit
     * @param syncOnInsert when true, synchronize the store after every insert operation
     */
    public AllegroSail(final String host,
                       final int port,
                       final boolean start,
                       final String name,
                       final File directory,
                       final int lookahead,
                       final int selectLimit,
                       final boolean reindexOnCommit,
                       final boolean syncOnInsert) {
        this.host = host;
        this.port = port;
        this.start = start;
        this.name = name;
        this.directory = directory;
        this.lookahead = lookahead;
        this.selectLimit = selectLimit;
        this.reindexOnCommit = reindexOnCommit;
        this.syncOnInsert = syncOnInsert;
    }
    
    /**
     * Create a Sail instance for an AllegroGraph triple store.
     * The application program creates the AllegroGraph and AllegroGraphConnection
     * instances needed to reach the server.  The application program is
     * responsible for disabling the connection and closing the triple store.
     * @param ag an AllegroGraph instance already connected to the server.
     */
    public AllegroSail ( AllegroGraph ag ) {
      agpreset = ag;
      syncOnInsert = ag.getSyncEveryTime();
      name = ag.storeName;
      directory = new File(ag.storeDirectory);
      port = ag.getConnection().getPort();
      host = ag.getConnection().getHost();
      start = false;
      
    }

    public AGSInternal getAGSInternal() {
    	return aGraph;
    }
    
    public void addSailChangedListener(final SailChangedListener listener) {
        listeners.add(listener);
    }

    private AllegroSailConnection sailConn = null;
    /**
     * Opens a connection on the Sail which can be used to query and update data.
     * @return A connection that allows operations on this store.
     *    Only <strong>one</strong> connection instance is created for each Sail
     *    instance.
     */
    public AllegroSailConnection getConnection() throws SailException {
        if ( sailConn==null )
        	sailConn = new AllegroSailConnection(aGraph, agConnection, this,  listeners );
        return sailConn;
    }

    public File getDataDir() {
        return directory;
    }

    public ValueFactory getValueFactory() {
        return valueFactory;
    }

    public void initialize() throws SailException {
    	if ( agpreset==null )
    	{
    		try {
    			agConnection = new AllegroGraphConnection();
    			agConnection.setPort(port);
    			agConnection.setHost(host);
    			agConnection.setDebug(DEBUG ? 1 : 0);

    			if (start) {
    				agConnection.startServer();
    			}
    			agConnection.enable();

    			directInstance = agConnection.access(name, directory.getAbsolutePath());
    			
//  			System.out.println("Connected to " + agConnection);
    		}

    		catch (IOException e) {
    			throw new AllegroSailException(e);
    		}

    		catch (AllegroGraphException e) {
    			throw new AllegroSailException(e);
    		}
    		//return;  //mm: way too soon to return!
    	}
    	else
    	{
    		directInstance = agpreset;
    		agConnection = directInstance.getConnection();
    	}
    	aGraph = new AGForSail(directInstance);
		aGraph.setSyncEveryTime(this.syncOnInsert);

		//valueFactory = new AllegroValueFactory(aGraph);
		valueFactory = new MemValueFactory();  //mm: AGForSail implements ValueFactory
		//valueFactory = aGraph;    //mm: but AGForSail is not fully implemented

		if (lookahead > 0) {
			aGraph.setLookAhead(lookahead);
		} else {
			lookahead = aGraph.getLookAhead();
		}

		if (selectLimit > 0) {
			aGraph.setSelectLimit(selectLimit);
		} else {
			selectLimit = aGraph.getSelectLimit();
		}
    	
    }

    public boolean isWritable() throws SailException {
        return true;
    }

    public void removeSailChangedListener(final SailChangedListener listener) {
        listeners.remove(listener);
    }

    public void setDataDir(final File dataDir) {
        // Data directory cannot be reset.
    }

    // Deprecated.
    public void setParameter(@SuppressWarnings("unused")
	final String key,
                             @SuppressWarnings("unused")
							final String value) {
    }

    public void shutDown() throws SailException {
        if (null == agConnection) {
            throw new SailException("Sail has not been initialized");
        }
        if ( agpreset==null )
        	agConnection.disable();
        
        agConnection = null;
    }

	public boolean isReindexOnCommit() {
		return reindexOnCommit;
	}
	public void setReindexOnCommit ( boolean roc ) {
		reindexOnCommit = roc;
	}

	public boolean isIndexAllOnCommit() {
		return indexAllOnCommit;
	}

	public void setIndexAllOnCommit(boolean indexAllOnCommit) {
		this.indexAllOnCommit = indexAllOnCommit;
	}

	public boolean isBackgroundIndexing() {
		return backgroundIndexing;
	}

	public void setBackgroundIndexing(boolean backgroundIndexing) {
		this.backgroundIndexing = backgroundIndexing;
	}
}
