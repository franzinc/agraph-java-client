/******************************************************************************
 ** Copyright (c) 2008-2016 Franz Inc.
 ** All rights reserved. This program and the accompanying materials
 ** are made available under the terms of the Eclipse Public License v1.0
 ** which accompanies this distribution, and is available at
 ** http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package test;

import com.franz.agraph.repository.AGTupleQuery;
import info.aduna.iteration.Iterations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.repository.RepositoryException;

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

    // @Parameters(name="{index}: {0}") -- need newer JUnit for that?
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { TupleQueryResultFormat.TSV },
                { TupleQueryResultFormat.SPARQL },
                { TupleQueryResultFormat.JSON }
        });
    }

    @Before
    public void setUpFormat() {
        oldFormat = conn.getHttpRepoClient().getPreferredTQRFormat();
        conn.getHttpRepoClient().setPreferredTQRFormat(format);
    }

    @After
    public void tearDownFormat() {
        conn.getHttpRepoClient().setPreferredTQRFormat(oldFormat);
    }

    public UnicodeTQRFormatTest(final TupleQueryResultFormat format) {
        this.format = format;
    }

    @Test
    public void testUnicodeSelect() throws RepositoryException, QueryEvaluationException {
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
}
