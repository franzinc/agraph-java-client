
package tutorial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.AllegroDataset;
import org.openrdf.repository.sail.AllegroRepository;
import org.openrdf.repository.sail.AllegroRepositoryConnection;
import org.openrdf.repository.sail.AllegroSail;
import org.openrdf.repository.sail.AllegroTupleQueryResult;
import org.openrdf.repository.sail.Catalog;
import org.openrdf.repository.sail.JDBCResultSet;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.RDFXMLWriter;

import franz.exceptions.SoftException;

public class LikeJUnits {
		
	public static void test0() {
	    System.out.println("Hello World");
	}

	/**
	 * Tests getting the repository up.  Is called by the other tests to do the startup.
	 */
	public static Repository test1() throws RepositoryException {
	    AllegroSail server = new AllegroSail("localhost", 8080);
	    System.out.println("Available catalogs " + server.listCatalogs());
	    Catalog catalog = server.openCatalog("scratch");    
	    System.out.println("Available repositories in catalog '" + catalog.getName() + "': " +
	    		catalog.listRepositories());    
	    AllegroRepository myRepository = catalog.getRepository("agraph_test4", AllegroRepository.RENEW);
	    myRepository.initialize();
	    System.out.println( "Repository " + myRepository.getName() + " is up!  It contains "
	    		+ myRepository.getConnection().size() + " statements.");
	    return myRepository;
	}

	public static void test2() throws RepositoryException {
	    AllegroRepository myRepository = (AllegroRepository)test1();	    
	    RepositoryConnection conn = myRepository.getConnection();
	    ValueFactory f = myRepository.getValueFactory();
	    String exns = "http://example.org/people/";
	    conn.setNamespace("ex", exns);
	    
	    Literal lit = f.createLiteral(" some \"special\" literal");
//	    URI uri = factory.createURI("http://www.fooco/ex#foo");
//	    conn.addTriple(uri, uri, lit, false, null);
//	    rows = conn.getStatements(None, None, None);
//	    for r in rows:
//	        print "LIT", r[2].getLabel()
	    
	}
	
	

	public static void main(String[] args) throws Exception {
		List<Integer> choices = new ArrayList<Integer>();
		int lastChoice = 6;
		for (int i = 1; i <= lastChoice; i++)
			choices.add(new Integer(i));
		if (true) {
			choices = new ArrayList<Integer>();
			choices.add(11);
		}
		try {
		for (Integer choice : choices) {
			System.out.println("Running test " + choice);
			switch(choice) {
			case 0: test0(); break;
			case 1: test1(); break;
//			case 2: test2(); break;			
//			case 3: test3(); break;			
//			case 4: test4(); break;						
//			case 5: test5(); break;									
//			case 6: test6(); break;	
//			case 7: test7(); break;
//			case 8: test8(); break;			
//			case 9: test9(); break;	
//			case 10: test10(); break;
//			case 11: test11(); break;			
//			case 12: test12(); break;						
//			case 12: test13(); break;									
			default: System.out.println("There is no choice for test " + choice);
			}
		}
		} catch (Exception ex) {
			System.out.println("Caught exception " + new SoftException(ex).getMessage());
			ex.printStackTrace();
		}	
	}
}