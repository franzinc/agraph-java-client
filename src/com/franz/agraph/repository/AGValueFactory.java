/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
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
import com.franz.agraph.http.exception.AGHttpException;
import com.hp.hpl.jena.graph.Node;

/**
 * Implements the Sesame ValueFactory interface for AllegroGraph.
 * 
 */
public class AGValueFactory extends ValueFactoryImpl {

	private final AGRepository repository;
	private final AGRepositoryConnection conn;
	
	private int blankNodeAmount = 100;
	private String[] blankNodeIds;
	private int index = -1;
	
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
	
	private void getBlankNodeIds() {
		try {
			if (conn == null) {
				blankNodeIds = getHTTPClient().getBlankNodes(getRepository().getRepositoryURL(), blankNodeAmount);
			} else {
				blankNodeIds = conn.getHttpRepoClient().getBlankNodes(blankNodeAmount);
			}
			index = blankNodeIds.length - 1;
		} catch (AGHttpException e) {
			// TODO: server's out of blank nodes?
			throw new RuntimeException(e);
		}		
	}
	
	String getNextBNodeId() {
		if (index==-1) {
			getBlankNodeIds();
		}
		String id = blankNodeIds[index];
		index--;
		// TODO: parse using NTriplesUtil here to create BNode?
		return id.substring(2);   // strip off leading '_:'; 
	}
	
	@Override
	public BNode createBNode(String nodeID) {
		if (nodeID == null || "".equals(nodeID))
			nodeID = getNextBNodeId();
		return super.createBNode(nodeID);
	}

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
			val = createBNode(node.getBlankNodeLabel());
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
