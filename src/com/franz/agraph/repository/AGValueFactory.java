/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.ntriples.NTriplesUtil;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGHttpException;
import com.hp.hpl.jena.graph.Node;

/**
 * Implements the Sesame ValueFactory interface for AllegroGraph.
 * 
 */
public class AGValueFactory extends ValueFactoryImpl {

	private final AGRepository repository;
	private final AGRepositoryConnection conn;
	
	private int blankNodesPerRequest = Integer.parseInt(System.getProperty("com.franz.agraph.repository.blankNodesPerRequest", "100"));
	private String[] blankNodeIds;
	private int index = -1;
	
	public String PREFIX_FOR_EXTERNAL_BNODES = "urn:x-bnode:";

	public AGValueFactory(AGRepository repository) {
		super();
		this.repository = repository;
		this.conn = null;
	}
	
	public AGValueFactory(AGRepository repository, AGRepositoryConnection conn) {
		super();
		this.repository = repository;
		this.conn = conn;
	}
	
	public AGRepository getRepository() {
		return repository;
	}
	
	public AGHTTPClient getHTTPClient() {
		return getRepository().getHTTPClient();
	}
	
	private void requestBlankNodeIds() {
		try {
			if (conn == null) {
				blankNodeIds = getHTTPClient().getBlankNodes(getRepository().getRepositoryURL(), blankNodesPerRequest);
			} else {
				blankNodeIds = conn.getHttpRepoClient().getBlankNodes(blankNodesPerRequest);
			}
			index = blankNodeIds.length - 1;
		} catch (AGHttpException e) {
			// TODO: server's out of blank nodes?
			throw new RuntimeException(e);
		}		
	}
	
	/**
	 * Sets the number of blank nodes to fetch per request.
	 * <p>
	 * This can be used to control the number and frequency of 
	 * HTTP requests made when automatically obtaining new sets
	 * of blank node ids from the server.
	 * <p>
	 * Defaults to the value of System property 
	 * com.franz.agraph.repository.blankNodesPerRequest 
	 * or to 100 if that property has not been set.
	 *   
	 * @param amount
	 */
	public void setBlankNodesPerRequest(int amount) {
		blankNodesPerRequest=amount;
	}
	
	/**
	 * Gets the number of blank nodes fetched per request.
	 * 
	 * @return the number of blank nodes fetched per request.
	 */
	public int getBlankNodesPerRequest() {
		return blankNodesPerRequest;
	}
	
	/**
	 * Returns the array of fetched blank node ids
	 * 
	 * Primarily for testing purposes, not for use in apps.
	 * 
	 * @return the array of fetched blank node ids
	 */
	public String[] getBlankNodeIds() {
		return blankNodeIds;
	}
	
	String getNextBNodeId() {
		if (index==-1) {
			requestBlankNodeIds();
		}
		String id = blankNodeIds[index];
		index--;
		// TODO: parse using NTriplesUtil here to create BNode?
		return id.substring(2);   // strip off leading '_:'; 
	}
	
	/**
	 * Returns a new blank node with the given id.
	 * 
	 * Consider using createBNode() instead to get an AG-allocated id,
	 * it is safer (avoids unintended blank node conflicts) and can be
	 * stored more efficiently.
	 * 
	 * If id is null or empty, returns a unique BNode with AG-allocated
	 * id; otherwise, returns a BNode with the given (a.k.a. "external")
	 * id (careful to avoid blank node conflicts).  See the javadoc for 
	 * allowing external blank nodes for more discussion.
	 *    
	 * @see AGHttpRepoClient#setAllowExternalBlankNodeIds(boolean)
	 * @see AGRepositoryConnection#getHttpRepoClient()
	 */
	@Override
	public BNode createBNode(String nodeID) {
		if (nodeID == null || "".equals(nodeID))
			nodeID = getNextBNodeId();
		return super.createBNode(nodeID);
	}

	/**
	 * Returns a new blank node.
	 * 
	 * If this value factory is for an AGRepository, returns a new BNode
	 * with an AG-allocated id; otherwise, returns a new BNode with an 
	 * "external" id (using ValueFactoryImpl).  See also the javadoc for 
	 * allowing external blank nodes for more discussion.
	 *    
	 * @see AGHttpRepoClient#setAllowExternalBlankNodeIds(boolean)
	 * @see AGRepositoryConnection#getHttpRepoClient()
	 */
	@Override
	public BNode createBNode() {
		if (repository instanceof AGRepository) {
			return createBNode(null);
		} else {
			return super.createBNode();
		}
	}

