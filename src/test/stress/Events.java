/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test.stress;

import static test.AGAbstractTest.findServerUrl;
import static test.AGAbstractTest.password;
import static test.AGAbstractTest.username;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.ntriples.NTriplesUtil;

import test.Closer;

import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGQueryLanguage;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGStreamTupleQuery;
import com.franz.agraph.repository.AGTupleQuery;
import com.franz.util.Closeable;
import com.franz.util.Util;

public class Events extends Closer {
    
    static private final test.Util.RandomLong RANDOM = new test.Util.RandomLong();
    
    long errors = 0;

    static class Defaults {
        
        private static CommandLine cmd;
        
        // The namespace
        static private final String NS = "http://franz.com/events#";
        
        // Number of worker processes
        static int LOAD_WORKERS = 20;
        static int QUERY_WORKERS = 8;
        static int DELETE_WORKERS = 2;
        static int MIXED_RUNS = 0;
        
        // Time to run queries in minutes
        static private int QUERY_TIME = 10;
        
        // Size of the Events
        static private int EVENT_SIZE = 50;
        
        // Events per commit in bulk load phase
        static private int BULK_EVENTS = 250;
        
        // Goal store size
        static long SIZE = (long) Math.pow(10, 9);
        
        // The catalog name
        static String CATALOG = "tests";
        
        // The repository name
        static String REPOSITORY = "events_test";
        
        static int PHASE = 1;
        
        static int STATUS = 50;
        
        static int VERBOSE = 0;
        
        static String URL = findServerUrl();
        
        static String USERNAME = username();
        
        static String PASSWORD = password();
        
        static boolean MONITOR = true;
        
        public enum LOGT {
            ALL, ELAPSED, NOTIME;
        }
        
        static LOGT LOG = LOGT.ALL;
        
        public enum STREAM {
            NONE, PULL, HAND, PULH;
        }
        
        static STREAM stream = STREAM.NONE;
        
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
        
        static boolean cmdVal(String opt, boolean defaultVal) {
            String val = cmd.getOptionValue(opt);
            if (val == null) {
                return defaultVal;
            } else {
                return Boolean.parseBoolean(val);
            }
        }
        
        @SuppressWarnings("static-access")
        static <EnumType extends Enum> EnumType cmdVal(String opt, EnumType defaultVal) {
            String val = cmd.getOptionValue(opt);
            if (val == null) {
                return defaultVal;
            } else {
                return (EnumType) defaultVal.valueOf(defaultVal.getClass(), val);
            }
        }
        
