package com.knowledgereefsystems.agsail;

import org.openrdf.sail.SailConnectionListener;
import org.openrdf.model.Statement;

/**
 * Author: josh
 * Date: Jun 27, 2008
 * Time: 6:30:09 PM
 */
class SimpleListener implements SailConnectionListener {
    private int added = 0, removed = 0;

    public void statementAdded(final Statement statement) {
        added++;
    }

    public void statementRemoved(final Statement statement) {
        removed++;
    }

    public int getAdded() {
        return added;
    }

    public int getRemoved() {
        return removed;
    }
}
