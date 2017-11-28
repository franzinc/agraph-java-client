package test;

import com.franz.agraph.repository.AGBooleanQuery;
import com.franz.agraph.repository.AGGraphQuery;
import com.franz.agraph.repository.AGQuery;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGTupleQuery;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tests for various download methods in AGRepositoryConnection
 * and AGQuery subclasses.
 */
public class DownloadTest extends AGAbstractTest {
    private static final String NS = "rm://";
    private static final String TEXT = "gazorpazorp";
    private AGRepositoryConnection conn;
    private File output;
    // ID of the test statement.
    private String id;
    // Queries
    private AGQuery genericQuery;
    private AGTupleQuery tupleQuery;
    private AGGraphQuery graphQuery;
    private AGBooleanQuery boolQuery;

    // Add the test triple and create a temp file before each test
    // WARNING: Do *not* name the method setUp, or you'll override
    // the superclass' method and get an NPE.
    @Before
    public void prepare() throws Exception {
        conn = getConnection();
        final URI uri = conn.getValueFactory().createURI(NS, TEXT);
        conn.add(uri, uri, uri);
        id = Util.getStatementId(conn, uri, uri, uri);

        genericQuery = (AGQuery)
                conn.prepareQuery(QueryLanguage.SPARQL, "select * { ?s ?p ?o }");
        tupleQuery = conn.prepareTupleQuery(
                QueryLanguage.SPARQL, "select * { ?s ?p ?o}");
        graphQuery = conn.prepareGraphQuery(
                QueryLanguage.SPARQL, "construct { ?s ?p ?o } where { ?s ?p ?o }");
        boolQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, "ASK { ?s ?p ?o }");

