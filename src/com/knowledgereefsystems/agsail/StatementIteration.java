/* Copyright (C) 2008 Knowledge Reef Systems.  All rights reserved. */

package com.knowledgereefsystems.agsail;

import org.openrdf.model.Statement;
import org.openrdf.sail.SailException;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;

import com.franz.agbase.AllegroGraphException;
import com.franz.agsail.AGSailCursor;

import info.aduna.iteration.CloseableIteration;

/**
 * @author josh
 */
public class StatementIteration implements CloseableIteration<Statement, SailException> {
    private AGSailCursor[] cursors;
    private int cursorIndex;
    private ValueFactory valueFactory;

    public StatementIteration(final AGSailCursor[] c, final ValueFactory vf) {
        cursors = c;
        cursorIndex = 0;
        this.valueFactory = vf;
    }

    /* (non-Javadoc)
      * @see info.aduna.iteration.CloseableIteration#close()
      */
    public void close() throws SailException {
        try {
            for (int i = cursorIndex; i < cursors.length; i++) {
                cursors[i].close();
            }
        } catch (Throwable t) {
            throw new AllegroSailException(t);
        }
    }

    /* (non-Javadoc)
      * @see info.aduna.iteration.Iteration#hasNext()
      */
    public boolean hasNext() throws SailException {
        if (cursorIndex >= cursors.length) {
            return false;
        }

        try {
            if (cursors[cursorIndex].hasNext()) {
                return true;
            } else {
                cursors[cursorIndex].close();
                cursorIndex++;
                if (cursorIndex < cursors.length) {
                    return cursors[cursorIndex].hasNext();
                } else {
                    return false;
                }
            }
        } catch (Throwable t) {
            throw new AllegroSailException(t);
        }
    }

    /* (non-Javadoc)
      * @see info.aduna.iteration.Iteration#next()
      */
    public Statement next() throws SailException {
        try {
//            Statement st = cursors[cursorIndex].next();

            // FIXME: temporary workaround for AG client 2.2.5
            AGSailCursor c = cursors[cursorIndex];
            c.step();
            Statement st = createStatement(c);

//            System.out.println("context = " + (Resource) st.getContext());
            return st;
        } catch (Throwable t) {
            throw new AllegroSailException(t);
        }
    }

    /* (non-Javadoc)
      * @see info.aduna.iteration.Iteration#remove()
      */
    public void remove() throws SailException {
        try {
            cursors[cursorIndex].remove();
        } catch (Throwable t) {
            throw new AllegroSailException(t);
        }
    }

    private Statement createStatement(final AGSailCursor c) throws AllegroGraphException {
/*        System.out.println("subject = " + c.getSubject());
        System.out.println("    predicate = " + c.getPredicate());
        System.out.println("    object = " + c.getObject());
System.out.println("    context = " + c.getContext() + ", class = " + c.getContext().getClass());*/
//        Object o = c.getContext();
//        Resource context = (null == o)
//                ? null : valueFactory.createURI(o.toString());
        Resource context = c.getContext();
        return (null == context)
                ? valueFactory.createStatement(c.getSubject(), c.getPredicate(), c.getObject())
                : valueFactory.createStatement(c.getSubject(), c.getPredicate(), c.getObject(), context);
    }
}
