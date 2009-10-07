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
	 * Relative location of the session service.
	 */
	public static final String SESSION = "session";
	
	/**
	 * Parameter name for the 'lifetime' parameter for sessions.
	 */
	public static final String LIFETIME_PARAM_NAME = "lifetime";
	
	/**
	 * Parameter name for the 'autoCommit' parameter for sessions.
	 */
	public static final String AUTOCOMMIT_PARAM_NAME = "autoCommit";
	
	/**
	 * Relative location of session close.
	 */
	public static final String CLOSE = "close";
	
	/**
	 * Relative location of the session ping service.
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
	 * Relative location of the autocommit service.
	 */
	public static final String AUTOCOMMIT = "autoCommit";
	
	/**
	 * Parameter name for the 'on' parameter of autoCommit.
	 */
	public static final String ON_PARAM_NAME = "on";
	
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

	public static final String getSessionURL(String serverLocation) {
		return serverLocation + "/" + SESSION;
	}
	
	public static final String getSessionCloseURL(String sessionRoot) {
		return sessionRoot + "/" + CLOSE;
	}
	
	public static final String getAutoCommitLocation(String sessionRoot) {
		return getSessionURL(sessionRoot) + "/" + AUTOCOMMIT;
	}
	
}
