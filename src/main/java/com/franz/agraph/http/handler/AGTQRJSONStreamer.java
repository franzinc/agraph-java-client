/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.http.handler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.UnsupportedQueryResultFormatException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Similar to {@link org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLParser}
 * but uses rdf4j-like JSON parsing instead of SAXParser so the results
 * streaming in the http response can be processed in a
 * {@link TupleQueryResult} pulling from the JSON stream.
 *
 * @since v2.1.0
 */
public class AGTQRJSONStreamer extends AGTQRStreamer {
    private InputStream in;

    public AGTQRJSONStreamer(AGValueFactory vf) {
        super(TupleQueryResultFormat.JSON.getDefaultMIMEType());
        this.vf = vf;
    }

    @Override
    public String getRequestMIMEType() { return TupleQueryResultFormat.JSON.getDefaultMIMEType(); }

    @Override
    public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
        this.method = method;
        in = AGResponseHandler.getInputStream(method);
    }

    @Override
    public TupleQueryResult getResult() {
        return new Result();
    }

    private class Result implements TupleQueryResult {
        private BindingSet next;
        private InputStreamReader r;
        private BufferedReader reader;
        private List<String> bindingNames;

        private final JsonParser jp;
        private final JsonFactory JSON_FACTORY;

        public Result() {
            r = new InputStreamReader(in, Charset.forName("UTF-8"));
            reader = new BufferedReader(r);

            JSON_FACTORY = new JsonFactory();
            JSON_FACTORY.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
            JSON_FACTORY.disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES);
            JSON_FACTORY.disable(com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            try {
                jp = JSON_FACTORY.createParser(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<String> getBindingNames() {
            try {
                parseBindingNames();
            } catch (IOException e) {
                throw new UnsupportedQueryResultFormatException(e);
            }
            return bindingNames;
        }

        @Override
        public void remove() throws QueryEvaluationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            try {
                if (next == null) {
                    next = parseNext();
                }
            } catch (IOException e) {
                throw new UnsupportedQueryResultFormatException(e);
            }
            return next != null;
        }

        @Override
        public BindingSet next() throws QueryEvaluationException {
            if (hasNext()) {
                BindingSet curr = next;
                next = null;
                return curr;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void close() throws QueryEvaluationException {
            method.releaseConnection();
        }

        private void parseBindingNames() throws IOException {
            if (bindingNames == null) {
                while (jp.nextToken() != JsonToken.END_OBJECT) {
                    String baseStr = jp.getCurrentName();
                    if (baseStr != null && baseStr.equals("head")) {
                        if (jp.nextToken() != JsonToken.START_OBJECT) {
                            throw new QueryResultParseException("Did not find object under " + baseStr + " field", (long) jp.getCurrentLocation().getLineNr(), (long) jp.getCurrentLocation().getColumnNr());
                        }
                        if (jp.nextToken() == JsonToken.END_OBJECT) {
                            break;
                        }
                        bindingNames = new ArrayList<>();
                        String headStr = jp.getCurrentName();
                        if (headStr.equals("vars")) {
                            if (jp.nextToken() != JsonToken.START_ARRAY) {
                                throw new QueryResultParseException("Expected variable labels to be an array", (long) jp.getCurrentLocation().getLineNr(), (long) jp.getCurrentLocation().getColumnNr());
                            }

                            while (jp.nextToken() != JsonToken.END_ARRAY) {
                                bindingNames.add(jp.getText());
                            }
                        } else {
                            throw new QueryResultParseException("Found unexpected object in head field: " + headStr, (long) jp.getCurrentLocation().getLineNr(), (long) jp.getCurrentLocation().getColumnNr());
                        }
                    }

                }
            }
        }

        private MapBindingSet parseNext() throws IOException, QueryResultParseException, QueryResultHandlerException {
            MapBindingSet bindings = null;
            parseBindingNames();
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                if (jp.getCurrentName().equals("bindings")) {
                    if (jp.nextToken() != JsonToken.START_ARRAY) {
                        throw new QueryResultParseException("Found unexpected token in bindings object", (long) jp.getCurrentLocation().getLineNr(), (long) jp.getCurrentLocation().getColumnNr());
                    }

                    while (jp.nextToken() != JsonToken.END_ARRAY) {
                        bindings = new MapBindingSet();
                        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
                            throw new QueryResultParseException("Did not find object in bindings array: " + jp.getCurrentName(), (long) jp.getCurrentLocation().getLineNr(), (long) jp.getCurrentLocation().getColumnNr());
                        }

                        while (jp.nextToken() != JsonToken.END_OBJECT) {
                            if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
                                throw new QueryResultParseException("Did not find binding name", (long) jp.getCurrentLocation().getLineNr(), (long) jp.getCurrentLocation().getColumnNr());
                            }

                            String bindingStr = jp.getCurrentName();
                            if (jp.nextToken() != JsonToken.START_OBJECT) {
                                throw new QueryResultParseException("Did not find object for binding value", (long) jp.getCurrentLocation().getLineNr(), (long) jp.getCurrentLocation().getColumnNr());
                            }

                            String lang = null;
                            String type = null;
                            String datatype = null;
                            String value = null;

                            while (jp.nextToken() != JsonToken.END_OBJECT) {
                                if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
                                    throw new QueryResultParseException("Did not find value attribute under " + bindingStr + " field", (long) jp.getCurrentLocation().getLineNr(), (long) jp.getCurrentLocation().getColumnNr());
                                }

                                String fieldName = jp.getCurrentName();
                                jp.nextToken();
                                if ("type".equals(fieldName)) {
                                    type = jp.getText();
                                } else if ("xml:lang".equals(fieldName)) {
                                    lang = jp.getText();
                                } else if ("datatype".equals(fieldName)) {
                                    datatype = jp.getText();
                                } else {
                                    if (!"value".equals(fieldName)) {
                                        throw new QueryResultParseException("Unexpected field name: " + fieldName, (long) jp.getCurrentLocation().getLineNr(), (long) jp.getCurrentLocation().getColumnNr());
                                    }

                                    value = jp.getText();
                                }
                            }

                            bindings.addBinding(bindingStr, AGHttpRepoClient.getApplicationValue(parseValue(type, value, lang, datatype), vf));
                        }
                    }
                } else {
                    jp.nextToken();
                }
            }
            return bindings;
        }

        private Value parseValue(String type, String value, String language, String datatype) {
            Value result = null;
            if (!type.equals("literal") && !type.equals("typed-literal")) {
                if (type.equals("bnode")) {
                    result = vf.createBNode(value);
                } else if (type.equals("uri")) {
                    result = vf.createIRI(value);
                }
            } else if (language != null) {
                result = vf.createLiteral(value, language);
            } else if (datatype != null) {
                result = vf.createLiteral(value, vf.createIRI(datatype));
            } else {
                result = vf.createLiteral(value);
            }

            return result;
        }
    }
}
