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

import java.io.*;
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

public class Events {

    static private final Random RANDOM = new Random();

    private static class Defaults {
	// The namespace
	static private final String NS = "http://franz.com/events#";
	
	// Number of worker processes
	static private final int LOAD_WORKERS = 25;
	static private final int QUERY_WORKERS = 16;
	
	// Time to run queries in minutes
	static private final int QUERY_TIME = 10;
	
	// Size of the Events
	static private final int EVENT_SIZE = 50;
	
	// Events per commit in bulk load phase
	static private final int BULK_EVENTS = 250;
	
	// Goal store size
	static private final int SIZE = (int) Math.pow(10, 8);
	
	// The catalog name
	static private final String CATALOG = "tests";
	
	// The repository name
	static private final String REPOSITORY = "events_test";
	
	// OPEN OR RENEW
	static private final boolean OPEN = false;
	    
	static private boolean MONITOR = true;
	    
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
	private static ThreadLocal<AGRepositoryConnection> connection =
	    new ThreadLocal<AGRepositoryConnection>();
	private static ThreadLocal<DatatypeFactory> datatypeFactory =
	    new ThreadLocal<DatatypeFactory>() {
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
    
    static private final RandomDate BaselineRange =
	new RandomDate(new GregorianCalendar(2008, Calendar.JANUARY, 1),
		       new GregorianCalendar(2008, Calendar.FEBRUARY, 1));
    static private final RandomDate BulkRange =
	new RandomDate(BaselineRange.end,
		       new GregorianCalendar(2009, Calendar.JANUARY, 1));
    static private final RandomDate SmallCommitsRange =
	new RandomDate(BulkRange.end,
		       new GregorianCalendar(2009, Calendar.FEBRUARY, 1));
    static private final RandomDate DeleteRangeOne =
	new RandomDate(BaselineRange.start,
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
	static RandomDate dateMaker;

	public Value makeValue() {
	    return CalendarToValue(dateMaker.getRandom());
	}
    }
	
    private static class RandomInteger implements RandomCallback {
	public Value makeValue() {
	    return ThreadVars.valueFactory.get().createLiteral(RANDOM.nextInt());
	}
    }

    private static class RandomName implements RandomCallback {
	static String[] firsts =
	    new String[] { "Sam", "Bruce", "Mandy", "Stacy", "Marcus", "Susan",
			   "Jason", "Chris", "Becky", "Britney", "David", "Paul", "Daniel", "James",
			   "Bradley", "Amy", "Tina", "Brandy", "Jessica", "Mary", "George",
			   "Jane" };
	static String[] lasts =
	    new String[] { "Smith", "Jones", "Flintstones", "Rubble", "Jetson",
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
	    return ThreadVars.valueFactory.get().createURI(Defaults.NS, "Customer-" +
							   RANDOM.nextInt(Defaults.SIZE/1000));
	}
    }
	
    private static class RandomAccount implements RandomCallback {
	public Value makeValue() {
	    return ThreadVars.valueFactory.get().createURI(Defaults.NS, "Account-" +
							   RANDOM.nextInt(Defaults.SIZE/1000));
	}
    }

    private static class RandomProductID implements RandomCallback {
	public Value makeValue() {
	    return ThreadVars.valueFactory.get().createURI(Defaults.NS, "ProductID-" +
							   RANDOM.nextInt(Defaults.SIZE/1000));
	}
    }

    private static class RandomPaymentTarget implements RandomCallback {
	public Value makeValue() {
	    return ThreadVars.valueFactory.get().createURI(Defaults.NS, "PaymentTarget-" +
							   RANDOM.nextInt(100));
	}
    }

    private static class RandomBusinessEntity implements RandomCallback {
	public Value makeValue() {
	    return ThreadVars.valueFactory.get().createURI(Defaults.NS, "DepositDesignation-" +
							   RANDOM.nextInt(100));
	}
    }
	
    private static class RandomDepositDesignation implements RandomCallback {
	public Value makeValue() {
	    return ThreadVars.valueFactory.get().createURI(Defaults.NS, "BusinessEntity-" +
							   RANDOM.nextInt(100));
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
	    return ThreadVars.valueFactory.get().createStatement(subject, predicate,
								 objectMaker.makeValue(),
								 context);
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
		conn = connect(false);
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
	    startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
	    endNT = NTriplesUtil.toNTriplesString(CalendarToValue(end));
			
	    // Perform the query in prolog
	    String queryString = String.format(
		"(select (?event ?pred ?obj)" +
		"(:use-planner nil)" +
		"(q- ?event !%s (? !%s !%s) !%s)" +
		"(q- ?event ?pred ?obj !%s))", timestamp, startNT, endNT, customerNT, customerNT);

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
	
    private static class Deleter implements Callable<Integer> {
	private int id;
		
	public Deleter(int theId) {
	    id = theId;
	}
			    
	public Integer call() {
	    Thread.currentThread().setName("deleter(" + id + ")");
	    AGRepositoryConnection conn;
	    try {
		conn = connect(false);
	    } catch (RepositoryException e) {
		e.printStackTrace();
		return -1;
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

	    String startNT, eodNT, queryString;
	    startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
	    eodNT = NTriplesUtil.toNTriplesString(CalendarToValue(eod));
	        
	    long events = 0, count;
	    while (start.before(range.end)) {
		queryString = String.format("(select0 (?event)" +
					    "(:limit %d) (:count-only t)" +
					    "(q- ?event !%s (? !%s !%s))" +
					    "(lisp (delete-triples :s ?event)))", limit, timestamp, startNT, eodNT);

		AGTupleQuery tupleQuery = conn.prepareTupleQuery(AGQueryLanguage.PROLOG, queryString);
		try {
		    count = tupleQuery.count();
		    events += count;

		    if (count == 0) {
			trace("Finished deleting %tF.", start);
			start = eod;
			startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
			eod = (GregorianCalendar) start.clone();
			eod.setTimeInMillis(eod.getTimeInMillis() + interval);
			eodNT = NTriplesUtil.toNTriplesString(CalendarToValue(eod));
		    }
		} catch (QueryEvaluationException e) {
		    trace("Error executing query:\n%s\n", queryString);
		    e.printStackTrace();
		}
	    }

	    trace("Found %d events (%d triples) to delete.", events, events * Defaults.EVENT_SIZE);
	        
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
	    if (Defaults.MONITOR) {
		try {
		    String[] commands = new String[]{"src/test/stress/monitor.sh", "start", phase};
		    Process p = Runtime.getRuntime().exec(commands);
		    BufferedReader input =
			new BufferedReader (new InputStreamReader(p.getInputStream()));
		    String line;
		    while ((line = input.readLine()) != null) {
			System.out.println(line);
			System.out.flush();
		    }
		    input.close();
		    trace("./monitor.sh was started.");
		} catch (IOException e) {
		    trace("./monitor.sh was not started.");
		}
	    }
	}
		
	static public void stop() {
	    if (Defaults.MONITOR) {
		try {
		    String[] commands = new String[]{"src/test/stress/monitor.sh", "end"};
		    Process p = Runtime.getRuntime().exec(commands);
		    BufferedReader input =
			new BufferedReader (new InputStreamReader(p.getInputStream()));
		    String line;
		    while ((line = input.readLine()) != null) {
			System.out.println(line);
			System.out.flush();
		    }
		    input.close();
		    trace("./monitor.sh was stopped.");
		} catch (IOException e) {
		    trace("./monitor.sh was not stopped.");
		}
	    }
	}
    }
	
    /**
     * @param args
     */
    public static void main(String[] args) throws RepositoryException {

	Thread.currentThread().setName("./events");

	if (args.length > 0) {
	    if (args.length >= 2 && args[0].equals("--seed")) {
		RANDOM.setSeed(Long.parseLong(args[1]));
		trace("Set random seed to %s.", args[1]);
	    }
	    if (args.length >= 4 && args[2].equals("--seed")) {
		RANDOM.setSeed(Long.parseLong(args[3]));
		trace("Set random seed to %s.", args[3]);
	    }
	    if (args.length >= 2 && args[0].equals("--monitor")) {
		Defaults.MONITOR = Boolean.parseBoolean(args[1]);
	    }
	    if (args.length >= 4 && args[2].equals("--monitor")) {
		Defaults.MONITOR = Boolean.parseBoolean(args[3]);
	    }
	}
		
	if (Defaults.OPEN) {
	    trace("OPENing %s:%s.", Defaults.CATALOG, Defaults.REPOSITORY);
	}
	else {
	    trace("RENEWing %s:%s.", Defaults.CATALOG, Defaults.REPOSITORY);
	}

	AGServer server = new AGServer(findServerUrl(), username(), password());
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

	executor = Executors.newFixedThreadPool(Defaults.LOAD_WORKERS);
	tasks = new ArrayList<Callable<Integer>>(Defaults.LOAD_WORKERS);
	for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
	    tasks.add(new Loader(task, Defaults.SIZE/10, 1));
	}
	trace("Phase 1 Begin: Baseline %d triple commits.", Defaults.EVENT_SIZE);
	Monitor.start("phase-1");
	startTime = start = GregorianCalendar.getInstance();
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
	end = GregorianCalendar.getInstance();
	triplesEnd = conn.size();
	triples = triplesEnd - triplesStart;
	seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
	trace("Phase 1 End: %d total triples processed in %f seconds (%f triples/second, %f commits/second). " +
	      "Store contains %d triples.", triples, seconds, triples/seconds,
	      triples/Defaults.EVENT_SIZE/seconds, triplesEnd);
	triplesStart = triplesEnd;
	Monitor.stop();

	RandomCalendar.dateMaker = BulkRange;
	for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
	    tasks.set(task, new Loader(task, Defaults.SIZE*9/10, Defaults.BULK_EVENTS));
	}
	trace("Phase 2 Begin: Grow store to about %d triples.", Defaults.SIZE);
	Monitor.start("phase-2");
	start = GregorianCalendar.getInstance();
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
	end = GregorianCalendar.getInstance();
	triplesEnd = conn.size();
	triples = triplesEnd - triplesStart;
	seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
	trace("Phase 2 End: %d total triples processed in %f seconds (%f triples/second, %f commits/second). " +
	      "Store contains %d triples.", triples, seconds, triples/seconds,
	      triples/Defaults.BULK_EVENTS/Defaults.EVENT_SIZE/seconds, triplesEnd);
	triplesStart = triplesEnd;
	Monitor.stop();

	RandomCalendar.dateMaker = SmallCommitsRange;
	for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
	    tasks.set(task, new Loader(task, Defaults.SIZE/10, 1));
	}
	trace("Phase 3 Begin: Perform %d triple commits.", Defaults.EVENT_SIZE);
	Monitor.start("phase-3");
	start = GregorianCalendar.getInstance();
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
	end = GregorianCalendar.getInstance();
	triplesEnd = conn.size();
	triples = triplesEnd - triplesStart;
	seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
	trace("Phase 3 End: %d total triples processed in %f seconds (%f triples/second, %f commits/second). " +
	      "Store contains %d triples.", triples, seconds, triples/seconds,
	      triples/Defaults.EVENT_SIZE/seconds, triplesEnd);
	triplesStart = triplesEnd;
	Monitor.stop();

	executor.shutdown();

	executor = Executors.newFixedThreadPool(Defaults.QUERY_WORKERS);
	    
	RandomCalendar.dateMaker = FullDateRange;
	List<Callable<QueryResult>> queriers =
	    new ArrayList<Callable<QueryResult>>(Defaults.QUERY_WORKERS);
	for (int task = 0; task < Defaults.QUERY_WORKERS; task++) {
	    queriers.add(new Querier(task, Defaults.QUERY_TIME*60));
	}
	trace("Phase 4 Begin: Perform customer/date range queries with %d processes for %d minutes.",
	      Defaults.QUERY_WORKERS, Defaults.QUERY_TIME);
	Monitor.start("phase-4");
	int queries = 0;
	triples = 0;
	start = GregorianCalendar.getInstance();
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
	end = GregorianCalendar.getInstance();
	seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
	trace("Phase 4 End: %d total triples returned over %d queries in " +
	      "%f seconds (%f triples/second, %f queries/second, " +
	      "%d triples/query).", triples, queries, seconds, triples/seconds,
	      queries/seconds, triples/queries);
	Monitor.stop();

	executor.shutdown();
        
	executor = Executors.newFixedThreadPool(2);
	tasks = new ArrayList<Callable<Integer>>(2);
	tasks.add(new Deleter(0));
	tasks.add(new Deleter(1));
	trace("Phase 5 Begin: Shrink store by 1 month.");
	Monitor.start("phase-5");
	start = GregorianCalendar.getInstance();
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
	end = GregorianCalendar.getInstance();
	executor.shutdown();
	triplesEnd = conn.size();
	triples = triplesEnd - triplesStart;
	seconds = (end.getTimeInMillis() - start.getTimeInMillis()) / 1000.0;
	trace("Phase 5 End: %d total triples processed in %f seconds (%f triples/second). " +
	      "Store contains %d triples.", triples, seconds, triples/seconds, triplesEnd);

	double totalSeconds = (GregorianCalendar.getInstance().getTimeInMillis()
			       - startTime.getTimeInMillis()) / 1000.0;
	triples = triplesEnd - triplesAtStart;

	Monitor.stop();

	trace("Test completed in %f total seconds - store contains %d triples (%d triples added/removed).",
	      totalSeconds, triplesEnd, triples);
        
	conn.close();
	repository.shutDown();
    }
}
