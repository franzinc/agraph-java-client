package com.franz.agsail.impl;

import com.franz.agbase.BlankNode;
import com.franz.agbase.LiteralNode;
import com.franz.agbase.Triple;
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.URINode;
import com.franz.agsail.AGForSail;
import com.franz.agsail.AGSailBlankNode;
import com.franz.agsail.AGSailCursor;
import com.franz.agsail.AGSailLiteral;
import com.franz.agsail.AGSailTriple;
import com.franz.agsail.AGSailURI;

public class AGSailFactory {
	
	public static AGSailBlankNode makeBlankNode ( AGForSail ts ) {
		return new AGSailBlankNodeImpl(ts);
	}
	
	public static AGSailBlankNode makeBlankNode ( AGForSail ts, BlankNode di ) {
		return new AGSailBlankNodeImpl(ts, di);
	}
	
	public static AGSailCursor makeCursor(AGForSail rts, TriplesIterator cursor ) {
		return new AGSailCursorImpl(rts, cursor);
	}
	
	public static AGSailLiteral makeLiteral ( AGForSail ts, LiteralNode agdef ) {
		return new AGSailLiteralImpl(ts, agdef);
	}
	
	public static AGSailURI makeNode( AGForSail ts, URINode base ) {
		return new AGSailURIImpl(ts, base);
	}
	
	public static AGSailTriple makeTriple ( AGForSail ts, Triple base ) {
		return new AGSailTripleImpl(ts, base);
	}
	
}
