package test.openrdf;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openrdf.query.parser.sparql.SPARQLUpdateTest;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

import test.AGAbstractTest;

import com.franz.agraph.repository.AGServer;

public class HTTPSPARQLUpdateTest extends SPARQLUpdateTest {

	private static final String TEST_DIR_PREFIX = ".";

	@Override
	protected Repository newRepository() throws Exception {
		new AGServer(AGAbstractTest.findServerUrl(),AGAbstractTest.username(), AGAbstractTest.password()).getCatalog(AGAbstractTest.CATALOG_ID).createRepository("HTTPSPARQLUpdateTest");
    	HTTPRepository httprepo = 
    		new HTTPRepository(AGAbstractTest.findServerUrl()+"/catalogs/"+AGAbstractTest.CATALOG_ID+"/repositories/HTTPSPARQLUpdateTest");
    	httprepo.setUsernameAndPassword(AGAbstractTest.username(), AGAbstractTest.password());
    	return httprepo;
	}

	/* protected methods */

	protected void loadDataset(String datasetFile)
		throws RDFParseException, RepositoryException, IOException
	{
		InputStream dataset = new FileInputStream(TEST_DIR_PREFIX + datasetFile);
		try {
			RDFParser parser = Rio.createParser(RDFFormat.TRIG, con.getValueFactory());
			parser.setPreserveBNodeIDs(true);
			StatementCollector collector = new StatementCollector();
			parser.setRDFHandler(collector);
			parser.parse(dataset, "");
			con.add(collector.getStatements());
		} catch (RDFParseException e) {
			throw new RuntimeException(e);
		} catch (RDFHandlerException e) {
			throw new RuntimeException(e);
		}
		finally {
			dataset.close();
		}
	}

}
