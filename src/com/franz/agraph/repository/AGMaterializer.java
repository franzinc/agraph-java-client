package com.franz.agraph.repository;

import java.util.ArrayList;
import java.util.List;

/**
 * A materializer governs how triples are inferred and added to a repository.
 * 
 * Note: The methods in this class are experimental and subject to change in
 * a future release.
 * 
 * @since v4.9
 *
 */
public class AGMaterializer {

	List<String> with; 
	List<String> without;
	Integer commitPeriod;
	Boolean useTypeSubproperty;

	/**
	 * Gets a default materializer that can be further configured.
	 * 
	 * See also the HTTP protocol documentation for
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-materialize-entailed">
	 * materializing entailed triples</a>.
	 * 
	 * @return a default materializer.
	 */
	static public AGMaterializer newInstance() {
		return new AGMaterializer();
	}
	
	protected AGMaterializer() {
		with = new ArrayList<String>();
		without = new ArrayList<String>();
		commitPeriod = null; // use the server's default
		useTypeSubproperty = null; // use the server's default
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
	public void withRuleset(String ruleset) {
		with.add(ruleset);
	}

	/**
	 * Gets the included rulesets.
	 *  
	 * @return the with rulesets
	 */
	public List<String> getWithRulesets() {
		return with;
	}
	
	/**
	 * Excludes a ruleset from materialization.
	 * 
	 * See also the HTTP protocol documentation for
	 * <a href="http://www.franz.com/agraph/support/documentation/current/http-protocol.html#put-materialize-entailed">
	 * materializing entailed triples</a>.
	 * 
	 * @param ruleset the name of the ruleset to exclude.
	 */
	public void withoutRuleset(String ruleset) {
		without.add(ruleset);
	}
	
	/**
	 * Gets the excluded rulesets.
	 *  
	 * @return the without rulesets
	 */
	public List<String> getWithoutRulesets() {
		return without;
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
	public void setCommitPeriod(Integer periodInTriples) {
		commitPeriod = periodInTriples;
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
	 * Sets the useTypeSubproperty flag.
	 * <p>
	 * When use is null (the default), the server's default setting is used. 
	 * 
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
	
}

