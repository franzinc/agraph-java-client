/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import com.franz.util.Closeable;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.NiceIterator;

public class AGTripleIterator extends NiceIterator<Triple>
implements Closeable {
	
	private RepositoryResult<Statement> result;

	AGTripleIterator(RepositoryResult<Statement> result) {
		this.result = result;
	}

	@Override
	public void close() {
		try {
			result.close();
		} catch (RepositoryException e) {
			throw new RuntimeException(e);		
		}
	}

	@Override
	public boolean hasNext() {
		try {
			return result.hasNext();
		} catch (RepositoryException e) {
			throw new RuntimeException(e);		
		}
	}

	@Override
	public Triple next() {
		Triple tr;
		try {
			tr = AGNodeFactory.asTriple(result.next());
		} catch (RepositoryException e) {
			throw new RuntimeException(e);		
		}
		return tr;
	}

	@Override
	public void remove() {
		try {
			result.remove();
		} catch (RepositoryException e) {
			throw new RuntimeException(e);		
		}
	}
}
