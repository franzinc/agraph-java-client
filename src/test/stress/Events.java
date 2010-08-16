/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.stress;

import static test.AGAbstractTest.findServerUrl;
import static test.AGAbstractTest.password;
import static test.AGAbstractTest.username;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.ntriples.NTriplesUtil;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.util.Util;

public class Events {
	
	static private final Random RANDOM = new Random();

	private static class Defaults {
		
		private static CommandLine cmd;

		// The namespace
	    static private final String NS = "http://franz.com/events#";
	
	    // Number of worker processes
	    static int LOAD_WORKERS = 20;
	    static int QUERY_WORKERS = 16;
	    static int DELETE_WORKERS = 2;
	    static int MIXED_WORKERS = 16;
	
	    // Time to run queries in minutes
	    static private int QUERY_TIME = 10;
	
	    // Size of the Events
	    static private int EVENT_SIZE = 50;
	
	    // Events per commit in bulk load phase
	    static private int BULK_EVENTS = 250;
	
	    // Goal store size
	    static int SIZE = (int) Math.pow(10, 8);
	
	    // The catalog name
	    static private String CATALOG = "tests";
	
	    // The repository name
	    static private String REPOSITORY = "events_test";
	    
	    static int STATUS = 50;
	    
	    static int VERBOSE = 0;

		static String URL = findServerUrl();

		static String USERNAME = username();

		static String PASSWORD = password();

		static String cmdVal(String opt, String defaultVal) {
			String val = cmd.getOptionValue(opt);
			if (val == null) {
				return defaultVal;
			} else {
				return val;
			}
		}
		
		static int cmdVal(String opt, int defaultVal) {
			String val = cmd.getOptionValue(opt);
			if (val == null) {
				return defaultVal;
			} else {
				return Integer.parseInt(val);
			}
		}
		
		static int cmdVal(String opt, int defaultVal, int defaultVal2) {
			if (cmd.hasOption(opt)) {
				String val = cmd.getOptionValue(opt);
				if (val == null) {
					return defaultVal2;
				} else {
					return Integer.parseInt(val);
				}
			} else {
				return defaultVal;
			}
		}
		
		@SuppressWarnings("static-access")
		public static void init(String[] args) throws Exception {
			Options options = new Options();
			options.addOption(new Option("h", "help", false, "print this message"));
			options.addOption(OptionBuilder.withLongOpt("sparql")
					.withDescription("Use only sparql, no prolog")
					.create());
			options.addOption(OptionBuilder.withLongOpt("supersede")
					.withDescription("SUPERSEDE the repository instead of OPENing it")
					.create());
			options.addOption(OptionBuilder.withLongOpt("status")
					.withArgName("STATUS").hasArg()
					.withDescription("use STATUS size [default=" + Defaults.STATUS + "]")
					.create());
			options.addOption(OptionBuilder.withLongOpt("load")
					.withArgName("LOAD").hasArg()
					.withDescription("use LOAD number of loading processes [default=" + Defaults.LOAD_WORKERS + "]")
					.create("l"));
			options.addOption(OptionBuilder.withLongOpt("query")
					.withArgName("QUERY").hasArg()
					.withDescription("use QUERY number of querying processes [default=" + Defaults.QUERY_WORKERS + "]")
					.create("q"));
			options.addOption(OptionBuilder.withLongOpt("delete")
					.withArgName("DELETE").hasArg()
					.withDescription("use DELETE number of deleting processes [default=" + Defaults.DELETE_WORKERS + "]")
					.create("d"));
			options.addOption(OptionBuilder.withLongOpt("size")
					.withArgName("SIZE").hasArg()
					.withDescription("SIZE triple limit for bulk load (e.g. 10,000, 100m, 2b, 1.5t) (minimum=1000) [default=" + Defaults.SIZE + "]")
					.create("s"));
			options.addOption(OptionBuilder.withLongOpt("time")
					.withArgName("MINUTES").hasArg()
					.withDescription("MINUTES time limit for query phase [default=" + Defaults.QUERY_TIME + "]")
					.create("t"));
			options.addOption(OptionBuilder.withLongOpt("event")
					.withArgName("EVENT_SIZE").hasArg()
					.withDescription("each event will contain EVENT_SIZE number of triples [default=" + Defaults.EVENT_SIZE + "]")
					.create("e"));
			options.addOption(OptionBuilder.withLongOpt("bulk")
					.withArgName("BULK_EVENTS").hasArg()
					.withDescription("commit BULK_EVENTS number of events per commit during bulk load [default=" + Defaults.BULK_EVENTS + "]")
					.create("b"));
			options.addOption(OptionBuilder.withLongOpt("mixed")
					.withArgName("MIXED").hasArg()
					.withDescription("Add MIXED workload phase after the normal run (runs until java process is killed) [default=do not run]")
					.create("m"));
			options.addOption(OptionBuilder.withLongOpt("verbose")
					.withArgName("VERBOSE").hasOptionalArg()
					.withDescription("Verbosity level for extra log messages if VERBOSE > 0. [default=0, no value=1]")
					.create("v"));
			options.addOption(OptionBuilder.withLongOpt("catalog")
					.withArgName("CATALOG").hasArg()
					.withDescription("AGraph catalog [default=" + Defaults.CATALOG + "]")
					.create());
			options.addOption(OptionBuilder.withLongOpt("catalog")
					.withArgName("CATALOG").hasArg()
					.withDescription("AGraph catalog [default=" + Defaults.CATALOG + "]")
					.create());
			options.addOption(OptionBuilder.withLongOpt("repository")
					.withArgName("REPOSITORY").hasArg()
					.withDescription("AGraph repository [default=" + Defaults.REPOSITORY + "]")
					.create());
			options.addOption(OptionBuilder.withLongOpt("url")
					.withArgName("URL").hasArg()
					.withDescription("AGraph URL [default=" + Defaults.URL +
							" from env var AGRAPH_HOST, java property AGRAPH_HOST, or \"localhost\";" +
							" and env var AGRAPH_PORT, java property AGRAPH_PORT, ../agraph/lisp/agraph.port, or 10035")
					.create());
			options.addOption(OptionBuilder.withLongOpt("username")
					.withArgName("USERNAME").hasArg()
					.withDescription("AGraph username [default=" + Defaults.USERNAME +
					" from env var AGRAPH_USER, or \"test\"]")
					.create());
			options.addOption(OptionBuilder.withLongOpt("password")
					.withArgName("PASSWORD").hasArg()
					.withDescription("AGraph password [default=" + Defaults.PASSWORD +
					" from env var AGRAPH_PASSWORD, or \"xyzzy\"]")
					.create());
			
			cmd = new PosixParser().parse(options, args);
			if (cmd.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("./events [options]",
						"To performance test AllegroGraph v4, connects as" +
						" a client, uses multiple threads, and adds data, makes" +
						" queries, deletes data, and runs mixed-load threads.",
						options,
						"Example usage:\n" +
						"./events.sh --help\n" +
						"# Prints help text.\n" +
						"./events.sh \n" +
						"# Runs default options: adds data to existing repo.\n" +
						"./events.sh --status 500 -v 1 -l 1 -d 0 -q 1 -m 1 --size 100000 --sparql\n" +
						"# Runs less verbose status, single-threaded, creating a small repo, no deletes, sparql queries.\n" +
						"./events.sh --supersede -l 10 -d 2 -q 2 -m 2 --size 1m\n" +
						"# Runs with a cleared repo, prolog queries, multi-threaded.\n",
						false);
				System.exit(0);
			}
			
			LOAD_WORKERS = cmdVal("load", LOAD_WORKERS);
			QUERY_WORKERS = cmdVal("query", QUERY_WORKERS);
			DELETE_WORKERS = cmdVal("delete", DELETE_WORKERS);
			MIXED_WORKERS = cmdVal("mixed", MIXED_WORKERS);
			STATUS = cmdVal("status", STATUS);
			SIZE = Math.max(1000, fromHumanInt( cmdVal("size", "" + SIZE) ));
			QUERY_TIME = cmdVal("time", QUERY_TIME);
			EVENT_SIZE = cmdVal("event", EVENT_SIZE);
			BULK_EVENTS = cmdVal("bulk", BULK_EVENTS);
			VERBOSE = cmdVal("verbose", VERBOSE, 1);
			CATALOG = cmdVal("catalog", CATALOG);
			REPOSITORY = cmdVal("repository", REPOSITORY);
			URL = cmdVal("url", URL);
			USERNAME = cmdVal("username", USERNAME);
			PASSWORD = cmdVal("password", PASSWORD);
		}

