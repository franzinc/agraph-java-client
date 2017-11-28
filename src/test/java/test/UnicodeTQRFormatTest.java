/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGTupleQuery;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests handling of non-ascii characters with various TQR formats.
 */
@RunWith(Parameterized.class)
public class UnicodeTQRFormatTest extends AGAbstractTest {
    private final TupleQueryResultFormat format;
    private TupleQueryResultFormat oldFormat;

    public UnicodeTQRFormatTest(final TupleQueryResultFormat format) {
        this.format = format;
    }

    // @Parameters(name="{index}: {0}") -- need newer JUnit for that?
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {TupleQueryResultFormat.TSV},
                {TupleQueryResultFormat.SPARQL},
                {TupleQueryResultFormat.JSON}
        });
    }

    @Before
    public void setUpFormat() {
        oldFormat = conn.prepareHttpRepoClient().getPreferredTQRFormat();
        conn.prepareHttpRepoClient().setPreferredTQRFormat(format);
    }

    @After
    public void tearDownFormat() {
        conn.prepareHttpRepoClient().setPreferredTQRFormat(oldFormat);
    }

    @Test
    public void testUnicodeSelect() throws RepositoryException, QueryEvaluationException {
        IRI s = vf.createIRI("http://franz.com/s");
        IRI p = vf.createIRI("http://franz.com/p");
        Literal o = vf.createLiteral("जुप");
        conn.add(s, p, o);
        AGTupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL,
                "SELECT ?o WHERE { ?s ?p ?o }");
        List<BindingSet> result = Iterations.asList(query.evaluate());
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue("o").stringValue(), is("जुप"));
    }
}
