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
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;

import com.franz.agraph.repository.AGCatalog;
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
	static private final String LOCALHOST = "10.211.55.5";
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
        
        //if (!shared) {
        	// Force an auto-committing non-shared backend 
        //	conn.setAutoCommit(false);
        //	conn.setAutoCommit(true);
        //}
        
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
		    Resource graph = (Resource) new RandomCustomer().makeValue();

		    for (int i = 0; i < Defaults.EVENT_SIZE; index++, i++) {
		    	statements.set(index, event[i].makeStatement(bnode, graph));
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

	        int statusSize = 50;
	        int count = 0, errors = 0;
	        Calendar start = GregorianCalendar.getInstance();
	        Calendar end = null;
	        Vector<Statement> statements = new Vector<Statement>(triplesPerCommit);
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
				System.out.println(furture.get());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
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
				System.out.println(furture.get());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
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
				System.out.println(furture.get());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
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

	    trace("Phase 4: Perform customer/date range queries with %d processes for %d minutes.",
        	Defaults.QUERY_WORKERS, Defaults.QUERY_TIME);
        Monitor.start("phase-4");
        Monitor.stop();

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

# The program options
OPT = None

PROG = sys.argv[0]

class LoadPhase:
    start, baseline, bulk, small_commits, query, delete_one,\
        delete_two, die = range(8)
    last = delete_one

class PhaseParameters(object):
    def __init__(self, date_range, events_in_commit, triples):
        object.__init__(self)
        self.date_range = date_range
        self.events_in_commit = events_in_commit
        self.triples = triples

    @property
    def commits(self):
        return int(self.triples / (self.events_in_commit * OPT.EVENT_SIZE))

    @property
    def commits_per_worker(self):
        return int(self.commits / OPT.LOAD_WORKERS)

# The Phase Parameters
PHASE_PARAMS = None

# The work queues for loading and querying
loadq = None
queryq = None

def load_events(proc_num):
    """
    load_files does the work of the child processes.
    """
    conn = None
    
    def dequeue():
        try:
            return loadq.get()
        except Empty:
            return None

    def load_phase(phase):
        params = PHASE_PARAMS[phase]
        random_datetime.range = params.date_range
        quads = [None]*(OPT.EVENT_SIZE*params.events_in_commit)

        status_size = 50 if params.events_in_commit == 1 else 25
        start_time = time.time()

        count = 0
        errors = 0
        for commit in range(params.commits_per_worker):
            index = 0
            for event in range(params.events_in_commit):
                index = random_event(conn, quads, index)

            if commit > 0 and commit % status_size == 0:
                end_time = time.time()
                tpc = OPT.EVENT_SIZE*params.events_in_commit
                trace('loader(%d) [%s]: Loading Status - %d triples loaded so '
                    'far at %s triples per commit (%f commits/sec, %f triples/'
                    'sec over last %d commits), %d loading errors.', (
                    proc_num, datetime.now(), count, tpc,
                    status_size/(end_time - start_time),
                    tpc*status_size/(end_time - start_time),
                    status_size,
                    errors))
                start_time = end_time

            try:
                conn.mini_repository.addStatements(quads)
                count += len(quads)
            except Exception:
                trace('loader(%d) [%s]: Error adding quads...', (
                    proc_num, datetime.now()))
                errors += 1
                traceback.print_exc()
            
        trace('loader(%d) [%s]: Loading done - %d triples at %s triples '
            'per commit, %d loading errors.', (proc_num, datetime.now(),
            count, OPT.EVENT_SIZE*params.events_in_commit, errors))
        sys.stdout.flush()
        loadq.task_done()

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

    def get_phase(expected):
        phase = dequeue()

        while phase not in expected:
            # Put it back
            loadq.put(phase)
            loadq.task_done()
            time.sleep(1)
            phase = dequeue()

        return phase

    with connect().session(True, max(3600, OPT.QUERY_TIME*60)) as conn:
        if OPT.PHASE <= LoadPhase.baseline:
            phase = get_phase([LoadPhase.baseline])
            load_phase(phase)
        
        if OPT.PHASE <= LoadPhase.bulk:
            phase = get_phase([LoadPhase.bulk])
            load_phase(phase)

        if OPT.PHASE <= LoadPhase.small_commits:
            phase = get_phase([LoadPhase.small_commits])
            load_phase(phase)

        if OPT.PHASE <= LoadPhase.die:
            phase = get_phase([LoadPhase.delete_one, LoadPhase.delete_two,
                LoadPhase.die])
            if phase in [LoadPhase.delete_one, LoadPhase.delete_two]:
                delete_phase(phase)
                phase = get_phase([LoadPhase.die])

        loadq.task_done()

    conn.close()

def query_events(proc_num, resultq):
    conn = connect()

    def dequeue():
        try:
            return queryq.get_nowait()
        except Empty:
            return None

    timestamp = URI(namespace=OPT.NS, localname='EventTimeStamp').toNTriples()

    def random_query():
        # Pick a random customer
        customer = random_customer()

        # Pick a random date range
        start, end = FullDateRange.random(), FullDateRange.random()
        if start > end:
            start, end = end, start

        start_nt = Literal(start).toNTriples()
        end_nt = Literal(end).toNTriples()

        # Perform the query in prolog or sparql
        #language = QueryLanguage.PROLOG if proc_num % 2 == 1 \
        #    else QueryLanguage.SPARQL

        language = QueryLanguage.PROLOG

        if language is QueryLanguage.PROLOG:
            queryString = """
                  (select (?event ?pred ?obj)
                    (:use-planner nil)
                    (q- ?event !%s (? !%s !%s) !%s)
                    (q- ?event ?pred ?obj))""" % (
                        timestamp, start_nt, end_nt, customer)
        else:
            queryString = """
                SELECT ?event ?pred ?obj {
                 GRAPH %s {
                   ?event %s ?time .
                   FILTER (%s <= ?time && ?time <= %s)
                 }
                 {?event ?pred ?obj}
                }""" % (customer, timestamp, start_nt, end_nt)

        try:
            # Actually pull the full results to the client, then just count them
            count = len(conn.prepareTupleQuery(language, queryString
                ).evaluate())
        except Exception:
            # During huge bulk deletions, some queries may be invalidated
            # and a error returned to indicate they should be rerun. Keep
            # track of it if this happens.
            trace('query(%d) [%s]: Error executing query:\n%s\n', (
                proc_num, datetime.now(), queryString))
            traceback.print_exc()
            count = -1

        return count

    status_size = 10
    start_time = the_time = time.time()
    sub_count = 0
    queries = 0
    count = 0
    restarts = 0

    # Run the query at least once
    stop = None
    while stop is None:
        result = random_query()
        if result >= 0:
            count += result
        else:
            restarts += 1

        queries += 1
        stop = dequeue()

        if queries % status_size == 0:
            end_time = time.time()
            sub_count = count - sub_count
            trace('query(%d) [%s]: Querying status - %d triple results '
                'returned for %d queries in %f seconds (%f queries/second, '
                '%f triples per query), %d queries aborted.', (proc_num,
                datetime.now(), sub_count, status_size,
                end_time-start_time, status_size/(end_time-start_time),
                sub_count/status_size, restarts))
            start_time = end_time
            sub_count = count
            
    the_time = time.time() - the_time
    trace('query(%d) [%s]: Querying done - %d triple results returned for %d '
        'queries in %f seconds (%f queries/second, %f triples per query), '
        ' %d queries aborted.', (proc_num, datetime.now(), count, queries,
        the_time, queries/the_time, count/queries, restarts))

    conn.close()
    resultq.put((queries, count, the_time))
    queryq.task_done()


@contextmanager
def monitor(phase):
    """
    Start and end the monitor for a phase.
    """
    try:
        subprocess.call(['./monitor.sh', 'start', phase])
    except OSError:
        pass
    yield
    try:
        subprocess.call(['./monitor.sh', 'end'])
    except OSError:
        pass

def main():
    """
    The parent main process.
    """
    global loadq, PHASE_PARAMS

    # Reduce the number of times we need to round-trip to the server
    # for blank nodes
    ValueFactory.BLANK_NODE_AMOUNT = OPT.BULK_EVENTS * 4

    # Initialize the Phase Parameters
    PHASE_PARAMS = [
        None,
        PhaseParameters(BaselineRange, 1, OPT.SIZE/10),
        PhaseParameters(BulkRange, OPT.BULK_EVENTS, (OPT.SIZE*9)/10),
        PhaseParameters(SmallCommitsRange, 1, OPT.SIZE/10),
        None,
        PhaseParameters(DeleteRangeOne, OPT.BULK_EVENTS, OPT.SIZE/10),
        PhaseParameters(DeleteRangeTwo, OPT.BULK_EVENTS, OPT.SIZE/10)]

    # Renew/Open the repository
    trace('%s [%s]: %s %s:%s.', (PROG, datetime.now(),
        "Opening" if OPT.OPEN else "Renewing", OPT.CATALOG, OPT.REPOSITORY))
    conn = connect(Repository.OPEN if OPT.OPEN else Repository.RENEW)
    triples = conn.size()

    trace('%s [%s]: Testing with %d loading, %d querying processes. '
        'Repository contains %d triples.', (
        PROG, datetime.now(), OPT.LOAD_WORKERS, OPT.QUERY_WORKERS, triples))
    
    # Create the work queue
    loadq = JoinableQueue(OPT.LOAD_WORKERS)

    # Start the loading processes
    for proc_num in range(OPT.LOAD_WORKERS):
        p = Process(target=load_events, args=(proc_num,))
        p.start()

    def load_phase(phase):
        params = PHASE_PARAMS[phase]
        triples_start = conn.size()
        phase_time = time.time()

        # Tell the processes what to do (We only need one deletion process)
        if phase != LoadPhase.delete_one:
            for proc_num in range(OPT.LOAD_WORKERS):
                loadq.put(phase)
        else:
            loadq.put(LoadPhase.delete_one)
            loadq.put(LoadPhase.delete_two)

        if phase == LoadPhase.last:
            for proc_num in range(OPT.LOAD_WORKERS):
                loadq.put(LoadPhase.die)

            # Signal that there is no more work for the queue
            loadq.close()

        # Wait for all the work to be completed
        loadq.join()

        triples_end = conn.size()
        triples = triples_end - triples_start
        phase_time = time.time() - phase_time
        commits = abs(triples/(params.events_in_commit*OPT.EVENT_SIZE))
        trace('%s [%s]: %d total triples processed in %f seconds '
            '(%f triples/second, %f commits/second). '
            'Store contains %d triples.', (
            PROG, datetime.now(), triples, phase_time, triples/phase_time,
            commits/phase_time, triples_end))

    @contextmanager
    def run_queries():
        global queryq
        queryq = JoinableQueue(OPT.QUERY_WORKERS)
        resultq = Queue(OPT.QUERY_WORKERS)

        # Start the query processes
        for proc_num in range(OPT.QUERY_WORKERS):
            p = Process(target=query_events, args=(proc_num, resultq))
            p.start()

        yield

        for proc_num in range(OPT.QUERY_WORKERS):
            queryq.put('Stop')

        # Signal that there is no more work for the queue
        queryq.close()
        queryq.join()

        queries = 0
        triples = 0
        phase_time = 0

        for proc_num in range(OPT.QUERY_WORKERS):
            result = resultq.get()
            queries += result[0]
            triples += result[1]
            phase_time = max(phase_time, result[2])

        trace('%s [%s]: %d total triples returned over %d queries in '
            '%f seconds (%f triples/second, %f queries/second, '
            '%f triples/query). ', (PROG, datetime.now(), triples, queries,
            phase_time, triples/phase_time, queries/phase_time,
            triples/queries))

    total_time = time.time()
    if OPT.PHASE <= LoadPhase.baseline:
        with monitor('phase-1'):
            trace('%s [%s]: Phase 1: Baseline %d triple commits.', (
                PROG, datetime.now(), OPT.EVENT_SIZE))
            load_phase(LoadPhase.baseline)

    if OPT.PHASE <= LoadPhase.bulk:
        with monitor('phase-2'):
            trace('%s [%s]: Phase 2: Grow store to about %d triples.', (
                PROG, datetime.now(), OPT.SIZE))
            load_phase(LoadPhase.bulk)

    if OPT.PHASE <= LoadPhase.small_commits:
        with monitor('phase-3'):
            trace('%s [%s]: Phase 3: Perform %d triple commits.',
                (PROG, datetime.now(), OPT.EVENT_SIZE))
            load_phase(LoadPhase.small_commits)

    if OPT.PHASE <= LoadPhase.query:
        with monitor('phase-4'):
            trace('%s [%s]: Phase 4: Perform customer/date range queries '
                'with %d processes for %d minutes.', (PROG, datetime.now(),
                OPT.QUERY_WORKERS, OPT.QUERY_TIME))
            with run_queries():
                time.sleep(OPT.QUERY_TIME*60)

    if OPT.PHASE <= LoadPhase.delete_one:
        with monitor('phase-5'):
            trace('%s [%s]: Phase 5: Shrink store by 1 month.', (
                PROG, datetime.now()))
            load_phase(LoadPhase.delete_one)
    
    # Display the results
    total_time = time.time() - total_time
    triples_end = conn.size()
    triples = triples_end - triples
    conn.close()

    trace('%s [%s]: Test completed in %f total seconds - '
        'store contains %d triples (%d triples added/removed).',
        (PROG, datetime.now(), total_time, triples_end, triples))
**/