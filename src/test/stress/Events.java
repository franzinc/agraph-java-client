package test.stress;

import java.util.*;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.ntriples.NTriplesUtil;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import java.io.IOException;
import java.lang.ThreadLocal;

public class Events {
	private static String with_default(String theValue, String theDefault) {
		if (theValue == null) {
			return theDefault;
		}
		
		return theValue;
	}
	
	static private final Random RANDOM = new Random();
	static private final String LOCALHOST = "localhost";
	static private final String AG_HOST = with_default(System.getenv("AGRAPH_HOST"), LOCALHOST);
	static private final int AG_PORT = Integer.parseInt(with_default(System.getenv("AGRAPH_PORT"), "10035"));
	static private final String AG_USER = with_default(System.getenv("AGRAPH_USER"), "test");
	static private final String AG_PASSWORD = with_default(System.getenv("AGRAPH_PASSWORD"), "xyzzy");

	private static class Defaults {
	    // The namespace
	    static private final String NS = "http://franz.com/events#";
	
	    // Number of worker processes
	    static private final int LOAD_WORKERS = 10;
	    static private final int QUERY_WORKERS = 10;
	
	    // Time to run queries in minutes
	    static private final int QUERY_TIME = 1;
	
	    // Size of the Events
	    static private final int EVENT_SIZE = 50;
	
	    // Events per commit in bulk load phase
	    static private final int BULK_EVENTS = 200;
	
	    // Goal store size
	    static private final int SIZE = (int) Math.pow(10, 6);
	
	    // The catalog name
	    static private final String CATALOG = "tests";
	
	    // The repository name
	    static private final String REPOSITORY = "events_test";
	
	    // OPEN OR RENEW
	    static private final boolean OPEN = false;
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
        AGServer server = new AGServer("http://" + AG_HOST + ":" + AG_PORT, AG_USER, AG_PASSWORD);
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
    
    static RandomDate BaselineRange = new RandomDate(new GregorianCalendar(2008, Calendar.JANUARY, 1),
    		new GregorianCalendar(2008, Calendar.FEBRUARY, 1));
    static RandomDate BulkRange = new RandomDate(BaselineRange.end,
    		new GregorianCalendar(2009, Calendar.JANUARY, 1));
    static RandomDate SmallCommitsRange = new RandomDate(BulkRange.end,
			new GregorianCalendar(2009, Calendar.FEBRUARY, 1));
    static RandomDate DeleteRangeOne = new RandomDate(BaselineRange.start,
			new GregorianCalendar(2009, Calendar.JANUARY, 16));
    static RandomDate DeleteRangeTwo = new RandomDate(DeleteRangeOne.end,
	        BaselineRange.end);
    static RandomDate FullDateRange = new RandomDate(BaselineRange.start,
			SmallCommitsRange.end);

	private static interface RandomCallback {
		public Value makeValue();
	}

	private static class RandomCalendar implements RandomCallback {
		static RandomDate dateMaker;

		public Value makeValue() {
			return ThreadVars.valueFactory.get().createLiteral(ThreadVars.datatypeFactory.get().newXMLGregorianCalendar(dateMaker.getRandom()));
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
		
		public Loader(int theId, int theTripleGoal, int theEventsPerCommit) {
			id = theId;
			triplesPerCommit = theEventsPerCommit * Defaults.EVENT_SIZE;
			loopCount = theTripleGoal / triplesPerCommit / Defaults.LOAD_WORKERS;
			eventsPerCommit = theEventsPerCommit;
		}
			    
		public Integer call() {
			Thread.currentThread().setName("loader(" + id + ")");
			AGRepositoryConnection conn;
			try {
				conn = connect(true);
			} catch (RepositoryException e) {
				e.printStackTrace();
				return -1;
			}
	        ThreadVars.connection.set(conn);
	        ThreadVars.valueFactory.set(conn.getValueFactory());

	        int statusSize = 50, count = 0, errors = 0;
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
						"far at %d triples per commit (%f commits/sec, %f triples/" +
						"sec over last %d commits), %d loading errors.", count,
						triplesPerCommit, statusSize/seconds,
						statusSize*triplesPerCommit/seconds, statusSize, errors);
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
				"per commit, %d loading errors.", count, triplesPerCommit, errors);

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
		
		public Querier(int theId, int theSecondsToRun) {
			id = theId;
			secondsToRun = theSecondsToRun;
		}
		
