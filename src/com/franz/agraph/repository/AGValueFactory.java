/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import static com.franz.agraph.http.AGProtocol.AMOUNT_PARAM_NAME;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.openrdf.http.protocol.UnauthorizedException;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGProtocol;
import com.franz.agraph.http.AGResponseHandler;
import com.hp.hpl.jena.graph.Node;

/**
 * Implements the Sesame ValueFactory interface for AllegroGraph.
 * 
 */
public class AGValueFactory extends ValueFactoryImpl {

	private final AGRepository repository;
	
	private int blankNodeAmount = 100;
	private String[] blankNodeIds;
	private int index = -1;
	
	public AGValueFactory(AGRepository repository) {
		super();
		this.repository = repository;
		blankNodeIds = new String[blankNodeAmount];
	}
	
	public AGRepository getRepository() {
		return repository;
	}
	
	public AGHTTPClient getHTTPClient() {
		return getRepository().getHTTPClient();
	}
	
	private void getBlankNodeIds() {
		try {
			blankNodeIds = getHTTPClient().getBlankNodes(getRepository().getRepositoryURL(),blankNodeAmount);
			index = blankNodeIds.length - 1;
		} catch (UnauthorizedException e) {
			// TODO: check on the proper exceptions to throw here
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (RepositoryException e) {
			throw new IllegalStateException(e);
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
		return createBNode(null);
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
	
	private String[] generateURIs(String namespace, int amount) {
		String[] uris;
		try {
			uris = getHTTPClient().generateURIs(getRepository().getRepositoryURL(),namespace,amount);
		} catch (UnauthorizedException e) {
			// TODO: check on the proper exceptions to throw here
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (RepositoryException e) {
			throw new IllegalStateException(e);
		}		
		return uris;
	}
	
	String getNextURI(String namespace) {
		if (index==-1) {
			generateURIs(namespace, 100);
		}
		String id = blankNodeIds[index];
		index--;
		// TODO: parse using NTriplesUtil here to create BNode?
		return id.substring(2);   // strip off leading '_:'; 
	}
	
	/**
	 * Returns a unique URI within the specified encodable namespace.
	 * 
	 * The generated URI will conform to the format that was specified
	 * when the encodable namespace was registered, and is guaranteed
	 * to be unique for the underlying repository.
	 * 
	 * @see AGRepositoryConnection#registerEncodableNamespace(String)
	 * 
	 * @return a unique URI within the specified namespace.
	 * @ throws RepositoryException
	 */
	public URI generateURI(String encodableNamespace) {
		return null; //TODO implement
	}
}
