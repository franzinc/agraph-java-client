package tutorial;

import java.io.File;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;

import test.AGRepositoryConnectionTest;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGServer;

public class CIAFactBookTest {
	
	
	private static void doTest() throws Exception {
        AGServer server = new AGServer(AGRepositoryConnectionTest.SERVER_URL, AGRepositoryConnectionTest.USERNAME, AGRepositoryConnectionTest.PASSWORD);
	    System.out.println("Available catalogs " + server.listCatalogs());
	    AGCatalog catalog = server.getCatalog(AGRepositoryConnectionTest.CATALOG_ID);    
//	    System.out.println("Available repositories in catalog '" + catalog.getName() + "': " +
//	    		catalog.listRepositories());    
	    AGRepository myRepository = catalog.createRepository("ciatest");
//      AllegroRepository.ACCESS
	    myRepository.initialize();
	    System.out.println( "Repository " + myRepository.getRepositoryID() + " is up!  It contains "
	    		+ myRepository.getConnection().size() + " statements.");
	
	    RepositoryConnection conn = myRepository.getConnection();
	    ValueFactory f = myRepository.getValueFactory();
	    if (conn.size() == 0) {
	    	System.out.println("Reading CIA Fact Book file.");
		    String path1 = "/FRANZ_CONSULTING/data/ciafactbook.nt";    
		    String baseURI = "http://example.org/example/local";
	    	conn.add(new File(path1), baseURI, RDFFormat.NTRIPLES); // true
	    }
//	    myRepository.indexTriples(true);
	    System.out.println("After loading, repository contains " + conn.size() + " triples");
	    
	    
	}
	
	public static void main (String[] args) throws Exception {
		
		doTest();
	}


}
