/**
 * 
 */
package com.franz.agraph.http;

import org.openrdf.http.protocol.Protocol;

/**
 *
 */
public class AGProtocol extends Protocol {

	/**
	 * Relative location of the catalogs service.
	 */
	public static final String CATALOGS = "catalogs";
	
	/**
	 * Relative location of the version service.
	 */
	public static final String VERSION = "version";
	
	/**
	 * Relative location of the reconfigure service.
	 */
	public static final String RECONFIGURE = "reconfigure";
	
	/**
	 * Relative location of the users service.
	 */
	public static final String USERS = "users";
	
	/**
	 * Relative location of the user roles service.
	 */
	public static final String USER_ROLES = "roles";
	
	/**
	 * Relative location of the user permissions service.
	 */
	public static final String USER_PERMISSIONS = "permissions";
	
	/**
	 * Relative location of the user data service.
	 */
	public static final String USER_DATA = "data";
	
	/**
	 * Relative location of the backends service.
	 */
	public static final String BACKENDS = "backends";
	
	/**
	 * Parameter name for the 'lifetime' parameter for backends.
	 */
	public static final String LIFETIME_PARAM_NAME = "lifetime";
	
	/**
	 * Header name for the 'backendid' request header.
	 */
	public static final String X_BACKEND_ID = "X-Backend-ID";
	
	/**
	 * Relative location of the backends ping service.
	 */
	public static final String PING = "ping";
	
	/**
	 * Relative location of the blank nodes service.
	 */
	public static final String BLANK_NODES = "blankNodes";
	
	/**
	 * Parameter name for the 'amount' parameter of blank node service.
	 */
	public static final String AMOUNT_PARAM_NAME = "amount";

	/**
	 * Relative location of the commit service.
	 */
	public static final String COMMIT = "commit";
	
	/**
	 * Relative location of the rollback service.
	 */
	public static final String ROLLBACK = "rollback";
	
	/**
	 * Relative location of the eval service.
	 */
	public static final String EVAL = "eval";
	
	/**
	 * Relative location of the functor service.
	 */
	public static final String FUNCTOR = "functor";
	
	/**
	 * Relative location of the freetext service.
	 */
	public static final String FREETEXT = "freetext";
	
	/**
	 * Relative location of the freetext predicates service.
	 */
	public static final String FTI_PREDICATES = "predicates";
	
	/**
	 * Relative location of the mapping service.
	 */
	public static final String MAPPING = "mapping";
	
	/**
	 * Relative location of the datatype mapping service.
	 */
	public static final String MAPPING_DATATYPE = "type";
	
	/**
	 * Relative location of the predicate mapping service.
	 */
	public static final String MAPPING_PREDICATE = "predicate";
	
	
	/**
	 * Location of the catalogs service
	 */
	public static final String getCatalogsURL(String serverURL) {
		return serverURL + "/" + CATALOGS;
	}
	
	/**
	 * Get the location of the blank nodes service for a repository
	 * 
	 * @param repositoryLocation
	 *        the base location of the repository.
	 * @return the location of the blank nodes service
	 */
	public static final String getBlankNodesURL(String repositoryLocation) {
		return repositoryLocation + "/" + BLANK_NODES;
	}

	/**
	 * Get the location of the blank nodes service for a repository
	 * 
	 * @param repositoryLocation
	 *        the base location of the repository.
	 * @return the location of the blank nodes service
	 */
	public static final String getBackendsURL(String serverLocation) {
		return serverLocation + "/" + BACKENDS;
	}
	
}
