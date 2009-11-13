package com.franz.ag;

import java.util.ArrayList;


/**
 * Instances of this class define mappings from prefixes or abbreviations to full
 * URI fragments.
 * 
 * @author mm
 *
 */
public class NamespaceRegistry {
	
	//FIXME
	// THIS CLASS CAN BE ELIMINATED by merging down to the subclass
	// in com.franz.agbase when com.franz.ag package is discarded.
	//
	// Since the constructors in package ag are public, we cannot ensure that
	// all instances are agbase instances.  Therefore, make sure that all declarations
	// in agbase code refer to the ag.NamespaceRegistry declaration.

	
	private ArrayList<Object> regs = new ArrayList<Object>();
	
	/**
	 * Create a new empty NamaspaceRegistry instance.
	 *
	 */
	public NamespaceRegistry () {}
	
	/**
	 * Create a new NamaspaceRegistry instance containing the same definitions
	 * as another.
	 * @param ns the NamaspaceRegistry instance from which definitions are copied.
	 */
	public NamespaceRegistry ( NamespaceRegistry ns ) {
		register(ns);
	}
	
	/**
	 * Create a new NamaspaceRegistry instance containing the definitions
	 * specified in an array of strings.
	 * @param defs an array of alternating prefix and fragment strings.
	 */
	public NamespaceRegistry ( String[] defs ) {
		register(defs);
	}
	
	
	
	
	private static final String[] preDefs = new String[] {
			"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
			"rdfs", "http://www.w3.org/2000/01/rdf-schema#",
			"owl", "http://www.w3.org/2002/07/owl#"
	};
	
	/**
	 *  A pre-defined namespace registry with three definitions:
	 *   <pre>
	 *   {
	 *   	"rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
	 *   	"rdfs", "http://www.w3.org/2000/01/rdf-schema#",
	 *   	"owl", "http://www.w3.org/2002/07/owl#"
	 *    }
	 *   </pre>
	 */
	public static final NamespaceRegistry RDFandOwl = new com.franz.agbase.NamespaceRegistry(preDefs);
	
	
//      register(prefix, uri) - add entry to collection or rplace
//                                existing entry
//                              if uri is null, delete the entry
//      clear() - delete all entries
//      getPrefixes()   -> array of prefixes
//      getURI(prefix)     -> uri string

	/**
	 * Add one prefix definition to this instance.
	 * @param prefix the prefix string
	 * @param uri the associated URI fragment
	 */
	public void register ( String prefix, String uri ) {
		boolean found = false;
		find:
			for (int i = 0; i < regs.size(); i=i+2) {
				String e = (String) regs.get(i);
				if ( e.equals(prefix) ) 
				{
					if ( uri==null )
					{
						regs.remove(i); regs.remove(i);
					}
					else
						regs.set(i+1, uri); 
					found = true;
					break find;
				}
			}
		if ( !found ) 
		{
			regs.add(prefix);  regs.add(uri);
		}
	}
	
	/**
	 * Add all the definitions in some other instance to this one.
	 * @param moreDefs the source instance
	 */
	public void register ( NamespaceRegistry moreDefs ) {
		ArrayList<Object> defs = moreDefs.regs;
		for (int i = 0; i < defs.size(); i=i+2) {
			register((String)defs.get(i), (String)defs.get(i+1));
		}
	}
	
	/**
	 * Add all the definitions in an array to this instance.
	 * @param moreDefs and alternating array of prefix strings and URI fragments
	 */
	public void register ( String[] moreDefs ) {
		for (int i = 0; i < moreDefs.length; i=i+2)
			register(moreDefs[i], moreDefs[i+1]);
	}
		
	
	/**
	 * Query the URI fragment associated with some prefix.
	 * @param prefix the prefix string
	 * @return the URI fragment
	 * @throws UndefinedPrefix
	 */
	public String getURI ( String prefix ) throws UndefinedPrefix {
		for (int i = 0; i < regs.size(); i=i+2) {
			String e = (String) regs.get(i);
			if ( e.equals(prefix) ) 
				return (String) regs.get(i+1);
		}
		throw new UndefinedPrefix();
	}
	
	public static class UndefinedPrefix extends AllegroGraphException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3244320712290446205L;
		
	}
	
	/**
	 * Query the prefixes defined in this instance.
	 * @return an array of the prefixes.
	 */
	public String[] getPrefixes () {
		String[] prefixes = new String[(regs.size())/2];
		for (int i = 0; i < prefixes.length; i++) {
			prefixes[i] = (String) regs.get(2*i);
		}
		return prefixes;
	}
	
	/**
	 * Discard all the definitions in this instance.
	 *
	 */
	public void clear () {
		regs = new ArrayList<Object>();
	}
	
	/**
	 * Convert a namespace registry to an array of strings.
	 * @return an array of alternating abbreviation and URI strings
	 */
	public String[] toArray () {
		String[] out = new String[regs.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = (String) regs.get(i);
		}
		return out;
	}
	
	public com.franz.agbase.NamespaceRegistry promote () {
		if ( this instanceof com.franz.agbase.NamespaceRegistry )
			return (com.franz.agbase.NamespaceRegistry) this;
		return new com.franz.agbase.NamespaceRegistry(this);
	}

}