	/**
	 * Creates an OpenRDF Value from a concrete Jena Node.
	 *  
	 * @param node a concrete Jena node.
	 * @return the corresponding Value.
	 */
	public Value asValue(Node node) {
		Value val;
		if (node==null || node==Node.ANY) {
			val = null;
		} else if (node.isURI()) {
			val = createURI(node.getURI());
		} else if (node.isBlank()) {
			String id = node.getBlankNodeLabel();
			val = createBNode(id); 
		} else if (node.isLiteral()) {
			String lang = node.getLiteralLanguage();
			if (node.getLiteralDatatypeURI()!=null) {
				URI datatype = createURI(node.getLiteralDatatypeURI());
				val = createLiteral(node.getLiteralLexicalForm(), datatype);
			} else if (lang!=null && !lang.equals("")) {
				val = createLiteral(node.getLiteralLexicalForm(),lang);
			} else {
				// TODO
				val = createLiteral(node.getLiteralLexicalForm());
			}
		} else {
			throw new IllegalArgumentException("Cannot convert Node to Value: " + node);
		}
		return val;
	}

	/**
	 * Return true iff id looks like an AG blank node id.
	 * 
	 * AG blank node ids currently have the following form:
	 * 
	 * bF010696Fx1
	 * 
	 * The printing is _:b[store ID in hex]x[blank node number].
	 * There is nothing sacrosanct about this but it is unlikely
	 * to change.
	 *  
	 * @param id the string to be tested
	 * @return true iff id looks like an AG blank node id
	 */
	public boolean isAGBlankNodeId(String id) {
		boolean startsWithB = id.startsWith("b");
		if (!startsWithB) return false;
		// store id's are currently 8 chars
		boolean storeIdThenX = id.length()>9 ? id.charAt(9)=='x' : false;
		if (!storeIdThenX) return false;
		boolean endsWithNumber;
		try {
			Long.parseLong(id.substring(10));
			endsWithNumber = true;
		} catch (NumberFormatException e) {
			endsWithNumber = false;
		}
		return endsWithNumber;
	}

	public boolean isURIForExternalBlankNode(Value v) {
		return v.stringValue().startsWith(PREFIX_FOR_EXTERNAL_BNODES);
	}
	
	public Resource asResource(Node node) {
		Resource res;
		if (node==null || node==Node.ANY) {
			res = null;
		} else if (node.isURI()) {
			res = createURI(node.getURI());
		} else if (node.isBlank()) {
			res = createBNode(node.getBlankNodeLabel()); 
		} else {
			throw new IllegalArgumentException("Cannot convert Node to Resource: " + node);
		}
		return res;
	}

	public URI asURI(Node node) {
		URI uri;
		if (node==null || node==Node.ANY) {
			uri = null;
		} else if (node.isURI()) {
			uri = createURI(node.getURI());
		} else if (node.isBlank()) {
			// TODO: research this more, seems to be needed for the test 
			// suite, as blank nodes appear in the predicate position
			uri = createURI("http://anon/" + node.getBlankNodeLabel());
		} else {
			throw new IllegalArgumentException("Cannot convert Node to URI: " + node);
		}
		return uri;
	}
	
	
	/***********************
	 * 
	 * Encodable Namespaces
	 * 
	 ***********************/
	
	/**
	 * Returns unique URIs within the specified encodable namespace.
	 * 
	 * <p>The generated URIs will conform to the format that was specified
	 * when the encodable namespace was registered, and are guaranteed
	 * to be unique for this namespace generator.  Note that this does
	 * not prevent other parties from independently using URIs that
	 * involve this namespace, however.</p>
	 * 
	 * <p>If amount cannot be generated, up to amount URIs will be returned,
	 * or an exception will be thrown if none are available.</p>
	 *   
	 * @see AGRepositoryConnection#registerEncodableNamespace(String, String)
	 * @see #generateURI(String)
	 * 
	 * @return a unique URI within the specified namespace.
	 */
	public URI[] generateURIs(String namespace, int amount) throws RepositoryException {
		String[] uri_strs;
		URI[] uris;
		try {
			uri_strs = getHTTPClient().generateURIs(getRepository().getRepositoryURL(),namespace,amount);
			uris = new URI[uri_strs.length];
			for (int i=0;i<uri_strs.length;i++) {
				uris[i] = NTriplesUtil.parseURI(uri_strs[i],this);
			}
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
		return uris;
	}
	
	/**
	 * Returns a unique URI within the specified encodable namespace.
	 * 
	 * <p>The generated URI will conform to the format that was specified
	 * when the encodable namespace was registered, and is guaranteed
	 * to be unique for this namespace generator.  Note that this does
	 * not prevent other parties from independently using URIs that
	 * involve this namespace, however.</p>
	 * 
	 * @see AGRepositoryConnection#registerEncodableNamespace(String, String)
	 * @see #generateURIs(String, int)
	 * 
	 * @return a unique URI within the specified namespace.
	 */
	public URI generateURI(String registeredEncodableNamespace) throws RepositoryException {
		return generateURIs(registeredEncodableNamespace,1)[0];
	}

}
