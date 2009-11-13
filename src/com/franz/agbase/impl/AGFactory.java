package com.franz.agbase.impl;

import com.franz.agbase.UPI;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.BlankNode;
import com.franz.agbase.DefaultGraph;
import com.franz.agbase.EncodedLiteral;
import com.franz.agbase.LiteralNode;
import com.franz.agbase.Triple;
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.URINode;
import com.franz.agbase.impl.LiteralNodeImpl.Lang;

public class AGFactory {
	
	public static BlankNode makeBlankNode ( AllegroGraph ts, UPI i, String ss ) {
		return new BlankNodeImpl(ts, i, ss);
	}
	
	public static TriplesIterator makeCursor(AllegroGraph rts, Object cursor, UPIImpl[] newts) {
		return new TriplesIteratorImpl(rts, cursor, newts);
	}
	
	public static TriplesIterator makeCursor(AllegroGraph rts, Object cursor, UPIImpl[] newts,
			String[] newdefs) {
		return new TriplesIteratorImpl(rts, cursor, newts, newdefs);
	}
	
	public static DefaultGraph makeDefaultGraph( AllegroGraph ag, UPI id ) {
		return new DefaultGraphImpl(ag, id);
	}
	
	public static EncodedLiteral makeEncodedLiteral ( AllegroGraph ts, long value, 
			String newEncoding ) {
		return new EncodedLiteralImpl(ts, value, newEncoding);
	}
	
	public static EncodedLiteral makeEncodedLiteral ( AllegroGraph ts, double value,
			String newEncoding ) {
		return new EncodedLiteralImpl(ts, value, newEncoding);
	}
	
	public static EncodedLiteral makeEncodedLiteral ( AllegroGraph ts, String value,
			String newEncoding ) {
		return new EncodedLiteralImpl(ts, value, newEncoding);
	}
	
	public static LiteralNode makeLiteral ( AllegroGraph ts, UPI i, String newLabel,
			UPI newTypeId, String newType, 
			Lang newLangSlot, String newLanguage ) {
		return new LiteralNodeImpl(ts, i, newLabel, newTypeId, newType, newLangSlot, newLanguage);
	}
	
	public static URINode makeNode( AllegroGraph ts, UPI i, String u ) {
		return new URINodeImpl(ts, i, u);
	}
	
	public static Triple makeTriple ( AllegroGraph ts, UPI ss, UPI pp, UPI oo  ) {
		return new TripleImpl(ts, ss, pp, oo);
	}
	
	public static Triple makeTriple ( AllegroGraph ts, UPI ss, UPI pp, UPI oo,
			UPI cc  ) {
		return new TripleImpl(ts, ss, pp, oo, cc);
	}
	
	public static Triple makeTriple ( AllegroGraph ts, long i, UPI ss, UPI pp, 
			UPI oo  ) {
		return new TripleImpl(ts, i, ss, pp, oo);
	}
	
	public static Triple makeTriple ( AllegroGraph ts, long i, UPI ss, UPI pp,
			UPI oo, UPI cc  ) {
		return new TripleImpl(ts, i, ss, pp, oo, cc);
	}
	
}
