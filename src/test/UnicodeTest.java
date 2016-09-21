/******************************************************************************
 ** Copyright (c) 2008-2016 Franz Inc.
 ** All rights reserved. This program and the accompanying materials
 ** are made available under the terms of the Eclipse Public License v1.0
 ** which accompanies this distribution, and is available at
 ** http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package test;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.handler.AGJSONHandler;
import com.franz.agraph.http.handler.AGStringHandler;
import com.franz.agraph.repository.AGGraphQuery;
import com.franz.agraph.repository.AGTupleQuery;
import info.aduna.iteration.Iterations;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.io.input.ReaderInputStream;

import org.json.JSONException;

import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.query.resultio.TupleQueryResultFormat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Test how various operations react to non-ascii characters.
 */
public class UnicodeTest extends AGAbstractTest {
    private RDFFormat oldRDFFormat;
    private TupleQueryResultFormat oldTQRFormat;
    
    @Before
    public void setUpFormat() {
        oldRDFFormat = conn.getHttpRepoClient().getPreferredRDFFormat();
        oldTQRFormat = conn.getHttpRepoClient().getPreferredTQRFormat();
    }

    @After
    public void tearDownFormat() {
        conn.getHttpRepoClient().setPreferredRDFFormat(oldRDFFormat);
        conn.getHttpRepoClient().setPreferredTQRFormat(oldTQRFormat);
    }

    @Test
    public void testEvalInServer() throws RepositoryException {
        assertThat(conn.evalInServer("\"जुप\""), is("जुप"));
    }

    @Test
    public void testStringHandler() throws IOException, AGHttpException {
        final AGStringHandler handler = new AGStringHandler();
        handler.handleResponse(mockResponse("जुप", "text/plain", "UTF-8"));
        assertThat(handler.getResult(), is("जुप"));
    }

    @Test
    public void testJSONHandler() throws IOException, AGHttpException, JSONException {
        final AGJSONHandler handler = new AGJSONHandler();
        handler.handleResponse(mockResponse("{\"value\":\"जुप\"}", "application/json", "UTF-8"));
        assertThat(handler.getResult().get("value"), is("जुप"));
    }

    public void testAddUnicodeLiteral(RDFFormat format) throws RepositoryException {
        conn.getHttpRepoClient().setPreferredRDFFormat(format);
        URI s = vf.createURI("http://franz.com/s");
        URI p = vf.createURI("http://franz.com/p");
        Literal o = vf.createLiteral("जुप");
        conn.add(s, p, o);
        List<Statement> result = Iterations.asList(conn.getStatements(s, p, null, false));
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getObject().stringValue(), is("जुप"));
    }

    @Test
    public void testAddUnicodeLiteralNQuads() throws Exception {
        testAddUnicodeLiteral(RDFFormat.NQUADS);
    }

    @Test
    public void testAddUnicodeLiteralTrix() throws Exception {
        testAddUnicodeLiteral(RDFFormat.TRIX);
    }
    
    public void testAddUnicodeSubject(RDFFormat format) throws RepositoryException {
        conn.getHttpRepoClient().setPreferredRDFFormat(format);
        URI s = vf.createURI("http://franz.com/जुप");
        URI p = vf.createURI("http://franz.com/p");
        Literal o = vf.createLiteral("o");
        conn.add(s, p, o);
        List<Statement> result = Iterations.asList(conn.getStatements(s, p, null, false));
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getSubject().stringValue(), is("http://franz.com/जुप"));
    }


    @Test
    public void testAddUnicodeSubjectNQuads() throws Exception {
        testAddUnicodeSubject(RDFFormat.NQUADS);
    }

    @Test
    public void testAddUnicodeSubjectTrix() throws Exception {
        testAddUnicodeSubject(RDFFormat.TRIX);
    }
    
    public void testUnicodeCreate(RDFFormat format) throws RepositoryException, QueryEvaluationException {
        conn.getHttpRepoClient().setPreferredRDFFormat(format);
        AGGraphQuery query = conn.prepareGraphQuery(QueryLanguage.SPARQL,
                "CONSTRUCT { <s> <p> \"जुप\"} WHERE {}");
        List<Statement> result = Iterations.asList(query.evaluate());
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getObject().stringValue(), is("जुप"));
    }

    @Test
    public void testUnicodeCreateNQuads() throws Exception {
        testUnicodeCreate(RDFFormat.NQUADS);
    }

    @Test
    public void testUnicodeCreateTrix() throws Exception {
        testUnicodeCreate(RDFFormat.TRIX);
    }
    
    public void testUnicodeSelect(TupleQueryResultFormat format) throws RepositoryException, QueryEvaluationException {
        conn.getHttpRepoClient().setPreferredTQRFormat(format);
        URI s = vf.createURI("http://franz.com/s");
        URI p = vf.createURI("http://franz.com/p");
        Literal o = vf.createLiteral("जुप");
        conn.add(s, p, o);
        AGTupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
                "SELECT ?o WHERE { ?s ?p ?o }");
        List<BindingSet> result = Iterations.asList(query.evaluate());
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue("o").stringValue(), is("जुप"));
    }

    @Test
    public void testUnicodeSelectTSV() throws Exception {
        testUnicodeSelect(TupleQueryResultFormat.TSV);
    }

    @Test
    public void testUnicodeSelectJSON() throws Exception {
        testUnicodeSelect(TupleQueryResultFormat.JSON);
    }

    @Test
    public void testUnicodeSelectSPARQL() throws Exception {
        testUnicodeSelect(TupleQueryResultFormat.SPARQL);
    }    
    
    /**
     * Creates a mock HttpResponse, suitable for testing AGResponseHandlers.
     *
     * Warning: not all response methods are properly mocked.
     *
     * @param text Response body/
     * @param mimeType Content-Type of the response.
     * @param encoding Charset used to encode the body/
     * @return A response object.
     */
    private HttpMethod mockResponse(String text, String mimeType, String encoding) {
        return new HttpMethodBase() {
            {
                setResponseStream(new ReaderInputStream(new StringReader(text), encoding));
            }
            @Override
            public String getName() {
                return "GET";
            }

            @Override
            public String getResponseCharSet() {
                return encoding;
            }

            @Override
            public Header[] getResponseHeaders(String headerName) {
                if (headerName.equalsIgnoreCase("content-type")) {
                    return new Header[] {
                            new Header("Content-Type", mimeType + ";charset=" + encoding)
                    };
                } else {
                    return super.getResponseHeaders(headerName);
                }
            }
        };
    }
}
