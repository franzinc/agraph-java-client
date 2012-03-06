package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

import com.franz.agraph.repository.config.AGRepositoryConfig;
import com.franz.agraph.repository.config.AGRepositoryFactory;

public class AGRepositoryFactoryTest extends AGAbstractTest {

	final String configFile = "src/test/repoconfig.ttl";
	final String ns = "http://franz.com/agraph/repository/config#";
	
    @Test
    @Category(TestSuites.Prepush.class)
    public void getRepositoryUsingConfig() throws Exception {
    	Graph graph = parseTurtleGraph(configFile);
    	updateGraphForTestServer(graph);
		Resource implNode = GraphUtil.getUniqueSubject(graph, vf.createURI("http://www.openrdf.org/config/repository#repositoryType"), vf.createLiteral(AGRepositoryFactory.REPOSITORY_TYPE));
		AGRepositoryFactory factory = new AGRepositoryFactory();
    	AGRepositoryConfig config = factory.getConfig();
    	config.parse(graph , implNode);
    	config.setServerUrl(AGAbstractTest.findServerUrl());
    	config.setUsername(AGAbstractTest.username());
    	config.setPassword(AGAbstractTest.password());
    	Repository repo = factory.getRepository(config);
    	Assert.assertEquals(0,repo.getConnection().size());
    	Assert.assertTrue(cat.hasRepository("callimachus"));
    	Graph graph2 = new GraphImpl();
    	config.export(graph2);
    	Assert.assertEquals(6, graph2.size());
    }

    @Test
    @Category(TestSuites.Prepush.class)
    public void getRepositoryUsingManager() throws Exception {
    	RepositoryManager manager = new LocalRepositoryManager(new File("tmp/repomgr"));
    	manager.initialize();
    	Graph graph = parseTurtleGraph(configFile);
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

    private Graph parseTurtleGraph(final String configFile) throws IOException,
    RDFParseException, RDFHandlerException, FileNotFoundException {
    	RDFParser parser = Rio.createParser(RDFFormat.TURTLE, vf);
    	parser.setPreserveBNodeIDs(true);
    	StatementCollector collector = new StatementCollector();
    	parser.setRDFHandler(collector);
    	final FileInputStream fis = new FileInputStream(configFile);
    	try {
    		parser.parse(fis, "");
    	} finally {
    		fis.close();
    	}
    	Graph graph = new GraphImpl(collector.getStatements());
    	return graph;
    }

    private void updateValue(Graph graph, URI pred, Value val) {
    	Iterator<Statement> it = graph.match(null, pred, null);
    	Statement s = it.next();
    	graph.remove(s);
    	graph.add(s.getSubject(),pred,val);
    }
    
    private void updateGraphForTestServer(Graph graph) {
    	updateValue(graph,vf.createURI(ns,"serverUrl"),vf.createURI(AGAbstractTest.findServerUrl()));
    	updateValue(graph,vf.createURI(ns,"username"),vf.createLiteral(AGAbstractTest.username()));
    	updateValue(graph,vf.createURI(ns,"password"),vf.createLiteral(AGAbstractTest.password()));
    }
}
