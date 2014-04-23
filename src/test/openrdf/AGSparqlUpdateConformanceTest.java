package test.openrdf;

import java.util.Map;

import junit.framework.Test;

import org.openrdf.model.URI;
import org.openrdf.query.parser.sparql.manifest.SPARQLUpdateConformanceTest;
import org.openrdf.repository.Repository;
import org.openrdf.repository.contextaware.ContextAwareRepository;

import test.AGAbstractTest;


public class AGSparqlUpdateConformanceTest extends SPARQLUpdateConformanceTest {

    public AGSparqlUpdateConformanceTest(String testURI, String name, String requestFile,
            URI defaultGraphURI, Map<String, URI> inputNamedGraphs, URI resultDefaultGraphURI,
            Map<String, URI> resultNamedGraphs)
    {
        super(testURI, name, requestFile, defaultGraphURI, inputNamedGraphs, resultDefaultGraphURI,
                resultNamedGraphs);
    }

    public static Test suite()
        throws Exception
    {
        return SPARQL11ManifestTest.suite(new Factory() {

            public AGSparqlUpdateConformanceTest createSPARQLUpdateConformanceTest(String testURI,
                    String name, String requestFile, URI defaultGraphURI, Map<String, URI> inputNamedGraphs,
                    URI resultDefaultGraphURI, Map<String, URI> resultNamedGraphs)
            {
                return new AGSparqlUpdateConformanceTest(testURI, name, requestFile, defaultGraphURI,
                        inputNamedGraphs, resultDefaultGraphURI, resultNamedGraphs);
            }

        }, true, true, true, "move");
    }

    @Override
    protected ContextAwareRepository newRepository()
        throws Exception
    {
        //Repository repo = new SailRepository(new MemoryStore());
    	Repository repo = AGAbstractTest.sharedRepository();

        return new ContextAwareRepository(repo);
    }

}
