/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;

/**
 * An immutable empty BindingSet.
 * 
 * @author Arjohn Kampman
 */
public class EmptyBindingSet implements BindingSet {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final EmptyBindingSet singleton = new EmptyBindingSet();

	public static BindingSet getInstance() {
		return singleton;
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	private EmptyBindingIterator iter = new EmptyBindingIterator();

	/*---------*
	 * Methods *
	 *---------*/

	public Iterator<Binding> iterator() {
		return iter;
	}

	public Set<String> getBindingNames() {
		return Collections.emptySet();
	}

	public Binding getBinding(String bindingName) {
		return null;
	}

	public boolean hasBinding(String bindingName) {
		return false;
	}

	public Value getValue(String bindingName) {
		return null;
	}

	public int size() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BindingSet) {
			return ((BindingSet)o).size() == 0;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return "[]";
	}

	/*----------------------------------*
	 * Inner class EmptyBindingIterator *
	 *----------------------------------*/

	private static class EmptyBindingIterator implements Iterator<Binding> {

		public boolean hasNext() {
			return false;
		}

		public Binding next() {
			throw new NoSuchElementException();
		}

		public void remove() {
			throw new IllegalStateException();
		}
	}
}
