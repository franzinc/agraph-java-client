package com.franz.agbase;

/**
 * This class holds a set of namespace abbreviation definitions.
 * @author mm
 *
 */
public class NamespaceRegistry extends com.franz.ag.NamespaceRegistry {
	
	/**
	 * Create a new empty NamaspaceRegistry instance.
	 *
	 */
	public NamespaceRegistry () { super(); }
	
	/**
	 * Create a new NamaspaceRegistry instance containing the same definitions
	 * as another.
	 * @param ns the NamaspaceRegistry instance from which definitions are copied.
	 */
	public NamespaceRegistry ( NamespaceRegistry ns ) { super(ns); }
	
	public NamespaceRegistry ( com.franz.ag.NamespaceRegistry ns ) { super(ns); }

	/**
	 * Create a new NamaspaceRegistry instance containing the definitions
	 * specified in an array of strings.
	 * @param defs an array of alternating prefix and fragment strings.
	 */
	public NamespaceRegistry ( String[] defs ) { super(defs); }
	
}
