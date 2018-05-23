/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class AGTQRHandler extends AGResponseHandler {

    private final TupleQueryResultFormat format;
    private final TupleQueryResultHandler tqrhandler;
    private final AGValueFactory vf;

    public AGTQRHandler(TupleQueryResultFormat format, TupleQueryResultHandler tqrhandler, AGValueFactory vf, boolean recoverExternalBNodes) {
        super(format.getDefaultMIMEType());
        this.format = format;
        if (recoverExternalBNodes) {
            this.tqrhandler = recoverBNodesTQRHandler(tqrhandler);
        } else {
            this.tqrhandler = tqrhandler;
        }
        this.vf = vf;
    }

    @Override
    public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
        String mimeType = getResponseMIMEType(method);
        if (!mimeType.equals(getRequestMIMEType())) {
            throw new AGHttpException("unexpected response MIME type: " + mimeType);
        }
        InputStream response = getInputStream(method);
        try {
            TupleQueryResultParser parser = QueryResultIO.createTupleParser(format, vf);
            parser.setQueryResultHandler(recoverBNodesTQRHandler(tqrhandler));
            parser.parseQueryResult(response);
        } catch (QueryResultParseException | TupleQueryResultHandlerException e) {
            throw new AGHttpException(e);
        }
    }

    private TupleQueryResultHandler recoverBNodesTQRHandler(final TupleQueryResultHandler handler) {
        return new TupleQueryResultHandler() {

            @Override
            public void startQueryResult(List<String> arg0)
                    throws TupleQueryResultHandlerException {
                handler.startQueryResult(arg0);
            }

            @Override
            public void endQueryResult()
                    throws TupleQueryResultHandlerException {
                handler.endQueryResult();
            }

            @Override
            public void handleSolution(BindingSet arg0)
                    throws TupleQueryResultHandlerException {
                Set<String> names = arg0.getBindingNames();
                MapBindingSet sol = new MapBindingSet(names.size());
                for (String n : names) {
                    Value v = AGHttpRepoClient.getApplicationValue(arg0.getValue(n), vf);
                    if (v != null) {
                        sol.addBinding(n, v);
                    }
                }
                handler.handleSolution(sol);
            }

            @Override
            public void handleBoolean(boolean arg0)
                    throws QueryResultHandlerException {
                throw new QueryResultHandlerException("Unexpected boolean result");
            }

            @Override
            public void handleLinks(List<String> arg0)
                    throws QueryResultHandlerException {
                // ignore
            }
        };
    }
}
