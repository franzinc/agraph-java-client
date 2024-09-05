/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.engine.binding.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Implements the Jena ResultSet interface for AllegroGraph.
 */
public class AGResultSet implements ResultSet {

    private final TupleQueryResult result;
    private final AGModel model;

    public AGResultSet(TupleQueryResult result, AGModel model) {
        this.result = result;
        this.model = model;
    }

    @Override
    public Model getResourceModel() {
        return model;
    }

    @Override
    public void close() {
        this.model.close();
    }

    @Override
    public List<String> getResultVars() {
        try {
            return result.getBindingNames();
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getRowNumber() {
        throw new AGUnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
        boolean res;
        try {
            res = result.hasNext();
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    @Override
    public AGQuerySolution next() {
        BindingSet bs;
        try {
            bs = result.next();
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
        return new AGQuerySolution(bs, model);
    }

    @Override
    public void forEachRemaining(Consumer<? super QuerySolution> action) {
        Objects.requireNonNull(action);
        while (this.hasNext()) {
            action.accept(this.next());
        }
    }

    @Override
    /**
     * This method is not supported.  Use next() instead and iterate
     * over the returned QuerySolution.
     *
     * @see #next()
     *
     */
    public Binding nextBinding() {
        throw new AGUnsupportedOperationException();
    }

    @Override
    public AGQuerySolution nextSolution() {
        return next();
    }

    @Override
    public void remove() {
        try {
            result.remove();
        } catch (QueryEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

}
