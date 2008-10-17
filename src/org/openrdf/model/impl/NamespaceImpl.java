/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import org.openrdf.model.Namespace;

/**
 * A default implementation of the {@link Namespace} interface.
 */
public class NamespaceImpl implements Namespace {

	private static final long serialVersionUID = -5829768428912588171L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The namespace's prefix.
	 */
	private String prefix;

	/**
	 * The namespace's name.
	 */
	private String name;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new NamespaceImpl object.
	 * 
	 * @param prefix
	 *        The namespace's prefix.
	 * @param name
	 *        The namespace's name.
	 */
	public NamespaceImpl(String prefix, String name) {
		setPrefix(prefix);
		setName(name);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the prefix of the namespace.
	 * 
	 * @return prefix of the namespace
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Sets the prefix of the namespace.
	 * 
	 * @param prefix
	 *        The (new) prefix for this namespace.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Gets the name of the namespace.
	 * 
	 * @return name of the namespace
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the namespace.
	 * 
	 * @param name
	 *        The (new) name for this namespace.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns a string representation of the object.
	 * 
	 * @return String representation of the namespace
	 */
	@Override
	public String toString() {
		return prefix + " :: " + name;
	}
}
