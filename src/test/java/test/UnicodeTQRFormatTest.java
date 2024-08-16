/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGTupleQuery;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests handling of non-ascii characters with various TQR formats.
 */
public class UnicodeTQRFormatTest extends AGAbstractTest {

    private static final List<TupleQueryResultFormat> FORMATS = Arrays.asList(
            TupleQueryResultFormat.TSV,
            TupleQueryResultFormat.SPARQL,
            TupleQueryResultFormat.JSON);

    private void setFormat(TupleQueryResultFormat format) {
        conn.prepareHttpRepoClient().setPreferredTQRFormat(format);
    }

    @ParameterizedTest @FieldSource("FORMATS")
    public void testUnicodeSelect(TupleQueryResultFormat format) {
        setFormat(format);
        IRI s = vf.createIRI("http://franz.com/s");
        IRI p = vf.createIRI("http://franz.com/p");
        Literal o = vf.createLiteral("जुप");
        conn.add(s, p, o);
        AGTupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
                "SELECT ?o WHERE { ?s ?p ?o }");
        List<BindingSet> result = Iterations.asList(query.evaluate());
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getValue("o").stringValue(), "जुप");
    }
}