        @SuppressWarnings("static-access")
        public static void init(String[] args) throws Exception {
            Options options = new Options();
            options.addOption(new Option("h", "help", false, "print this message"));
            options.addOption(OptionBuilder.withLongOpt("sparql")
                    .withDescription("Use only sparql, no prolog [default is prolog]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("open")
                    .withDescription("OPEN the repository and add to it [default is to SUPERSEDE]")
                    .create("o"));
            options.addOption(OptionBuilder.withLongOpt("status")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("use STATUS size [default=" + STATUS + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("load")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("use number of loading processes [default=" + LOAD_WORKERS + "]")
                    .create("l"));
            options.addOption(OptionBuilder.withLongOpt("query")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("use number of querying processes [default=" + QUERY_WORKERS + "]")
                    .create("q"));
            options.addOption(OptionBuilder.withLongOpt("delete")
                    .withArgName("0-2").hasArg()
                    .withDescription("use 0-2 deleting processes [default=" + DELETE_WORKERS + "]")
                    .create("d"));
            options.addOption(OptionBuilder.withLongOpt("size")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("triple limit for bulk load (e.g. 10,000, 100m, 2b, 1.5t) (minimum=1000) [default=" + SIZE + "]")
                    .create("s"));
            options.addOption(OptionBuilder.withLongOpt("time")
                    .withArgName("MINUTES").hasArg()
                    .withDescription("time limit for query phase [default=" + QUERY_TIME + "]")
                    .create("t"));
            options.addOption(OptionBuilder.withLongOpt("event")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("each event will contain number of triples [default=" + EVENT_SIZE + "]")
                    .create("e"));
            options.addOption(OptionBuilder.withLongOpt("bulk")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("commit number of events per commit during bulk load [default=" + BULK_EVENTS + "]")
                    .create("b"));
            options.addOption(OptionBuilder.withLongOpt("mixed")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("Add MIXED workload phases after the normal run (-1, runs until java process is killed) [default=" + MIXED_RUNS + "]")
                    .create("m"));
            options.addOption(OptionBuilder.withLongOpt("verbose")
                    .withArgName("INTEGER").hasOptionalArg()
                    .withDescription("Verbosity level for extra log messages if VERBOSE > 0. [default=" + VERBOSE +", no value=1]")
                    .create("v"));
            options.addOption(OptionBuilder.withLongOpt("catalog")
                    .withArgName("NAME").hasArg()
                    .withDescription("AGraph catalog [default=" + CATALOG + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("repository")
                    .withArgName("NAME").hasArg()
                    .withDescription("AGraph repository [default=" + REPOSITORY + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("url")
                    .withArgName("URL").hasArg()
                    .withDescription("AGraph URL [default=" + URL +
                            " from env var AGRAPH_HOST, java property AGRAPH_HOST, or \"localhost\";" +
                    " and env var AGRAPH_PORT, java property AGRAPH_PORT, ../agraph/lisp/agraph.port, or 10035")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("username")
                    .withArgName("NAME").hasArg()
                    .withDescription("AGraph username [default=" + USERNAME +
                    " from env var AGRAPH_USER, or \"test\"]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("password")
                    .withArgName("PASSWORD").hasArg()
                    .withDescription("AGraph password [default=" + PASSWORD +
                    " from env var AGRAPH_PASSWORD, or \"xyzzy\"]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("seed")
                    .withArgName("INTEGER").hasArg()
                    .withDescription("seed to the random number generator so that one can do repeated runs with" +
                    " the exact same data generated. Default: random.")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("phase")
                    .withArgName("1-6").hasArg()
                    .withDescription("Run the test starting at phase 1-6 [default=" + PHASE + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("monitor")
                    .withArgName("true|false").hasArg()
                    .withDescription("Try to start monitor.sh or not. [default=" + MONITOR + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("log")
                    .withArgName("LOG").hasArg()
                    .withDescription("One of: " + Arrays.asList(LOGT.values()) + ". [default=" + LOG + "]")
                    .create());
            options.addOption(OptionBuilder.withLongOpt("stream")
                    .withArgName("STREAM").hasArg()
                    .withDescription("Stream results. One of: " + Arrays.asList(STREAM.values()) + ". [default=" + stream + "]")
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
                        "# Runs default options: supersedes repo, runs test.\n" +
                        "./events.sh --open --status 500 -v 1 -l 1 -d 0 -q 1 -m 1 --size 100000 --sparql\n" +
                        "# Runs less verbose status, single-threaded, adding to an existing repo, no deletes, sparql queries.\n" +
                        "./events.sh -l 10 -q 2 -m 2 --size 1m\n" +
                        "# Runs with a superseded repo, prolog queries, multi-threaded.\n",
                        false);
                System.exit(0);
            }
            
            LOAD_WORKERS = cmdVal("load", LOAD_WORKERS);
            QUERY_WORKERS = cmdVal("query", QUERY_WORKERS);
            DELETE_WORKERS = cmdVal("delete", DELETE_WORKERS);
            MIXED_RUNS = cmdVal("mixed", MIXED_RUNS);
            STATUS = cmdVal("status", STATUS);
            SIZE = Math.max(1000, test.Util.fromHumanInt( cmdVal("size", "" + SIZE) ));
            QUERY_TIME = cmdVal("time", QUERY_TIME);
            EVENT_SIZE = cmdVal("event", EVENT_SIZE);
            BULK_EVENTS = cmdVal("bulk", BULK_EVENTS);
            VERBOSE = cmdVal("verbose", VERBOSE, 1);
            CATALOG = cmdVal("catalog", CATALOG);
            REPOSITORY = cmdVal("repository", REPOSITORY);
            URL = cmdVal("url", URL);
            USERNAME = cmdVal("username", USERNAME);
            PASSWORD = cmdVal("password", PASSWORD);
            PHASE = cmdVal("phase", PHASE);
            MONITOR = cmdVal("monitor", MONITOR);
            LOG = cmdVal("log", LOG);
            stream = cmdVal("stream", stream);
            
            if (cmd.hasOption("seed")) {
                long seed = Long.parseLong(cmd.getOptionValue("seed"));
                RANDOM.setSeed(seed);
                trace("Set random seed to %s.", seed);
            }
            trace("Parameters:"
            		+ " catalog=" + CATALOG
            		+ " repository=" + REPOSITORY
            		+ " url=" + URL
            		+ " size=" + SIZE
            		+ " stream=" + stream);
        }
        
        public static boolean hasOption(String opt) {
            return cmd.hasOption(opt);
        }

    }
    
    static double logtime(double time) {
        return Defaults.LOG == Defaults.LOGT.NOTIME ? 0 : time;
    }
    
    static final long START = System.currentTimeMillis();
    
    public static void trace(String format, Object... values) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        if (Defaults.LOG == Defaults.LOGT.ALL) {
            formatter.format("%s [%2$tF %2$tT.%2$tL]: ", Thread.currentThread().getName(), GregorianCalendar.getInstance());
        } else if (Defaults.LOG == Defaults.LOGT.ELAPSED) {
            formatter.format("%s [%d]: ", Thread.currentThread().getName(), (System.currentTimeMillis() - START));
        } else if (Defaults.LOG == Defaults.LOGT.NOTIME) {
            formatter.format("%s: ", Thread.currentThread().getName());
        }
        formatter.format(format, values);
        System.out.println(sb.toString());
    }

    public AGRepositoryConnection connect() throws RepositoryException {
        AGServer server = closeLater( new AGServer(findServerUrl(), username(), password()) );
        AGCatalog cat = server.getCatalog(Defaults.CATALOG);
        AGRepository repo = closeLater( cat.createRepository(Defaults.REPOSITORY) );
        repo.initialize();
        AGRepositoryConnection conn = closeLater( repo.getConnection() );
        // Force an auto-committing non-shared backend 
        conn.setAutoCommit(false);
        conn.setAutoCommit(true);
        trace("Dedicated backend: " + conn.getHttpRepoClient().getRoot());
        return conn;
    }

    private static class ThreadVars {
        private static ThreadLocal<ValueFactory> valueFactory = new ThreadLocal<ValueFactory>();
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
        
        public RandomDate next(int field, int val) {
            GregorianCalendar c = (GregorianCalendar) end.clone();
            c.add(Calendar.DAY_OF_YEAR, 30);
            return new RandomDate(end, c);
        }
        
        @Override
        public String toString() {
            DateFormat f = SimpleDateFormat.getDateInstance();
            return "[" + f.format(start.getTime()) + " - " + f.format(end.getTime()) + "]";
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
            return ThreadVars.valueFactory.get().createURI(Defaults.NS, "Customer-" + RANDOM.nextLong(Defaults.SIZE/1000));
        }
    }
    
    private static class RandomAccount implements RandomCallback {
        public Value makeValue() {
            return ThreadVars.valueFactory.get().createURI(Defaults.NS, "Account-" + RANDOM.nextLong(Defaults.SIZE/1000));
        }
    }
    
    private static class RandomProductID implements RandomCallback {
        public Value makeValue() {
            return ThreadVars.valueFactory.get().createURI(Defaults.NS, "ProductID-" + RANDOM.nextLong(Defaults.SIZE/1000));
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
    
    class Loader implements Callable<Object>, Closeable {
        private int id;
        private long loopCount;
        private int eventsPerCommit;
        private int triplesPerCommit;
        private final RandomDate dateMaker;
        private AGRepositoryConnection conn;
        
        public Loader(int theId, long theTripleGoal, int theEventsPerCommit, RandomDate dateMaker) throws Exception {
            id = theId;
            this.dateMaker = dateMaker;
            triplesPerCommit = theEventsPerCommit * Defaults.EVENT_SIZE;
            loopCount = theTripleGoal / triplesPerCommit / Defaults.LOAD_WORKERS;
            eventsPerCommit = theEventsPerCommit;
            conn = connect();
        }
        
        public Integer call() {
            Thread.currentThread().setName("loader(" + id + ")");
            ThreadVars.dateMaker.set(dateMaker);
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
                            triplesPerCommit, logtime(statusSize/seconds),
                            logtime(statusSize*triplesPerCommit/seconds),
                            statusSize, errors);
                    start = end;
                }
                
                try {
                    conn.add(statements);
                    count += triplesPerCommit;
                } catch (Exception e) {
        			errors++;
                    trace("Error adding statements...");
                    e.printStackTrace();
                }
            }
            
            trace("Loading Done - %d triples at %d triples " +
                    "per commit, %d errors.", count, triplesPerCommit, errors);
            
            return 0;
        }

		@Override
		public void close() {
			Events.this.close(conn);
		}
    }
    
    private static class QueryResult {
        public long queries;
        public long triples;
        
        public QueryResult(long theQueries, long theTriples) {
            queries = theQueries;
            triples = theTriples;
        }
    }
    
    class Querier implements Callable<Object>, Closeable {
        private int secondsToRun;
        private int id;
        private String timestamp;
        private final RandomDate dateMaker;
        AGRepositoryConnection conn;
        
        public Querier(int theId, int theSecondsToRun, RandomDate dateMaker) throws Exception {
            id = theId;
            secondsToRun = theSecondsToRun;
            this.dateMaker = dateMaker;
            conn = connect();
        }
        
        private long randomQuery(AGRepositoryConnection conn, ValueFactory vf, boolean trace) {
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
            AGTupleQuery tupleQuery;
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
            tupleQuery = streamQuery(tupleQuery);
            
            // Actually pull the full results to the client, then just count them
            TupleQueryResult result = null;
            long count = 0;
            try {
                if (Defaults.VERBOSE > 0 && trace) {
                    trace("query: %s", queryString);
                }
                if (Defaults.stream == Defaults.STREAM.HAND || Defaults.stream == Defaults.STREAM.PULH) {
                	CountingHandler handler = new CountingHandler();
                    tupleQuery.evaluate(handler);
                    count = handler.count;
                } else {
                    result = tupleQuery.evaluate();
                    count = count(result);
                }
                // test sparql and prolog return same results:
                //				Set<Stmt> stmts = Stmt.statementSet(result);
                //				count = stmts.size();
                //				AGAbstractTest.assertSetsEqual(queryString, stmts,
                //						Stmt.statementSet(tupleQuery2.evaluate()));
            } catch (Exception e) {
    			errors++;
                trace("Error executing query:\n%s\n", queryString);
                e.printStackTrace();
                count = -1;
            } finally {
                Events.this.close(result);
            }
            
            return count;
        }
        
        class CountingHandler implements TupleQueryResultHandler {
            long count = 0;
			public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
			}
			public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
				count++;
			}
			public void endQueryResult() throws TupleQueryResultHandlerException {
			}
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
            ValueFactory vf;
            ThreadVars.valueFactory.set(vf = conn.getValueFactory());
            
            timestamp = NTriplesUtil.toNTriplesString(vf.createURI(Defaults.NS, "EventTimeStamp"));
            
            final int statusSize = Math.max(1, Defaults.STATUS / 5);
            long count = 0, subcount = 0, queries = 0, restarts = 0;
            Calendar startTime, start, end;
            startTime = start= GregorianCalendar.getInstance();
            
            while (true) {
                // Do the query
                long result = randomQuery(conn, vf, (queries % statusSize == 0));
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
                            logtime(seconds), logtime(statusSize/seconds), subcount/statusSize, restarts);
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
                    count, queries, logtime(seconds), logtime(queries/seconds), count/queries, restarts);
            
            return new QueryResult(queries, count);
        }

		@Override
		public void close() {
			Events.this.close(conn);
		}
    }
    
    class Deleter implements Callable<Object>, Closeable {
    	
        private final RandomDate range;
        private AGRepositoryConnection conn;
        
        public Deleter(RandomDate range) throws Exception {
            this.range = range;
            conn = connect();
        }
        
        public Integer call() {
            Thread.currentThread().setName("deleter(" + range + ")");
            ValueFactory vf;
            ThreadVars.valueFactory.set(vf = conn.getValueFactory());
            
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
                trace("deleting %s to %s", start, eod);
            }
            
            while (start.before(range.end)) {
                if (Defaults.hasOption("sparql")) {
                    queryString = String.format(
                            "select ?s ?p ?o " +
                            "where { " +
                            "    ?s %s ?date . " +
                            "    filter ( ( ?date >= %s ) && ( ?date <= %s ) ) . " +
                            "    ?s ?p ?o . " +
                            "}",
                            timestamp, startNT, endNT);
                    if (Defaults.VERBOSE > 0) {
                        trace(queryString);
                    }
                    AGTupleQuery query = conn.prepareTupleQuery(AGQueryLanguage.SPARQL, queryString);
                    query = streamQuery(query);
                    TupleQueryResult result = null;
                    count = 0;
                    try {
                        long before = conn.size();
                        if (Defaults.stream == Defaults.STREAM.HAND || Defaults.stream == Defaults.STREAM.PULH) {
                        	DeletingHandler handler = new DeletingHandler(conn);
                        	query.evaluate(handler);
                            count = handler.count;
                        } else {
                            result = query.evaluate();
                            while (result.hasNext()) {
                                BindingSet bs = result.next();
                                conn.remove(conn.getValueFactory().createStatement(
                                        (BNode)bs.getValue("s"),
                                        (URI)bs.getValue("p"),
                                        bs.getValue("o")));
                                count++;
                            }
                        }
                        long sizeDiff = before - conn.size();
                        if (Defaults.VERBOSE > 0 && count != sizeDiff) {
                            trace("delete counts differ: size-diff: %d, query-count: %d", count, sizeDiff);
                        }
                    } catch (Exception e) {
            			errors++;
                        trace("Error executing query:\n%s\n", queryString);
                        e.printStackTrace();
                        count = -1;
                    } finally {
                    	Events.this.close(result);
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
                        long before = conn.size();
                        long count1 = tupleQuery.count();
                        count = before - conn.size();
                        if (Defaults.VERBOSE > 0 && count != count1) {
                            trace("delete counts differ: size-diff: %d, query-count: %d", count, count1);
                        }
                    } catch (Exception e) {
            			errors++;
                        trace("Error executing query:\n%s\n", queryString);
                        e.printStackTrace();
                    }
                }
                events += count;
                
                if (count == 0) {
                    trace("Finished deleting %d triples, %tF.", events, start);
                    start = eod;
                    startNT = NTriplesUtil.toNTriplesString(CalendarToValue(start));
                    eod = (GregorianCalendar) start.clone();
                    eod.setTimeInMillis(eod.getTimeInMillis() + interval);
                    endNT = NTriplesUtil.toNTriplesString(CalendarToValue(eod));
                }
            }
            
            trace("Found %d events (%d triples) to delete.", events, events * Defaults.EVENT_SIZE);
            
            return -1;
        }

        @Override
        public void close() {
            Events.this.close(conn);
        }

    }

