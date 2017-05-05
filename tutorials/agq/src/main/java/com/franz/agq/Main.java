/////////////////////////////////////////////////////////////////////
// See the file LICENSE for the full license governing this code.
/////////////////////////////////////////////////////////////////////

package com.franz.agq;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.franz.agraph.repository.*;
import com.franz.util.Util;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.apache.log4j.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.resultio.*;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLBooleanJSONWriterFactory;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriterFactory;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLBooleanXMLWriterFactory;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriterFactory;
import org.eclipse.rdf4j.query.resultio.text.BooleanTextWriterFactory;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriterFactory;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVWriterFactory;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.jsonld.JSONLDWriterFactory;
import org.eclipse.rdf4j.rio.nquads.NQuadsWriterFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriterFactory;
import org.eclipse.rdf4j.rio.rdfjson.RDFJSONWriterFactory;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLWriterFactory;
import org.eclipse.rdf4j.rio.trig.TriGWriterFactory;
import org.eclipse.rdf4j.rio.trix.TriXWriterFactory;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;

public class Main {
    // Size of the buffer used when reading stdin
    private static final int IO_BUFFER_SIZE = 4096;

    /** Possible types of queries (and query results). */
    private enum QueryType {
        /** ASK and UPDATE queries (return true/false) */
        BOOLEAN,
        /** SELECT queries (return bindings) */
        TUPLE,
        /** CONSTRUCT and DESCRIBE queries (return RDF data) */
        GRAPH
    }

    // Known output formats. Note that Sesame registry is also consulted,
    // which means that the format can be a MIME type or it can be guessed
    // from the output file name.
    // Keys in the maps must use lower case.

    private static Map<String, TupleQueryResultWriterFactory> tupleWriters =
            new HashMap<>();

    private static Map<String, RDFWriterFactory> rdfWriters =
            new HashMap<>();

    private static Map<String, BooleanQueryResultWriterFactory> booleanWriters =
            new HashMap<>();

    // JCommander validators *sometimes* fail if the class is not public.
    public static final class Options {
        @Parameter(
                description = "<store>",
                arity = 1,
                required = true)
        private List<String> store = null;

        @Parameter(
                names = {"--url"},
                description = "Full URL of the AG server, including scheme, host and port." +
                        "If this argument is provided, --scheme, --host and --port are ignored.")
        private String url = null;

        @Parameter(
                names = {"--host"},
                description = "AG server address (default: $AGRAPH_HOST or localhost).")
        private String host = env("AGRAPH_HOST", "127.0.0.1");

        @Parameter(
                names = {"-p", "--port"},
                description =
                        "AG server port (default: either $AGRAPH_PORT or " +
                        "10035 for HTTP and 10036 for HTTPS).")
        private Integer port;

        @Parameter(
                names = {"--scheme"},
                description = "Protocol - either HTTP (default) or HTTPS.",
                validateWith = SchemeValidator.class)
        private String scheme = "http";

        @Parameter(
                names = {"-l", "--log-level"},
                description = "Log4J log level (default: INFO)")
        private String logLevel = "INFO";

        @Parameter(
                names = {"-F", "--query-file", },
                description = "Query file. Use - to read from stdin (default)")
        private String queryFile = null;

        @Parameter(
                names = {"-q", "--query"},
                description = "Query string (use -F to read from file.")
        private String query = null;

        @Parameter(
                names = {"--user", "-u"},
                description = "AG user name (default: $AGRAPH_USER, " +
                              "if that is not set a prompt will be displayed).",
                validateWith = CanPromptValidator.class)
        private String user = env("AGRAPH_USER");

        @Parameter(
                names = {"--password", "-a"},
                description = "AG password (default: $AGRAPH_PASSWORD, " +
                              "if that is not set a prompt will be displayed).",
                validateWith = CanPromptValidator.class)
        private String password = env("AGRAPH_PASSWORD");

        @Parameter(
                names = {"--base-uri", "-b"},
                description = "Base URI used to resolve relative URIs.")
        private String base = null;

        // TODO: describe known formats.
        @Parameter(
                names = {"--format", "-f"},
                description = "Result format. Default: try to guess from " +
                              "the output file name (if supplied), use CSV for stdout.")
        private String format = null;  // Hack - this works for all query types

