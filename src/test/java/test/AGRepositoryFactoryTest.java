package test;

import com.franz.agraph.repository.config.AGRepositoryConfig;
import com.franz.agraph.repository.config.AGRepositoryFactory;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.GraphUtil;
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
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class AGRepositoryFactoryTest extends AGAbstractTest {

    final String configFile = "/test/repoconfig.ttl";
    final String ns = "http://franz.com/agraph/repository/config#";

    @Test
    @Category(TestSuites.Prepush.class)
    public void getRepositoryUsingConfig() throws Exception {
        Model graph = parseTurtleGraph(configFile);
        updateGraphForTestServer(graph);
        Resource implNode = GraphUtil.getUniqueSubject(graph, vf.createIRI("http://www.openrdf.org/config/repository#repositoryType"), vf.createLiteral(AGRepositoryFactory.REPOSITORY_TYPE));
        AGRepositoryFactory factory = new AGRepositoryFactory();
        AGRepositoryConfig config = factory.getConfig();
        config.parse(graph, implNode);
        config.setServerUrl(AGAbstractTest.findServerUrl());
        config.setUsername(AGAbstractTest.username());
        config.setPassword(AGAbstractTest.password());
        Repository repo = factory.getRepository(config);
        Assert.assertEquals(0, repo.getConnection().size());
        Assert.assertTrue(cat.hasRepository("callimachus"));
        Model graph2 = new LinkedHashModel();
        config.export(graph2);
        Assert.assertEquals(6, graph2.size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void getRepositoryUsingManager() throws Exception {
        final Path confDir = Files.createTempDirectory("repomgr");
        closeLater(() -> FileUtils.deleteDirectory(confDir.toFile()));

        RepositoryManager manager = new LocalRepositoryManager(confDir.toFile());
        manager.initialize();
        closeLater(manager::shutDown);
        Model graph = parseTurtleGraph(configFile);
        updateGraphForTestServer(graph);
        Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE, RepositoryConfigSchema.REPOSITORY);
        String id = GraphUtil.getUniqueObjectLiteral(graph, node, RepositoryConfigSchema.REPOSITORYID).stringValue();
        RepositoryConfig config = RepositoryConfig.create(graph, node);
        config.validate();
        manager.addRepositoryConfig(config);
        Repository repo = manager.getRepository(id);
        repo.initialize();
        closeLater(repo::shutDown);
        Assert.assertEquals(0, repo.getConnection().size());
        Assert.assertTrue(cat.hasRepository("callimachus"));
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
        Iterator<Statement> it = graph.match(null, pred, null);
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
