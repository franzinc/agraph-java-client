package com.franz.agsail.util;

import java.util.Hashtable;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.franz.agbase.AllegroGraph;
import com.franz.agbase.BlankNode;
import com.franz.agbase.DefaultGraph;
import com.franz.agbase.LiteralNode;
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.UPI;
import com.franz.agbase.URINode;
import com.franz.agsail.AGForSail;
import com.franz.agsail.AGSailCursor;
import com.franz.agsail.AGSailTriple;
import com.franz.agsail.impl.AGSailBlankNodeImpl;
import com.franz.agsail.impl.AGSailFactory;
import com.franz.agsail.impl.AGSailValueObjectImpl;

public class AGSInternal {
	
	public AGSInternal ( AllegroGraph d) {
		directInstance = d;
	}
	
	private AllegroGraph directInstance = null;
	
	public AllegroGraph getDirectInstance () { return directInstance; }
	
	public Object coerceToAGPart ( Object openrdfPart ) {
		if ( openrdfPart==null ) return null;
		if ( openrdfPart instanceof AGSailValueObjectImpl ) {
			AGSailValueObjectImpl impl = (AGSailValueObjectImpl) openrdfPart;
			Object di = impl.getDirectInstance();
			if ( di!=null ) return di;
		}
		// Remove refs to AGSailDefaultGraph [bug18178
//		if ( openrdfPart instanceof AGSailDefaultGraph )
//			// a default-graph without a UPI is a generic reference to the 
//			// null contect of any store
//			return "";
		if ( openrdfPart instanceof URI ) 
			return directInstance.createURI(openrdfPart.toString());
		if ( openrdfPart instanceof Literal ) {
			Literal lit = (Literal) openrdfPart;
			String lang = lit.getLanguage();
			if ( lang!=null )
				return directInstance.createLiteral(lit.getLabel(), lang);
			URI dt = lit.getDatatype();
			if ( dt!=null )
				return directInstance.createTypedLiteral(lit.getLabel(), dt.toString());
			return directInstance.createLiteral(lit.getLabel());
		}
		if ( openrdfPart instanceof BNode ) {
			return lookupBNode((BNode) openrdfPart);
		}
		if ( openrdfPart instanceof String ) return openrdfPart;
		throw new IllegalArgumentException
		 	("Cannot coerce to AGPart " + openrdfPart);
	}
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public AGSailCursor coerceToSailCursor ( TriplesIterator agcursor ) {
		return AGSailFactory.makeCursor((AGForSail) this, agcursor);
	}
	
	public AGSailTriple coerceToSailTriple ( com.franz.agbase.Triple tr ) {
		return AGSailFactory.makeTriple((AGForSail) this, tr);
	}
	
	public Value coerceToSailValue ( com.franz.agbase.ValueNode agInstance ) {
		if ( agInstance==null ) return null;
		if ( agInstance instanceof URINode )
			return AGSailFactory.makeNode((AGForSail) this, (URINode) agInstance);
		if ( agInstance instanceof BlankNode )
			return registerBlankNode((BlankNode) agInstance, null);
		if ( agInstance instanceof LiteralNode )
			return AGSailFactory.makeLiteral((AGForSail)this, (LiteralNode)agInstance);
		if ( agInstance instanceof DefaultGraph )
			return null;  // Use a null value in Sesame [bug18178]
		throw new IllegalArgumentException
	 		("Cannot coerce to Sesame Part " + agInstance);
	}
	
	

	private Hashtable<UPI, BNode> agToSail = new Hashtable<UPI, BNode>();
	private Hashtable<BNode, BlankNode> sailToAG = new Hashtable<BNode, BlankNode>();
	
	public BlankNode lookupBNode ( BNode sb ) {
		if ( sb instanceof AGSailBlankNodeImpl )
		{
			AGSailBlankNodeImpl bn = (AGSailBlankNodeImpl)sb;
			return (BlankNode) bn.getDirectInstance();
		}
		BlankNode agb = null;
		if ( sb!=null ) agb = sailToAG.get(sb);
		if ( agb!=null ) return agb;
		agb = directInstance.createBNode();
		if ( sb==null ) 
			sb = AGSailFactory.makeBlankNode((AGForSail) this, agb);
		agToSail.put(agb.queryAGId(), sb);
		sailToAG.put(sb, agb);
		return agb;
	}
	
	public BNode registerBlankNode ( BlankNode agb, BNode sb ) {
		BNode old = agToSail.get(agb.queryAGId());
		if ( old!=null ) return old;
		if ( sb==null )
			old = AGSailFactory.makeBlankNode((AGForSail) this, agb);
		else
			old = sb;
		agToSail.put(agb.queryAGId(), old);
		sailToAG.put(old, agb);    // [bug18168]
		return old;
	}
	
	
	
	
}
