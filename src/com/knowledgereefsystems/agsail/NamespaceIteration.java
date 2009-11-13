/* Copyright (C) 2008 Knowledge Reef Systems.  All rights reserved. */

package com.knowledgereefsystems.agsail;

import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.Namespace;
import org.openrdf.sail.SailException;

import info.aduna.iteration.CloseableIteration;

public class NamespaceIteration implements CloseableIteration<Namespace, SailException> {
    private String[] nsArray;
    private int cur, size;
    private boolean closed = false;

    public NamespaceIteration(final String[] nsArray) throws SailException {
        this.nsArray = nsArray;
        cur = 0;

        if (null == nsArray) {
            close();
        } else {
            size = nsArray.length / 2;
        }
    }

    public void close() throws SailException {
        nsArray = null;
        closed = true;
        size = 0;
    }

    public boolean hasNext() throws SailException {
        return (cur < size);
    }

    public Namespace next() throws SailException {
        if (closed) {
            throw new AllegroSailException("iterator has been closed");
        }

        String prefix = nsArray[cur * 2];
        String name = nsArray[cur * 2 + 1];

        Namespace ns = new NamespaceImpl(prefix, name);

        cur++;
        return ns;
    }

    /**
     * Does nothing.
     */
    public void remove() throws SailException {
        if (closed) {
            throw new AllegroSailException("iterator has been closed");
        }
    }

}