		public static boolean hasOption(String opt) {
			return cmd.hasOption(opt);
		}
		
		static int fromHumanInt(String value) {
			int len = value.length();
			if (len > 1) {
				char c = value.charAt(len-1);
				if ( ! Character.isDigit(c)) {
					int n = Integer.parseInt(value.substring(0, len-1));
					if (c == 'm')
						return n * (int) Math.pow(10, 6);
					else if (c == 'b')
						return n * (int) Math.pow(10, 9);
					else if (c == 't')
						return n * (int) Math.pow(10, 12);
				}
			}
			return Integer.parseInt(value);
		}
		
	}

	public static void trace(String format, Object... values) {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		Calendar now = GregorianCalendar.getInstance();
		
		formatter.format("%s [%2$tF %2$tT.%2$tL]: ", Thread.currentThread().getName(), now);
		formatter.format(format, values);
		System.out.println(sb.toString());
	}
	
	public static AGRepositoryConnection connect(boolean shared) throws RepositoryException {
        AGServer server = new AGServer(findServerUrl(), username(), password());
        AGCatalog catalog = server.getCatalog(Defaults.CATALOG);
        AGRepository repository = catalog.createRepository(Defaults.REPOSITORY);
        repository.initialize();
        AGRepositoryConnection conn = repository.getConnection();
        
        if (!shared) {
        	// Force an auto-committing non-shared backend 
        	conn.setAutoCommit(false);
        	conn.setAutoCommit(true);
        }
        
        return conn;
    }

	private static class ThreadVars {
		private static ThreadLocal<ValueFactory> valueFactory = new ThreadLocal<ValueFactory>();
		private static ThreadLocal<AGRepositoryConnection> connection = new ThreadLocal<AGRepositoryConnection>();
		private static ThreadLocal<RandomDate> dateMaker = new ThreadLocal<RandomDate>();
		private static ThreadLocal<DatatypeFactory> datatypeFactory = new ThreadLocal<DatatypeFactory>() {
			protected DatatypeFactory initialValue() {
				DatatypeFactory factory = null;
				try {
					factory = DatatypeFactory.newInstance();
				} catch (DatatypeConfigurationException e) {
					System.err.println("Couldn't create DatatypeFactory!");
					e.printStackTrace();
				}
				return factory;
			}
		};
	}
	    
    private static class RandomDate {
		public GregorianCalendar start, end;
    	int seconds;
    	
    	public RandomDate(GregorianCalendar theStart, GregorianCalendar theEnd) {
    		assert theStart.before(theEnd);
    		start = theStart;
    		end = theEnd;
    		seconds = (int) ((end.getTimeInMillis() - start.getTimeInMillis()) / 1000L); 
    	}
    	
    	public GregorianCalendar getRandom() {
    		GregorianCalendar rand = (GregorianCalendar) start.clone();
    		rand.setTimeInMillis(rand.getTimeInMillis() + RANDOM.nextInt(seconds)*1000L);
    		return rand;
    	}
    }
    
