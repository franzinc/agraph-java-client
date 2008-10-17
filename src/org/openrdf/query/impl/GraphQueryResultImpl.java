/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2006-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.CloseableIteratorIteration;
import info.aduna.iteration.IterationWrapper;

import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;

/**
 * An utility implementation of the {@link GraphQueryResult} interface.
 * 
 * @author Arjohn Kampman
 * @author jeen
 */
public class GraphQueryResultImpl extends IterationWrapper<Statement, QueryEvaluationException> implements
		GraphQueryResult
{

	/*-----------*
	 * Variables *
	 *-----------*/

	private Map<String, String> namespaces;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public GraphQueryResultImpl(Map<String, String> namespaces, Iterable<? extends Statement> statements) {
		this(namespaces, statements.iterator());
	}

	public GraphQueryResultImpl(Map<String, String> namespaces, Iterator<? extends Statement> statementIter) {
		this(namespaces, new CloseableIteratorIteration<Statement, QueryEvaluationException>(statementIter));
	}

	public GraphQueryResultImpl(Map<String, String> namespaces,
			CloseableIteration<? extends Statement, ? extends QueryEvaluationException> statementIter)
	{
		super(statementIter);
		this.namespaces = Collections.unmodifiableMap(namespaces);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Map<String, String> getNamespaces() {
		return namespaces;
	}
}
