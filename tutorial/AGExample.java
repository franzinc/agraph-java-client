import java.io.IOException;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.Triple;
import com.franz.agbase.TriplesIterator;

/**
 * Sample code accessing a Triple Store database.
 * 
 * Start with:
 * 
 * <pre>
 *    java AGExample [-p lll] [-h hhh] [-d ddd] [-n nnn] [-r rrr]
 *                   [-t ttt] [-w www] [-z] [-x]
 *                   
 *    lll - AllegroGraph server port number
 *    hhh - Hostname where server is listening -- default is &quot;localhost&quot;
 *    ddd - Database directory (folder)
 *    nnn - Database name -- default &quot;tsx&quot; 
 *    rrr - Name of RDF input file (optional)
 *    ttt - Name of Triples XML file (optional) 
 *    www - wait this many seconds before closeDatabase and exit 
 *     -x - if specified, start AllegroGraph server 
 * </pre>
 */
public class AGExample {

	static int debug = 0;
	static boolean quiet = false;

	static String lit(String s) {
		return "\"" + s + "\"";
	}

	static String uri(String s) {
		return "<http://" + s + ">";
	}

	public static void main(String[] args) throws AllegroGraphException,
			IOException {

		int port = 4567; // where server is listening
		String host = "localhost"; // where server is listening
		String dbDir = "*"; // where Triple Store will go
		String dbName = "tsx"; // the database name
//		String rdfFile = ""; // a source file for database
		String tripleFile = ""; // a source file for database
		int exitWait = 0;
		boolean start = false;

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
			else if ((args[i]).equals("-w"))
				exitWait = Integer.parseInt(args[++i]);
			else if ((args[i]).equals("-z"))
				debug = 1;
			else if ((args[i]).equals("-zz"))
				debug = 2;
			else if ((args[i]).equals("-x"))
				start = true;
			else if ((args[i]).equals("-q"))
				quiet = true;
			i++;
		}

		if (dbDir.equals("*")) {
			pr("Database folder argument (-d) is required.");
			System.exit(1);
		}
		prd("port=" + port + "  dbDir=" + dbDir + "  dbName=" + dbName);

		// Connect to the server.
		AllegroGraphConnection ags = new AllegroGraphConnection();
		ags.setPort(port);
		ags.setHost(host);
		ags.setDebug(debug);
		boolean started = false;
		if ( start ) {
			ags.startServer();
			started = true;
		}
		ags.enable();
		prd("Connected to " + ags);