    class DeletingHandler implements TupleQueryResultHandler {
        long count = 0;
        private final AGRepositoryConnection conn;
        DeletingHandler(AGRepositoryConnection conn) {
            this.conn = conn;
        }
        public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
        }
        public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
            try {
                conn.remove(conn.getValueFactory().createStatement(
                                                                   (BNode)bs.getValue("s"),
                                                                   (URI)bs.getValue("p"),
                                                                   bs.getValue("o")));
            } catch (RepositoryException e) {
                throw new TupleQueryResultHandlerException("failed to remove " + bs, e);
            }
            count++;
        }
        public void endQueryResult() throws TupleQueryResultHandlerException {
        }
    }

    public static class Monitor {

        private static void printOutput(Process p) throws IOException {
            BufferedReader input = new BufferedReader (new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                System.out.println(line);
                System.out.flush();
            }
            input.close();
        }
        
        static public void start(String phase) {
            if (Defaults.MONITOR) {
                try {
                    String[] commands = 
			new String[]{"src/test/stress/monitor.sh", "start", phase,
				     Defaults.CATALOG, Defaults.REPOSITORY};
                    Process p = Runtime.getRuntime().exec(commands);
                    printOutput(p);
                    trace("./monitor.sh was started.");
                } catch (IOException e) {
                    trace("./monitor.sh was not started.");
                }
            }
        }
        
        static public void stop() {
            if (Defaults.MONITOR) {
                try {
                    String[] commands = new String[]{"src/test/stress/monitor.sh", "end",
						     "0", Defaults.CATALOG, Defaults.REPOSITORY};
                    Process p = Runtime.getRuntime().exec(commands);
                    printOutput(p);
                    trace("./monitor.sh was stopped.");
                } catch (IOException e) {
                    trace("./monitor.sh was not stopped.");
                }
            }
        }

    }

	static AGTupleQuery streamQuery(AGTupleQuery tupleQuery) {
		if (Defaults.stream == Defaults.STREAM.PULL || Defaults.stream == Defaults.STREAM.PULH) {
			tupleQuery = new AGStreamTupleQuery(tupleQuery);
		}
		return tupleQuery;
	}

    /**
     * @param args Run with --help
     */
    public static void main(String[] args) throws Exception {
    	Events events = new Events();
    	try {
    		events.run(args);
    	} catch (Exception e) {
    		System.err.println(e);
    		e.printStackTrace();
    		System.exit(-1);
    	} finally {
    		Util.close(events);
    	}
    	if (events.errors > 0) {
    		// exit with error
    		throw new Exception("Errors during execution: " + events.errors);
    	}
		System.exit(0);
    }
    
    public void run(String[] args) throws Exception {
        Defaults.init(args);
        
        Thread.currentThread().setName("./events");
        
        if (Defaults.hasOption("open")) {
            trace("OPENING %s %s:%s.", Defaults.URL, Defaults.CATALOG, Defaults.REPOSITORY);
        } else {
            trace("SUPERSEDING %s %s:%s.", Defaults.URL, Defaults.CATALOG, Defaults.REPOSITORY);
        }
        
        AGServer server = new AGServer(Defaults.URL, Defaults.USERNAME, Defaults.PASSWORD);
        AGCatalog catalog = server.getCatalog(Defaults.CATALOG);
        if (false == Defaults.hasOption("open")) {
            catalog.deleteRepository(Defaults.REPOSITORY);
        }
        server.close();
        
        AGRepositoryConnection conn = connect();
        ThreadVars.valueFactory.set(conn.getValueFactory());
        
        AllEvents.initialize();
        
        final long triplesAtStart = conn.size();
        
        trace("Testing with %d loading, %d querying processes. Repository contains %d triples.", 
                Defaults.LOAD_WORKERS, Defaults.QUERY_WORKERS, triplesAtStart);
        final long startTime = System.currentTimeMillis();
        
        if (Defaults.LOAD_WORKERS > 0) {
            long triplesStart = triplesAtStart;
            long triplesEnd = triplesStart;
            long start, end, triples;
            double seconds;
            ExecutorService executor = Executors.newFixedThreadPool(Defaults.LOAD_WORKERS);
            
            /////////////////////////////////////////////////////////////////////// PHASE 1
            if (Defaults.PHASE <= 1) {
                List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(Defaults.LOAD_WORKERS);
            	for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
            		tasks.add(new Loader(task, Defaults.SIZE / 10, 1, BaselineRange));
            	}
                trace("Phase 1 Begin: Baseline %d triple commits.", Defaults.EVENT_SIZE);
                Monitor.start("phase-1");
                start = System.currentTimeMillis();
                invokeAndGetAll(executor, tasks);
                end = System.currentTimeMillis();
                triplesEnd = conn.size();
                triples = triplesEnd - triplesStart;
                seconds = (end - start) / 1000.0;
                trace("Phase 1 End: %d total triples added in %.1f seconds " +
                		"(%.2f triples/second, %.2f commits/second). " +
                		"Store contains %d triples.", triples, logtime(seconds),
                		logtime(triples/seconds),
                		logtime(triples/Defaults.EVENT_SIZE/seconds), triplesEnd);
                Monitor.stop(); // sync phase after phase-1 complete.
                closeAll(tasks);
            }
            
            /////////////////////////////////////////////////////////////////////// PHASE 2
            if (Defaults.PHASE <= 2) {
                triplesStart = triplesEnd;
                List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(Defaults.LOAD_WORKERS);
                for (int task = 0; task < (Defaults.LOAD_WORKERS); task++) {
                    tasks.add(new Loader(task, (Defaults.SIZE/10)*9, Defaults.BULK_EVENTS, BulkRange));
                }
                trace("Phase 2 Begin: Grow store by about %d triples.", (Defaults.SIZE*9/10));
                Monitor.start("phase-2");
                start = System.currentTimeMillis();
                invokeAndGetAll(executor, tasks);
                end = System.currentTimeMillis();
                triplesEnd = conn.size();
                triples = triplesEnd - triplesStart;
                seconds = (end - start) / 1000.0;
                trace("Phase 2 End: %d total triples bulk-loaded in %.1f seconds " +
                		"(%.2f triples/second, %.2f commits/second). " +
                		"Store contains %d triples.", triples, seconds, triples/seconds,
                		triples/Defaults.BULK_EVENTS/Defaults.EVENT_SIZE/seconds, triplesEnd);
                Monitor.stop();
                closeAll(tasks);
            }
            
            /////////////////////////////////////////////////////////////////////// PHASE 3
            if (Defaults.PHASE <= 3) {
                triplesStart = triplesEnd;
                List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(Defaults.LOAD_WORKERS);
                for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
                    tasks.add(task, new Loader(task, Defaults.SIZE/10, 1, SmallCommitsRange));
                }
                trace("Phase 3 Begin: Perform %d triple commits.", Defaults.EVENT_SIZE);
                Monitor.start("phase-3");
                start = System.currentTimeMillis();
                invokeAndGetAll(executor, tasks);
                end = System.currentTimeMillis();
                triplesEnd = conn.size();
                triples = triplesEnd - triplesStart;
                seconds = (end - start) / 1000.0;
                trace("Phase 3 End: %d total triples added in %.1f seconds " +
                		"(%.2f triples/second, %.2f commits/second). " +
                		"Store contains %d triples.", triples, seconds, triples/seconds,
                		triples/Defaults.EVENT_SIZE/seconds, triplesEnd);
                Monitor.stop();
                executor.shutdown();
                closeAll(tasks);
            }
        }
        
        /////////////////////////////////////////////////////////////////////// PHASE 4
        if (Defaults.PHASE <= 4) {
            if (Defaults.QUERY_WORKERS > 0 && Defaults.PHASE > 0) {
                ExecutorService executor = Executors.newFixedThreadPool(Defaults.QUERY_WORKERS);
                
                List<Callable<Object>> queriers = new ArrayList<Callable<Object>>(Defaults.QUERY_WORKERS);
                for (int task = 0; task < Defaults.QUERY_WORKERS; task++) {
                    queriers.add(new Querier(task, Defaults.QUERY_TIME*60, FullDateRange));
                }
                trace("Phase 4 Begin: Perform customer/date range queries with %d processes for %d minutes.",
                        Defaults.QUERY_WORKERS, Defaults.QUERY_TIME);
                Monitor.start("phase-4");
                int queries = 0;
                long triples = 0;
                long start = System.currentTimeMillis();
                try {
                    List<Future<Object>> fs = executor.invokeAll(queriers);
                    for (Future<Object> f : fs) {
                        QueryResult queryResult = (QueryResult) f.get();
                        queries += queryResult.queries;
                        triples += queryResult.triples;
                    }
                } catch (InterruptedException e) {
        			errors++;
                    e.printStackTrace();
                } catch (ExecutionException e) {
        			errors++;
                    e.printStackTrace();
                }
                long end = System.currentTimeMillis();
                double seconds = (end - start) / 1000.0;
                trace("Phase 4 End: %d total triples returned over %d queries in " +
		      "%.1f seconds (%.2f triples/second, %.2f queries/second, " +
		      "%d triples/query) MemUsed %d.", triples, queries, logtime(seconds),
		      logtime(triples/seconds), logtime(queries/seconds), triples/queries,
		      ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                Monitor.stop();
                executor.shutdown();
                closeAll(queriers);
            }
        }
        
        /////////////////////////////////////////////////////////////////////// PHASE 5
        if (Defaults.PHASE <= 5 && Defaults.DELETE_WORKERS > 0) {
            long triplesStart = conn.size();
            ExecutorService executor = Executors.newFixedThreadPool(2);
            List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(2);
            tasks.add(new Deleter(DeleteRangeOne));
            if (Defaults.DELETE_WORKERS > 1) {
                tasks.add(new Deleter(DeleteRangeTwo));
            }
            trace("Phase 5 Begin: Shrink store by 1 month.");
            Monitor.start("phase-5");
            long start = System.currentTimeMillis();
            invokeAndGetAll(executor, tasks);
            long end = System.currentTimeMillis();
            closeAll(tasks);
            executor.shutdown();
            long triplesEnd = conn.size();
            long triples = triplesEnd - triplesStart;
            double seconds = (end - start) / 1000.0;
            trace("Phase 5 End: %d total triples deleted in %.1f seconds " +
		  "(%.2f triples/second). Store contains %d triples.", triples,
		  logtime(seconds), logtime(triples/seconds), triplesEnd);
            Monitor.stop();
        }
        
        if (Defaults.PHASE <= 6 && Defaults.MIXED_RUNS != 0) {
            Monitor.start("phase-6");
            RandomDate smallCommitsRange = SmallCommitsRange;
            RandomDate fullDateRange = FullDateRange;
            RandomDate deleteRangeOne = DeleteRangeOne;
            RandomDate deleteRangeTwo = DeleteRangeTwo;
            
            ExecutorService executor = Executors.newFixedThreadPool(Defaults.LOAD_WORKERS + Defaults.QUERY_WORKERS + 2);
            for (int run=0; run < Defaults.MIXED_RUNS || Defaults.MIXED_RUNS == -1; run++) {
                int queries = 0;
                long triples = 0;
                long added = 0;
                long deleted = 0;
                smallCommitsRange = smallCommitsRange.next(Calendar.DAY_OF_YEAR, 30);
                fullDateRange = new RandomDate(deleteRangeTwo.end, smallCommitsRange.end);
                deleteRangeOne = deleteRangeTwo.next(Calendar.DAY_OF_YEAR, 15);
                deleteRangeTwo = deleteRangeOne.next(Calendar.DAY_OF_YEAR, 15);
                
                trace("Phase 6 Begin: Mixed workload - adds, queries, deletes. %s", run);
                List<Callable<Object>> tasks = new ArrayList<Callable<Object>>(Defaults.LOAD_WORKERS + Defaults.QUERY_WORKERS + 2);
                
                for (int task = 0; task < Defaults.LOAD_WORKERS; task++) {
                    tasks.add(new Loader(task, Defaults.SIZE/10, 1, smallCommitsRange));
                }
                for (int task = 0; task < Defaults.QUERY_WORKERS; task++) {
                    tasks.add(new Querier(task, Defaults.QUERY_TIME*60, fullDateRange));
                }
                if (Defaults.DELETE_WORKERS > 0) {
                    tasks.add(new Deleter(deleteRangeOne));
                    if (Defaults.DELETE_WORKERS > 1) {
                        tasks.add(new Deleter(deleteRangeTwo));
                    }
                }
                
                long start = System.currentTimeMillis();
                try {
                    List<Future<Object>> fs = executor.invokeAll(tasks);
                    for (Future f : fs) {
                        Object o = f.get();
                        if (o instanceof QueryResult) {
                            QueryResult queryResult = (QueryResult) o;
                            queries += queryResult.queries;
                            triples += queryResult.triples;
                        } else {
                            int i = (Integer)o;
                            if (i < 0) {
                                deleted += i;
                            } else {
                                added += i;
                            }
                        }
                    }
                } catch (InterruptedException e) {
        			errors++;
                    e.printStackTrace();
                } catch (ExecutionException e) {
        			errors++;
                    e.printStackTrace();
                }
                long end = System.currentTimeMillis();
                double seconds = (end - start) / 1000.0;
                trace("Phase 6 End: %d total triples returned over %d queries in " +
                		"%.1f seconds (%.2f triples/second, %.2f queries/second, " +
                		"%d triples/query, %d triples added, %d deletes).", triples, queries,
                		logtime(seconds), logtime(triples/seconds), logtime(queries/seconds),
                		(queries==0 ? 0 : triples/queries), added, deleted);
                closeAll(tasks);
            }
            executor.shutdown();
            Monitor.stop();
        }
        
        /////////////////////////////////////////////////////////////////////// END
        long triplesEnd = conn.size();
        double totalSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        long triples = triplesEnd - triplesAtStart;
        
        trace("Test completed in %.1f total seconds - store contains %d triples (%d triples added/removed).",
                logtime(totalSeconds), triplesEnd, triples);
    }
    
    private <Type> void invokeAndGetAll(ExecutorService executor,
            List<Callable<Type>> tasks) {
        try {
            List<Future<Type>> fs = executor.invokeAll(tasks);
            for (Future f : fs) {
                f.get();
            }
        } catch (InterruptedException e) {
			errors++;
            e.printStackTrace();
        } catch (ExecutionException e) {
			errors++;
            e.printStackTrace();
        }
    }
    
}
