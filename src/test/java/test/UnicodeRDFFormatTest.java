/******************************************************************************
** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGGraphQuery;
import info.aduna.iteration.Iterations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openrdf.model.IRI;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests handling of non-ascii characters with various RDF formats.
 */
@RunWith(Parameterized.class)
public class UnicodeRDFFormatTest extends AGAbstractTest {
    private final RDFFormat format;
    private RDFFormat oldFormat;

    // @Parameters(name="{index}: {0}") -- need newer JUnit for that?
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { RDFFormat.NQUADS },
                { RDFFormat.TRIX }
        });
    }

    @Before
    public void setUpFormat() {
        oldFormat = conn.getHttpRepoClient().getPreferredRDFFormat();
        conn.getHttpRepoClient().setPreferredRDFFormat(format);
    }

    @After
    public void tearDownFormat() {
        conn.getHttpRepoClient().setPreferredRDFFormat(oldFormat);
    }

    public UnicodeRDFFormatTest(final RDFFormat format) {
        this.format = format;
    }

    @Test
    public void testAddUnicodeLiteral() throws RepositoryException {
        IRI s = vf.createIRI("http://franz.com/s");
        IRI p = vf.createIRI("http://franz.com/p");
        Literal o = vf.createLiteral("जुप");
        conn.add(s, p, o);
        List<Statement> result = Iterations.asList(conn.getStatements(s, p, null, false));
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getObject().stringValue(), is("जुप"));
    }

    @Test
    public void testAddUnicodeSubject() throws RepositoryException {
        IRI s = vf.createIRI("http://franz.com/जुप");
        IRI p = vf.createIRI("http://franz.com/p");
        Literal o = vf.createLiteral("o");
        conn.add(s, p, o);
        List<Statement> result = Iterations.asList(conn.getStatements(s, p, null, false));
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getSubject().stringValue(), is("http://franz.com/जुप"));
    }

    @Test
    public void testUnicodeCreate() throws RepositoryException, QueryEvaluationException {
        AGGraphQuery query = conn.prepareGraphQuery(QueryLanguage.SPARQL,
                "CONSTRUCT { <s> <p> \"जुप\"} WHERE {}");
        List<Statement> result = Iterations.asList(query.evaluate());
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getObject().stringValue(), is("जुप"));
    }
}
