/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.NiceIterator;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.io.Closeable;

/**
 * A utility class for iterating over Jena Triples.
 */
public class AGTripleIterator extends NiceIterator<Triple> implements Closeable {

    private final AGGraph graph;
    private final RepositoryResult<Statement> result;
    private Statement current = null;

    AGTripleIterator(AGGraph graph, RepositoryResult<Statement> result) {
        this.graph = graph;
        this.result = result;
    }

    @Override
    public void close() {
        try {
            result.close();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return result.hasNext();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Triple next() {
        Triple tr;
        try {
            current = result.next();
            tr = AGNodeFactory.asTriple(current);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return tr;
    }

    @Override
    public void remove() {
        if (current != null) {
            Triple tr = AGNodeFactory.asTriple(current);
            graph.delete(tr);
            // TODO the following only removes triples from the underlying
            // collection (in memory), rather than from the store.
            //result.remove();
            current = null;
        }
    }
}
