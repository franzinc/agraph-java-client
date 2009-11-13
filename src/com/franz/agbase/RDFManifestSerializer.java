package com.franz.agbase;

import com.franz.agbase.AllegroGraphException;



/**
 * The RDF Manifest serializer is described in more detail in the 
 *   AllegroGraph Reference Guide for Lisp users
 *   as the serialize-rdf-manifest function.
 *
 */
public class RDFManifestSerializer extends AllegroGraphSerializer {

	private boolean verbose;
	private boolean singleStream;

	protected Object run() throws AllegroGraphException {
		
		if ( null==destination ) throw new IllegalStateException
				("The destination must be a string containing a folder path.");

		Object[] args = new Object[] {
			"source-type", sourceType,
			"source-id", sourceId,
			"serialization", "rdf-manifest",
			"destination", destination,
			"verbosep", verbose?(new Integer(1)):null,
			"single-stream-p", singleStream?(new Integer(1)):null
		};
		return ag.verifyEnabled().serializeTriples(ag, args);
	
	}

	/**
	 * @return the singleStream
	 */
	public boolean isSingleStream() {
		return singleStream;
	}

	/**
	 * @param singleStream the singleStream to set
	 */
	public void setSingleStream(boolean singleStream) {
		this.singleStream = singleStream;
	}

	/**
	 * @return the verbose
	 */
	public boolean isVerbose() {
		return verbose;
	}

	/**
	 * @param verbose the verbose to set
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	
	
}
