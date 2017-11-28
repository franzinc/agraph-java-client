package test.openrdf;

import junit.framework.Test;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.parser.sparql.manifest.SPARQLUpdateConformanceTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.contextaware.ContextAwareRepository;
import test.AGAbstractTest;

import java.util.Map;


public class AGSparqlUpdateConformanceTest extends SPARQLUpdateConformanceTest {

    public AGSparqlUpdateConformanceTest(String testURI, String name, String requestFile,
                                         IRI defaultGraphURI, Map<String, IRI> inputNamedGraphs, IRI resultDefaultGraphURI,
                                         Map<String, IRI> resultNamedGraphs) {
        super(testURI, name, requestFile, defaultGraphURI, inputNamedGraphs, resultDefaultGraphURI,
                resultNamedGraphs);
    }

    public static Test suite()
            throws Exception {
        return SPARQL11ManifestTest.suite(new Factory() {

            public AGSparqlUpdateConformanceTest createSPARQLUpdateConformanceTest(String testURI,
                                                                                   String name, String requestFile, IRI defaultGraphURI, Map<String, IRI> inputNamedGraphs,
                                                                                   IRI resultDefaultGraphURI, Map<String, IRI> resultNamedGraphs) {
                return new AGSparqlUpdateConformanceTest(testURI, name, requestFile, defaultGraphURI,
                        inputNamedGraphs, resultDefaultGraphURI, resultNamedGraphs);
            }

        }, true, true, true, "move");
    }

    @Override
    protected ContextAwareRepository newRepository()
            throws Exception {
        //Repository repo = new SailRepository(new MemoryStore());
        Repository repo = AGAbstractTest.sharedRepository();

        return new ContextAwareRepository(repo);
    }

}
