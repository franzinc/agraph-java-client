/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * An implementation of the {@link Statement} interface for statements that
 * don't have an associated context. For statements that do have an associated
 * context, {@link ContextStatementImpl} can be used.
 */
public class StatementImpl implements Statement {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 8707542157460228077L;

	/**
	 * The statement's subject.
	 */
	private final Resource subject;

	/**
	 * The statement's predicate.
	 */
	private final URI predicate;

	/**
	 * The statement's object.
	 */
	private final Value object;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new Statement with the supplied subject, predicate and object.
	 * 
	 * @param subject
	 *        The statement's subject, must not be <tt>null</tt>.
	 * @param predicate
	 *        The statement's predicate, must not be <tt>null</tt>.
	 * @param object
	 *        The statement's object, must not be <tt>null</tt>.
	 */
	public StatementImpl(Resource subject, URI predicate, Value object) {
		assert (subject != null);
		assert (predicate != null);
		assert (object != null);

		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	/*---------*
	 * Methods *
	 *---------*/

	// Implements Statement.getSubject()
	public Resource getSubject() {
		return subject;
	}

	// Implements Statement.getPredicate()
	public URI getPredicate() {
		return predicate;
	}

	// Implements Statement.getObject()
	public Value getObject() {
		return object;
	}

	// Implements Statement.getContext()
	public Resource getContext() {
		return null;
	}

	// Overrides Object.equals(Object), implements Statement.equals(Object)
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof Statement) {
			Statement otherSt = (Statement)other;

			// The object is potentially the cheapest to check, as types
			// of these references might be different.

			// In general the number of different predicates in sets of
			// statements is the smallest, so predicate equality is checked
			// last.
			return object.equals(otherSt.getObject()) && subject.equals(otherSt.getSubject())
					&& predicate.equals(otherSt.getPredicate());
		}

		return false;
	}

	// Overrides Object.hashCode(), implements Statement.hashCode()
	@Override
	public int hashCode() {
		return 961 * subject.hashCode() + 31 * predicate.hashCode() + object.hashCode();
	}

	/**
	 * Gives a String-representation of this Statement that can be used for
	 * debugging.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(256);

		sb.append("(");
		sb.append(getSubject());
		sb.append(", ");
		sb.append(getPredicate());
		sb.append(", ");
		sb.append(getObject());
		sb.append(")");

		return sb.toString();
	}
}
