package com.franz.agraph.http.handler;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

//TODO: make it so it uses only RDF4J.
public abstract class AGTQRStreamer extends AGResponseHandler {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected AGValueFactory vf;
    protected ResultSet resultSet;
    protected HttpMethod method;

    public AGTQRStreamer(String mimeType) {
        super(mimeType);
    }

    @Override
    public abstract String getRequestMIMEType();

    /**
     * False because the Result will release the HTTP resources.
     * For most responses, AGHTTPClient releases resources after
     * calling {@link #handleResponse(HttpMethod)},
     * but here the results are pulled when needed from the {@link Result}.
     */
    @Override
    public boolean releaseConnection() {
        return false;
    }

    @Override
    public abstract void handleResponse(HttpMethod method) throws IOException, AGHttpException;

    public TupleQueryResult getResult() {
        return new AGTQRStreamer.Result();
    }

    public static AGTQRStreamer createStreamer(TupleQueryResultFormat format, AGValueFactory vf) {
        if (format.equals(TupleQueryResultFormat.TSV)) {
            return new AGTQRTSVStreamer(vf);
        } else if (format.equals(TupleQueryResultFormat.SPARQL)) {
            return new AGTQRXMLStreamer(vf);
        } else if (format.equals(TupleQueryResultFormat.JSON)) {
            return new AGTQRJSONStreamer(vf);
        } else {
            throw new IllegalArgumentException("Unable to find AGTQRStreamer for format " + format);
        }
    }

    class Result implements TupleQueryResult {

        private List<String> bindingNames;

        private MapBindingSet next = null;
        private boolean closed = false;

        @Override
        public List<String> getBindingNames() {
            if (bindingNames == null) {
                bindingNames = resultSet.getResultVars();
            }
            return bindingNames;
        }

        @Override
        public BindingSet next() throws QueryEvaluationException {
            Binding b = resultSet.nextBinding();
            next = new MapBindingSet(b.size());
            Iterator<Var> it = b.vars();
            while (it.hasNext()) {
                Var var = it.next();
                String bindingName = var.getVarName();
                Node node = b.get(var);
                if (node.isLiteral()) {
                    next.addBinding(bindingName, vf.createLiteral(node.getLiteralLexicalForm()));
                } else if (node.isURI()) {
                    next.addBinding(bindingName, AGHttpRepoClient.getApplicationResource(vf.createIRI(node.getURI()), vf));
                } else if (node.isBlank()) {
                    next.addBinding(bindingName, vf.createBNode(node.getBlankNodeLabel()));
                } else {
                    log.warn("unknown elem: " + node.toString(true));
                }
            }
            return next;
        }

        @Override
        public boolean hasNext() throws QueryEvaluationException {
            return resultSet.hasNext();
        }

        @Override
        public void close() throws QueryEvaluationException {
            if (!closed) {
                closed = true;
                method.releaseConnection();
            }
        }

        @Override
        public void remove() throws QueryEvaluationException {
            throw new UnsupportedOperationException();
        }
    }

}