        @Parameter(
                names = {"--output", "-o"},
                description = "Output file (Use '-' for stdout (default)).")
        private String output = null;

        @Parameter(
                names = {"-h", "--help"},
                description = "Print help message.",
                help = true)
        private boolean help = false;

        @Parameter(
                names = {"--list-formats"},
                description = "Show a list of supported output formats.",
                help = true)
        private boolean listFormats = false;

        /** Validator for the --scheme argument. */
        public static final class SchemeValidator implements IParameterValidator {
            @Override
            public void validate(final String param, final String value) throws ParameterException {
                if (!value.equalsIgnoreCase("http") &&
                        !value.equalsIgnoreCase("https")) {
                    throw new ParameterException(
                            String.format("%s must be either HTTP or HTTPS", param));
                }
            }
        }

        /** A validator that ensures we can prompt for a value - but only if we do not have one. */
        public static final class CanPromptValidator implements IParameterValidator {
            @Override
            public void validate(final String param, final String value) throws ParameterException {
                // System.console() will be null() if either stdin or stdout is redirected.
                // This is not perfect, but ....
                if (value == null && System.console() == null) {
                    throw new ParameterException(
                            String.format("Unable to prompt for the value of %s", param));
                }
            }
        }

        /**
         * Gets the name of the repository to be queried.
         *
         * @return Repo name, without catalog.
         */
        String getRepository() {
            return Util.getRepoFromSpec(store.get(0));
        }

        /**
         * Gets the catalog name if it was specified.
         *
         * Returns null if the default catalog should be used.
         *
         * @return Catalog name or null.
         */
        String getCatalog() {
            return Util.getCatalogFromSpec(store.get(0));
        }

        /**
         * Gets the protocol to be used ("http" or "https").
         *
         * @return Protocol.
         */
        private String getScheme() {
            return scheme.toLowerCase();
        }

        /**
         * Gets the host to connect to.
         *
         * @return AG server address.
         */
        private String getHost() {
            return host;
        }

        /**
         * Gets the port number to connect to.
         *
         * @return AG server port.
         */
        private int getPort() {
            if (port == null) {
                final String rawPort = env("AGRAPH_PORT");
                if (rawPort == null) {
                   port = isUseHTTPS() ?
                           AGServer.DEFAULT_HTTPS_PORT :
                           AGServer.DEFAULT_HTTP_PORT;
                } else {
                    port = Integer.parseInt(rawPort);
                }
            }
            return port;
        }

        /**
         * Gets the full server URL - either passed explicitly or through --host, --port
         * and --scheme.
         *
         * @return Server URL.
         */
        String getURL() {
            if (url == null) {
                return String.format("%s://%s:%d", getScheme(), getHost(), getPort());
            }
            return url;
        }

        /**
         * Checks if HTTPS should be used to connect to the server.
         *
         * @return True for HTTPS, false for HTTP.
         */
        private boolean isUseHTTPS() {
            return scheme.equalsIgnoreCase("https");
        }

        String getLogLevel() {
            return logLevel;
        }

        /**
         * Gets the query specified on the command line.
         *
         * @return Query or {@ocde null}
         */
        String getQuery() {
            return query;
        }

        /**
         * Gets the path to the file that contains the query.
         *
         * Returns null if stdin should be used.
         *
         * @return File path or null.
         */
        String getQueryFile() {
            if (queryFile == null || queryFile.equals("-")) {
                return null;
            }
            return queryFile;
        }

        /**
         * Returns the username, prompting for it
         * if it was not given on command line and
         * is not available in the env AG_USER variable.
         *
         * @return Username.
         */
        String getUser() {
            if (user == null) {
                user = System.console().readLine("AG username: ");
            }
            return user;
        }

        /**
         * Returns the password, prompting for it
         * if it was not given on command line
         * is not available in the env AG_PASSWORD variable..
         *
         * @return Username.
         */
        String getPassword() {
            if (password == null) {
                password = new String(System.console().readPassword(
                        "Password for %s:", getUser()));
            }
            return password;
        }

        /**
         * Gets the base URI used to resolve relative URIs in the query.
         *
         * @return URI or null (not specified).
         */
        String getBaseURI() {
            return base;
        }

