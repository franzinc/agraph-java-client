/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import org.eclipse.rdf4j.model.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A materializer governs how triples are inferred and added to a repository.
 *
 * @since v4.9
 */
public class AGMaterializer {

    private final List<String> with;
    private final List<String> without;
    private Integer commitPeriod;
    private Boolean useTypeSubproperty;
    private Resource inferredGraph;

    private AGMaterializer() {
        with = new ArrayList<>();
        without = new ArrayList<>();
        commitPeriod = null; // use the server's default
        useTypeSubproperty = null; // use the server's default
    }

    /**
     * Gets a default materializer that can be further configured.
     * <p>
     * See also the HTTP protocol documentation for
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-materialize-entailed">
     * materializing entailed triples</a>.
     *
     * @return a default materializer.
     */
    static public AGMaterializer newInstance() {
        return new AGMaterializer();
    }

    /**
     * Adds a ruleset to be used during materialization.
     * <p>
     * See also the HTTP protocol documentation for
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-materialize-entailed">
     * materializing entailed triples</a>.
     *
     * @param ruleset the name of the ruleset to include.
     */
    public void withRuleset(final String ruleset) {
        with.add(ruleset);
    }

    /**
     * Gets the included rulesets.
     *
     * @return the with rulesets
     */
    public List<String> getWithRulesets() {
        return Collections.unmodifiableList(with);
    }

    /**
     * Excludes a ruleset from materialization.
     * <p>
     * See also the HTTP protocol documentation for
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-materialize-entailed">
     * materializing entailed triples</a>.
     *
     * @param ruleset the name of the ruleset to exclude.
     */
    public void withoutRuleset(final String ruleset) {
        without.add(ruleset);
    }

    /**
     * Gets the excluded rulesets.
     *
     * @return the without rulesets
     */
    public List<String> getWithoutRulesets() {
        return Collections.unmodifiableList(without);
    }

    /**
     * Gets the commit period for materialized triples.
     * <p>
     * See also the HTTP protocol documentation for
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-materialize-entailed">
     * materializing entailed triples</a>.
     *
     * @return the commit period in triples.
     */
    public Integer getCommitPeriod() {
        return commitPeriod;
    }

    /**
     * Sets the commit period for materialized triples.
     * <p>
     * Useful for limiting the back-end's memory usage during a large materialization.
     * When periodInTriples is null (the default), the server's default setting is used.
     * <p>
     * See also the HTTP protocol documentation for
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-materialize-entailed">
     * materializing entailed triples</a>.
     *
     * @param periodInTriples commit every time this many triples are added
     */
    public void setCommitPeriod(final Integer periodInTriples) {
        commitPeriod = periodInTriples;
    }

    /**
     * Gets the useTypeSubproperty flag.
     * <p>
     * See also the HTTP protocol documentation for
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-materialize-entailed">
     * materializing entailed triples</a>.
     *
     * @return the current setting
     */
    public Boolean getUseTypeSubproperty() {
        return useTypeSubproperty;
    }

    /**
     * Sets the useTypeSubproperty flag.
     * <p>
     * When use is null (the default), the server's default setting is used.
     * <p>
     * See also the HTTP protocol documentation for
     * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-materialize-entailed">
     * materializing entailed triples</a>.
     *
     * @param use true when using this inference
     */
    public void setUseTypeSubproperty(Boolean use) {
        useTypeSubproperty = use;
    }

    /**
     * Gets the graph the inferred triples will be placed in.
     * <p>
     * If the value is null (the default), triples will be placed in the default graph.
     *
     * @return Graph URI or null (meaning 'the default graph')
     */
    public Resource getInferredGraph() {
        return inferredGraph;
    }

    /**
     * Sets the the graph the inferred triples will be placed in.
     *
     * @param inferredGraph Graph URI or null (meaning 'the default graph')
     */
    public void setInferredGraph(final Resource inferredGraph) {
        this.inferredGraph = inferredGraph;
    }
}
