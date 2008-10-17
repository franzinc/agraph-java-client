/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.impl;

import org.openrdf.query.Binding;

import org.openrdf.model.Value;

/**
 * An implementation of the {@link Binding} interface.
 */
public class BindingImpl implements Binding {

	private String name;

	private Value value;

	/**
	 * Creates a binding object with the supplied name and value.
	 * 
	 * @param name
	 *        The binding's name.
	 * @param value
	 *        The binding's value.
	 */
	public BindingImpl(String name, Value value) {
		assert name != null : "name must not be null";
		assert value != null : "value must not be null";

		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Value getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Binding) {
			Binding other = (Binding)o;

			return name.equals(other.getName()) && value.equals(other.getValue());
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode() ^ value.hashCode();
	}

	@Override
	public String toString()
	{
		return name + "=" + value.toString();
	}
}
