package tutorial;

import java.io.File;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.AllegroRepository;
import org.openrdf.repository.sail.AllegroRepositoryConnection;
import org.openrdf.repository.sail.AllegroSail;
import org.openrdf.repository.sail.Catalog;
import org.openrdf.rio.RDFFormat;

public class CIAFactBookTest {
	
	
	private static void doTest() throws Exception {
	    AllegroSail server = new AllegroSail("localhost", 8080);
	    System.out.println("Available catalogs " + server.listCatalogs());
	    Catalog catalog = server.openCatalog("scratch");    
	    System.out.println("Available repositories in catalog '" + catalog.getName() + "': " +
	    		catalog.listRepositories());    
	    AllegroRepository myRepository = catalog.getRepository("ciatest", AllegroRepository.ACCESS);
	    myRepository.initialize();
	    System.out.println( "Repository " + myRepository.getName() + " is up!  It contains "
	    		+ myRepository.getConnection().size() + " statements.");
	
	    RepositoryConnection conn = myRepository.getConnection();
	    ValueFactory f = myRepository.getValueFactory();
	    if (conn.size() == 0) {
	    	System.out.println("Reading CIA Fact Book file.");
		    String path1 = "/FRANZ_CONSULTING/data/ciafactbook.nt";    
		    String baseURI = "http://example.org/example/local";
	    	((AllegroRepositoryConnection)conn).add(new File(path1), baseURI, RDFFormat.NTRIPLES, true);
	    }
	    myRepository.indexTriples(true);
	    System.out.println("After loading, repository contains " + conn.size() + " triples");
	    
	    
	}
	
	public static void main (String[] args) throws Exception {
		
		doTest();
	}


}
