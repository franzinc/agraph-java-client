package test;

import com.franz.agraph.repository.config.AGRepositoryConfig;
import com.franz.agraph.repository.config.AGRepositoryFactory;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class AGRepositoryFactoryTest extends AGAbstractTest {

    final String configFile = "/test/repoconfig.ttl";
    final String ns = "http://franz.com/agraph/repository/config#";

    private AGRepositoryConfig getConfig(Model graph) {
        IRI repoTypePredicate = vf.createIRI("http://www.openrdf.org/config/repository#repositoryType");
        Literal repoType = vf.createLiteral(AGRepositoryFactory.REPOSITORY_TYPE);
        Set<Resource> subjects = graph.filter(null, repoTypePredicate, repoType).subjects();
        if (subjects.size() != 1) {
            fail(String.format("Expected only one repository of %s %s", repoTypePredicate, repoType));
        }
        Resource implNode = subjects.iterator().next();
        AGRepositoryFactory factory = new AGRepositoryFactory();
        AGRepositoryConfig config = factory.getConfig();
        config.parse(graph, implNode);
        return config;
    }

    @Test
    public void getRepositoryUsingConfig() throws Exception {
        Model graph = parseTurtleGraph(configFile);
        updateGraphForTestServer(graph);
        AGRepositoryConfig config = getConfig(graph);
        deleteLater(config.getRepositoryId(), 
                    config.getCatalogId());
        config.setServerUrl(AGAbstractTest.findServerUrl());
        config.setUsername(AGAbstractTest.username());
        config.setPassword(AGAbstractTest.password());
        AGRepositoryFactory factory = new AGRepositoryFactory();
        Repository repo = factory.getRepository(config);
        repo.init();
        closeLater(repo::shutDown);
        assertEquals(0, repo.getConnection().size());
        Model graph2 = new LinkedHashModel();
        config.export(graph2);
        assertEquals(6, graph2.size());
    }

    /**
     * TODO: org.eclipse.rdf4j.repository.config.RepositoryConfigSchema is deprecated, maybe the test needs a rewrite?
     */
    @Test
    public void getRepositoryUsingManager() throws Exception {
        final Path confDir = Files.createTempDirectory("repomgr");
        closeLater(() -> FileUtils.deleteDirectory(confDir.toFile()));

        RepositoryManager manager = new LocalRepositoryManager(confDir.toFile());
        manager.init();
        closeLater(manager::shutDown);
        Model graph = parseTurtleGraph(configFile);
        updateGraphForTestServer(graph);

        // Cleanup
        AGRepositoryConfig agConfig = getConfig(graph);
        deleteLater(agConfig.getRepositoryId(),
                    agConfig.getCatalogId());

        Set<Resource> repos = graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY).subjects();
        assertEquals(1, repos.size());
        Resource node = repos.iterator().next();

        Set<Value> ids = graph.filter(node, RepositoryConfigSchema.REPOSITORYID, null).objects();
        assertEquals(1, ids.size());
        String id = ids.iterator().next().stringValue();

        RepositoryConfig config = RepositoryConfig.create(graph, node);
        config.validate();
        manager.addRepositoryConfig(config);
        Repository repo = manager.getRepository(id);
        repo.init();
        closeLater(repo::shutDown);
        assertEquals(0, repo.getConnection().size());
    }

    private Model parseTurtleGraph(final String configFile) throws IOException,
            RDFParseException, RDFHandlerException, FileNotFoundException {
        RDFParser parser = Rio.createParser(RDFFormat.TURTLE, vf);
        parser.setPreserveBNodeIDs(true);
        StatementCollector collector = new StatementCollector();
        parser.setRDFHandler(collector);

        try (final InputStream fis = Util.resourceAsStream(configFile)) {
            parser.parse(fis, "");
        }
        return new LinkedHashModel(collector.getStatements());
    }

    private void updateValue(Model graph, IRI pred, Value val) {
        Iterator<Statement> it = graph.filter(null, pred, null).iterator();
        Statement s = it.next();
        graph.remove(s);
        graph.add(s.getSubject(), pred, val);
    }

    private void updateGraphForTestServer(Model graph) {
        updateValue(graph, vf.createIRI(ns, "serverUrl"), vf.createIRI(AGAbstractTest.findServerUrl()));
        updateValue(graph, vf.createIRI(ns, "username"), vf.createLiteral(AGAbstractTest.username()));
        updateValue(graph, vf.createIRI(ns, "password"), vf.createLiteral(AGAbstractTest.password()));
    }
}
