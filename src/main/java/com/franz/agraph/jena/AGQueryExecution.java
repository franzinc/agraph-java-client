/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import com.franz.agraph.repository.AGBooleanQuery;
import com.franz.agraph.repository.AGGraphQuery;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.agraph.repository.AGUpdate;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResult;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.UpdateExecutionException;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implements the Jena QueryExecution interface for AllegroGraph.
 */
public class AGQueryExecution implements QueryExecution, Closeable {

    private static final long TIMEOUT_UNSET = -1;
    private final AGQuery query;
    private final AGModel model;
    protected long timeout = TIMEOUT_UNSET;
    private QuerySolution binding;
    private boolean closed = false;
    // When we close this execution object, we must also close
    // the result object, but only for select queries. Ask and describe
    // results are read completely into models and remain valid after
    // close, as described in javadoc for QueryExecution#close()
    private QueryResult<?> resultToClose;

    public AGQueryExecution(AGQuery query, AGModel model) {
        this.query = query;
        this.model = model;
    }


    @Override
    public void abort() {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (resultToClose != null) {
                 resultToClose.close();
                 resultToClose = null;
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean execAsk() {
        if (query.getLanguage() != QueryLanguage.SPARQL) {
            throw new UnsupportedOperationException(query.getLanguage().getName() + " language does not support ASK queries.");
        }
        AGBooleanQuery bq = model.getGraph().getConnection().prepareBooleanQuery(query.getLanguage(), query.getQueryString());
        bq.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
        bq.setEntailmentRegime(model.getGraph().getEntailmentRegime());
        bq.setCheckVariables(query.isCheckVariables());
        if (binding != null) {
            Iterator<String> vars = binding.varNames();
            while (vars.hasNext()) {
                String var = vars.next();
                bq.setBinding(var, model.getGraph().vf.asValue(binding.get(var).asNode()));
            }
        }
        boolean result;
        try {
            bq.setDataset(model.getGraph().getDataset());
            if (timeout > 0) {
                bq.setMaxExecutionTime((int) (timeout / 1000));
            }
            result = bq.evaluate();
        } catch (QueryEvaluationException e) {
            throw new QueryException(e);
        }
        return result;
    }

    @Override
    public Model execConstruct() {
        return execConstruct(null);
    }

    private GraphQueryResult getConstructResult() {
        if (query.getLanguage() != QueryLanguage.SPARQL) {
            throw new UnsupportedOperationException(query.getLanguage().getName() + " language does not support CONSTRUCT queries.");
        }
        AGGraphQuery gq = model.getGraph().getConnection().prepareGraphQuery(query.getLanguage(), query.getQueryString());
        gq.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
        gq.setEntailmentRegime(model.getGraph().getEntailmentRegime());
        gq.setCheckVariables(query.isCheckVariables());
        gq.setLimit(query.getLimit());
        gq.setOffset(query.getOffset());
        if (binding != null) {
            Iterator<String> vars = binding.varNames();
            while (vars.hasNext()) {
                String var = vars.next();
                gq.setBinding(var, model.getGraph().vf.asValue(binding.get(var).asNode()));
            }
        }
        GraphQueryResult result;
        try {
            gq.setDataset(model.getGraph().getDataset());
            if (timeout > 0) {
                gq.setMaxExecutionTime((int) (timeout / 1000));
            }
            result = gq.evaluate();
        } catch (QueryEvaluationException e) {
            throw new QueryException(e);
        }
        return result;
    }

    @Override
    public Model execConstruct(Model m) {
        GraphQueryResult result = getConstructResult();
        if (m == null) {
            m = ModelFactory.createDefaultModel();
        }
        try {
            m.setNsPrefixes(result.getNamespaces());
            while (result.hasNext()) {
                m.add(model.asStatement(AGNodeFactory.asTriple(result.next())));
            }
        } catch (QueryEvaluationException e) {
            throw new QueryException(e);
        }
        return m;
    }

    @Override
    public Model execDescribe() {
        return execDescribe(null);
    }

    @Override
    public Model execDescribe(Model m) {
        return execConstruct(m);
    }

    @Override
    public ResultSet execSelect() {
        AGTupleQuery tq = model.getGraph().getConnection().prepareTupleQuery(query.getLanguage(), query.getQueryString());
        tq.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
        tq.setEntailmentRegime(model.getGraph().getEntailmentRegime());
        tq.setCheckVariables(query.isCheckVariables());
        tq.setLimit(query.getLimit());
        tq.setOffset(query.getOffset());
        if (binding != null) {
            Iterator<String> vars = binding.varNames();
            while (vars.hasNext()) {
                String var = vars.next();
                tq.setBinding(var, model.getGraph().vf.asValue(binding.get(var).asNode()));
            }
        }
        TupleQueryResult result;
        try {
            tq.setDataset(model.getGraph().getDataset());
            if (timeout > 0) {
                tq.setMaxExecutionTime((int) (timeout / 1000));
            }
            result = tq.evaluate();
        } catch (QueryEvaluationException e) {
            throw new QueryException(e);
        }
        resultToClose = result;
        return new AGResultSet(result, model);
    }

    /**
     * Executes SPARQL Update.
     * <p>
     * Executes as a <a href="http://www.w3.org/TR/sparql11-update/">SPARQL Update</a>
     * to modify the model/repository.
     */
    public void execUpdate() {
        AGUpdate u = model.getGraph().getConnection().prepareUpdate(query.getLanguage(), query.getQueryString());
        u.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
        u.setEntailmentRegime(model.getGraph().getEntailmentRegime());
        u.setCheckVariables(query.isCheckVariables());
        u.setLimit(query.getLimit());
        u.setOffset(query.getOffset());
        if (binding != null) {
            Iterator<String> vars = binding.varNames();
            while (vars.hasNext()) {
                String var = vars.next();
                u.setBinding(var, model.getGraph().vf.asValue(binding.get(var).asNode()));
            }
        }
        try {
            u.setDataset(model.getGraph().getDataset());
            if (timeout > 0) {
                u.setMaxExecutionTime((int) (timeout / 1000));
            }
            u.execute();
        } catch (UpdateExecutionException e) {
            throw new QueryException(e);
        }
    }

    public long countSelect() {
        AGTupleQuery tq = model.getGraph().getConnection().prepareTupleQuery(query.getLanguage(), query.getQueryString());
        tq.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
        tq.setEntailmentRegime(model.getGraph().getEntailmentRegime());
        tq.setCheckVariables(query.isCheckVariables());
        tq.setLimit(query.getLimit());
        tq.setOffset(query.getOffset());
        tq.setDataset(model.getGraph().getDataset());
        if (binding != null) {
            Iterator<String> vars = binding.varNames();
            while (vars.hasNext()) {
                String var = vars.next();
                tq.setBinding(var, model.getGraph().vf.asValue(binding.get(var).asNode()));
            }
        }
        long count;
        try {
            count = tq.count();
        } catch (QueryEvaluationException e) {
            throw new QueryException(e);
        }
        return count;
    }

    public long countConstruct() {
        if (query.getLanguage() != QueryLanguage.SPARQL) {
            throw new UnsupportedOperationException(query.getLanguage().getName() + " language does not support CONSTRUCT queries.");
        }
        AGGraphQuery gq = model.getGraph().getConnection().prepareGraphQuery(query.getLanguage(), query.getQueryString());
        gq.setIncludeInferred(model.getGraph() instanceof AGInfGraph);
        gq.setEntailmentRegime(model.getGraph().getEntailmentRegime());
        gq.setCheckVariables(query.isCheckVariables());
        gq.setLimit(query.getLimit());
        gq.setOffset(query.getOffset());
        gq.setDataset(model.getGraph().getDataset());
        if (binding != null) {
            Iterator<String> vars = binding.varNames();
            while (vars.hasNext()) {
                String var = vars.next();
                gq.setBinding(var, model.getGraph().vf.asValue(binding.get(var).asNode()));
            }
        }
        long count;
        try {
            count = gq.count();
        } catch (QueryEvaluationException e) {
            throw new QueryException(e);
        }
        return count;
    }

    @Override
    public Context getContext() {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public Dataset getDataset() {
        throw new UnsupportedOperationException(AGUnsupportedOperation.message);
    }

    @Override
    public void setInitialBinding(QuerySolution binding) {
        this.binding = binding;
    }

    @Override
    public Iterator<Triple> execConstructTriples() {
        try (GraphQueryResult result = getConstructResult()) {
            List<Triple> tripleArrayList = new ArrayList<>();
            while (result.hasNext()) {
                tripleArrayList.add(AGNodeFactory.asTriple(result.next()));
            }
            //Getting Iterator from List
            return tripleArrayList.iterator();
        } catch (QueryEvaluationException e) {
            throw new QueryException(e);
        }
    }

    @Override
    public Iterator<Triple> execDescribeTriples() {
        return execConstructTriples();
    }


    @Override
    public Query getQuery() {
        return QueryFactory.create(query.getQueryString());
    }


    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }


    @Override
    public void setTimeout(long arg0, TimeUnit arg1) {
        this.timeout = arg1.toMillis(arg0);
    }


    @Override
    public void setTimeout(long arg0, long arg1) {
        setTimeout(arg0, TimeUnit.MILLISECONDS, arg1, TimeUnit.MILLISECONDS);
    }


    @Override
    public void setTimeout(long timeout1, TimeUnit timeUnit1, long timeout2, TimeUnit timeUnit2) {
        this.timeout = asMillis(timeout1, timeUnit1);
    }

    private long asMillis(long duration, TimeUnit timeUnit) {
        return (duration < 0) ? duration : timeUnit.toMillis(duration);
    }

    @Override
    public long getTimeout1() {
        return 0;
    }


    @Override
    public long getTimeout2() {
        return 0;
    }

    @Override
    public Iterator<Quad> execConstructQuads() {
        try (GraphQueryResult result = getConstructResult()) {
            List<Quad> quadArrayList = new ArrayList<>();
            while (result.hasNext()) {
                quadArrayList.add(AGNodeFactory.asQuad(result.next()));
            }
            //Getting Iterator from List
            return quadArrayList.iterator();
        } catch (QueryEvaluationException e) {
            throw new QueryException(e);
        }
    }

    @Override
    public Dataset execConstructDataset() {
        final Dataset result = DatasetFactory.createGeneral();
        execConstructDataset(result);
        return result;
    }

    @Override
    public Dataset execConstructDataset(Dataset dataset) {
        DatasetGraph dsg = dataset.asDatasetGraph();
        try {
            execConstructQuads().forEachRemaining(dsg::add);
        } finally {
            this.close();
        }
        return dataset;
    }

}
