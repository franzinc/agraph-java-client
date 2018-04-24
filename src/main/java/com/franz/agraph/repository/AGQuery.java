/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.exception.AGHttpException;
import com.franz.agraph.http.exception.AGQueryTimeoutException;
import com.franz.agraph.http.handler.AGDownloadHandler;
import com.franz.agraph.http.handler.AGRawStreamer;
import com.franz.agraph.http.handler.AGResponseHandler;
import com.franz.agraph.http.handler.AGStringHandler;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.AbstractQuery;

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;

/**
 * An abstract query class common to Boolean, Graph and Tuple Queries.
 */
public abstract class AGQuery extends AbstractQuery {

    /**
     * The default query planner for SPARQL.
     */
    public static final String SPARQL_COVERAGE_PLANNER = "coverage";  // TODO add to protocol

    /**
     * A query planner for SPARQL that processes queries without doing
     * any reordering of clauses or optimization, useful if the user
     * knows the best order for processing the query.
     */
    public static final String SPARQL_IDENTITY_PLANNER = "identity";

    /**
     * The default entailment regime to use when inferences are included.
     */
    public static final String RDFS_PLUS_PLUS = "rdfs++";

    /**
     * An entailment regime that includes hasValue, someValuesFrom and
     * allValuesFrom reasoning in addition to RDFS++ entailment.
     */
    public static final String RESTRICTION = "restriction";

    //private static long prepareId = 0L;

    protected AGRepositoryConnection httpCon;

    protected QueryLanguage queryLanguage;

    protected String queryString;

    protected String baseURI;

    protected String entailmentRegime = RDFS_PLUS_PLUS;

    protected String planner;
    protected String saveName = null;
    protected boolean prepared = false;
    protected boolean checkVariables = false;
    protected int limit = -1;
    protected int offset = -1;
    protected boolean loggingEnabled = false;
    private String engine;


    public AGQuery(AGRepositoryConnection con, QueryLanguage ql, String queryString, String baseURI) {
        super.setIncludeInferred(false); // set default
        this.httpCon = con;
        this.queryLanguage = ql;
        this.queryString = queryString;
        this.baseURI = baseURI;
        // AG queries exclude inferences by default
        super.includeInferred = false;
    }

    /**
     * Returns a String of the form "PREFIX franzOption_OPTION: &lt;franz:VALUE&gt; "
     * suitable for appending to a SPARQL query. VALUE is encoded per the rules
     * for percent-encoding the query part of a URI (namely that the space char ' '
     * encoded as '%20' instead of '+'.
     *
     * @param option the name of a valid AllegroGraph SPARQL Query Option
     * @param value  String value to be encoded as the value of the prefixOption
     * @return String  the generated prefix.
     * @throws URIException if there is an error while encoding <code>value</code>
     * @see <a href="../../../../../sparql-reference.html#sparql-queryoptions">SPARQL Query Options</a>
     */
    public static String getFranzOptionPrefixString(String option, String value) throws URIException {
        // this will only throw if utf-8 is an unsupported charset.
        return "PREFIX franzOption_" + option + ": <franz:" + URIUtil.encodeQuery(value, "utf-8") + "> ";
    }

    /**
     * Determine whether evaluation results of this query should include inferred
     * statements (if any inferred statements are present in the repository). The
     * default setting is 'false'.
     *
     * @param includeInferred indicates whether inferred statements should included in the
     *                        result.
     * @see #setEntailmentRegime(String)
     */
    @Override
    public void setIncludeInferred(boolean includeInferred) {
        super.setIncludeInferred(includeInferred);
    }

    /**
     * Gets the entailment regime being used when including inferences
     * with this query.
     *
     * @return String  the name of the entailment regime
     */
    public String getEntailmentRegime() {
        return entailmentRegime;
    }

    /**
     * Sets the entailment regime to use when including inferences with this
     * query.  Default is 'rdfs++'.
     *
     * @param entailmentRegime indicates the entailment regime to use when reasoning
     * @see #RDFS_PLUS_PLUS
     * @see #RESTRICTION
     * @see #setIncludeInferred(boolean)
     */
    public void setEntailmentRegime(String entailmentRegime) {
        this.entailmentRegime = entailmentRegime;
    }

