/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;

/**
 * A Map-based implementation of the {@link BindingSet} interface.
 */
public class MapBindingSet implements BindingSet {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Map<String, Binding> bindings;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public MapBindingSet() {
		this(8);
	}

	/**
	 * Creates a new Map-based BindingSet with the specified initial capacity.
	 * Bindings can be added to this solution using the {@link #addBinding}
	 * methods.
	 * 
	 * @param capacity
	 *        The initial capacity of the created BindingSet object.
	 */
	public MapBindingSet(int capacity) {
		// Create bindings map, compensating for HashMap's load factor
		bindings = new LinkedHashMap<String, Binding>(capacity * 2);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Adds a binding to the solution.
	 * 
	 * @param name
	 *        The binding's name.
	 * @param value
	 *        The binding's value.
	 */
	public void addBinding(String name, Value value) {
		addBinding(new BindingImpl(name, value));
	}

	/**
	 * Adds a binding to the solution.
	 * 
	 * @param binding
	 *        The binding to add to the solution.
	 */
	public void addBinding(Binding binding) {
		bindings.put(binding.getName(), binding);
	}

	/**
	 * Removes a binding from the solution.
	 * 
	 * @param name
	 *        The binding's name.
	 */
	public void removeBinding(String name) {
		bindings.remove(name);
	}

	public Iterator<Binding> iterator() {
		return bindings.values().iterator();
	}

	public Set<String> getBindingNames() {
		return bindings.keySet();
	}

	public Binding getBinding(String bindingName) {
		return bindings.get(bindingName);
	}

	public boolean hasBinding(String bindingName) {
		return bindings.containsKey(bindingName);
	}

	public Value getValue(String bindingName) {
		Binding binding = getBinding(bindingName);

		if (binding != null) {
			return binding.getValue();
		}

		return null;
	}

	public int size() {
		return bindings.size();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		else if (other instanceof BindingSet) {
			int otherSize = 0;

			// Compare other's bindings to own
			for (Binding binding : (BindingSet)other) {
				Value ownValue = getValue(binding.getName());

				if (!binding.getValue().equals(ownValue)) {
					// Unequal bindings for this name
					return false;
				}

				otherSize++;
			}

			// All bindings have been matched, sets are equal if this solution
			// doesn't have any additional bindings.
			return otherSize == bindings.size();
		}

		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;

		for (Binding binding : this) {
			hashCode ^= binding.hashCode();
		}

		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(32 * size());

		sb.append('[');

		Iterator<Binding> iter = iterator();
		while (iter.hasNext()) {
			sb.append(iter.next().toString());
			if (iter.hasNext()) {
				sb.append(';');
			}
		}

		sb.append(']');

		return sb.toString();
	}
}