    static private final RandomDate BaselineRange = new RandomDate(new GregorianCalendar(2008, Calendar.JANUARY, 1),
    		new GregorianCalendar(2008, Calendar.FEBRUARY, 1));
    static private final RandomDate BulkRange = new RandomDate(BaselineRange.end,
    		new GregorianCalendar(2009, Calendar.JANUARY, 1));
    static private final RandomDate SmallCommitsRange = new RandomDate(BulkRange.end,
			new GregorianCalendar(2009, Calendar.FEBRUARY, 1));
    static private final RandomDate DeleteRangeOne = new RandomDate(BaselineRange.start,
			new GregorianCalendar(2008, Calendar.JANUARY, 16));
    static private final RandomDate DeleteRangeTwo = new RandomDate(DeleteRangeOne.end,
	        BaselineRange.end);
    static private final RandomDate FullDateRange = new RandomDate(BaselineRange.start,
			SmallCommitsRange.end);

	private static interface RandomCallback {
		public Value makeValue();
	}

	private static Value CalendarToValue(GregorianCalendar calendar) {
		return ThreadVars.valueFactory.get().createLiteral(ThreadVars.datatypeFactory.get().newXMLGregorianCalendar(calendar));
	}
	
	private static class RandomCalendar implements RandomCallback {
		public Value makeValue() {
			return CalendarToValue(ThreadVars.dateMaker.get().getRandom());
		}
	}
	
	private static class RandomInteger implements RandomCallback {
		public Value makeValue() {
			return ThreadVars.valueFactory.get().createLiteral(RANDOM.nextInt());
		}
	}

	private static class RandomName implements RandomCallback {
		static String[] firsts = new String[] { "Sam", "Bruce", "Mandy", "Stacy", "Marcus", "Susan",
			    "Jason", "Chris", "Becky", "Britney", "David", "Paul", "Daniel", "James",
			    "Bradley", "Amy", "Tina", "Brandy", "Jessica", "Mary", "George", "Jane" };
		static String[] lasts = new String[] { "Smith", "Jones", "Flintstones", "Rubble", "Jetson",
			    "Wayne", "McFly", "Stadtham", "Lee", "Chan", "Brown", "Quinn",
			    "Henderson", "Anderson", "Roland" };

	    public Value makeValue() {
	    	int first = RANDOM.nextInt(firsts.length);
	    	int last = RANDOM.nextInt(lasts.length);
			return ThreadVars.valueFactory.get().createLiteral(firsts[first] + " " + lasts[last]);
		}
	}

	private static class RandomDirection implements RandomCallback {
		static String[] directions = new String[] { "Inbound", "Outbound" };
		
	    public Value makeValue() {
	    	int direction = RANDOM.nextInt(directions.length);
			return ThreadVars.valueFactory.get().createLiteral(directions[direction]);
		}
	}

	private static class RandomBoolean implements RandomCallback {
	    public Value makeValue() {
	    	boolean isOdd = RANDOM.nextInt(2) == 1;
	    	return ThreadVars.valueFactory.get().createLiteral(isOdd);
		}
	}

	private static class RandomOrigin implements RandomCallback {
		static String[] origins = new String[] { "Call Center", "Sales", "Front Desk" };
		
	    public Value makeValue() {
	    	int origin = RANDOM.nextInt(origins.length);
			return ThreadVars.valueFactory.get().createLiteral(origins[origin]);
		}
	}

	private static class RandomPayOption implements RandomCallback {
		static String[] payoptions = new String[] { "Cash", "Credit", "Money Order" };
		
	    public Value makeValue() {
	    	int payoption = RANDOM.nextInt(payoptions.length);
			return ThreadVars.valueFactory.get().createLiteral(payoptions[payoption]);
		}
	}

	private static class RandomDelivery implements RandomCallback {
		static String[] deliverytypes = new String[] { "Mail", "FedEx", "UPS", "Truck Freight" };
		
	    public Value makeValue() {
	    	int deliverytype = RANDOM.nextInt(deliverytypes.length);
			return ThreadVars.valueFactory.get().createLiteral(deliverytypes[deliverytype]);
		}
	}

	private static class RandomMoney implements RandomCallback {
	    public Value makeValue() {
			return ThreadVars.valueFactory.get().createLiteral(RANDOM.nextFloat()*10000.0);
		}
	}

	private static class RandomAction implements RandomCallback {
		static String[] actions = new String[] { "Add", "Modify" };
		
	    public Value makeValue() {
	    	int action = RANDOM.nextInt(actions.length);
			return ThreadVars.valueFactory.get().createLiteral(actions[action]);
		}
	}

	private static class RandomHandling implements RandomCallback {
		static String[] handlings = new String[] { "Nothing", "Past Due Notice", "Collections" };
		
	    public Value makeValue() {
	    	int handling = RANDOM.nextInt(handlings.length);
			return ThreadVars.valueFactory.get().createLiteral(handlings[handling]);
		}
	}

	private static class RandomCustomer implements RandomCallback {
		public Value makeValue() {
			return ThreadVars.valueFactory.get().createURI(Defaults.NS, "Customer-" + RANDOM.nextInt(Defaults.SIZE/1000));
		}
	}
	
	private static class RandomAccount implements RandomCallback {
		public Value makeValue() {
			return ThreadVars.valueFactory.get().createURI(Defaults.NS, "Account-" + RANDOM.nextInt(Defaults.SIZE/1000));
		}
	}

	private static class RandomProductID implements RandomCallback {
		public Value makeValue() {
			return ThreadVars.valueFactory.get().createURI(Defaults.NS, "ProductID-" + RANDOM.nextInt(Defaults.SIZE/1000));
		}
	}

	private static class RandomPaymentTarget implements RandomCallback {
		public Value makeValue() {
			return ThreadVars.valueFactory.get().createURI(Defaults.NS, "PaymentTarget-" + RANDOM.nextInt(100));
		}
	}

	private static class RandomBusinessEntity implements RandomCallback {
		public Value makeValue() {
			return ThreadVars.valueFactory.get().createURI(Defaults.NS, "DepositDesignation-" + RANDOM.nextInt(100));
		}
	}
	
