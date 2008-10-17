/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.query.Dataset;

/**
 * @author Arjohn Kampman
 */
public class DatasetImpl implements Dataset {

	protected Set<URI> defaultGraphs = new LinkedHashSet<URI>();

	protected Set<URI> namedGraphs = new LinkedHashSet<URI>();

	public DatasetImpl() {
	}

	public Set<URI> getDefaultGraphs() {
		return Collections.unmodifiableSet(defaultGraphs);
	}

	/**
	 * Adds a graph URI to the set of default graph URIs.
	 */
	public void addDefaultGraph(URI graphURI) {
		defaultGraphs.add(graphURI);
	}

	/**
	 * Removes a graph URI from the set of default graph URIs.
	 * 
	 * @return <tt>true</tt> if the URI was removed from the set,
	 *         <tt>false</tt> if the set did not contain the URI.
	 */
	public boolean removeDefaultGraph(URI graphURI) {
		return defaultGraphs.remove(graphURI);
	}

	/**
	 * Gets the (unmodifiable) set of named graph URIs.
	 */
	public Set<URI> getNamedGraphs() {
		return Collections.unmodifiableSet(namedGraphs);
	}

	/**
	 * Adds a graph URI to the set of named graph URIs.
	 */
	public void addNamedGraph(URI graphURI) {
		namedGraphs.add(graphURI);
	}

	/**
	 * Removes a graph URI from the set of named graph URIs.
	 * 
	 * @return <tt>true</tt> if the URI was removed from the set,
	 *         <tt>false</tt> if the set did not contain the URI.
	 */
	public boolean removeNamedGraph(URI graphURI) {
		return namedGraphs.remove(graphURI);
	}

	/**
	 * Removes all graph URIs (both default and named) from this dataset.
	 */
	public void clear() {
		defaultGraphs.clear();
		namedGraphs.clear();
	}

	@Override
	public String toString() {
		if (defaultGraphs.isEmpty() && namedGraphs.isEmpty())
			return "## empty dataset ##";
		StringBuilder sb = new StringBuilder();
		for (URI uri : defaultGraphs) {
			sb.append("FROM ");
			appendURI(sb, uri);
		}
		for (URI uri : namedGraphs) {
			sb.append("FROM NAMED ");
			appendURI(sb, uri);
		}
		return sb.toString();
	}

	private void appendURI(StringBuilder sb, URI uri) {
		String str = uri.toString();
		if (str.length() > 50) {
			sb.append("<").append(str, 0, 19).append("..");
			sb.append(str, str.length() - 29, str.length()).append(">\n");
		} else {
			sb.append("<").append(uri).append(">\n");
		}
	}
}
