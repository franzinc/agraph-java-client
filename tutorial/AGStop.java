import java.io.IOException;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.Triple;

/**
 * Stop the AllegroGraph server.
 * 
 * Start with:
 * 
 * <pre>
 *   java AGStop [-p lll] [-h hhh] [-j jjj] [-z] [ -moded | -modej ]
 *                  
 *   lll - Primary server port number
 *   hhh - Hostname where server is listening -- default is "localhost"
 *   jjj - Second port number (needed if mode=jlinker) 
 * </pre>
 */
public class AGStop {
	
	static int debug = 0;

	static String lit(String s) {
		return "\"" + s + "\"";
	}

	static String uri(String s) {
		return "<http://" + s + ">";
	}

	public static void main(String[] args) throws AllegroGraphException,
			IOException {

		int port = 4567; // where jlinker is listening
		int jport = 4568; // other port where jlinker is listening
		String host = "localhost"; // where jlinker is listening
		//String mode = "direct";

		// Scan startup parameters
		for (int i = 0; i < args.length;) {
			if ((args[i]).equals("-p"))
				port = Integer.parseInt(args[++i]);
			else if ((args[i]).equals("-j"))
				jport = Integer.parseInt(args[++i]);
			else if ((args[i]).equals("-h"))
				host = args[++i];
			else if ((args[i]).equals("-z"))
				debug = 1;
			//else if ((args[i]).equals("-modej")) mode = "jlinker";
			//else if ((args[i]).equals("-moded")) mode = "direct";
			i++;
		}

		prd("port=" + port + "  jport=" + jport);

		// Connect to the Lisp server.
		AllegroGraphConnection ags = new AllegroGraphConnection();
		ags.setPort(port);
		ags.setPort2(jport);
		ags.setHost(host);
		ags.setDebug(debug);
		//if (!mode.equals("direct")) ags.setJLinker();
		
		ags.enable();
		prd("Connected to " + ags);

		int rc = ags.stopServer(true);
		if ( rc==0 )
			pr("Stopped the AllegroGraph server application.");
		else if ( rc<0 )
			pr("Could not connect to AllegroGraph server application.");
		else
			pr("Attempt to stop AllegroGraph server application failed: " + rc);
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
