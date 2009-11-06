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
	 * Parameter name for the 'file' parameter for statements.
	 */
	public static final String FILE_PARAM_NAME = "file";
	
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
	 * Parameter name for the 'on' parameter of autoCommit.
	 */
	public static final String FTI_PREDICATE_PARAM_NAME = "predicate";
	
	/**
	 * Relative location of the mapping service.
	 */
	public static final String MAPPING = "mapping";
	
	/**
	 * Relative location of the datatype mapping service.
	 */
	public static final String MAPPING_DATATYPE = "type";
	
	/**
	 * Parameter name for the 'type' parameter for datatype mapping.
	 */
	public static final String TYPE_PARAM_NAME = "type";
	
	/**
	 * Parameter name for the 'encoding' parameter for mappings.
	 */
	public static final String ENCODED_TYPE_PARAM_NAME = "encoding";
	
	/**
	 * Relative location of the predicate mapping service.
	 */
	public static final String MAPPING_PREDICATE = "predicate";
	
	/**
	 * Relative location of the federated service.
	 */
	public static final String FEDERATED = "federated";
	
	/**
	 * Parameter name for the 'url' parameter for federation.
	 */
	public static final String URL_PARAM_NAME = "url";
	
	/**
	 * Parameter name for the 'repo' parameter for federation
	 */
	public static final String REPO_PARAM_NAME = "repo";
	
	
	/**
	 * Location of the root catalog service
	 */
	public static final String getRootCatalogURL(String serverURL) {
		return serverURL;
	}
	
	/**
	 * Location of the federated pseudo-catalog service
	 */
	public static final String getFederatedCatalogURL(String serverURL) {
		return serverURL + "/" + FEDERATED;
	}
	
	/**
	 * Location of the named catalogs service
	 */
	public static final String getNamedCatalogsURL(String serverURL) {
		return serverURL + "/" + CATALOGS;
	}
	
	/**
	 * Location of a named catalog
	 */
	public static final String getNamedCatalogLocation(String serverURL, String catalogName) {
		return getNamedCatalogsURL(serverURL) + "/" + catalogName;
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

	public static String getFreetextLocation(String sessionRoot) {
		return sessionRoot + "/" + FREETEXT;
	}
	
	public static String getFreetextPredicatesLocation(String sessionRoot) {
		return getFreetextLocation(sessionRoot) + "/" + FTI_PREDICATES;
	}

	public static String getMappingLocation(String sessionRoot) {
		return sessionRoot + "/" + MAPPING;
	}
	
	public static String getDatatypeMappingLocation(String sessionRoot) {
		return getMappingLocation(sessionRoot) + "/" + MAPPING_DATATYPE;
	}
	
	public static String getPredicateMappingLocation(String sessionRoot) {
		return getMappingLocation(sessionRoot) + "/" + MAPPING_PREDICATE;
	}
	
	public static String getFederatedLocation(String serverRoot) {
		return serverRoot + "/" + FEDERATED;
	}
	
	public static String getFederationLocation(String serverRoot, String federationName) {
		return getFederatedLocation(serverRoot) + "/" + federationName;
	}

	public static String getFunctorLocation(String serverRoot) {
		return serverRoot + "/" + FUNCTOR;
	}

	public static String getNamedCatalogRepositoriesLocation(String catalogURL) {
		return catalogURL + "/" + REPOSITORIES;
	}

	public static String getRootCatalogRepositoriesLocation(String catalogURL) {
		return catalogURL + "/" + REPOSITORIES;
	}

	public static String getFederatedRepositoriesLocation(String catalogURL) {
		return catalogURL;
	}
	
}