	private static class RandomDepositDesignation implements RandomCallback {
		public Value makeValue() {
			return ThreadVars.valueFactory.get().createURI(Defaults.NS, "BusinessEntity-" + RANDOM.nextInt(100));
		}
	}

	private static class TypeURI implements RandomCallback {
		private String typeLabel;
		
		public TypeURI(String label) {
			typeLabel = label;
		}
		
		public Value makeValue() {
			return ThreadVars.valueFactory.get().createURI(Defaults.NS, typeLabel);
		}
	}

	private static class StringValue implements RandomCallback {
		private String value;
		
		public StringValue(String theValue) {
			value = theValue;
		}
		
		public Value makeValue() {
			return ThreadVars.valueFactory.get().createLiteral(value);
		}
	}

	private static class PredicateInfo {
		private URI predicate;
		private RandomCallback objectMaker;
		
		public PredicateInfo(URI thePredicate, RandomCallback theMaker) {
			predicate = thePredicate;
			objectMaker = theMaker;
		}
		
		public PredicateInfo(String label, RandomCallback theMaker) {
			predicate = ThreadVars.valueFactory.get().createURI(Defaults.NS, label);
			objectMaker = theMaker;
		}
		
		public Statement makeStatement(Resource subject, Resource context) {
			return ThreadVars.valueFactory.get().createStatement(subject, predicate, objectMaker.makeValue(), context);
		}
	}
	
	private static class AllEvents {
		private static void PadEvent(String padName, PredicateInfo[] to, PredicateInfo[] from) {
			System.arraycopy(from, 0, to, 0, from.length);
			
			for (int i = from.length; i < Defaults.EVENT_SIZE; i++) {
				to[i] = new PredicateInfo(padName + i, new RandomInteger());
			}
		}

		private static PredicateInfo[] interaction = new PredicateInfo[Defaults.EVENT_SIZE];
		private static PredicateInfo[] invoice = new PredicateInfo[Defaults.EVENT_SIZE];
		private static PredicateInfo[] payment = new PredicateInfo[Defaults.EVENT_SIZE];
		private static PredicateInfo[] purchase = new PredicateInfo[Defaults.EVENT_SIZE];

		public static void initialize() {
			PadEvent("Interaction", interaction, new PredicateInfo[] {
					new PredicateInfo(RDF.TYPE, new TypeURI("CustomerInteraction")),
			        new PredicateInfo("EventTimeStamp", new RandomCalendar()),
			        new PredicateInfo("EntityId", new RandomInteger()),
			        new PredicateInfo("OriginatingSystem", new StringValue("CRM")),
			        new PredicateInfo("Agent", new RandomName()),
			        new PredicateInfo("Direction", new RandomDirection()),
			        new PredicateInfo("DoneInOne", new RandomBoolean()),
			        new PredicateInfo("EndDate", new RandomCalendar()),
			        new PredicateInfo("FeeBased", new RandomBoolean()),
			        new PredicateInfo("InteractionId", new RandomInteger()),
			        new PredicateInfo("Origin", new RandomOrigin()),
			        new PredicateInfo("PayOption", new RandomPayOption()),
			        new PredicateInfo("ReasonLevel1", new RandomInteger()),
			        new PredicateInfo("ReasonLevel2", new RandomInteger()),
			        new PredicateInfo("OriginatingSystem", new RandomInteger()),
			        new PredicateInfo("Result", new RandomInteger())
				});

			PadEvent("Invoice", invoice, new PredicateInfo[] {
					new PredicateInfo(RDF.TYPE, new TypeURI("Invoice")),
			        new PredicateInfo("EventTimeStamp", new RandomCalendar()),
			        new PredicateInfo("AccountId", new RandomAccount()),
			        new PredicateInfo("OriginatingSystem", new StringValue("Invoicing")), 
			        new PredicateInfo("DueDate", new RandomCalendar()),
			        new PredicateInfo("Action", new RandomAction()),
			        new PredicateInfo("TotalAmountDue", new RandomMoney()),
			        new PredicateInfo("AmountDueHandling", new RandomHandling()),
			        new PredicateInfo("LegalInvoiceNumber", new RandomInteger()),
			        new PredicateInfo("PreviousBalanceAmount", new RandomMoney()),
			        new PredicateInfo("TotalFinanceActivites", new RandomMoney()),
			        new PredicateInfo("BillDate", new RandomCalendar()),
			        new PredicateInfo("TotalUsageCharges", new RandomMoney()),
			        new PredicateInfo("TotalRecurringCharges", new RandomMoney()),
			        new PredicateInfo("TotalOneTimeCharges", new RandomMoney())
			});
		
			PadEvent("Payment", payment, new PredicateInfo[] {
					new PredicateInfo(RDF.TYPE, new TypeURI("AccountPayment")),
					new PredicateInfo("EventTimeStamp", new RandomCalendar()),
					new PredicateInfo("AccountId", new RandomAccount()),
					new PredicateInfo("OriginatingSystem", new StringValue("Ordering")),
					new PredicateInfo("SubscriberId", new RandomCustomer()),
					new PredicateInfo("InvoiceId", new RandomInteger()),
					new PredicateInfo("PaymentAmount", new RandomMoney()),
					new PredicateInfo("OriginalAmount", new RandomMoney()),
					new PredicateInfo("AmountDue", new RandomMoney()),
					new PredicateInfo("DepositDate", new RandomCalendar()),
					new PredicateInfo("PaymentType", new RandomPayOption()),
					new PredicateInfo("OriginalPostedAmount", new RandomMoney()),
					new PredicateInfo("PaymentTarget", new RandomPaymentTarget()),
					new PredicateInfo("DepositDesignation", new RandomDepositDesignation()),
			        new PredicateInfo("BusinessEntity", new RandomBusinessEntity())
			});

			PadEvent("Purchase", purchase, new PredicateInfo[] {
					new PredicateInfo(RDF.TYPE, new TypeURI("Purchase")),
					new PredicateInfo("EventTimeStamp", new RandomCalendar()),
					new PredicateInfo("AccountId", new RandomAccount()),
					new PredicateInfo("OriginatingSystem", new StringValue("Sales")),
					new PredicateInfo("PurchaseDate", new RandomCalendar()),
					new PredicateInfo("PurchaseAmount", new RandomMoney()),
					new PredicateInfo("InvoiceID", new RandomInteger()),
					new PredicateInfo("ProductID", new RandomProductID()), 
					new PredicateInfo("LegalInvoiceNumber", new RandomInteger()),
					new PredicateInfo("PreviousBalanceAmount", new RandomMoney()),
					new PredicateInfo("DeliveredVia", new RandomDelivery()),
					new PredicateInfo("PaidVia", new RandomPayOption()),
					new PredicateInfo("CCApprovalNumber", new RandomInteger()),
					new PredicateInfo("TotalRecurringCharges", new RandomMoney()),
					new PredicateInfo("TotalOneTimeCharges", new RandomMoney())
			});
		};
		