		try {

			// Create a AllegroGraph database instance.
			AllegroGraph ts = ags.renew(dbName, dbDir);

			pr("  numberOfTriples() = " + ts.numberOfTriples());

			// Create some individual triples.
			ts.addStatement(uri("sub1"), uri("pred1"), lit("obj1"));
			ts.addStatement(uri("sub2"), uri("pred2"), lit("obj2"));
			pr("  numberOfTriples() = " + ts.numberOfTriples());

			// Create several triples in one call.
			ts.addStatements(new String[] { uri("sub3"), uri("sub4"),
					lit("sub5") }, new String[] { uri("pred3"), uri("pred4"),
					lit("pred5") }, new String[] { uri("obj3"), uri("obj4"),
					lit("obj5") });
			pr("  numberOfTriples() = " + ts.numberOfTriples());

			// Create several triples with common content.
			ts.addStatements(new String[] { uri("sub6") }, new String[] {
					uri("pred3"), uri("pred4"), uri("pred5") }, new String[] {
					lit("obj3"), lit("obj4") });
			pr("  numberOfTriples() = " + ts.numberOfTriples());

			// Another way to create triples.
			ts.newTriple(uri("sub7"), uri("pred7"), lit("obj7"));
			ts.newTriples(new String[] { uri("sub7") },
					new String[] { uri("pred6") }, new String[] { lit("obj3"),
							lit("obj4"), lit("obj5") });
			pr("  numberOfTriples() = " + ts.numberOfTriples());

			// Find some triples.
			TriplesIterator cc = ts.getStatements(uri("sub7"), uri("pred6"), null);
			pr("atTriple=" + cc.atTriple() + "   nextP=" + cc.hasNext());
			pr("toString: " + cc);
			if (cc.step())
				pr("getTriple: " + cc.getTriple());
			if (cc.step())
				pr("getTriple: " + cc.getTriple());
			if (cc.step())
				pr("getTriple: " + cc.getTriple());
			pr("toString: " + cc);
			pr("next of empty " + cc.step() + "  " + cc);

			cc = ts.getStatements(uri("sub7"), uri("pred6"), null);
			Triple[] tb = cc.step(3);
			pr("next(3) returns " + tb.length);
			for (int i = 0; i < tb.length; i++)
				pr(i + " - " + tb[i]);

			cc = ts.getStatements(uri("sub7"), uri("pred6"), null);
			tb = cc.step(4);
			pr("next(4) returns " + tb.length);
			for (int i = 0; i < tb.length; i++)
				pr(i + " - " + tb[i]);

			// Get many triples and extaract their components.
			cc = ts.getStatements(null, null, lit("obj4"));
			pr("get(nil nil obj4) = " + cc);
			if (cc.step()) {
				pr("S=" + cc.getS() + "  P=" + cc.getP() + "  O=" + cc.getO());
				pr("id=" + cc.get_id() + "   " + cc);
				pr("Literals: " + cc.getSubject() + "  " + cc.getPredicate()
						+ "  " + cc.getObject());
				pr("Triple: " + cc.getTriple());
			} else
				pr("NO NEXT: " + cc);

			// Create many triples, one at a time.
			pr("Before 10  numberOfTriples() = " + ts.numberOfTriples());
			for (int i = 100; i < 110; i++)
				ts
						.addStatement(uri("sub" + i), uri("pred" + i),
								lit("obj" + i));
			pr("After 10  numberOfTriples() = " + ts.numberOfTriples());

			// Create many triples in one operation.
			String[] ss = new String[100];
			String[] pp = new String[100];
			String[] oo = new String[100];
			for (int i = 0; i < 100; i++) {
				ss[i] = uri("suba" + i);
				pp[i] = uri("preda" + i);
				oo[i] = lit("obja" + i);
			}
			pr("Before 100  numberOfTriples() = " + ts.numberOfTriples());
			ts.addStatements(ss, pp, oo);
			pr("After 100  numberOfTriples() = " + ts.numberOfTriples());

			// Get all the triples in the database.
			String s = null;
			cc = ts.getStatements(s, null, null);
			int count = 0;
			Triple tr = null;
			while (cc.step()) {
				count++;
				if (!quiet) pr("all " + count + " " + cc);
				// save one
				if (count == 7)
					tr = cc.getTriple();
			}
			// get the components of the saved triple
			pr("Query call returns null values for components.");
			prq(tr);
			pr("Get call returns actual values for components.");
			prg(tr);
			pr("After a get, query call returns actual values for components.");
			prq(tr);

			if (exitWait > 0)
				try {
					Thread.sleep(1000 * exitWait);
				} catch (java.lang.InterruptedException e) {
				}
			ts.closeTripleStore();

			// The following call sets the expected number of
			// unique resources. A rough rule of thumb is to
			// specify a number that is one third of the number of
			// triples.
			// 
			// ags.setDefaultExpectedResources(10000000);

//			if (!rdfFile.equals("")) {
//				// Create a new database
//				ts = ags.replace(dbName, dbDir);
//				pr("  numberOfTriples() = " + ts.numberOfTriples());
//				ts.loadRDF(rdfFile);
//				pr("  numberOfTriples() = " + ts.numberOfTriples());
//
//				// The triples just loaded are immediately available,
//				// but access is faster when triples are indexed.
//				// indexAll() indexes all the triples in the database.
//				// indexTriples() indexes unindexed triples.
//				ts.indexAllTriples();
//
//				String sr = null;
//				cc = ts.getStatements(sr, null, null);
//
//				count = 0;
//				tr = null;
//				while (cc.step() && count < 10) {
//					pr("from RDF file " + (++count) + " " + cc);
//					prg(cc.getTriple());
//				}
//				ts.closeTripleStore();
//			}

			if (!tripleFile.equals("")) {
				// Create a new database
				ts = ags.replace(dbName, dbDir);
				pr("  numberOfTriples() = " + ts.numberOfTriples());
				ts.loadNTriples(tripleFile);
				pr("  numberOfTriples() = " + ts.numberOfTriples());

				// The triples just loaded are immediately available,
				// but access is faster when triples are indexed.
				// indexAll() indexes all the triples in the database.
				// indexTriples() indexes unindexed triples.
				ts.indexAllTriples();

				String sn = null;
				cc = ts.getStatements(sn, null, null);

				count = 0;
				tr = null;
				while (cc.step() && count < 10) {
					pr("from ntriple file " + (++count) + " " + cc);
					prg(cc.getTriple());
				}
				ts.closeTripleStore();
			}

			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Close down the store

			pr("ALL DONE.");
			ags.disable();
			if (started)
				ags.stopServer();
		}
	}
	

	static void pr(String arg) { System.out.println(arg); }

	static void pr(String[] arg) {
		for (int i = 0; i < arg.length; i++)
			System.out.println(arg[i]);
	}

	static void prd(String arg) {
		if ( debug>0 )  pr(arg);
	}

	static void prq(Triple tr) {
		pr("Triple " + tr);
		pr("   triple subject:   " + tr.querySubject());
		pr("   triple predicate: " + tr.queryPredicate());
		pr("   triple object:    " + tr.queryObject());
	}

	static void prg(Triple tr) throws AllegroGraphException {
		pr("Triple " + tr);
		pr("   triple subject:   " + tr.getSubjectLabel());
		pr("   triple predicate: " + tr.getPredicateLabel());
		pr("   triple object:    " + tr.getObjectLabel());
	}

}