    /**
     * Gets the query language for this query.
     *
     * @return the query language
     */
    public QueryLanguage getLanguage() {
        return queryLanguage;
    }

    /**
     * Gets the query string for this query.
     *
     * @return the query string
     */
    public String getQueryString() {
        long timeout = getMaxExecutionTime();
        String timeoutPrefix = timeout > 0 ? "PREFIX franzOption_queryTimeout: <franz:" + timeout + ">     # timeout in seconds\n " : "";
        return timeoutPrefix + queryString;
    }

    /**
     * Gets the loggingEnabled setting for this query.
     *
     * @return Boolean  true if logging is enabled, else false
     */
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * Sets the loggingEnabled parameter for this query.
     * <p>
     * Default is false.
     *
     * @param loggingEnabled boolean indicating whether logging is enabled
     */
    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    /**
     * Gets the baseURI for this query.
     *
     * @return the base URI
     */
    public String getBaseURI() {
        return baseURI;
    }

    /**
     * Gets the query planner that processes the query.
     *
     * @return the planner name
     */
    public String getPlanner() {
        return planner;
    }

    /**
     * Sets the query planner to use when processing the query.
     *
     * @param planner the planner name
     */
    public void setPlanner(String planner) {
        this.planner = planner;
    }

    /**
     * @return String  the name of the engine used to perform this query
     * @deprecated internal use only
     */
    public String getEngine() {
        return engine;
    }

    /**
     * This method is not for general use - configure server agraph.cfg QueryEngine instead.
     *
     * @param engine the name of the query engine to use for this query
     * @see #getEngine()
     * @deprecated internal use only
     */
    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * Schedules the query to be prepared.
     * <p>
     * Note: this is a no-op pending further cost-benefit analysis of
     * the server's saved query service.
     */
    synchronized void prepare() {
        //setSaveName(String.valueOf(prepareId++));
    }

    /**
     * Sets the name to use when saving this query with the
     * server's saved query service.
     *
     * @param name the saved name
     */
    public void setSaveName(String name) {
        saveName = name;
    }

    /**
     * Gets the savedName for the query.
     *
     * @return the saved name
     */
    public String getName() {
        return saveName;
    }

    /**
     * Gets the prepared flag for the query.
     *
     * @return the prepared flag
     */
    public boolean isPrepared() {
        return prepared;
    }

    /**
     * Sets the prepared flag for the query.
     *
     * @param prepared the prepared flag
     */
    public void setPrepared(boolean prepared) {
        this.prepared = prepared;
    }

    /**
     * Evaluates the query and processes the result in handler.
     *
     * @param handler processes or stores the result
     * @throws QueryEvaluationException
     */
    protected void evaluate(AGResponseHandler handler)
            throws QueryEvaluationException {
        evaluate(false, handler);
    }

    /**
     * Evaluates the query and processes the result in handler.
     * <p>
     * When the analyzeOnly flag is true, only a query analysis is
     * performed; when false, the query is executed.
     *
     * @param analyzeOnly flags for analyzing or executing
     * @param handler     processes or stores the result
     * @throws QueryEvaluationException
     */
    protected void evaluate(boolean analyzeOnly, AGResponseHandler handler)
            throws QueryEvaluationException {
        try {
            httpCon.prepareHttpRepoClient().query(this, analyzeOnly, handler);
        } catch (AGQueryTimeoutException e) {
            throw new QueryInterruptedException(e);
        } catch (AGHttpException e) {
            throw new QueryEvaluationException(e);
        }
    }

    /**
     * Evaluates the query and saves the results to a file.
     * <p>
     * Output format is determined by the server.
     *
     * @param file Output path.
     * @throws QueryEvaluationException if there is an error while evaluating query
     */
    public void download(final File file) throws QueryEvaluationException {
        evaluate(new AGDownloadHandler(file));
    }

