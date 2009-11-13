
//***** BEGIN LICENSE BLOCK *****
//Version: MPL 1.1
//
//The contents of this file are subject to the Mozilla Public License Version
//1.1 (the "License"); you may not use this file except in compliance with
//the License. You may obtain a copy of the License at
//http://www.mozilla.org/MPL/
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
//for the specific language governing rights and limitations under the
//License.
//
//The Original Code is the AllegroGraph Java Client interface.
//
//The Original Code was written by Franz Inc.
//Copyright (C) 2006 Franz Inc.  All Rights Reserved.
//
//***** END LICENSE BLOCK *****

package com.franz.agjena;

import java.util.HashMap;
import java.util.Map;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.BlankNode;
import com.franz.agbase.ValueObject;
import com.franz.agbase.impl.BlankNodeImpl;
import com.franz.agbase.impl.URINodeImpl;
import com.franz.agbase.util.AGC;
import com.franz.agbase.util.AGInternals;
import com.franz.agjena.exceptions.NiceException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Blank;
import com.hp.hpl.jena.graph.Node_Literal;
import com.hp.hpl.jena.graph.Node_URI;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

/**
 * The 'JenaToAGManager' handles translations to/from Jena Terms and
 * AllegroGraph equivalents.  There is only one per store, since the
 * same terms may span multiple (Jena) graphs.
 */
public class JenaToAGManager {
	
	private AllegroGraph agStore;
	// THIS SHOULD BE AN LRU CACHE!!!
	Map<Node, UPI> blankNodeToUPIMap = new HashMap<Node, UPI>();
	Map<UPI, Node> UPIToBlankNodeMap = new HashMap<UPI, Node>();
	
	private static JenaToAGManager managerInstance = null;
	
	private JenaToAGManager(AllegroGraph agStore) {
		this.agStore = agStore;
	}
	
	/**
	 * Retrieve the (one-and-only) manager.  Note, if there can be more than one
	 * value for 'agStore' within a JVM, then this code is faulty; in that case,
	 * rewrite this to use a hashmap that returns the manager for a particular 
	 * AG store.
	 * @param agStore
	 * @return
	 */
	public static synchronized JenaToAGManager getInstance (AllegroGraph agStore) {
		if (JenaToAGManager.managerInstance == null) {
			JenaToAGManager.managerInstance = new JenaToAGManager(agStore); 
		}
		return JenaToAGManager.managerInstance;
	}
	
	/**
	 * Map a blank (anonymous) node from Jena to AllegroGraph.
	 * 
	 * @param node A Jena Node_Blank instance.
	 * @param createIfNew If true, create a new AllegroGraph blank node when
	 *      this is the first time we see the Node_Blank instance.
	 * @return The AllegroGraph node UPI or null.
	 * @throws AllegroGraphException if the blank node cannot be created.
	 */
	private synchronized UPI lookupUPIForBlankNode ( Node_Blank node, boolean createIfNew ) throws AllegroGraphException {
		UPI v = this.blankNodeToUPIMap.get(node);
		if ( v != null) return v;
		if ( !createIfNew ) return null;
		//String blankNodeId = node.getBlankNodeId().getLabelString();
		// mm: blankNodeId is the bogus string label that openrdf requires.
		// now, maybe 'blankNodeId' encodes a UPI:
		// unfortunately, I can't figure out how to create an AllegroGraph
		// BNode from a string (or if its even possible).  For now, drop
		// 'blankNodeId' on the floor:
		// THIS IS A BUG IF WE SWITCH TO LRU CACHE:
		UPI lv = this.agStore.createBNodeIds(1)[0];
		this.UPIToBlankNodeMap.put(lv, node);
		this.blankNodeToUPIMap.put(node, lv);
		return lv;
	}

	/**
	 * Map a blank (anonymous) node from AllegroGraph to Jena.
	 * 
	 * @param key The AllegroGraph node UPI of the blank node.
	 * @param createIfNew If true, create a new Jena Node_Blank instance when 
	 *     the UPI is not in the table.
	 * @return a Jena Node_Blank instance or null
	 */
	protected synchronized Node lookupBlankNodeForUPI (UPI key, boolean createIfNew) {
		Node val = this.UPIToBlankNodeMap.get(key);
		if (val != null)
			return val;
		if ( !createIfNew ) return null;
		// THIS SEEMS WRONG (COPIED FROM 'JenaCursor' CODE), PROBABLY WORKS
		// OK AS LONG AS WE DON'T SWITCH TO LRU CACHE:
		//Node node = Node.createAnon();
		// THIS LOOKS BETTER:
		Node node = Node.createAnon(AnonId.create(key.toString()));		
		this.UPIToBlankNodeMap.put(key, node);
		this.blankNodeToUPIMap.put(node, key);
		return node;
	}
	
	private static String getLanguageTag (Node_Literal lit) {
		String lang = lit.getLiteralLanguage();
		if ( lang==null ) return null;
		if ( lang.equals("") ) return null;
		return lang;
	}
	