		public static int makeEvent(Vector<Statement> statements, int index) {
		    PredicateInfo[] event = null;
		
 		    switch (RANDOM.nextInt(4)) {
		    case 0:
		    	event = interaction;
		    	break;
		    case 1:
		    	event = invoice;
		    	break;
		    case 2:
		    	event = payment;
		    	break;
		    case 3:
		    	event = purchase;
		    	break;
		    }

		    BNode bnode = ThreadVars.valueFactory.get().createBNode();
		    Resource context = (Resource) new RandomCustomer().makeValue();
		    
		    for (int i = 0; i < Defaults.EVENT_SIZE; index++, i++) {
		    	statements.set(index, event[i].makeStatement(bnode, context));
		    }
			
			return index;
		}
	}
	
	private static class Loader implements Callable<Integer> {
		private int id;
		private int loopCount;
		private int eventsPerCommit;
		private int triplesPerCommit;
		private final RandomDate dateMaker;
		
		public Loader(int theId, int theTripleGoal, int theEventsPerCommit, RandomDate dateMaker) {
			id = theId;
			this.dateMaker = dateMaker;
			triplesPerCommit = theEventsPerCommit * Defaults.EVENT_SIZE;
			loopCount = theTripleGoal / triplesPerCommit / Defaults.LOAD_WORKERS;
			eventsPerCommit = theEventsPerCommit;
		}
			    
		public Integer call() {
			Thread.currentThread().setName("loader(" + id + ")");
	        ThreadVars.dateMaker.set(dateMaker);
			AGRepositoryConnection conn;
			try {
				conn = connect(false);
			} catch (RepositoryException e) {
				e.printStackTrace();
				return -1;
			}
	        ThreadVars.connection.set(conn);
	        ThreadVars.valueFactory.set(conn.getValueFactory());

	        final int statusSize = Defaults.STATUS;
	        int count = 0, errors = 0;
	        Vector<Statement> statements = new Vector<Statement>(triplesPerCommit);
	        Calendar start = GregorianCalendar.getInstance(), end;

	        statements.setSize(triplesPerCommit);

	        for (int loop = 0; loop < loopCount; loop++) {
	        	// Fill in the statements to commit
	        	for (int event = 0, index = 0; event < eventsPerCommit; event++) {
	        		index = AllEvents.makeEvent(statements, index);
	        	}
	        	
	        	if (loop > 0 && (loop % statusSize) == 0) {
	        		end = GregorianCalendar.getInstance();
	        		double seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
					trace("Loading Status - %d triples loaded so " +
						"far at %d triples per commit (%.2f commits/sec, %.2f triples/sec" +
						" over last %d commits), %d errors.", count,
						triplesPerCommit, statusSize/seconds,
						statusSize*triplesPerCommit/seconds,
						statusSize, errors);
					start = end;
	        	}

	        	try {
	        		conn.add(statements);
	        		count += triplesPerCommit;
	        	} catch (Exception e) {
	        	    trace("Error adding statements...");
	        		e.printStackTrace();
	        	}
	        }
	        	
			trace("Loading Done - %d triples at %d triples " +
				"per commit, %d errors.", count, triplesPerCommit, errors);

		 	try {
				conn.close();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
			
			return 0;
		}
	}

	private static class QueryResult {
		public int queries;
		public int triples;
		
		public QueryResult(int theQueries, int theTriples) {
			queries = theQueries;
			triples = theTriples;
		}
	}
	
	private static class Querier implements Callable<QueryResult> {
		private int secondsToRun;
		private int id;
		private String timestamp;
		private final RandomDate dateMaker;
		
		public Querier(int theId, int theSecondsToRun, RandomDate dateMaker) {
			id = theId;
			secondsToRun = theSecondsToRun;
			this.dateMaker = dateMaker;
		}
		
		private int randomQuery(AGRepositoryConnection conn, ValueFactory vf, boolean trace) {
			// Pick a random customer
			String customerNT = NTriplesUtil.toNTriplesString(new RandomCustomer().makeValue());
			
			// Pick a random date range
			GregorianCalendar start, end;
			start = FullDateRange.getRandom();
			end = FullDateRange.getRandom();

			if (start.after(end)) {
				GregorianCalendar swap = end;
				end = start;
				start = swap;
			}

			String startNT, endNT;
			startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
			endNT = NTriplesUtil.toNTriplesString(CalendarToValue(end));
			
			String queryString;
		    TupleQuery tupleQuery;
		    if (Defaults.hasOption("sparql")) {
		    	queryString = String.format(
		    			"select ?s ?p ?o " +
		    			"from %s " +
		    			"where { " +
		    			"  ?s %s ?date . " +
		    			"  filter ( ( ?date >= %s ) && ( ?date <= %s ) ) " +
		    			"  ?s ?p ?o " +
		    			"}",
						customerNT, timestamp, startNT, endNT);
			    tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.SPARQL, queryString);
		    } else {
				queryString = String.format(
						"(select (?s ?p ?o)" +
						"(:use-planner nil)" +
						"(q- ?s !%s (? !%s !%s) !%s)" +
						"(q- ?s ?p ?o !%s))",
						timestamp, startNT, endNT, customerNT, customerNT);
			    tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
		    }
		    
