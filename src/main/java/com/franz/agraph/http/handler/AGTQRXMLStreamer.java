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
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Similar to {@link org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLParser}
 * but uses {@link XMLStreamReader} instead of SAXParser so the results
 * streaming in the http response can be processed in a
 * {@link TupleQueryResult} pulling from the stream.
 *
 * @since v4.3
 */
public class AGTQRXMLStreamer extends AGTQRStreamer {

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    private final AGValueFactory vf;
    private XMLStreamReader xml;
    private HttpMethod method;

    public AGTQRXMLStreamer(AGValueFactory vf) {
        super(TupleQueryResultFormat.SPARQL.getDefaultMIMEType());
        this.vf = vf;
    }

    @Override
    public String getRequestMIMEType() {
        return TupleQueryResultFormat.SPARQL.getDefaultMIMEType();
    }

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
    public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
        this.method = method;
        try {
            xml = xmlInputFactory.createXMLStreamReader(AGResponseHandler.getInputStream(method));
        } catch (XMLStreamException e) {
            throw new AGHttpException(e);
        }
    }

    @Override
    public TupleQueryResult getResult() {
        return new Result();
    }

    class Result implements TupleQueryResult {

        private List<String> bindingNames;

        private MapBindingSet next = null;
        private boolean closed = false;

        /**
         * This function reads the data from `XMLStreamReader xml` (if it has not been done already)
         * and parses it as binding names. This modifies the underlying stream.
         */
        private void parseBindingNames() {
            if (bindingNames == null) {
                try {
                    while (xml.hasNext()) {
                        switch (xml.next()) {
                            case XMLStreamConstants.START_ELEMENT:
                                switch (xml.getLocalName()) {
                                    case "head":
                                        bindingNames = new ArrayList<>();
                                        break;
                                    case "variable":
                                        for (int i = 0; i < xml.getAttributeCount(); i++) {
                                            if ("name".equals(xml.getAttributeLocalName(i))) {
                                                bindingNames.add(xml.getAttributeValue(i));
                                            }
                                        }
                                        break;
                                    case "sparql":
                                        continue;
                                    default:
                                        throw new RuntimeException("Unexpected tag: " + xml.getLocalName());
                                }
                                break;
                            case XMLStreamConstants.END_ELEMENT:
                                if ("head".equals(xml.getLocalName())) {
                                    return;
                                }
                                break;
                        }
                    }
                } catch (XMLStreamException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public List<String> getBindingNames() {
            parseBindingNames();
            return bindingNames;
        }

        @Override
        public boolean hasNext() throws QueryEvaluationException {
            if (next == null) {
                if (closed) {
                    return false;
                }
                parseBindingNames(); // must be done first
                try {
                    String bindingName = null;
                    while (xml.hasNext()) {
                        final int event = xml.next();
                        if (xml.isStartElement()) {
                            String name = xml.getLocalName();
                            if ("result".equals(name)) {
                                next = new MapBindingSet(bindingNames.size());
                            } else if ("results".equals(xml.getLocalName())) {
                                // continue
                            } else if ("binding".equals(name)) {
                                for (int i = 0; i < xml.getAttributeCount(); i++) {
                                    if ("name".equals(xml.getAttributeLocalName(i))) {
                                        bindingName = xml.getAttributeValue(i);
                                    }
                                }
                            } else if ("literal".equals(name)) {
                                String lang = null;
                                String datatype = null;
                                for (int i = 0; i < xml.getAttributeCount(); i++) {
                                    if ("lang".equals(xml.getAttributeLocalName(i))
                                            && "http://www.w3.org/XML/1998/namespace".equals(xml.getAttributeNamespace(i))) {
                                        lang = xml.getAttributeValue(i);
                                    } else if ("datatype".equals(xml.getAttributeLocalName(i))) {
                                        datatype = xml.getAttributeValue(i);
                                    }
                                }
                                String text = xml.getElementText();
                                Value value;
                                if (datatype != null) {
                                    try {
                                        value = vf.createLiteral(text, vf.createIRI(datatype));
                                    } catch (IllegalArgumentException e) {
                                        // Illegal datatype URI
                                        throw new QueryEvaluationException(e.getMessage(), e);
                                    }
                                } else if (lang != null) {
                                    value = vf.createLiteral(text, lang);
                                } else {
                                    value = vf.createLiteral(text);
                                }
                                next.addBinding(bindingName, value);
                            } else if ("uri".equals(name)) {
                                next.addBinding(bindingName, AGHttpRepoClient.getApplicationResource(vf.createIRI(xml.getElementText()), vf));
                            } else if ("bnode".equals(name)) {
                                next.addBinding(bindingName, vf.createBNode(xml.getElementText()));
                            } else {
                                log.warn("unknown elem: " + name + " attrs=" + xml.getAttributeCount());
                            }
                        } else if (xml.isEndElement()) {
                            if ("result".equals(xml.getLocalName())) {
                                return next != null;
                            } else if ("results".equals(xml.getLocalName())) {
                                // in normal use, closed will be called here
                                close();
                                return false;
                            } else if ("sparql".equals(xml.getLocalName())) {
                                close();
                                return false;
                            } else if (!"binding".equals(xml.getLocalName())) {
                                log.warn("unknown elem end: " + xml.getLocalName());
                            }
                        } else if (event == XMLStreamConstants.END_DOCUMENT) {
                            close();
                            return false;
                        }
                    }
                 } catch (XMLStreamException e) {
                    throw new QueryEvaluationException(e);
                 }
            }
            return next != null;
        }


        @Override
        public BindingSet next() throws QueryEvaluationException {
            if (hasNext()) {
                BindingSet curr = next;
                next = null;
                return curr;
            } else if (closed) {
                throw new RuntimeException("closed");
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void close() throws QueryEvaluationException {
            if (!closed) {
                closed = true;
                try {
                    method.getResponseBodyAsStream().close();
                    method.releaseConnection();
                } catch (IOException e) {
                    throw new QueryEvaluationException("I/O error closing resources", e);
                }
            }
        }

        @Override
        public void remove() throws QueryEvaluationException {
            throw new UnsupportedOperationException();
        }
    }

}