    /**
     * Evaluates the query and saves the results to a file.
     * <p>
     * Output format is determined by the server.
     *
     * @param file Output path.
     * @throws QueryEvaluationException if there is an error while evaluating query
     */
    public void download(final String file) throws QueryEvaluationException {
        evaluate(new AGDownloadHandler(file));
    }

    /**
     * Evaluates the query and saves the results to a file.
     *
     * @param file     Output path.
     * @param mimeType MIME type that will be requested from the server (i.e. output format).
     * @throws QueryEvaluationException if there is an error while evaluating query
     */
    public void download(final File file, final String mimeType)
            throws QueryEvaluationException {
        evaluate(new AGDownloadHandler(file, mimeType));
    }

    /**
     * Evaluates the query and saves the results to a file.
     *
     * @param file     Output path.
     * @param mimeType MIME type that will be requested from the server (i.e. output format).
     * @throws QueryEvaluationException if there is an error while evaluating query
     */
    public void download(final String file, final String mimeType)
            throws QueryEvaluationException {
        evaluate(new AGDownloadHandler(file, mimeType));
    }

    /**
     * Evaluates the query and returns the result as an input stream.
     * <p>
     * Note that it is important to close the returned stream, to avoid
     * resource leaks.
     *
     * @param mimeType MIME type that will be requested from the server (i.e. output format).
     * @return An input stream containing response data.
     * The caller MUST close this stream to release connection resources.
     * @throws QueryEvaluationException if there is an error while evaluating query
     */
    public InputStream stream(final String mimeType)
            throws QueryEvaluationException {
        final AGRawStreamer handler = new AGRawStreamer(mimeType);
        evaluate(handler);
        try {
            return handler.getStream();
        } catch (final AGHttpException e) {
            throw new QueryEvaluationException(e);
        }
    }

    /**
     * Evaluates the query and returns the result as an input stream.
     * <p>
     * The output format will be chosen by the server.
     *
     * @return An input stream containing response data.
     * The caller MUST close this stream to release connection resources.
     * @throws QueryEvaluationException if there is an error while evaluating query
     */
    public InputStream stream()
            throws QueryEvaluationException {
        return stream("*/*");
    }

    /**
     * Returns the query analysis for the query.
     * <p>
     * The query is not evaluated.
     *
     * @return the query analysis as a string
     * @throws QueryEvaluationException if there is an error while evaluating query
     */
    public String analyze() throws QueryEvaluationException {
        AGStringHandler handler = new AGStringHandler();
        evaluate(true, handler);
        return handler.getResult();
    }

    /**
     * Gets the flag for checkVariables.
     *
     * @return the checkVariables flag
     */
    public boolean isCheckVariables() {
        return checkVariables;
    }

    /**
     * A boolean that defaults to false, indicating whether an error
     * should be raised when a SPARQL query selects variables that
     * are not mentioned in the query body.
     *
     * @param checkVariables the checkVariables flag
     */
    public void setCheckVariables(boolean checkVariables) {
        this.checkVariables = checkVariables;
    }

    public Binding[] getBindingsArray() {
        BindingSet bindings = this.getBindings();

        Binding[] bindingsArray = new Binding[bindings.size()];

        Iterator<Binding> iter = bindings.iterator();
        for (int i = 0; i < bindings.size(); i++) {
            bindingsArray[i] = iter.next();
        }

        return bindingsArray;
    }

    @Override
    public String toString() {
        return queryString;
    }

    @Override
    protected void finalize() throws Throwable {
        if (saveName != null) {
            httpCon.prepareHttpRepoClient().savedQueryDeleteQueue.add(saveName);
        }
        super.finalize();
    }

    /**
     * Gets the limit on the number of solutions for this query.
     *
     * @return limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Sets the limit.
     * <p>
     * By default, the value is -1, meaning no constraint is imposed.
     *
     * @param limit the max number of solutions to collect for this query
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Gets the offset, the number of solutions to skip for this query.
     *
     * @return offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Sets the offset.
     *
     * @param offset the number of solutions to skip for this query
     *               <p>
     *               By default, the value is -1, meaning no constraint is imposed.
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }
}
