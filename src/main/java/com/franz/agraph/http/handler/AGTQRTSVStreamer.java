package com.franz.agraph.http.handler;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
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
 * but uses rdf4j-like TSV parsing instead of SAXParser so the results
 * streaming in the http response can be processed in a
 * {@link TupleQueryResult} pulling from the TSV stream.
 *
 * @since v2.1.0
 */
public class AGTQRTSVStreamer extends AGTQRStreamer {
    private InputStream in;

    public AGTQRTSVStreamer(AGValueFactory vf) {
        super(TupleQueryResultFormat.TSV.getDefaultMIMEType());
        this.vf = vf;
    }

    @Override
    public String getRequestMIMEType() {
        return TupleQueryResultFormat.TSV.getDefaultMIMEType();
    }

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

        public Result() {
            r = new InputStreamReader(in, Charset.forName("UTF-8"));
            reader = new BufferedReader(r);
        }

        @Override
        public List<String> getBindingNames() {
            parseBindingNames();
            return bindingNames;
        }

        @Override
        public void remove() throws QueryEvaluationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                next = parse();
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

        private void parseBindingNames() {
            if (bindingNames == null) {
                String nextLine = readNextLine();
                String[] lineTokens = nextLine.split("\t", -1);
                bindingNames = new ArrayList<>(lineTokens.length);
                for (String name : lineTokens) {
                    if ('?' == name.charAt(0)) {
                        bindingNames.add(name.substring(1));
                    } else {
                        bindingNames.add(name);
                    }
                }
            }
        }

        private String readNextLine() {
            try {
                return reader.readLine();
            } catch (IOException e) {
                throw new UnsupportedQueryResultFormatException(e);
            }
        }

        public BindingSet parse() throws QueryResultParseException, TupleQueryResultHandlerException {
            parseBindingNames();

            String nextLine = readNextLine();
            if (nextLine == null) return null;

            String[] lineTokens;
            lineTokens = nextLine.split("\t", -1);
            List<Value> values = new ArrayList<>();

            for (String valueString : lineTokens) {
                Value v = null;
                if (valueString.startsWith("_:")) {
                    v = vf.createBNode(valueString.substring(2));
                } else if (valueString.startsWith("<") && valueString.endsWith(">")) {
                    try {
                        v = vf.createIRI(valueString.substring(1, valueString.length() - 1));
                    } catch (IllegalArgumentException var14) {
                        v = vf.createLiteral(valueString);
                    }
                } else if (valueString.startsWith("\"")) {
                    v = this.parseLiteral(valueString);
                } else if (!"".equals(valueString)) {
                    if (valueString.matches("^[+\\-]?[\\d.].*")) {
                        IRI datatype = null;
                        if (XMLDatatypeUtil.isValidInteger(valueString)) {
                            if (XMLDatatypeUtil.isValidNegativeInteger(valueString)) {
                                datatype = XMLSchema.NEGATIVE_INTEGER;
                            } else {
                                datatype = XMLSchema.INTEGER;
                            }
                        } else if (XMLDatatypeUtil.isValidDecimal(valueString)) {
                            datatype = XMLSchema.DECIMAL;
                        } else if (XMLDatatypeUtil.isValidDouble(valueString)) {
                            datatype = XMLSchema.DOUBLE;
                        }

                        if (datatype != null) {
                            v = vf.createLiteral(valueString, datatype);
                        } else {
                            v = vf.createLiteral(valueString);
                        }
                    } else {
                        v = vf.createLiteral(valueString);
                    }
                }
                values.add(AGHttpRepoClient.getApplicationValue(v, vf));
            }

            return new ListBindingSet(bindingNames, values.toArray(new Value[values.size()]));
        }

        private Literal parseLiteral(String literal) throws IllegalArgumentException {
            if (literal.startsWith("\"")) {
                int endLabelIdx = this.findEndOfLabel(literal);
                if (endLabelIdx != -1) {
                    int startLangIdx = literal.indexOf("@", endLabelIdx);
                    int startDtIdx = literal.indexOf("^^", endLabelIdx);
                    if (startLangIdx != -1 && startDtIdx != -1) {
                        throw new IllegalArgumentException("Literals can not have both a language and a datatype");
                    }

                    String label = literal.substring(1, endLabelIdx);
                    label = decodeString(label);
                    String datatype;
                    if (startLangIdx != -1) {
                        datatype = literal.substring(startLangIdx + 1);
                        return vf.createLiteral(label, datatype);
                    }

                    if (startDtIdx != -1) {
                        datatype = literal.substring(startDtIdx + 2);
                        datatype = datatype.substring(1, datatype.length() - 1);
                        IRI dtURI = vf.createIRI(datatype);
                        return vf.createLiteral(label, dtURI);
                    }

                    return vf.createLiteral(label);
                }
            }

            throw new IllegalArgumentException("Not a legal literal: " + literal);
        }

        private int findEndOfLabel(String literal) {
            return literal.lastIndexOf("\"");
        }

        private String decodeString(String s) {
            int backSlashIdx = s.indexOf('\\');
            if (backSlashIdx == -1) {
                return s;
            } else {
                int startIdx = 0;
                int sLength = s.length();

                StringBuilder sb;
                for (sb = new StringBuilder(sLength); backSlashIdx != -1; backSlashIdx = s.indexOf('\\', startIdx)) {
                    sb.append(s.substring(startIdx, backSlashIdx));
                    if (backSlashIdx + 1 >= sLength) {
                        throw new IllegalArgumentException("Unescaped backslash in: " + s);
                    }

                    char c = s.charAt(backSlashIdx + 1);
                    if (c == 't') {
                        sb.append('\t');
                        startIdx = backSlashIdx + 2;
                    } else if (c == 'r') {
                        sb.append('\r');
                        startIdx = backSlashIdx + 2;
                    } else if (c == 'n') {
                        sb.append('\n');
                        startIdx = backSlashIdx + 2;
                    } else if (c == '"') {
                        sb.append('"');
                        startIdx = backSlashIdx + 2;
                    } else if (c == '>') {
                        sb.append('>');
                        startIdx = backSlashIdx + 2;
                    } else if (c == '\\') {
                        sb.append('\\');
                        startIdx = backSlashIdx + 2;
                    } else {
                        String xx;
                        if (c == 'u') {
                            if (backSlashIdx + 5 >= sLength) {
                                throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
                            }

                            xx = s.substring(backSlashIdx + 2, backSlashIdx + 6);

                            try {
                                c = (char) Integer.parseInt(xx, 16);
                                sb.append(c);
                                startIdx = backSlashIdx + 6;
                            } catch (NumberFormatException var9) {
                                throw new IllegalArgumentException("Illegal Unicode escape sequence '\\u" + xx + "' in: " + s);
                            }
                        } else {
                            if (c != 'U') {
                                throw new IllegalArgumentException("Unescaped backslash in: " + s);
                            }

                            if (backSlashIdx + 9 >= sLength) {
                                throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
                            }

                            xx = s.substring(backSlashIdx + 2, backSlashIdx + 10);

                            try {
                                sb.appendCodePoint(Integer.parseInt(xx, 16));
                                startIdx = backSlashIdx + 10;
                            } catch (NumberFormatException var8) {
                                throw new IllegalArgumentException("Illegal Unicode escape sequence '\\U" + xx + "' in: " + s);
                            }
                        }
                    }
                }

                sb.append(s.substring(startIdx));
                return sb.toString();
            }
        }
    }
}
