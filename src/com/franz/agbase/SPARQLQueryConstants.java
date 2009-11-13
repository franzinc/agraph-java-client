package com.franz.agbase;

/**
 * Constants that identify various options in SPARQLQuery. 
 * @author mm
 *
 */
public interface SPARQLQueryConstants  {

	/**
	 * Identify the query engine used by the SPARQL query processor.
	 * See the AllegroGraph SPARQL documentation for details.
	 * @author mm
	 *
	 */
public static enum ENGINE {
		
		AG2("allegrograph-2"),
		
		ALGEBRA("algebra")
		;
		
		private String value;
		private ENGINE ( String value ) { this.value = value; }
		public String value () { return value; }
		public static final String attrName = "engine";  
	}

/**
 * Identify the query planner used by the SPARQL query processor.
 * See the AllegroGraph SPARQL documentation for details.
 * @author mm
 *
 */
public static enum PLANNER {
	
	IDENTITY("identity"),
	COVERAGE("coverage")
	;
	
	private String value;
	private PLANNER ( String value ) { this.value = value; }
	public String value () { return value; }
	public static final String attrName = "planner"; 
}

}