			// Actually pull the full results to the client, then just count them
			TupleQueryResult result = null;
			int count = 0;
			try {
				if (Defaults.VERBOSE > 0 && trace) {
					trace("query: %s", queryString);
				}
				result = tupleQuery.evaluate();
				count = count(result);
				// test sparql and prolog return same results:
//				Set<Stmt> stmts = Stmt.statementSet(result);
//				count = stmts.size();
//				AGAbstractTest.assertSetsEqual(queryString, stmts,
//						Stmt.statementSet(tupleQuery2.evaluate()));
			} catch (Exception e) {
	            trace("Error executing query:\n%s\n", queryString);
				e.printStackTrace();
	            count = -1;
			} finally {
				Util.close(result);
			}

			return count;
		}

		private int count(TupleQueryResult result) throws Exception {
			int count = 0;
			while (result.hasNext()) {
				result.next();
			    count++;
			}
			return count;
		}		
			    
		public QueryResult call() {
			Thread.currentThread().setName("query(" + id + ")");
	    	ThreadVars.dateMaker.set(dateMaker);
			AGRepositoryConnection conn;
			ValueFactory vf;
			try {
				conn = connect(true);
			} catch (RepositoryException e) {
				e.printStackTrace();
				return null;
			}
	        ThreadVars.connection.set(conn);
	        ThreadVars.valueFactory.set(vf = conn.getValueFactory());

	        timestamp = NTriplesUtil.toNTriplesString(vf.createURI(Defaults.NS, "EventTimeStamp"));

	        final int statusSize = Math.max(1, Defaults.STATUS / 5);
	        int count = 0, subcount = 0, queries = 0, restarts = 0;
	        Calendar startTime, start, end;
	        startTime = start= GregorianCalendar.getInstance();

	        while (true) {
	        	// Do the query
	        	int result = randomQuery(conn, vf, (queries % statusSize == 0));
	        	if (result < 0) {
	        		restarts++;
	        	} else {
	        		queries++;
	        		count += result;
	        	}

	        	if (queries % statusSize == 0) {
	        		//if (queries % statusSize == 0 || restarts % statusSize == 0) {
		            end = GregorianCalendar.getInstance();
	        		double seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
		            subcount = count - subcount;
		            trace("Querying status - %d triples returned for %d queries in %.2f sec (%.2f queries/sec, " +
		                "%d triples per query), %d queries aborted.", subcount, statusSize,
		                seconds, statusSize/seconds, subcount/statusSize, restarts);
		            start = end;
		            subcount = count;
	        	
	        		seconds = (end.getTimeInMillis() - startTime.getTimeInMillis()) / 1000.0;
	        		if (seconds > secondsToRun) {
	        			break;
	        		}
	        	}
	        }
	        
	        double seconds = (GregorianCalendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())/1000.0;
	        trace("Querying done - %d triple results returned for %d queries in %f seconds " +
		    	"(%f queries/second, %d triples per query), %d queries aborted.",
		    	count, queries, seconds, queries/seconds, count/queries, restarts);
			
		 	try {
				conn.close();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
			
			return new QueryResult(queries, count);
		}
	}	
	
	private static class Deleter implements Callable<QueryResult> {
		private int id;
		
		public Deleter(int theId) {
			id = theId;
		}
			    
		public QueryResult call() {
			Thread.currentThread().setName("deleter(" + id + ")");
			AGRepositoryConnection conn;
			try {
				conn = connect(false);
			} catch (RepositoryException e) {
				e.printStackTrace();
				return new QueryResult(0, -1);
			}
			ValueFactory vf;
	        ThreadVars.connection.set(conn);
	        ThreadVars.valueFactory.set(vf = conn.getValueFactory());

	        RandomDate range;
	        
	        if (id == 0) {
	        	range = DeleteRangeOne;
	        } else {
	        	range = DeleteRangeTwo;
	        }
	        
	        String timestamp = NTriplesUtil.toNTriplesString(vf.createURI(Defaults.NS, "EventTimeStamp"));
	        int limit = (9 * (int) Math.pow(10,5))/Defaults.EVENT_SIZE;

	        // interval is 1 day's worth of milliseconds.
	        long interval = 24*60*60*1000;
	        
	        GregorianCalendar start, eod;
	        start = range.start;
	        eod = (GregorianCalendar) range.start.clone();
	        eod.setTimeInMillis(eod.getTimeInMillis() + interval);

			String startNT, endNT, queryString;
			startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
			endNT = NTriplesUtil.toNTriplesString(CalendarToValue(eod));
	        
	        long events = 0;
	        long count = 0;
	        if (Defaults.VERBOSE > 0) {
		        trace("deleting %s to %s", start.getTimeInMillis(), eod.getTimeInMillis());
	        }
	        
	        while (start.before(range.end)) {
			    if (Defaults.hasOption("sparql")) {
			    	queryString = String.format(
			    			//"select count(?s) " +
			    			"delete { ?s ?p ?o }" +
			    			"where { " +
			    			//"  graph ?g {" +
			    			"    ?s %s ?date . " +
			    			"    filter ( ( ?date >= %s ) && ( ?date <= %s ) ) " +
			    			"    ?s ?p ?o " +
			    			"}",
							timestamp, startNT, endNT);
			        if (Defaults.VERBOSE > 0) {
			        	trace(queryString);
			        }
			    	AGTupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.SPARQL, queryString);
					TupleQueryResult result = null;
					count = 0;
					try {
						long before = conn.size();
						tupleQuery.count(); // executes it
						count = before - conn.size();
					} catch (Exception e) {
			            trace("Error executing query:\n%s\n", queryString);
						e.printStackTrace();
			            count = -1;
					} finally {
						Util.close(result);
					}
			    } else {
		            queryString = String.format("(select0 (?event)" +
			            	"(:limit %d) (:count-only t)" +
			            	"(q- ?event !%s (? !%s !%s))" +
			            	"(lisp (delete-triples :s ?event)))", limit, timestamp, startNT, endNT);
			        if (Defaults.VERBOSE > 0) {
			        	trace(queryString);
			        }
		            AGTupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
		            try {
		            	count = tupleQuery.count();
		            } catch (QueryEvaluationException e) {
			            trace("Error executing query:\n%s\n", queryString);
						e.printStackTrace();
					}
			    }
			    events += count;
			    
			    if (count == 0) {
			    	trace("Finished deleting %tF.", start);
			    	start = eod;
			    	startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
			    	eod = (GregorianCalendar) start.clone();
			    	eod.setTimeInMillis(eod.getTimeInMillis() + interval);
			    	endNT = NTriplesUtil.toNTriplesString(CalendarToValue(eod));
			    }
	        }