        /**
         * Gets either the desired output format or the output file name.
         *
         * @return Format or file name.
         */
        String getFormat() {
            // Explicitly specified format
            if (format != null) {
                return format;
            }
            // Writing to stdout - use default format
            if (isOutputStdout()) {
                return "csv";
            }
            // Try to guess from the file name
            return output;
        }

        /**
         * Gets the output file name, returns {@code null} for stdout.
         *
         * @return File name or {@code null} (stdout).
         */
        String getOutput() {
            if (output == null || output.equals("-")) {
                return null;
            }
            return output;
        }

        /**
         * Checks if the output should go to stdout.
         *
         * @return True if the output target is stdout.
         */
        boolean isOutputStdout() {
            return getOutput() == null;
        }

        /**
         * Checks if the help message should be printed.
         * @return True if help should be printed.
         */
        boolean isHelp() {
            return help;
        }
    }

    /** Fills the maps of known output formats. */
    private static void initFormats() {
        final TupleQueryResultWriterFactory tupleCSV =
                new SPARQLResultsCSVWriterFactory();
        tupleWriters.put("csv", tupleCSV);
        tupleWriters.put("sparqlcsv", tupleCSV);
        tupleWriters.put("sparql+csv", tupleCSV);
        tupleWriters.put("sparql/csv", tupleCSV);

        final TupleQueryResultWriterFactory tupleTSV =
                new SPARQLResultsTSVWriterFactory();
        tupleWriters.put("tsv", tupleTSV);
        tupleWriters.put("sparqltsv", tupleTSV);
        tupleWriters.put("sparql+tsv", tupleTSV);
        tupleWriters.put("sparql/tsv", tupleTSV);

        final TupleQueryResultWriterFactory tupleXML =
                new SPARQLResultsXMLWriterFactory();
        tupleWriters.put("xml", tupleXML);
        tupleWriters.put("sparqlxml", tupleXML);
        tupleWriters.put("sparql+xml", tupleXML);
        tupleWriters.put("sparql/xml", tupleXML);

        final TupleQueryResultWriterFactory tupleJSON =
                new SPARQLResultsJSONWriterFactory();
        tupleWriters.put("json", tupleJSON);
        tupleWriters.put("sparqljson", tupleJSON);
        tupleWriters.put("sparql+json", tupleJSON);
        tupleWriters.put("sparql/json", tupleJSON);

        // Add all other formats known by Sesame/rdf4j
        final TupleQueryResultWriterRegistry tupleReg =
                TupleQueryResultWriterRegistry.getInstance();
        for (TupleQueryResultWriterFactory factory : tupleReg.getAll()) {
            tupleWriters.put(
                    factory.getTupleQueryResultFormat().getName().toLowerCase(),
                    factory);
        }

        final RDFWriterFactory ntriples = new NTriplesWriterFactory();
        rdfWriters.put("ntriples", ntriples);
        rdfWriters.put("n-triples", ntriples);
        rdfWriters.put("n/triples", ntriples);
        rdfWriters.put("nt", ntriples);

        final RDFWriterFactory turtle = new TurtleWriterFactory();
        rdfWriters.put("turtle", turtle);

        final RDFWriterFactory trig = new TriGWriterFactory();
        rdfWriters.put("trig", trig);

        final RDFWriterFactory trix = new TriXWriterFactory();
        rdfWriters.put("trix", trix);

        final RDFWriterFactory nquads = new NQuadsWriterFactory();
        rdfWriters.put("nquads", nquads);
        rdfWriters.put("n-quads", nquads);
        rdfWriters.put("n/quads", nquads);
        rdfWriters.put("nq", nquads);

        final RDFWriterFactory rdfXml = new RDFXMLWriterFactory();
        rdfWriters.put("xml", rdfXml);
        rdfWriters.put("rdf-xml", rdfXml);
        rdfWriters.put("rdf+xml", rdfXml);
        rdfWriters.put("rdfxml", rdfXml);

        final RDFWriterFactory rdfJson = new RDFJSONWriterFactory();
        rdfWriters.put("json", rdfJson);
        rdfWriters.put("rdf-json", rdfJson);
        rdfWriters.put("rdf+json", rdfJson);
        rdfWriters.put("rdfjson", rdfJson);

        final RDFWriterFactory jsonld = new JSONLDWriterFactory();
        rdfWriters.put("jsonld", jsonld);
        rdfWriters.put("json/ld", jsonld);
        rdfWriters.put("json ld", jsonld);

        // Add all other formats known by Sesame/rdf4j
        final RDFWriterRegistry graphReg = RDFWriterRegistry.getInstance();
        for (RDFWriterFactory factory : graphReg.getAll()) {
            rdfWriters.put(
                    factory.getRDFFormat().getName().toLowerCase(),
                    factory);
        }

        final BooleanQueryResultWriterFactory boolJson =
                new SPARQLBooleanJSONWriterFactory();
        booleanWriters.put("json", boolJson);
        booleanWriters.put("sparql-json", boolJson);
        booleanWriters.put("sparql+json", boolJson);
        booleanWriters.put("sparqljson", boolJson);

        final BooleanQueryResultWriterFactory boolXml =
                new SPARQLBooleanXMLWriterFactory();
        booleanWriters.put("xml", boolXml);
        booleanWriters.put("sparql-xml", boolXml);
        booleanWriters.put("sparql+xml", boolXml);
        booleanWriters.put("sparqlxml", boolXml);

        final BooleanQueryResultWriterFactory boolText =
                new BooleanTextWriterFactory();
        booleanWriters.put("text", boolText);
        // I want CSV to work for all query types, so ...
        booleanWriters.put("csv", boolText);

        // Add all other formats known by Sesame/rdf4j
        final BooleanQueryResultWriterRegistry boolReg =
                BooleanQueryResultWriterRegistry.getInstance();
        for (BooleanQueryResultWriterFactory factory : boolReg.getAll()) {
            booleanWriters.put(
                    factory.getBooleanQueryResultFormat().getName().toLowerCase(),
                    factory);
        }
    }

