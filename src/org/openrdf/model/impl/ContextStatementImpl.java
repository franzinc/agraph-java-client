/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.model.impl;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * An extension of {@link StatementImpl} that adds a context field.
 */
public class ContextStatementImpl extends StatementImpl {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -4747275587477906748L;

	/**
	 * The statement's context, if applicable.
	 */
	private final Resource context;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new Statement with the supplied subject, predicate and object
	 * for the specified associated context.
	 * 
	 * @param subject
	 *        The statement's subject, must not be <tt>null</tt>.
	 * @param predicate
	 *        The statement's predicate, must not be <tt>null</tt>.
	 * @param object
	 *        The statement's object, must not be <tt>null</tt>.
	 * @param context
	 *        The statement's context, <tt>null</tt> to indicate no context is
	 *        associated.
	 */
	public ContextStatementImpl(Resource subject, URI predicate, Value object, Resource context) {
		super(subject, predicate, object);
		this.context = context;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Resource getContext()
	{
		return context;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(256);

		sb.append(super.toString());
		sb.append(" [").append(getContext()).append("]");

		return sb.toString();
	}
}