	/**
	 * Always return a %encoded string (used when filling array).
	 * @param node
	 * @return
	 */
	public String jenaNodeToAgStringTerm (Node node ) {
		Object p = jenaNodeToAGTerm(node, false);
		// mm 2008-06-23: this is never the case
		//if ( p instanceof Long ) return p.toString();
		if ( p instanceof UPI ) {
			String e = this.agStore.refToString(p);
			// THIS DEBUG MESSAGE SEEMS TO HAVE DISAPPEARED IN THE 3.0 RELEASE  - RMM
			//AllegroGraph.prdb( "jenaNodeToAgStringTerm", ((node instanceof Node_Blank)?p:node) + " \"" + e + "\"");
			return e;
		}
		return (String)p;
	}
	
	public Object jenaNodeToAGTerm(Node node) {
		return this.jenaNodeToAGTerm(node, false);
	}
	
	/**
	 * Convert a Jena Node instance to a suitable AllegroGraph
	 * node reference at the AGConnector interface.
	 * 
	 * @param node a Jena Node instance 
	 * @param nullOK If true, allow a wild node ref
	 * @return a node UPI or a %encoded string reference to a node or literal
	 */
	public Object jenaNodeToAGTerm(Node node, boolean nullOK ) {
		if ( node instanceof Node_Blank) 
		{
			// lookup Jena id in Jena/AG map
			// if found, return new Long(AGid)
			// otherwise make a new BlankNode, add to map, return new Long(AGid)
			try {
				return lookupUPIForBlankNode((Node_Blank) node, true);
			} catch (AllegroGraphException e) {
				throw new IllegalStateException(e.toString());
			}
		}
		else if ( node instanceof Node_URI ) 
			return AGInternals.refNodeToString(node.getURI());
			
		else if ( node instanceof Node_Literal )
		{
			Node_Literal lit = (Node_Literal)node;
			return AGInternals.refLitToString(lit.getLiteralLexicalForm(),
								getLanguageTag(lit),
								lit.getLiteralDatatypeURI());
		}
		else if ( nullOK ) return null;
		else throw new IllegalArgumentException("Must be concrete Node");
	}
	
	/**
	 * Create a Jena Graph Node instance from AllegroGraph parts.
	 * 
	 * @param id The node id number
	 * @param type The node type
	 * @param val The node label
	 * @param mod The node language or datatype modifier
	 * @return a Jena Graph Node instance
	 */
	public Node assembleJenaGraphNode( UPI id, int type, String val, String mod ) {
		// types:  1     2     3        4             5
		//        anon  node  literal  literal/lang  typed-literal
		switch (type) {
		case 1:	return this.lookupBlankNodeForUPI(id, true);
		case 2:	return Node.createURI(val);
		case 3:	return Node.createLiteral(val);
		case 4:	return Node.createLiteral(val, mod, false);	
		case 5:	{
				RDFDatatype datatype = TypeMapper.getInstance().getTypeByName(mod);
				return Node.createLiteral(val, null, datatype);
			}
		default: throw new IllegalArgumentException("Cannot convert AG " 
				+ type + "=" + this.agStore.typeToString(type) +
				"/" + val + "/" + mod + " to Jena Graph Node");
		}
	}
	
	/**
	 * Convert an AG ValueObject into a Jena graph node.
	 */
	public Node valueObjectToJenaNode (ValueObject vo) {
		try {
			if (vo instanceof com.franz.agbase.LiteralNode) {
				com.franz.agbase.LiteralNode lit = (com.franz.agbase.LiteralNode)vo;
				Node n = null;
				if (!Utils.isNullString(lit.getLanguage())) {
					n = assembleJenaGraphNode(lit.getAGId(), 4, lit.getLabel(), lit.getLanguage());
				} else {
					n = assembleJenaGraphNode(lit.getAGId(), AGC.AGU_LITERAL, lit.getLabel(), null);
				}				
				return n;
			} else if (vo instanceof BlankNode) {
				BlankNode res = (BlankNode)vo;
				Node n = assembleJenaGraphNode(((BlankNodeImpl) res).getAGId(), 1, res.getID(), null);
				return n;
			} else {
				URINodeImpl res = (URINodeImpl)vo;
				Node n = assembleJenaGraphNode(res.getAGId(), 2, res.getURI(), null);
				return n;
			}
		} catch (AllegroGraphException ex) {
			throw new NiceException(ex);
		}
	}

	/**
	 * Convert an AG ValueObject into a resource or literal usable by a Jena model.
	 */
	@SuppressWarnings("cast")
	public RDFNode valueObjectToRDFNode (ValueObject vo, ModelCom model) {
		Node node = valueObjectToJenaNode(vo);
		if (node.isLiteral()) {
			return new LiteralImpl((Node_Literal)node, model);
		} else {
			return new ResourceImpl(node, model);
		}
	}


}
