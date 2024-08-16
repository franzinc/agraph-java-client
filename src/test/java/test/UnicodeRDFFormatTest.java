/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGGraphQuery;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests handling of non-ascii characters with various RDF formats.
 */
public class UnicodeRDFFormatTest extends AGAbstractTest {

    private static final List<RDFFormat> FORMATS = Arrays.asList(RDFFormat.NQUADS, RDFFormat.TRIX);

    private void setFormat(RDFFormat format) {
        conn.prepareHttpRepoClient().setPreferredRDFFormat(format);
    }

    @ParameterizedTest @FieldSource("FORMATS")
    public void testAddUnicodeLiteral(RDFFormat format) throws RepositoryException {
        setFormat(format);
        IRI s = vf.createIRI("http://franz.com/s");
        IRI p = vf.createIRI("http://franz.com/p");
        Literal o = vf.createLiteral("जुप");
        conn.add(s, p, o);
        List<Statement> result = Iterations.asList(conn.getStatements(s, p, null, false));
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getObject().stringValue(), "जुप");
    }

    @ParameterizedTest @FieldSource("FORMATS")
    public void testAddUnicodeSubject(RDFFormat format) throws RepositoryException {
        setFormat(format);
        IRI s = vf.createIRI("http://franz.com/जुप");
        IRI p = vf.createIRI("http://franz.com/p");
        Literal o = vf.createLiteral("o");
        conn.add(s, p, o);
        List<Statement> result = Iterations.asList(conn.getStatements(s, p, null, false));
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getSubject().stringValue(), "http://franz.com/जुप");
    }

    @ParameterizedTest @FieldSource("FORMATS")
    public void testUnicodeCreate(RDFFormat format) throws RepositoryException, QueryEvaluationException {
        setFormat(format);
        AGGraphQuery query = conn.prepareGraphQuery(QueryLanguage.SPARQL,
                "CONSTRUCT { <s> <p> \"जुप\"} WHERE {}");
        List<Statement> result = Iterations.asList(query.evaluate());
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getObject().stringValue(), "जुप");
    }
}
