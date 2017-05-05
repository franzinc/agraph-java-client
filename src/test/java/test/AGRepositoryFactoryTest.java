package test;

import com.franz.agraph.repository.config.AGRepositoryConfig;
import com.franz.agraph.repository.config.AGRepositoryFactory;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.*;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.rio.*;
import org.openrdf.rio.helpers.StatementCollector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
    	config.parse(graph , implNode);
    	config.setServerUrl(AGAbstractTest.findServerUrl());
    	config.setUsername(AGAbstractTest.username());
    	config.setPassword(AGAbstractTest.password());
    	Repository repo = factory.getRepository(config);
    	Assert.assertEquals(0,repo.getConnection().size());
    	Assert.assertTrue(cat.hasRepository("callimachus"));
    	Model graph2 = new LinkedHashModel();
    	config.export(graph2);
    	Assert.assertEquals(6, graph2.size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void getRepositoryUsingManager() throws Exception {
    	RepositoryManager manager = new LocalRepositoryManager(new File("tmp/repomgr"));
    	manager.initialize();
    	Model graph = parseTurtleGraph(configFile);
    	updateGraphForTestServer(graph);
    	Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE, RepositoryConfigSchema.REPOSITORY);
    	String id = GraphUtil.getUniqueObjectLiteral(graph, node, RepositoryConfigSchema.REPOSITORYID).stringValue();
    	RepositoryConfig config = RepositoryConfig.create(graph, node);
    	config.validate();
    	manager.addRepositoryConfig(config);
    	Repository repo = manager.getRepository(id);
    	repo.initialize();
    	Assert.assertEquals(0,repo.getConnection().size());
    	Assert.assertTrue(cat.hasRepository("callimachus"));
    	repo.shutDown();
    }

    private Model parseTurtleGraph(final String configFile) throws IOException,
    RDFParseException, RDFHandlerException, FileNotFoundException {
    	RDFParser parser = Rio.createParser(RDFFormat.TURTLE, vf);
    	parser.setPreserveBNodeIDs(true);
    	StatementCollector collector = new StatementCollector();
    	parser.setRDFHandler(collector);

    	try (final InputStream fis = Util.resourceAsStream(configFile)){
    		parser.parse(fis, "");
    	}
    	return new LinkedHashModel(collector.getStatements());
    }

    private void updateValue(Model graph, IRI pred, Value val) {
    	Iterator<Statement> it = graph.match(null, pred, null);
    	Statement s = it.next();
    	graph.remove(s);
    	graph.add(s.getSubject(),pred,val);
    }
    
    private void updateGraphForTestServer(Model graph) {
    	updateValue(graph,vf.createIRI(ns,"serverUrl"),vf.createIRI(AGAbstractTest.findServerUrl()));
    	updateValue(graph,vf.createIRI(ns,"username"),vf.createLiteral(AGAbstractTest.username()));
    	updateValue(graph,vf.createIRI(ns,"password"),vf.createLiteral(AGAbstractTest.password()));
    }
}
