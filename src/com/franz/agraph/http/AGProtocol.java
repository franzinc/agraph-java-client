/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

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
	 * Relative location to delete statements.
	 */
	public static final String DELETE = "delete";

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
	 * Parameter name for the 'predicate' parameter for freetext.
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
	 * Parameter name for the 'planner' to use during a query
	 */
	public static final String PLANNER_PARAM_NAME = "planner";
		
	/**
	 * Relative location of the Geo service.
	 */
	public static final String GEO = "geo";
	
	/**
	 * Relative location of the Geo Types service.
	 */
	public static final String TYPES = "types";
	
	/**
	 * Relative location of the Geo Types Cartesian service.
	 */
	public static final String CARTESIAN = "cartesian";
	
	/**
	 * Relative location of the Geo Types Spherical service.
	 */
	public static final String SPHERICAL = "spherical";
	
	/**
	 * Parameter name for the 'stripWidth' of a Geo type
	 */
	public static final String STRIP_WIDTH_PARAM_NAME = "stripWidth";
	
	/**
	 * Parameter name for the 'xmin' of a Geo type
	 */
	public static final String XMIN_PARAM_NAME = "xmin";
	
	/**
	 * Parameter name for the 'xmax' of a Geo type
	 */
	public static final String XMAX_PARAM_NAME = "xmax";
	
	/**
	 * Parameter name for the 'ymin' of a Geo type
	 */
	public static final String YMIN_PARAM_NAME = "ymin";
	
	/**
	 * Parameter name for the 'ymax' of a Geo type
	 */
	public static final String YMAX_PARAM_NAME = "ymax";
	
	/**
	 * Parameter name for the 'latmin' of a Geo type
	 */
	public static final String LATMIN_PARAM_NAME = "latmin";
	
	/**
	 * Parameter name for the 'longmin' of a Geo type
	 */
	public static final String LONGMIN_PARAM_NAME = "longmin";
	
	/**
	 * Parameter name for the 'latmax' of a Geo type
	 */
	public static final String LATMAX_PARAM_NAME = "latmax";
	
	/**
	 * Parameter name for the 'longmax' of a Geo type
	 */
	public static final String LONGMAX_PARAM_NAME = "longmax";
	
	/**
	 * Parameter name for the 'unit' of a Geo type
	 */
	public static final String UNIT_PARAM_NAME = "unit";
	
	/**
	 * Parameter value 'degree' for the 'unit' of a Geo type
	 */
	public static final String DEGREE_PARAM_VALUE = "degree";
	
	/**
	 * Parameter value 'radian' for the 'unit' of a Geo type
	 */
	public static final String RADIAN_PARAM_VALUE = "radian";
	
	/**
	 * Parameter value 'km' for the 'unit' of a Geo type
	 */
	public static final String KM_PARAM_VALUE = "km";
	
	/**
	 * Parameter value 'mile' for the 'unit' of a Geo type
	 */
	public static final String MILE_PARAM_VALUE = "mile";
	
	/**
	 * Parameter name for the 'limit' on results returned.
	 */
	public static final String LIMIT_PARAM_NAME = "limit";
	
	/**
	 * Relative location of the Geo Box service.
	 */
	public static final String BOX = "box";
	
	/**
	 * Parameter name for the 'predicate' to search for in geo searches.
	 */
	public static final String GEO_PREDICATE_PARAM_NAME = "predicate";
	
	/**
	 * Relative location of the Geo Circle service.
	 */
	public static final String CIRCLE = "circle";
	
	/**
	 * Parameter name for the 'x' ordinate of a circle
	 */
	public static final String X_PARAM_NAME = "x";
	
	/**
	 * Parameter name for the 'y' ordinate of a circle
	 */
	public static final String Y_PARAM_NAME = "y";
	
	/**
	 * Parameter name for the 'radius' of a circle
	 */
	public static final String RADIUS_PARAM_NAME = "radius";
	
	/**
	 * Relative location of the Geo Haversine service.
	 */
	public static final String HAVERSINE = "haversine";
	
	/**
	 * Parameter name for the 'lat' ordinate of a haversine
	 */
	public static final String LAT_PARAM_NAME = "lat";
	
	/**
	 * Parameter name for the 'lon' ordinate of a haversine
	 */
	public static final String LON_PARAM_NAME = "long";
	
	/**
	 * Relative location of the Geo Polygon registration service.
	 */
	public static final String POLYGON = "polygon";
	
	/**
	 * Parameter name 'resource' for the polygon being registered
	 */
	public static final String RESOURCE_PARAM_NAME = "resource";
	
	/**
	 * Parameter name for the 'polygon' being referenced
	 */
	public static final String POLYGON_PARAM_NAME = "polygon";
	
	/**
	 * Parameter name for the 'point' of a polygon
	 */
	public static final String POINT_PARAM_NAME = "point";
	
	/**
	 * Relative location of the SNA Generators registration service.
	 */
	public static final String SNA_GENERATORS = "snaGenerators";
	
	/**
	 * Parameter name for the 'objectOf' predicates in the generator
	 */
	public static final String OBJECTOF_PARAM_NAME = "objectOf";
	
	/**
	 * Parameter name for the 'subjectOf' predicates in the generator
	 */
	public static final String SUBJECTOF_PARAM_NAME = "subjectOf";
	
	/**
	 * Parameter name for the 'undirected' predicates in the generator
	 */
	public static final String UNDIRECTED_PARAM_NAME = "undirected";
	
	/**
	 * Relative location of the SNA Neighbor Matrices registration service.
	 */
	public static final String NEIGHBOR_MATRICES = "neighborMatrices";
	
	/**
	 * Parameter name for the 'generator' for the neighbor matrix
	 */
	public static final String GENERATOR_PARAM_NAME = "generator";
	
	/**
	 * Parameter name for the 'group' used in seeding a neighbor matrix
	 */
	public static final String GROUP_PARAM_NAME = "group";
	
	/**
	 * Parameter name for the 'depth' for the neighbor matrix
	 */
	public static final String DEPTH_PARAM_NAME = "depth";
	
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

	public static final String getSessionCloseLocation(String sessionRoot) {
		return getSessionURL(sessionRoot) + "/" + CLOSE;
	}

	public static final String getSessionPingLocation(String sessionRoot) {
		return getSessionURL(sessionRoot) + "/" + PING;
	}

	public static final String getAutoCommitLocation(String sessionRoot) {
		return getSessionURL(sessionRoot) + "/" + AUTOCOMMIT;
	}

	public static String getStatementsDeleteLocation(String sessionRoot) {
		return getStatementsLocation(sessionRoot) + "/" + DELETE;
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

	public static String getEvalLocation(String sessionRoot) {
		return sessionRoot + "/" + EVAL;
	}

	public static String getGeoLocation(String sessionRoot) {
		return sessionRoot + "/" + GEO;
	}
	
	public static String getGeoTypesLocation(String sessionRoot) {
		return getGeoLocation(sessionRoot) + "/" + TYPES;
	}
	
	public static String getGeoTypesCartesianLocation(String sessionRoot) {
		return getGeoTypesLocation(sessionRoot) + "/" + CARTESIAN;
	}
	
	public static String getGeoTypesSphericalLocation(String sessionRoot) {
		return getGeoTypesLocation(sessionRoot) + "/" + SPHERICAL;
	}

	public static String getGeoBoxLocation(String sessionRoot) {
		return getGeoLocation(sessionRoot) + "/" + BOX;
	}
	
	public static String getGeoCircleLocation(String sessionRoot) {
		return getGeoLocation(sessionRoot) + "/" + CIRCLE;
	}
	
	public static String getGeoHaversineLocation(String sessionRoot) {
		return getGeoLocation(sessionRoot) + "/" + HAVERSINE;
	}
	
	public static String getGeoPolygonLocation(String sessionRoot) {
		return getGeoLocation(sessionRoot) + "/" + POLYGON;
	}
	
	public static String getSNAGeneratorsLocation(String sessionRoot) {
		return sessionRoot + "/" + SNA_GENERATORS;
	}

	public static String getSNAGeneratorLocation(String sessionRoot,
			String generator) {
		return getSNAGeneratorsLocation(sessionRoot) + "/" + generator;
	}

	public static String getSNANeighborMatricesLocation(String sessionRoot) {
		return sessionRoot + "/" + NEIGHBOR_MATRICES;
	}

	public static String getSNANeighborMatrixLocation(String sessionRoot,
			String matrix) {
		// TODO Auto-generated method stub
		return getSNANeighborMatricesLocation(sessionRoot) + "/" + matrix;
	}
	
}
