/* Copyright (C) 2008 Knowledge Reef Systems.  All rights reserved. */

package com.knowledgereefsystems.agsail;

import info.aduna.iteration.CloseableIteration;
import junit.framework.TestCase;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.net.URL;

public abstract class AllegroSailTestCase extends TestCase {

    protected AllegroSail sail;

    public void setUp() throws Exception {
        String host = "localhost";
        int port = 4567;
        boolean start = false;
        boolean reindexOnCommit = false;
        boolean syncOnInsert = false;
        String name = "test";
        File directory = new File("/opt/allegro");

        sail = new AllegroSail(host, port, start, name, directory, 0, 0, reindexOnCommit, syncOnInsert);
        // Note: this sail is never shut down.
        sail.initialize();

        // Remove any previous test data.
        SailConnection sc = sail.getConnection();
        sc.removeStatements(null, null, null);
        sc.commit();
        sc.close();
    }

    public void tearDown() throws Exception {
        sail.shutDown();
    }

    public AllegroSailTestCase(final String name) throws Exception {
        super(name);
    }

    protected void loadTrig(final URL url) throws Exception {
        Repository repo = new SailRepository(sail);
        RepositoryConnection rc = repo.getConnection();
        rc.add(url, "", RDFFormat.TRIG);
        rc.commit();
        rc.close();
    }
    
    protected void loadTrig(final File url) throws Exception {
        Repository repo = new SailRepository(sail);
        RepositoryConnection rc = repo.getConnection();
        rc.add(url, "", RDFFormat.TRIG);
        rc.commit();
        rc.close();
    }

    protected <T> Set<T> toSet(final CloseableIteration<? extends T, SailException> iter) throws SailException {
        Set<T> set = new HashSet<T>();
        while (iter.hasNext()) {
            set.add(iter.next());
        }
        iter.close();
        return set;
    }

    protected int count(CloseableIteration<?, SailException> statements) throws SailException {
        int count = 0;

        while (statements.hasNext()) {
            statements.next();
            count++;
        }

        statements.close();
        return count;
    }
}