		private int randomQuery(AGRepositoryConnection conn, ValueFactory vf) {
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
			startNT = NTriplesUtil.toNTriplesString(vf.createLiteral(ThreadVars.datatypeFactory.get().newXMLGregorianCalendar(start)));
			endNT = NTriplesUtil.toNTriplesString(vf.createLiteral(ThreadVars.datatypeFactory.get().newXMLGregorianCalendar(end)));
			
			// Perform the query in prolog
			String queryString = String.format(
					"(select (?event ?pred ?obj)" +
					"(:use-planner nil)" +
					"(q- ?event !%s (? !%s !%s) !%s)" +
					"(q- ?event ?pred ?obj))", timestamp, startNT, endNT, customerNT);

			int count = 0;

			// Actually pull the full results to the client, then just count them
		    TupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
	        TupleQueryResult result;
			try {
				result = tupleQuery.evaluate();
				while (result.hasNext()) {
		            result.next();
		            count++;
		        }
		        result.close();
			} catch (QueryEvaluationException e) {
	            trace("Error executing query:\n%s\n", queryString);
				e.printStackTrace();
	            count = -1;
			}

			return count;
		}		
			    
		public QueryResult call() {
			Thread.currentThread().setName("query(" + id + ")");
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

	        int statusSize = 10, count = 0, subcount = 0, queries = 0, restarts = 0;
	        Calendar startTime, start, end;
	        startTime = start= GregorianCalendar.getInstance();

	        while (true) {
	        	// Do the query
	        	int result = randomQuery(conn, vf);
	        	if (result < 0) {
	        		restarts++;
	        	} else {
	        		queries++;
	        		count += result;
	        	}

	        	if (queries % statusSize == 0) {
		            end = GregorianCalendar.getInstance();
	        		double seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
		            subcount = count - subcount;
		            trace("Querying status - %d triple results returned for %d queries in %f seconds (%f queries/second, " +
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
	
	public static class Monitor {
		static public void start(String phase) {
			try {
			    // Execute a command with an argument that contains a space
			    String[] commands = new String[]{"./monitor.sh", "start", phase};
			    Process child = Runtime.getRuntime().exec(commands);
			} catch (IOException e) {
				trace("./monitor.sh was not started.");
			}
		}
		
		static public void stop() {
			try {
			    // Execute a command with an argument that contains a space
			    String[] commands = new String[]{"./monitor.sh", "end"};
			    Process child = Runtime.getRuntime().exec(commands);
			} catch (IOException e) {
				trace("./monitor.sh was not stopped.");
			}
		}
	}
	
    /**
	 * @param args
	 */
	public static void main(String[] args) throws RepositoryException {
		Thread.currentThread().setName("./events");
		
        if (Defaults.OPEN) {
    	    trace("OPENing %s:%s.", Defaults.CATALOG, Defaults.REPOSITORY);
        }
        else {
       	    trace("RENEWing %s:%s.", Defaults.CATALOG, Defaults.REPOSITORY);
       	}

	    AGServer server = new AGServer("http://" + AG_HOST + ":" + AG_PORT, AG_USER, AG_PASSWORD);
        AGCatalog catalog = server.getCatalog(Defaults.CATALOG);
        if (!Defaults.OPEN) {
        	catalog.deleteRepository(Defaults.REPOSITORY);
        }
        AGRepository repository = catalog.createRepository(Defaults.REPOSITORY);
        repository.initialize();
        AGRepositoryConnection conn = repository.getConnection();
        ThreadVars.connection.set(conn);
        ThreadVars.valueFactory.set(conn.getValueFactory());

        RandomCalendar.dateMaker = BaselineRange;
	    AllEvents.initialize();

	    Calendar start, end, startTime;
	    long triplesAtStart, triplesStart, triplesEnd, triples;
		double seconds;
	    ExecutorService executor;
	    List<Callable<Integer>> tasks;
	    
	    triplesAtStart = triplesStart = conn.size();

	    trace("Testing with %d loading, %d querying processes. Repository contains %d triples.", 
            Defaults.LOAD_WORKERS, Defaults.QUERY_WORKERS, triplesStart);

        startTime = start = GregorianCalendar.getInstance();
	    executor = Executors.newFixedThreadPool(Defaults.LOAD_WORKERS);
	    tasks = new ArrayList<Callable<Integer>>(Defaults.LOAD_WORKERS);
	    for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
	    	tasks.add(new Loader(task, Defaults.SIZE/10, 1));
	    }
        trace("Phase 1: Baseline %d triple commits.", Defaults.EVENT_SIZE);
        Monitor.start("phase-1");
	    try {
			List<Future<Integer>> futures = executor.invokeAll(tasks);
			
			for (Future<Integer> furture : futures) {
				furture.get();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        Monitor.stop();
		end = GregorianCalendar.getInstance();
		triplesEnd = conn.size();
		triples = triplesEnd - triplesStart;
		seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
        trace("%d total triples processed in %f seconds (%f triples/second, %f commits/second). " +
        	"Store contains %d triples.", triples, seconds, triples/seconds,
	            triples/Defaults.EVENT_SIZE/seconds, triplesEnd);
	    triplesStart = triplesEnd;

	    RandomCalendar.dateMaker = BulkRange;
        start = GregorianCalendar.getInstance();
	    for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
	    	tasks.set(task, new Loader(task, Defaults.SIZE*9/10, Defaults.BULK_EVENTS));
	    }
        trace("Phase 2: Grow store to about %d triples.", Defaults.SIZE);
        Monitor.start("phase-2");
	    try {
			List<Future<Integer>> futures = executor.invokeAll(tasks);

			for (Future<Integer> furture : futures) {
				furture.get();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        Monitor.stop();
		end = GregorianCalendar.getInstance();
		triplesEnd = conn.size();
		triples = triplesEnd - triplesStart;
		seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
        trace("%d total triples processed in %f seconds (%f triples/second, %f commits/second). " +
        	"Store contains %d triples.", triples, seconds, triples/seconds,
	            triples/Defaults.EVENT_SIZE/seconds, triplesEnd);
	    triplesStart = triplesEnd;

	    RandomCalendar.dateMaker = SmallCommitsRange;
        start = GregorianCalendar.getInstance();
	    for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
	    	tasks.set(task, new Loader(task, Defaults.SIZE/10, 1));
	    }
        trace("Phase 3: Perform %d triple commits.", Defaults.EVENT_SIZE);
        Monitor.start("phase-3");
	    try {
			List<Future<Integer>> futures = executor.invokeAll(tasks);

			for (Future<Integer> furture : futures) {
				furture.get();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        Monitor.stop();
		end = GregorianCalendar.getInstance();
		triplesEnd = conn.size();
		triples = triplesEnd - triplesStart;
		seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
        trace("%d total triples processed in %f seconds (%f triples/second, %f commits/second). " +
        	"Store contains %d triples.", triples, seconds, triples/seconds,
	            triples/Defaults.EVENT_SIZE/seconds, triplesEnd);
	    triplesStart = triplesEnd;

	    executor.shutdown();

	    executor = Executors.newFixedThreadPool(Defaults.QUERY_WORKERS);
	    
	    RandomCalendar.dateMaker = FullDateRange;
        start = GregorianCalendar.getInstance();
	    List<Callable<QueryResult>> queriers = new ArrayList<Callable<QueryResult>>(Defaults.QUERY_WORKERS);
	    for (int task = 0; task < Defaults.QUERY_WORKERS; task++) {
	    	queriers.add(new Querier(task, Defaults.QUERY_TIME*60));
	    }
	    trace("Phase 4: Perform customer/date range queries with %d processes for %d minutes.",
        	Defaults.QUERY_WORKERS, Defaults.QUERY_TIME);
        Monitor.start("phase-4");
        int queries = 0;
        triples = 0;
        try {
			List<Future<QueryResult>> futures = executor.invokeAll(queriers);

			for (Future<QueryResult> furture : futures) {
				QueryResult queryResult = furture.get();
				queries += queryResult.queries;
				triples += queryResult.triples;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        Monitor.stop();
		end = GregorianCalendar.getInstance();
		seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
        trace("%d total triples returned over %d queries in " +
        	"%f seconds (%f triples/second, %f queries/second, " +
        	"%d triples/query).", triples, queries, seconds, triples/seconds,
        	queries/seconds, triples/queries);
        
        trace("Phase 5: Shrink store by 1 month.");
        Monitor.start("phase-5");
        Monitor.stop();

        double totalSeconds = (GregorianCalendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis()) / 1000.0;
        triples = triplesEnd - triplesAtStart;

	    trace("Test completed in %f total seconds - store contains %d triples (%d triples added/removed).",
	        totalSeconds, triplesEnd, triples);
        
	    conn.close();
	    repository.shutDown();
	}
}

/**
    def delete_phase(phase):
        params = PHASE_PARAMS[phase]
        timestamp = URI(namespace=OPT.NS, localname='EventTimeStamp'
            ).toNTriples()
        limit = 9*(10**5)/OPT.EVENT_SIZE

        interval = timedelta(seconds=24*60*60)
        start, end = params.date_range.start, params.date_range.end
        eod = start + interval
        start_nt = Literal(start).toNTriples()
        eod_nt = Literal(eod).toNTriples()

        events = 0
        while start < end:
            queryString = """
                (select0 (?event)
                  (:limit %d)
                  (:count-only t)
                  (q- ?event !%s (? !%s !%s))
                  (lisp (delete-triples :s ?event)))""" % (
                      limit, timestamp, start_nt, eod_nt)

            try:
                count = conn.prepareTupleQuery(QueryLanguage.PROLOG, queryString
                    ).evaluate(count=True)

                assert count <= limit
                events += count

                if count == 0:
                    trace('loader(%d) [%s]: Finished deleting %s.', (
                        proc_num, datetime.now(), start.date()))
                    start = eod
                    start_nt = Literal(start).toNTriples()
                    eod = start + interval
                    eod_nt = Literal(eod).toNTriples()
            except Exception:
                trace('loader(%d) [%s]: Error deleting triples...\n%s', (
                    proc_num, datetime.now(), queryString))
                traceback.print_exc()

        trace('loader(%d) [%s]: Found %d events (%d triples) to delete.', (
            proc_num, datetime.now(), events, events * OPT.EVENT_SIZE))
        
        loadq.task_done()
**/