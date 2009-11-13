import java.io.IOException;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.Triple;
import com.franz.agbase.TriplesIterator;

/**
 * Load a Triple Store database from a file.
 * 
 * Start with:
 * 
 * <pre>
 *   java AGLoad [-p ppp] [-h hhh] [-d ddd] [-n nnn] [-r rrr]
 *                  [-t ttt] [-z] [ -moded | -modej ] [-x] 
 *                  [-index ccc] [-res rrr] [-log lll]
 *                  
 *   ppp - Lisp port number
 *   hhh - Hostname where AllegroGraph is listening -- default is "localhost"
 *   ddd - Database directory (folder)
 *   nnn - Database name -- default "tsx" 
 *   rrr - Name of RDF input file (optional)
 *   ttt - Name of Triples XML file (optional) 
 *    -x - if specified, start AllegroGraph server 
 *   ccc - chunk size, 0 means use default, -1 means skip indexing (default),
 *         if less than 65 it is exponent of 2, otherwise it is an exact value
 *   rrr - expected resource setting
 *         0 means use default, -1 means skip indexing (default),
 *         if less than 65 it is exponent of 2, otherwise it is an exact value
 * </pre>
 */
public class AGLoad {
	
	static int debug = 0;

	public static void main(String[] args) throws AllegroGraphException,
			IOException {

		int port = 4567; // where AllegroGraph is listening
		String host = "localhost"; // where AllegroGraph is listening
		String dbDir = "*"; // where Triple Store will go
		String dbName = "tsx"; // the database name
//		String rdfFile = ""; // a source file for database
		String tripleFile = ""; // a source file for database
		//String mode = "direct";
		boolean start = false;
		long doIndex = -1;
		long expected = 0;
		String logFile = null;


		// Scan startup parameters
		for (int i = 0; i < args.length;) {
			if ((args[i]).equals("-p"))
				port = Integer.parseInt(args[++i]);
			else if ((args[i]).equals("-h"))
				host = args[++i];
			else if ((args[i]).equals("-d"))
				dbDir = args[++i];
			else if ((args[i]).equals("-n"))
				dbName = args[++i];
//			else if ((args[i]).equals("-r"))
//				rdfFile = args[++i];
			else if ((args[i]).equals("-t"))
				tripleFile = args[++i];
			else if ((args[i]).equals("-log"))
				logFile = args[++i];
			else if ((args[i]).equals("-z"))
				debug = 1;
			else if ((args[i]).equals("-index"))
				doIndex = Integer.parseInt(args[++i]);
			else if ((args[i]).equals("-res"))
				expected = Integer.parseInt(args[++i]);
			else if ((args[i]).equals("-x"))
				start = true;
			i++;
		}

		if (dbDir.equals("*")) {
			pr("Database folder argument (-d) is requires.");
			System.exit(1);
		}
		prd("port=" + port + "  dbDir=" + dbDir + "  dbName=" + dbName);

		// Connect to the Lisp server.
		AllegroGraphConnection ags = new AllegroGraphConnection();
		ags.setPort(port);
		ags.setHost(host);
		ags.setDebug(debug);
		boolean started = false;
		if ( start )
		{
			ags.startServer(logFile);
			started = true;
		}
		ags.enable();
		prd("Connected to " + ags);
		
		AllegroGraph ts;
		TriplesIterator cc;
		int count;
		
		if ( doIndex>0 )
		{
			if ( doIndex<65 )
				ags.setChunkSize(1<<doIndex);
			else
				ags.setChunkSize(doIndex);
		}
		if ( expected>0 ) {
			if ( expected<65 )
				ags.setDefaultExpectedResources(1<<expected);
			else
				ags.setDefaultExpectedResources(expected);
		}
		

//		if (!rdfFile.equals("")) {
//			// Create a new database
//			ts = ags.replace(dbName, dbDir);
//			pr("  numberOfTriples() = " + ts.numberOfTriples());
//			ts.loadRDF(rdfFile);
//			pr("  numberOfTriples() = " + ts.numberOfTriples());
//			if ( doIndex>-1 ) ts.indexAllTriples();
//			String sr = null;
//			cc = ts.getStatements(sr, null, null);
//
//			count = 0;
//			while (cc.step() && count < 10) {
//				pr("from RDF file " + (++count) + " " + cc);
//				prg(cc.getTriple());
//			}
//			ts.closeTripleStore();
//		}

		if (!tripleFile.equals("")) {
			// Create a new database
			ts = ags.replace(dbName, dbDir);
			pr("  numberOfTriples() = " + ts.numberOfTriples());
			ts.loadNTriples(tripleFile);
			pr("  numberOfTriples() = " + ts.numberOfTriples());
			if ( doIndex>-1 ) ts.indexAllTriples();
			String sn = null;
			cc = ts.getStatements(sn, null, null);

			count = 0;
			while (cc.step() && count < 3) {
				pr("from ntriple file " + (++count) + " " + cc);
				prg(cc.getTriple());
			}
			ts.closeTripleStore();
		}

		// Close down the store

		
		pr("ALL DONE.");
		ags.disable();
		if ( started ) ags.stopServer();
	}

	static void pr(String arg) {
		System.out.println(arg);
	}

	static void pr(String[] arg) {
		for (int i = 0; i < arg.length; i++)
			System.out.println(arg[i]);
	}

	static void prd(String arg) {
		if ( debug>0 )  pr(arg);
	}

	static void prg(Triple tr) throws AllegroGraphException {
		pr("Triple " + tr);
		pr("   triple subject:   " + tr.getSubjectLabel());
		pr("   triple predicate: " + tr.getPredicateLabel());
		pr("   triple object:    " + tr.getObjectLabel());
	}

}