    /**
     * Retrieves the value of an environment variable,
     * or the specified default value if the variable is not set.
     *
     * @param var Variable name.
     * @param defaultValue Value to be returned if the variable is not set.
     *
     * @return Variable value.
     */
    private static String env(final String var, final String defaultValue) {
        final String value = System.getenv(var);
        return value == null ? defaultValue : value;
    }

    /**
     * Retrieves the value of an environment variable,
     * or null if the variable is not set.
     *
     * @param var Variable name.
     *
     * @return Variable value or null.
     */
    private static String env(final String var) {
        return env(var, null);
    }

    /**
     * Reads all data from stdin into a byte array.
     *
     * @return Data from stdin.
     * @throws IOException .
     */
    private static byte[] readStdin() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final byte[] buffer = new byte[IO_BUFFER_SIZE];
        int size;

        while ((size = System.in.read(buffer)) > 0) {
            os.write(buffer, 0, size);
        }
        return os.toByteArray();
    }

    /**
     * Reads the query from given path.
     *
     * If the path is null, reads the query from stdin.
     *
     * @param path File path or null.
     *
     * @return Query text.
     * @throws IOException .
     */
    private static String readQuery(final String path) throws IOException {
        final byte[] data;
        if (path == null) {
            // Prompt if we're not redirecting
            if (System.console() != null) {
                System.err.println("Query: ");
            }
            data = readStdin();
        } else {
            data = Files.readAllBytes(Paths.get(path));
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Run the query and print the result.
     *
     * @param conn Connection object.
     * @param queryString Query text.
     * @param baseURI Base for resolving relative URIs. Can be {@code null}.
     * @param resultFormat Output format. Valid values depend on query type.
     *                     {@code null} always means 'use default'.
     * @param output Stream to print the result to.
     *
     * @throws Exception if something goes wrong.
     */
    private static void runQuery(final AGRepositoryConnection conn,
                                 final String queryString,
                                 final String baseURI,
                                 final String resultFormat,
                                 final OutputStream output) throws Exception {
        final AGQuery query = conn.prepareQuery(
                QueryLanguage.SPARQL, queryString, baseURI);
        // prepareQuery detects query type... but you can't evaluate
        // an abstract query
        final QueryType queryType = getQueryType(query);
        switch (queryType) {
            case TUPLE:
                run((TupleQuery)query, resultFormat, output);
                break;
            case GRAPH:
                run((GraphQuery)query, resultFormat, output);
                break;
            case BOOLEAN:
                run((BooleanQuery)query, resultFormat, output);
                break;
            default: assert false;
        }
    }

    /**
     * Deduce query type from a prepared query.
     *
     * @param query A prepared query.
     *
     * @return Query type.
     * @throws Exception If the query type is unknown or not supported.
     */
    private static QueryType getQueryType(AGQuery query) throws Exception {
        if (query instanceof TupleQuery) {
            return QueryType.TUPLE;
        } else if (query instanceof GraphQuery) {
            return QueryType.GRAPH;
        } else if (query instanceof BooleanQuery) {
            return QueryType.BOOLEAN;
        } else {
            throw new Exception("Unknown query type.");
        }
    }

    /**
     * Gets a result writer for a given query result format.
     *
     * Sesame has a writer registry, but writers can only be retrieved
     * using MIME types or file names.
     *
     * @param registry Sesame registry appropriate for the query type.
     * @param formats Map of known formats.
     * @param name Format name OR file name.
     *
     * @return A result writer factory.
     * @throws RuntimeException if the format is not known.
     */
    private static<WF,
                   F extends FileFormat,
                   WR extends FileFormatServiceRegistry<F, WF>>
    WF getWriterFactory(
            final WR registry, final Map<String, WF> formats,
            final String name) {
        final String normalized = name.toLowerCase().trim();
        if (formats.containsKey(normalized)) {
            return formats.get(normalized);
        }
        F format;
        format = registry.getFileFormatForMIMEType(name).orElse(null);;
        if (format == null) {
            format = registry.getFileFormatForFileName(name).orElse(null);
        }
        if (format == null) {
            throw new RuntimeException("Unknown output format: " + name);
        }
        return registry.get(format).orElse(null);
    }

    /**
     * Gets a result writer for a given tuple query result format.
     *
     * Sesame has a writer registry, but writers can only be retrieved
     * using MIME types or file names.
     *
     * @param name Format name OR file name.
     *
     * @return A result writer factory.
     * @throws RuntimeException if the format is not known.
     */
    private static TupleQueryResultWriterFactory parseTupleResultFormat(
            final String name) {
        final TupleQueryResultWriterRegistry registry =
                TupleQueryResultWriterRegistry.getInstance();
        return getWriterFactory(registry, tupleWriters, name);
    }

    /**
     * Gets a result writer for a given graph query result format.
     *
     * Sesame has a writer registry, but writers can only be retrieved
     * using MIME types or file names.
     *
     * @param name Format name OR file name.
     *
     * @return A result writer factory.
     * @throws RuntimeException if the format is not known.
     */
    private static RDFWriterFactory parseGraphResultFormat(
            final String name) {
        final RDFWriterRegistry registry = RDFWriterRegistry.getInstance();
        return getWriterFactory(registry, rdfWriters, name);
    }

    /**
     * Gets a result writer for a given boolean query result format.
     *
     * Sesame has a writer registry, but writers can only be retrieved
     * using MIME types or file names.
     *
     * @param name Format name OR file name.
     *
     * @return A result writer factory.
     * @throws RuntimeException if the format is not known.
     */
    private static BooleanQueryResultWriterFactory parseBooleanResultFormat(
            final String name) throws Exception {
        final BooleanQueryResultWriterRegistry registry =
                BooleanQueryResultWriterRegistry.getInstance();
        return getWriterFactory(registry, booleanWriters, name);
    }

    private static void run(final TupleQuery query,
                            final String resultFormat,
                            final OutputStream out) throws Exception {
        final TupleQueryResultHandler handler;
        handler = parseTupleResultFormat(resultFormat).getWriter(out);
        query.evaluate(handler);
    }

    private static void run(final GraphQuery query,
                            final String resultFormat,
                            final OutputStream out) throws Exception {
        final RDFHandler handler;
        handler = parseGraphResultFormat(resultFormat).getWriter(out);
        query.evaluate(handler);
    }

    private static void run(final BooleanQuery query,
                            final String resultFormat,
                            final OutputStream out) throws Exception {
        final BooleanQueryResultHandler handler;
        handler = parseBooleanResultFormat(resultFormat).getWriter(out);
        handler.handleBoolean(query.evaluate());
    }

    /** Lists known output formats. */
    private static void listFormats() {
        // Construct a map to group all things that map
        // to each WriterFactory
        final Map<String, Set<String>> formats = new HashMap<>();

        BiConsumer<String, String> push = (String key, String elt) -> {
            if (!formats.containsKey(key)) {
                formats.put(key, new TreeSet<>());
            }
            formats.get(key).add(elt);
        };

        Runnable print = () -> {
            for (Map.Entry<String, Set<String>> entry : formats.entrySet()) {
                final String[] values = entry.getValue().toArray(new String[]{});
                System.err.printf(
                        "    %s: %s\n",
                        entry.getKey(), String.join(", ", values));
            }
            formats.clear();
        };

        System.out.println("Formats for SELECT queries:");
        for (Map.Entry<String, TupleQueryResultWriterFactory> entry : tupleWriters.entrySet()) {
            final String name = entry.getValue().getTupleQueryResultFormat().getName();
            push.accept(name, entry.getKey());
        }
        print.run();

        System.out.println("Formats for CONSTRUCT and DESCRIBE queries:");
        for (Map.Entry<String, RDFWriterFactory> entry : rdfWriters.entrySet()) {
            final String name = entry.getValue().getRDFFormat().getName();
            push.accept(name, entry.getKey());
        }
        print.run();

        System.out.println("Formats for ASK and UPDATE queries:");
        for (Map.Entry<String, BooleanQueryResultWriterFactory> entry : booleanWriters.entrySet()) {
            final String name = entry.getValue().getBooleanQueryResultFormat().getName();
            push.accept(name, entry.getKey());
        }
        print.run();
    }

    /**
     * Entry point after option parsing.
     *
     * @param options Parsed options.
     * @throws Exception If something goes wrong.
     */
    private static void main(final Options options) throws Exception {
        Logger.getRootLogger().setLevel(Level.toLevel(options.getLogLevel()));
        final AGServer server = new AGServer(
                options.getURL(), options.getUser(), options.getPassword());

        // Hack: we must read the query AFTER getPassword, to avoid messing up
        // prompts when reading from stdin.
        final String query;
        if (options.getQuery() != null) {
            query = options.getQuery();
        } else {
            query = readQuery(options.getQueryFile());
        }

        try {
            final AGCatalog catalog = server.getCatalog(options.getCatalog());
            final AGRepository repo = catalog.openRepository(options.getRepository());
            try {
                repo.initialize();
                final AGRepositoryConnection conn = repo.getConnection();
                try {
                    if (options.isOutputStdout()) {
                        runQuery(conn, query, options.getBaseURI(),
                                options.getFormat(), System.out);
                    } else {
                        try (FileOutputStream out =
                                     new FileOutputStream(options.output)) {
                            runQuery(conn, query, options.getBaseURI(),
                                    options.getFormat(), out);
                        }
                    }
                } finally {
                    conn.close();
                }
            } finally {
                repo.shutDown();
            }
        } finally {
            server.close();
        }
    }

    private static void stopBotheringMe() {
        // We need to provide a log4j config.
        // The main goal is to avoid polluting stdout.
        final Layout layout = new PatternLayout();
        final ConsoleAppender appender = new ConsoleAppender(layout, ConsoleAppender.SYSTEM_ERR);
        appender.setThreshold(Level.ERROR);
        org.apache.log4j.BasicConfigurator.configure(appender);
    }

    /**
     * Main entry point.
     * @param args Command-line arguments.
     * @throws Exception If it feels like it.
     */
    public static void main(final String... args) throws Exception {
        stopBotheringMe();
        initFormats();
        final Options options = new Options();
        final JCommander optParser = JCommander.newBuilder().addObject(options).build();
        optParser.setProgramName("java -jar agq.jar");

        try {
            optParser.parse(args);
        } catch (final ParameterException e) {
            System.err.printf("Error: %s\n\n", e.getMessage());
            optParser.usage();
            System.exit(1);
        }

        if (options.isHelp()) {
            optParser.usage();
            System.exit(0);
        } else if (options.listFormats) {
            listFormats();
            System.exit(0);
        }
        main(options);
    }
}
