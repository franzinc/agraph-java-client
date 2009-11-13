package com.franz.ag.repository;

import info.aduna.iteration.ExceptionConvertingIteration;
import info.aduna.iteration.Iteration;

import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.SailException;

public class AGCloseableIteration<E> extends
		ExceptionConvertingIteration<E, RepositoryException> {

	public AGCloseableIteration(
			Iteration<? extends E, ? extends SailException> iter) {
		super(iter);
	}

	@Override
	protected RepositoryException convert(Exception e) {
		if (e instanceof SailException) {
			return new RepositoryException(e);
		} else if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		} else if (e == null) {
			throw new IllegalArgumentException("e must not be null");
		} else {
			throw new IllegalArgumentException("Unexpected exception type: "
					+ e.getClass());
		}
	}
}

