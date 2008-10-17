/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import org.openrdf.model.BNode;

/**
 * An implementation of the {@link BNode} interface.
 * 
 * @author Arjohn Kampman
 */
public class BNodeImpl implements BNode {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 5273570771022125970L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The blank node's identifier.
	 */
	private String id;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new, unitialized blank node. This blank node's ID needs to be
	 * {@link #setID(String) set} before the normal methods can be used.
	 */
	protected BNodeImpl() {
	}

	/**
	 * Creates a new blank node with the supplied identifier.
	 * 
	 * @param id
	 *        The identifier for this blank node, must not be <tt>null</tt>.
	 */
	public BNodeImpl(String id) {
		this();
		setID(id);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getID() {
		return id;
	}

	protected void setID(String id) {
		this.id = id;
	}

	public String stringValue() {
		return id;
	}

	// Overrides Object.equals(Object), implements BNode.equals(Object)
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof BNode) {
			BNode otherNode = (BNode)o;
			return this.getID().equals(otherNode.getID());
		}

		return false;
	}

	// Overrides Object.hashCode(), implements BNode.hashCode()
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	// Overrides Object.toString()
	@Override
	public String toString() {
		return "_:" + id;
	}
}