        output = File.createTempFile("java-download-test", ".xyz");
        output.deleteOnExit();
    }

    // downloadStatements tests

    @Test
    public void testDownloadStatementsDefaultFormat() throws Exception {
        conn.downloadStatements(output, null, null, null, false);
        checkOutput();
    }

    @Test
    public void testDownloadStatementsRDFFormat() throws Exception {
        conn.downloadStatements(output, RDFFormat.RDFXML,
                null, null, null, false);
        checkOutput();
    }

    @Test
    public void testDownloadStatementsMIMEType() throws Exception {
        conn.downloadStatements(output, RDFFormat.RDFXML.getDefaultMIMEType(),
                null, null, null, false);
        checkOutput();
    }

    @Test
    public void testDownloadStatementsStrDefaultFormat() throws Exception {
        conn.downloadStatements(output.getAbsolutePath(),
                null, null, null, false);
        checkOutput();
    }

    @Test
    public void testDownloadStatementsStrRDFFormat() throws Exception {
        conn.downloadStatements(output.getAbsolutePath(), RDFFormat.RDFXML,
                null, null, null, false);
        checkOutput();
    }

    @Test
    public void testDownloadStatementsStrMIMEType() throws Exception {
        conn.downloadStatements(output.getAbsolutePath(),
                RDFFormat.RDFXML.getDefaultMIMEType(),
                null, null, null, false);
        checkOutput();
    }

    // streamStatements tests
    // Note: don't worry about the input stream, it will be autoclosed.

    @Test
    public void testStreamStatementsDefaultFormat() throws Exception {
        final InputStream stream =
                conn.streamStatements(null, null, null, false);
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    @Test
    public void testStreamStatementsRDFFormat() throws Exception {
        final InputStream stream =
                conn.streamStatements(
                        RDFFormat.RDFXML,
                        null, null, null, false);
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    @Test
    public void testStreamStatementsMIMEType() throws Exception {
        final InputStream stream =
                conn.streamStatements(
                        RDFFormat.RDFXML.getDefaultMIMEType(),
                        null, null, null, false);
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    // downloadStatements(id, ...) tests

    @Test
    public void testDownloadStatementsByIDRDFFormat() throws Exception {
        conn.downloadStatements(output, RDFFormat.RDFXML, id);
        checkOutput();
    }

    @Test
    public void testDownloadStatementsByIDMIMEType() throws Exception {
        conn.downloadStatements(output, RDFFormat.RDFXML.getDefaultMIMEType(), id);
        checkOutput();
    }


    @Test
    public void testDownloadStatementsByIDStrRDFFormat() throws Exception {
        conn.downloadStatements(output.getAbsolutePath(), RDFFormat.RDFXML, id);
        checkOutput();
    }

    @Test
    public void testDownloadStatementsByIDStrMIMEType() throws Exception {
        conn.downloadStatements(output.getAbsolutePath(),
                RDFFormat.RDFXML.getDefaultMIMEType(), id);
        checkOutput();
    }

    // streamStatements(id....) tests
    // Note: don't worry about the input stream, it will be autoclosed.

    @Test
    public void testStreamStatementsByIdRDFFormat() throws Exception {
        final InputStream stream =
                conn.streamStatements(RDFFormat.RDFXML, id);
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    @Test
    public void testStreamStatementsByIdMIMEType() throws Exception {
        final InputStream stream =
                conn.streamStatements(
                        RDFFormat.RDFXML.getDefaultMIMEType(), id);
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    // AGQuery.download tests

    @Test
    public void testQueryDownload() throws Exception {
        genericQuery.download(output);
        checkOutput();
    }

    @Test
    public void testQueryDownloadStr() throws Exception {
        genericQuery.download(output.getAbsolutePath());
        checkOutput();
    }

    @Test
    public void testQueryDownloadMIMEType() throws Exception {
        genericQuery.download(output, TupleQueryResultFormat.CSV.getDefaultMIMEType());
        checkOutput();
    }

    @Test
    public void testQueryDownloadStrMIMEType() throws Exception {
        genericQuery.download(output.getAbsolutePath(),
                TupleQueryResultFormat.CSV.getDefaultMIMEType());
        checkOutput();
    }

    // AGQuery.stream tests

    @Test
    public void testQueryStream() throws Exception {
        final InputStream stream = genericQuery.stream();
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    @Test
    public void testQueryStreamMIMEType() throws Exception {
        final InputStream stream =
                genericQuery.stream(TupleQueryResultFormat.JSON.getDefaultMIMEType());
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    // AGTupleQuery.download tests

    @Test
    public void testTupleQueryDownload() throws Exception {
        tupleQuery.download(output);
        checkOutput();
    }

    @Test
    public void testTupleQueryDownloadStr() throws Exception {
        tupleQuery.download(output.getAbsolutePath());
        checkOutput();
    }

    @Test
    public void testTupleQueryDownloadFormat() throws Exception {
        tupleQuery.download(output, TupleQueryResultFormat.CSV);
        checkOutput();
    }

    @Test
    public void testTupleQueryDownloadStrFormat() throws Exception {
        tupleQuery.download(output.getAbsolutePath(), TupleQueryResultFormat.CSV);
        checkOutput();
    }

    @Test
    public void testTupleQueryDownloadMIMEType() throws Exception {
        tupleQuery.download(output, TupleQueryResultFormat.CSV.getDefaultMIMEType());
        checkOutput();
    }

    @Test
    public void testTupleQueryDownloadStrMIMEType() throws Exception {
        tupleQuery.download(output.getAbsolutePath(),
                TupleQueryResultFormat.CSV.getDefaultMIMEType());
        checkOutput();
    }

    // AGTupleQuery.stream tests

    @Test
    public void testTupleQueryStream() throws Exception {
        final InputStream stream = tupleQuery.stream();
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    @Test
    public void testTupleQueryStreamFormat() throws Exception {
        final InputStream stream =
                tupleQuery.stream(TupleQueryResultFormat.CSV);
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    @Test
    public void testTupleQueryStreamMIMEType() throws Exception {
        final InputStream stream =
                tupleQuery.stream(TupleQueryResultFormat.CSV.getDefaultMIMEType());
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    // AGGraphQuery.download tests

    @Test
    public void testGraphQueryDownload() throws Exception {
        graphQuery.download(output);
        checkOutput();
    }

    @Test
    public void testGraphQueryDownloadStr() throws Exception {
        graphQuery.download(output.getAbsolutePath());
        checkOutput();
    }

    @Test
    public void testGraphQueryDownloadFormat() throws Exception {
        graphQuery.download(output, RDFFormat.RDFXML);
        checkOutput();
    }

    @Test
    public void testGraphQueryDownloadStrFormat() throws Exception {
        graphQuery.download(output.getAbsolutePath(), RDFFormat.RDFXML);
        checkOutput();
    }

    @Test
    public void testGraphQueryDownloadMIMEType() throws Exception {
        graphQuery.download(output, RDFFormat.RDFXML.getDefaultMIMEType());
        checkOutput();
    }

    @Test
    public void testGraphQueryDownloadStrMIMEType() throws Exception {
        graphQuery.download(output.getAbsolutePath(),
                RDFFormat.RDFXML.getDefaultMIMEType());
        checkOutput();
    }

    // AGGraphQuery.stream tests

    @Test
    public void testGraphQueryStream() throws Exception {
        final InputStream stream = graphQuery.stream();
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    @Test
    public void testGraphQueryStreamFormat() throws Exception {
        final InputStream stream =
                graphQuery.stream(RDFFormat.RDFXML);
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    @Test
    public void testGraphQueryStreamMIMEType() throws Exception {
        final InputStream stream =
                graphQuery.stream(RDFFormat.RDFXML.getDefaultMIMEType());
        FileUtils.copyInputStreamToFile(stream, output);
        checkOutput();
    }

    // AGBooleanQuery.download tests

    @Test
    public void testBooleanQueryDownload() throws Exception {
        boolQuery.download(output);
        checkBoolOutput();
    }

    @Test
    public void testBooleanQueryDownloadStr() throws Exception {
        boolQuery.download(output.getAbsolutePath());
        checkBoolOutput();
    }

    @Test
    public void testBooleanQueryDownloadFormat() throws Exception {
        boolQuery.download(output, BooleanQueryResultFormat.TEXT);
        checkBoolOutput();
    }

    @Test
    public void testBooleanQueryDownloadStrFormat() throws Exception {
        boolQuery.download(output.getAbsolutePath(), BooleanQueryResultFormat.TEXT);
        checkBoolOutput();
    }

    @Test
    public void testBooleanQueryDownloadMIMEType() throws Exception {
        boolQuery.download(output, BooleanQueryResultFormat.TEXT.getDefaultMIMEType());
        checkBoolOutput();
    }

    @Test
    public void testBooleanQueryDownloadStrMIMEType() throws Exception {
        boolQuery.download(output.getAbsolutePath(),
                BooleanQueryResultFormat.TEXT.getDefaultMIMEType());
        checkBoolOutput();
    }

    // AGBooleanQuery.stream tests

    @Test
    public void testBooleanQueryStream() throws Exception {
        final InputStream stream = boolQuery.stream();
        FileUtils.copyInputStreamToFile(stream, output);
        checkBoolOutput();
    }

    @Test
    public void testBooleanQueryStreamFormat() throws Exception {
        final InputStream stream =
                boolQuery.stream(BooleanQueryResultFormat.TEXT);
        FileUtils.copyInputStreamToFile(stream, output);
        checkBoolOutput();
    }

    @Test
    public void testBooleanQueryStreamMIMEType() throws Exception {
        final InputStream stream =
                boolQuery.stream(BooleanQueryResultFormat.TEXT.getDefaultMIMEType());
        FileUtils.copyInputStreamToFile(stream, output);
        checkBoolOutput();
    }

    private void checkOutput() throws IOException {
        // Just check if the test string appears anywhere.
        final String data = FileUtils.readFileToString(output);
        Assert.assertTrue(
                "Output file should contain the test string.",
                data.contains(TEXT));
    }

    private void checkBoolOutput() throws IOException {
        // Just make sure it's not false
        // Be aware that the result can be a bunch of XML with comments,
        // so disallowing words like 'no' will not work.
        final String data = FileUtils.readFileToString(output).toLowerCase();
        Assert.assertFalse(data + " should look truthy", data.contains("false"));
        Assert.assertFalse(data + " should be healthy", data.contains("cholesterol"));
    }
}
