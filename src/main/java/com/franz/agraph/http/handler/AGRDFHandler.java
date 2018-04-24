/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class AGRDFHandler extends AGResponseHandler {

    private final RDFFormat format;
    private final RDFHandler rdfhandler;
    private final AGValueFactory vf;

    public AGRDFHandler(RDFFormat format, RDFHandler rdfhandler, AGValueFactory vf, boolean recoverExternalBNodes) {
        super(format.getDefaultMIMEType());
        this.format = format;
        if (recoverExternalBNodes) {
            this.rdfhandler = recoverBNodesRDFHandler(rdfhandler);
        } else {
            this.rdfhandler = rdfhandler;
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
        // Note: we ignore charset specified in the response.
        Reader reader = new InputStreamReader(response, StandardCharsets.UTF_8);
        try {
            RDFParser parser = Rio.createParser(format, vf);
            parser.setPreserveBNodeIDs(true);
            parser.setRDFHandler(rdfhandler);
            parser.parse(reader, method.getURI().getURI());
        } catch (RDFParseException | RDFHandlerException e) {
            throw new AGHttpException(e);
        }
    }

    private RDFHandler recoverBNodesRDFHandler(final RDFHandler handler) {
        return new RDFHandler() {

            @Override
            public void startRDF() throws RDFHandlerException {
                handler.startRDF();
            }

            @Override
            public void endRDF() throws RDFHandlerException {
                handler.endRDF();
            }

            @Override
            public void handleNamespace(String prefix, String uri)
                    throws RDFHandlerException {
                handler.handleNamespace(prefix, uri);
            }

            @Override
            public void handleStatement(Statement st)
                    throws RDFHandlerException {
                Resource s = AGHttpRepoClient.getApplicationResource(st.getSubject(), vf);
                Value o = AGHttpRepoClient.getApplicationValue(st.getObject(), vf);
                Resource c = AGHttpRepoClient.getApplicationResource(st.getContext(), vf);
                st = vf.createStatement(s, st.getPredicate(), o, c);
                handler.handleStatement(st);
            }

            @Override
            public void handleComment(String comment)
                    throws RDFHandlerException {
                handler.handleComment(comment);
            }
        };
    }
}