	        trace("Found %d events (%d triples) to delete.", events, events * Defaults.EVENT_SIZE);

	        Util.close(conn);
			
			return new QueryResult(0, 0);
		}
	}

	public static class Monitor {
		static public void start(String phase) {
			try {
			    String[] commands = new String[]{"monitor.sh", "start", phase};
			    Runtime.getRuntime().exec(commands);
			} catch (IOException e) {
				trace("./monitor.sh was not started.");
			}
		}
		
		static public void stop() {
			try {
			    String[] commands = new String[]{"monitor.sh", "end"};
			    Runtime.getRuntime().exec(commands);
			} catch (IOException e) {
				trace("./monitor.sh was not stopped.");
			}
		}
	}
	
    /**
	 * @param args Run with --help
	 */
	public static void main(String[] args) throws Exception {
		Defaults.init(args);
		
		Thread.currentThread().setName("./events");
		
	    AGServer server = new AGServer(Defaults.URL, Defaults.USERNAME, Defaults.PASSWORD);
        AGCatalog catalog = server.getCatalog(Defaults.CATALOG);
        
		if (Defaults.hasOption("supersede")) {
       	    trace("SUPERSEDING %s:%s.", Defaults.CATALOG, Defaults.REPOSITORY);
        	catalog.deleteRepository(Defaults.REPOSITORY);
        } else {
    	    trace("OPENing %s:%s.", Defaults.CATALOG, Defaults.REPOSITORY);
       	}

        AGRepository repository = catalog.createRepository(Defaults.REPOSITORY);
        repository.initialize();
        AGRepositoryConnection conn = repository.getConnection();
        ThreadVars.connection.set(conn);
        ThreadVars.valueFactory.set(conn.getValueFactory());

	    AllEvents.initialize();

	    final long triplesAtStart = conn.size();
	    
	    trace("Testing with %d loading, %d querying processes. Repository contains %d triples.", 
            Defaults.LOAD_WORKERS, Defaults.QUERY_WORKERS, triplesAtStart);
	    final long startTime = System.currentTimeMillis();
    	
	    /////////////////////////////////////////////////////////////////////// PHASE 1
	    if (Defaults.LOAD_WORKERS > 0) {
		    long triplesStart = triplesAtStart;
	    	ExecutorService executor = Executors.newFixedThreadPool(Defaults.LOAD_WORKERS);
	    	List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>(Defaults.LOAD_WORKERS);
	    	for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
	    		tasks.add(new Loader(task, Defaults.SIZE / Defaults.LOAD_WORKERS, 1, BaselineRange));
	    	}
	    	trace("Phase 1: Baseline %d triple commits.", Defaults.EVENT_SIZE);
	    	Monitor.start("phase-1");
	    	long start = startTime;
	    	invokeAndGetAll(executor, tasks);
	    	long end = System.currentTimeMillis();
	    	Monitor.stop();
	    	long triplesEnd = conn.size();
	    	long triples = triplesEnd - triplesStart;
	    	double seconds = (end - start) / 1000.0;
	    	trace("%d total triples added in %.1f seconds (%.2f triples/second, %.2f commits/second). " +
	    			"Store contains %d triples.", triples, seconds, triples/seconds,
	    			triples/Defaults.EVENT_SIZE/seconds, triplesEnd);

		    /////////////////////////////////////////////////////////////////////// PHASE 2
	    	triplesStart = triplesEnd;
		    for (int task = 0; task < (Defaults.LOAD_WORKERS); task++) {
		    	tasks.set(task, new Loader(task, Defaults.SIZE*9/10, Defaults.BULK_EVENTS, BulkRange));
		    }
	        trace("Phase 2: Grow store by about %d triples.", Defaults.LOAD_WORKERS * (Defaults.SIZE*9/10));
	        Monitor.start("phase-2");
	        start = System.currentTimeMillis();
		    invokeAndGetAll(executor, tasks);
			end = System.currentTimeMillis();
	        Monitor.stop();
			triplesEnd = conn.size();
			triples = triplesEnd - triplesStart;
			seconds = (end - start) / 1000.0;
	        trace("%d total triples bulk-loaded in %.1f seconds (%.2f triples/second, %.2f commits/second). " +
	        	"Store contains %d triples.", triples, seconds, triples/seconds,
		            triples/Defaults.BULK_EVENTS/Defaults.EVENT_SIZE/seconds, triplesEnd);

		    /////////////////////////////////////////////////////////////////////// PHASE 3
		    triplesStart = triplesEnd;
		    for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
		    	tasks.set(task, new Loader(task, Defaults.SIZE/10, 1, SmallCommitsRange));
		    }
	        trace("Phase 3: Perform %d triple commits.", Defaults.EVENT_SIZE);
	        Monitor.start("phase-3");
	        start = System.currentTimeMillis();
		    invokeAndGetAll(executor, tasks);
			end = System.currentTimeMillis();
	        Monitor.stop();
			triplesEnd = conn.size();
			triples = triplesEnd - triplesStart;
			seconds = (end - start) / 1000.0;
	        trace("%d total triples added in %.1f seconds (%.2f triples/second, %.2f commits/second). " +
	        	"Store contains %d triples.", triples, seconds, triples/seconds,
		            triples/Defaults.EVENT_SIZE/seconds, triplesEnd);
		    executor.shutdown();
	    }

	    /////////////////////////////////////////////////////////////////////// PHASE 4
	    if (Defaults.QUERY_WORKERS > 0) {
	    	ExecutorService executor = Executors.newFixedThreadPool(Defaults.QUERY_WORKERS);
	    	
	    	List<Callable<QueryResult>> queriers = new ArrayList<Callable<QueryResult>>(Defaults.QUERY_WORKERS);
	    	for (int task = 0; task < Defaults.QUERY_WORKERS; task++) {
	    		queriers.add(new Querier(task, Defaults.QUERY_TIME*60, FullDateRange));
	    	}
	    	trace("Phase 4: Perform customer/date range queries with %d processes for %d minutes.",
	    			Defaults.QUERY_WORKERS, Defaults.QUERY_TIME);
	    	Monitor.start("phase-4");
	    	int queries = 0;
	    	long triples = 0;
	    	Calendar start = GregorianCalendar.getInstance();
	    	try {
	    		List<Future<QueryResult>> fs = executor.invokeAll(queriers);
	    		for (Future<QueryResult> f : fs) {
	    			QueryResult queryResult = f.get();
	    			queries += queryResult.queries;
	    			triples += queryResult.triples;
	    		}
	    	} catch (InterruptedException e) {
	    		e.printStackTrace();
	    	} catch (ExecutionException e) {
	    		e.printStackTrace();
	    	}
	    	Calendar end = GregorianCalendar.getInstance();
	    	Monitor.stop();
	    	double seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
	    	trace("%d total triples returned over %d queries in " +
        	"%.1f seconds (%.2f triples/second, %.2f queries/second, " +
        	"%d triples/query).", triples, queries, seconds, triples/seconds,
        	queries/seconds, triples/queries);
	    	executor.shutdown();
	    }
	    
	    /////////////////////////////////////////////////////////////////////// PHASE 5
	    if (Defaults.DELETE_WORKERS > 0) {
		    long triplesStart = conn.size();
	    	ExecutorService executor = Executors.newFixedThreadPool(Defaults.DELETE_WORKERS);
	    	List<Callable<QueryResult>> tasks = new ArrayList<Callable<QueryResult>>(Defaults.DELETE_WORKERS);
	    	for (int task = 0; task < Defaults.DELETE_WORKERS; task++) {
	    		tasks.add(new Deleter(task));
	    	}
	    	trace("Phase 5: Shrink store by 1 month.");
	    	Monitor.start("phase-5");
	    	long start = System.currentTimeMillis();
	    	invokeAndGetAll(executor, tasks);
	    	long end = System.currentTimeMillis();
	    	Monitor.stop();
	    	executor.shutdown();
	    	long triplesEnd = conn.size();
	    	long triples = triplesEnd - triplesStart;
	    	double seconds = (end - start) / 1000.0;
	    	trace("%d total triples deleted in %.1f seconds (%.2f triples/second). " +
	    			"Store contains %d triples.", triples, seconds, triples/seconds, triplesEnd);
	    }
	    
	    if (Defaults.MIXED_WORKERS > 0) {
	    	trace("Phase 6: Mixed workload - adds, queries, deletes.");
	    	
	    	ExecutorService executor = Executors.newFixedThreadPool(Defaults.MIXED_WORKERS + 2);
	    	
	    	List<Callable<QueryResult>> tasks = new ArrayList<Callable<QueryResult>>(Defaults.MIXED_WORKERS + 2);
	    	for (int task = 0; task < Defaults.MIXED_WORKERS; task++) {
	    		tasks.add(new Querier(task, Defaults.QUERY_TIME*60, FullDateRange));
	    	}
    		tasks.add(new Deleter(0));
	    	Monitor.start("phase-6");
	    	int queries = 0;
	    	long triples = 0;
	    	Calendar start = GregorianCalendar.getInstance();
	    	try {
	    		List<Future<QueryResult>> fs = executor.invokeAll(tasks);
	    		for (Future<QueryResult> f : fs) {
	    			QueryResult queryResult = f.get();
	    			queries += queryResult.queries;
	    			triples += queryResult.triples;
	    		}
	    	} catch (InterruptedException e) {
	    		e.printStackTrace();
	    	} catch (ExecutionException e) {
	    		e.printStackTrace();
	    	}
	    	Calendar end = GregorianCalendar.getInstance();
	    	Monitor.stop();
	    	double seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
	    	trace("%d total triples returned over %d queries in " +
        	"%.1f seconds (%.2f triples/second, %.2f queries/second, " +
        	"%d triples/query).", triples, queries, seconds, triples/seconds,
        	queries/seconds, triples/queries);
	    	executor.shutdown();
	    }
	    
	    /////////////////////////////////////////////////////////////////////// END
    	long triplesEnd = conn.size();
        double totalSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        long triples = triplesEnd - triplesAtStart;

	    trace("Test completed in %.1f total seconds - store contains %d triples (%d triples added/removed).",
	        totalSeconds, triplesEnd, triples);
        
	    conn.close();
	    repository.shutDown();
	}

	private static <Type> void invokeAndGetAll(ExecutorService executor,
			List<Callable<Type>> tasks) {
		try {
			List<Future<Type>> fs = executor.invokeAll(tasks);
			for (Future f : fs) {
				f.get();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

}
