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

package com.franz.agbase.impl;


import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.Triple;
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.ValueNode;
import com.franz.agbase.ValueObject;
import com.franz.agbase.util.AGC;
import com.franz.agbase.util.AGInternals;

/**
 * This class implements a generator for multiple Triple instances.
 * <p>
 * Many triple store search operations may generate an indeterminate number of
 * results. These operations return a Cursor instance which may be used to
 * iterate through the available results.
 * <p>
 * There are no public constructors.  Instances are created by search
 * operations.
 */
public class TriplesIteratorImpl implements TriplesIterator {
	/**
	 * The default number of triples to transfer from AG to Java whenever a
	 * Cursor is advanced. The built-in initial value is 1000.
	 */
	public static int defaultLookAhead = 1000;

	/**
	 * Query the default look-ahead value for this cursor instance.
	 * @return an integer
	 * @see #setDefaultLookAhead(int)
	 */
	public static int getDefaultLookAhead() {
		return defaultLookAhead;
	}

	/**
	 * Set the default look-ahead value for this cursor instance.
	 * @param lh an integer.  Any value less than 1 denotes the built-in default of 1000.
	 * @see #setLookAhead(int)
	 */
	public static void setDefaultLookAhead(int lh) {
		if (lh < 1)
			defaultLookAhead = 1000;
		else
			defaultLookAhead = lh;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getLookAhead()
	 */
	public int getLookAhead() {
		return lookAhead;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#setLookAhead(int)
	 */
	public void setLookAhead(int lh) {
		if (lh < 1)
			lookAhead = defaultLookAhead;
		else
			lookAhead = lh;
	}

	/**
	 * Value returned when a triple part is not there.
	 */
	static final long NO_VALUE = -1;

	private AllegroGraph ag;
	public AllegroGraph getAG () { return ag; }

	private Object source;
	public Object getSource () { return source; }

	int lookAhead = defaultLookAhead;

	long id = TripleImpl.NO_TRIPLE;

	UPIImpl s = null;

	UPIImpl p = null;

	UPIImpl o = null;

	UPIImpl c = null; // quad-store

	String sVal = null;

	int sType = 0;

	String sMod = null;

	String pVal = null;

	int pType = 0;

	String pMod = null;

	String oVal = null;

	int oType = 0;

	String oMod = null;

	String cVal = null;

	int cType = 0;

	String cMod = null;

	boolean nextp = false;

	boolean truncated = false;

	int cacheIndex = -1;
	
	// NEW ACCESSOR HERE, INSTEAD OF CALLING PUBLIC FIELD DIRECTLY - RMM
	public AGInternals getStore () {return this.ag;}

	/**
	 * Cache array is null or a sequence of ids triple-id subject-id
	 * predicate-id object-id context-id tr-id s-id p-id o-id c-id tr-id s-id
	 * p-id o-id c-id ... continued-p triple-id and continued-p are stored in
	 * code field of UPI instances.
	 * 
	 * continued-p is >0 if more, 0 if no more, -n if n entries were left
	 * behind.
	 * 
	 * Cache has shadow arrays cTypes cVals cMods that hold triple parts info
	 * when that info has been fetched.
	 * 
	 * In cache array, a negative calue for triple or triple part is a relative
	 * index to where the real data is for a duplicate triple or part.
	 */
	UPI[] cache = null;
	static final int CACHE_CYCLE = 5;

	// 3 arrays may hold details of the cursor triple parts
	int[] cTypes = null; // AG type codes

	String[] cVals = null; // URI or literal value

	String[] cMods = null; // modifier of literal (lang tag or datatype uri)

	boolean withParts;

	public static final TriplesIterator emptyCursor = new TriplesIteratorImpl(null, null);
	
	public static TriplesIterator emptyCursor () { return emptyCursor; }
	 
	static int initLookAhead ( AGInternals rts ) {
		if ( rts==null ) return 0;
		if ( 0==rts.defaultLookAhead ) return defaultLookAhead;
		return rts.defaultLookAhead;
	}

//	 Use package access here because only use should be in AGFactory
	TriplesIteratorImpl(AllegroGraph rts, Object cursor) {
		super();
		lookAhead = initLookAhead(rts);
		source = cursor;
		ag = rts;
		withParts = false;
	}

//	 Use package access here because only use should be in AGFactory
	TriplesIteratorImpl(AllegroGraph rts, Object cursor, UPIImpl[] newts) {
		super();
		lookAhead = initLookAhead(rts);
		source = cursor;
		ag = rts;
		setCache(newts, true);
		withParts = false;
	}

//	 Use package access here because only use should be in AGFactory
	TriplesIteratorImpl(AllegroGraph rts, Object cursor, UPIImpl[] newts,
			String[] newdefs) {
		super();
		lookAhead = initLookAhead(rts);
		source = cursor;
		ag = rts;
		setCache(newts, newdefs);
		withParts = true;
	}

	void setCache(UPI[] newts, boolean setNext) {
		if (setNext)
			cacheIndex = -1;
		if (newts == null) {
			if (setNext)
				nextp = false;
			return;
		}
		// Set these to null only if we are storing a new cache array
		// otherwise, keep the old cache available.
		cacheIndex = -1;
		cTypes = null;
		cVals = null;
		cMods = null;
		if (0 == newts.length) {
			if (setNext)
				nextp = false;
			return;
		}
		if (1 == newts.length) {
			if (0 < ((UPIImpl) newts[0]).getCode())
				nextp = true;
			return;
		}
		// If we received only the triples, and not the part strings, then
		//  there are no negative back indexes in the cache.
		cache = (newts.clone());
		cacheIndex = 0;
		nextp = true;
	}

	void setCache(UPI[] newts, String[] newdefs) {
		cacheIndex = -1;
		cTypes = null;
		cVals = null;
		cMods = null;
		if (0 == newts.length) {
			nextp = false;
			return;
		}
		if (1 == newts.length) {
			cacheIndex = 0;
			if (0 < ((UPIImpl) newts[0]).getCode())
				nextp = true;
			return;
		}
		cache = (newts.clone());
		cacheIndex = 0;
		nextp = true;

		// Expand array of defs into cTypes cVals cMods
		int clen = cache.length;
		cTypes = new int[clen];
		cVals = new String[clen];
		cMods = new String[clen];
		int sx = 0;
		int cy = 0;

		for (int i = 0; i < clen; i++) {
			UPI ce = cache[i];
			long cen = ((UPIImpl) ce).getCode();
			if (((((UPIImpl) ce).getUpi()) == null) && cen < 0) {
				int cx = i + (int) cen;
				cache[i] = cache[cx];
				cTypes[i] = cTypes[cx];
				cVals[i] = cVals[cx];
				cMods[i] = cMods[cx];
			} else
				switch (cy) {
				case 0:
					cTypes[i] = 6;
					cVals[i] = null;
					cMods[i] = null;
					break;
				default:
					sx = decodeDef(newdefs, sx, i, cTypes, cVals, cMods);
				}
			if ( cy == (CACHE_CYCLE-1) )
				cy = 0;
			else
				cy++;
		}
	}

	static int decodeDef(String[] defs, int sx, int i, int[] types,
			String[] vals, String[] mods) {
		String def = defs[sx];
		int dl = def.length();
		if (!def.startsWith("%") || dl < 2)
			throw new IllegalArgumentException("Ill-formed node ref(a) " + def);
		if (def.regionMatches(true, 1, "P", 0, 1))
			return decodeDef(defs, sx + 1, i, types, vals, mods);
		if (def.regionMatches(true, 1, "B", 0, 1)) {
			types[i] = AGC.AGU_ANON;
			vals[i] = def.substring(2);
			mods[i] = null;
		} else if (def.regionMatches(true, 1, "N", 0, 1)) {
			types[i] = AGC.AGU_NODE;
			vals[i] = def.substring(2);
			mods[i] = null;
		} else if (def.regionMatches(true, 1, "L", 0, 1)) {
			types[i] = AGC.AGU_LITERAL;
			vals[i] = def.substring(2);
			mods[i] = null;
		} else if (def.regionMatches(true, 1, "G", 0, 1)) {
			types[i] = AGC.AGU_LITERAL_LANG;
			decodeBaseFifty(def, i, mods, vals);
		} else if (def.regionMatches(true, 1, "T", 0, 1)) {
			types[i] = AGC.AGU_TYPED_LITERAL;
			decodeBaseFifty(def, i, vals, mods);
		} else if (def.regionMatches(true, 1, "M", 0, 1)) {
			types[i] = AGC.AGU_NODE;
			decodeNodeWithPrefix(defs, sx, i, vals, mods);
		} else if (def.regionMatches(true, 1, "XD", 0, 2)) {
			types[i] = AGC.AGU_DEFAULT_GRAPH;
			vals[i] = "default graph";
			mods[i] = null;
		} else if (def.regionMatches(true, 1, "E", 0, 1)) {
			int br = def.indexOf(";", 2);
			if ( br<4 ) throw new IllegalArgumentException("Ill-formed node ref(c) " + def);
			mods[i] = def.substring(2, br);
			vals[i] = def.substring(br+2);
			if ( def.regionMatches(true, br+1, "S", 0, 1) ) 
				types[i] = AGC.AGU_ENCODED_STRING; 
			else if ( def.regionMatches(true, br+1, "N", 0, 1) ) 
				types[i] = AGC.AGU_ENCODED_INTEGER;
			else if ( def.regionMatches(true, br+1, "D", 0, 1) ) 
				types[i] = AGC.AGU_ENCODED_FLOAT;
			else throw new IllegalArgumentException("Ill-formed node ref(d) " + def);
			
		} else	throw new IllegalArgumentException("Ill-formed node ref(e) " + def);
		return sx + 1;
	}

	static int baseFiftyDigit(String def, int j, int d) {
		char digitChar = def.charAt(j);
		int digit = "0123456789abcdefghijklmnopqrstABCDEFGHIJKLMNOPQRST"
				.indexOf(digitChar);
		return d * 50 + digit;
	}

	static void decodeBaseFifty(String def, int i, String[] s1, String[] s2) {
		int d = 0;
		for (int j = 2; j < def.length(); j++) {
			if (def.regionMatches(true, j, "X", 0, 1)) {
				s1[i] = def.substring(j + 1, j + 1 + d);
				s2[i] = def.substring(j + 1 + d);
				return;
			}
			d = baseFiftyDigit(def, j, d);
		}
		throw new IllegalArgumentException("Ill-formed node ref(b) " + def);
	}

	static void decodeNodeWithPrefix(String[] defs, int cx, int i, String[] s1,
			String[] s2) {
		String def = defs[cx];
		int d = 0;
		for (int j = 2; j < def.length(); j++) {
			if (def.regionMatches(true, j, "X", 0, 1)) {
				s1[i] = defs[d].substring(2) + def.substring(j + 1);
				s2[i] = null;
				return;
			}
			d = baseFiftyDigit(def, j, d);
		}
		throw new IllegalArgumentException("Ill-formed node ref(d) " + def);
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#get_id()
	 */
	public long get_id() {
		return id;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getS()
	 */
	public synchronized UPI getS() {
		if (atTriple())
			return s;
		return null;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#querySubject()
	 */
	public String querySubject() {
		return sVal;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#queryObject()
	 */
	public String queryObject() {
		return oVal;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#queryPredicate()
	 */
	public String queryPredicate() {
		return pVal;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#queryContext()
	 */
	public String queryContext() {
		return cVal;
	} // quad-store

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getSubjectLabel()
	 */
	public synchronized String getSubjectLabel() throws AllegroGraphException {
		sVal = getPartLabel(s, 1, sType, sVal);
		return sVal;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getSubject()
	 */
	public ValueNode getSubject() throws AllegroGraphException {
		return (ValueNode) getTripleComponent(s, 1);
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getContext()
	 */
	public ValueNode getContext() throws AllegroGraphException {
		return (ValueNode) getTripleComponent(s, 4);
	}

	ValueObject getTripleComponent(UPIImpl part, int partIndex)
			throws AllegroGraphException {
		UPIImpl sb = null;
		int tp = 0;
		String val = null;
		String mod = null;
		synchronized (this) {
			if (!atTriple())
				return null;
			if (part == null)
				return null;
			sb = part;
			tp = getPartType(partIndex);
			val = getPartLabel(partIndex);
			mod = getPartMod(partIndex);
		}
		return ag.newValue(sb, tp, val, mod);
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getP()
	 */
	public synchronized UPI getP() {
		if (atTriple())
			return p;
		return null;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getPredicateLabel()
	 */
	public synchronized String getPredicateLabel() throws AllegroGraphException {
		pVal = getPartLabel(p, 2, pType, pVal);
		return pVal;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getPredicate()
	 */
	public ValueNode getPredicate() throws AllegroGraphException {
		return (ValueNode) getTripleComponent(p, 2);
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getO()
	 */
	public synchronized UPI getO() {
		if (atTriple())
			return o;
		return null;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getC()
	 */
	public synchronized UPI getC() {
		if (atTriple())
			return c;
		return null;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getObjectLabel()
	 */
	public synchronized String getObjectLabel() throws AllegroGraphException {
		oVal = getPartLabel(o, 3, oType, oVal);
		return oVal;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getContextLabel()
	 */
	public synchronized String getContextLabel() throws AllegroGraphException {
		cVal = getPartLabel(c, 4, cType, cVal);
		return cVal;
	}

	int queryPartType(int px) {
		switch (px) {
		case 1:
			return sType;
		case 2:
			return pType;
		case 3:
			return oType;
		case 4:
			return cType;
		}
		return 0;
	}

	String queryPartLabel(int px) {
		switch (px) {
		case 1:
			return sVal;
		case 2:
			return pVal;
		case 3:
			return oVal;
		case 4:
			return cVal;
		}
		return null;
	}

	String queryPartMod(int px) {
		switch (px) {
		case 1:
			return sMod;
		case 2:
			return pMod;
		case 3:
			return oMod;
		case 4:
			return cMod;
		}
		return null;
	}

	public synchronized String getPartLabel(int partIndex)
			throws AllegroGraphException {
		switch (partIndex) {
		case 1:
			sVal = getPartLabel(s, partIndex, sType, sVal);
			return sVal;
		case 2:
			pVal = getPartLabel(p, partIndex, pType, pVal);
			return pVal;
		case 3:
			oVal = getPartLabel(o, partIndex, oType, oVal);
			return oVal;
		case 4:
			cVal = getPartLabel(c, partIndex, cType, cVal);
			return cVal;
		}
		throw new IllegalArgumentException("Not a triple part index: "
				+ partIndex);
	}

	synchronized String getPartLabel(UPI part, int partIndex, int type,
			String label) throws AllegroGraphException {
		if (!atTriple())
			return null;
		if (part == null)
			return null;
		if (type == 0) {
			getCachedAll(partIndex);
			type = queryPartType(partIndex);
			label = queryPartLabel(partIndex);
		}
		if (type == 0) {
			Object[] v = getAll(part, partIndex);
			label = ((String) v[1]);
		}
		return label;
	}
	
	// Called by Jena API  MAKE-PUBLIC 
	public synchronized String getPartMod(int partIndex) throws AllegroGraphException {
		switch (partIndex) {
		case 1:
			sMod = getPartMod(s, partIndex, sType, sMod);
			return sMod;
		case 2:
			pMod = getPartMod(p, partIndex, pType, pMod);
			return pMod;
		case 3:
			oMod = getPartMod(o, partIndex, oType, oMod);
			return oMod;
		case 4:
			cMod = getPartMod(c, partIndex, cType, cMod);
			return cMod;
		}
		throw new IllegalArgumentException("Not a triple part index: "
				+ partIndex);
	}

	synchronized String getPartMod(UPI part, int partIndex, int type, String mod)
			throws AllegroGraphException {
		if (!atTriple())
			return null;
		if (part == null)
			return null;
		if (mod != null)
			return mod;
		if (type == 0) {
			getCachedAll(partIndex);
			type = queryPartType(partIndex);
			mod = queryPartMod(partIndex);
		}
		if (type == 0) {
			Object[] v = getAll(part, partIndex);
			mod = ((String) v[2]);
		}
		return mod;
	}

	// Called by Jena API  MAKE-PUBLIC 
	public synchronized int getPartType(int partIndex) throws AllegroGraphException {
		switch (partIndex) {
		case 1:
			sType = getPartType(s, partIndex, sType);
			return sType;
		case 2:
			pType = getPartType(p, partIndex, pType);
			return pType;
		case 3:
			oType = getPartType(o, partIndex, oType);
			return oType;
		case 4:
			cType = getPartType(c, partIndex, cType);
			return cType;
		}
		throw new IllegalArgumentException("Not a triple part index: "
				+ partIndex);
	}

	synchronized int getPartType(UPI part, int partIndex, int type)
			throws AllegroGraphException {
		if (!atTriple())
			return 0;
		if (part == null)
			return 0;
		if (type == 0) {
			getCachedAll(partIndex);
			type = queryPartType(partIndex);
		}
		if (type == 0) {
			Object[] v = getAll(part, partIndex);
			type = ((Integer) v[0]).intValue();
		}
		return type;
	}

	synchronized Object[] getAll(UPI part, int partIndex)
			throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().getParts(ag, part);
		int type = ((Integer) v[0]).intValue();
		switch (partIndex) {
		case 1:
			sType = type;
			sVal = (String) v[1];
			sMod = (String) v[2];
			break;
		case 2:
			pType = type;
			pVal = (String) v[1];
			pMod = (String) v[2];
			break;
		case 3:
			oType = type;
			oVal = (String) v[1];
			oMod = (String) v[2];
			break;
		case 4:
			cType = type;
			cVal = (String) v[1];
			cMod = (String) v[2];
			break;
		}
		return v;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getObject()
	 */
	public ValueNode getObject() throws AllegroGraphException {
		return (ValueNode) getTripleComponent(o, 3);
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#queryTriple()
	 */
	public synchronized TripleImpl queryTriple() {
		if (!atTriple())
			return null;
		TripleImpl tr = new TripleImpl(ag, id, s, p, o, c);
		tr.subject = sVal;
		tr.sType = sType;  // copy all the fields when making a Triple [bug17667]
		tr.subjMod = sMod;
		tr.object = oVal;
		tr.oType = oType;
		tr.objMod = oMod;
		tr.predicate = pVal;
		tr.pType = pType;
		tr.predMod = pMod;
		tr.context = cVal;
		tr.cType = cType;
		tr.cxMod = cMod;
		return tr;
	}
	
	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getTriple()
	 */
	public synchronized Triple getTriple() throws AllegroGraphException {
		if (!atTriple())
			return null;
		TripleImpl tr = new TripleImpl(ag, id, s, p, o, c);
		tr.subject = getSubjectLabel();
		tr.object = getObjectLabel();
		tr.predicate = getPredicateLabel();
		tr.context = getContextLabel();
		
		tr.sType = sType;  // copy all the fields when making a Triple [bug17667]
		tr.subjMod = sMod;
	
		tr.oType = oType;
		tr.objMod = oMod;
	
		tr.pType = pType;
		tr.predMod = pMod;
		
		tr.cType = cType;
		tr.cxMod = cMod;
		
		return tr;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#next()
	 */
	public synchronized Triple next() {
		if (nextp)
			try {
				step();
			} catch (AllegroGraphException e) {
				throw new IllegalStateException("Cursor.next " + e);
			}
		return queryTriple();
	}
	
	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#getNext()
	 */
	public synchronized Triple getNext() {
		if (nextp)
			try {
				step();
			} catch (AllegroGraphException e) {
				throw new IllegalStateException("Cursor.next " + e);
			}
		return queryTriple();
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#atTriple()
	 */
	public boolean atTriple() {
		return (id != TripleImpl.NO_TRIPLE);
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#hasNext()
	 */
	public boolean hasNext() {
		return nextp;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#limitReached()
	 */
	public boolean limitReached() {
		return truncated;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#close()
	 */
	public void close() {
		if (source == null) return;
		if ( ag == null ) return;
		try {
			ag.discardCursor(source);
		} catch (AllegroGraphException e) {
			throw new IllegalStateException("Cursor.close " + e);
		} finally { source = null; }
	}
	
	protected synchronized void finalize() throws Throwable {
		if ( ag == null ) return;
		if ( null==ag.ags ) return;
		if ( null!=source ) ag.ags.oldTokens.add(source);
	}

	boolean isCacheAvailable() {
		return nextp && (cache != null) && (0 <= cacheIndex)
				&& (cacheIndex < (cache.length - 1));
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#step()
	 */
	synchronized public boolean step() throws AllegroGraphException {
		if (isCacheAvailable())
			return stepCache();
		if (!nextp)
			return false;
		if (source == null)
			return false;
		if (withParts) {
			Object[] v = ag.verifyEnabled().nextCursorAndParts(ag, source, lookAhead);
			UPI[] r = (UPI[]) v[0];
			String[] d = (String[]) v[1];
			if (r == null) {
				setCache(null, true);
				return false;
			}
			setCache(r, d);
		} else {
			UPI[] r = ag.verifyEnabled().nextCursor(ag, source, lookAhead);
			if (r == null) {
				setCache(null, true);
				return false;
			}
			setCache(r, true);

		}

		return stepCache();

	}

	String stepVal() {
		// Called only from stepCache which is already synchronized
		if (cVals != null)
			return cVals[cacheIndex];
		return null;
	}

	int stepType() {
		// Called only from stepCache which is already synchronized
		if (cTypes != null)
			return cTypes[cacheIndex];
		return 0;
	}

	String stepMod() {
		// Called only from stepCache which is already synchronized
		if (cMods != null)
			return cMods[cacheIndex];
		return null;
	}

	synchronized void setTriple() {
		id = TripleImpl.NO_TRIPLE;
		s = null;
		p = null;
		o = null;
		c = null;
		sVal = null;
		pVal = null;
		oVal = null;
		cVal = null;
		sType = 0;
		pType = 0;
		oType = 0;
		cType = 0;
		sMod = null;
		pMod = null;
		oMod = null;
		cMod = null;
	}

	UPIImpl getCache() {
		UPIImpl cupi = (UPIImpl) cache[cacheIndex];
		if ( null==cupi ) return null;
		if (null != cupi.getUpi())
			return cupi;
		long idnum = cupi.getCode();
		if (idnum < 0)
			return (UPIImpl) cache[cacheIndex + (int) idnum];
		return cupi;
	}

	synchronized boolean stepCache() {
		// This method and setTriple() are the only places
		// where id and components are set.
		id = (getCache()).getCode();
		cacheIndex++;
		s = getCache();
		sVal = stepVal();
		sType = stepType();
		sMod = stepMod();
		cacheIndex++;
		p = getCache();
		pVal = stepVal();
		pType = stepType();
		pMod = stepMod();
		cacheIndex++;
		o = getCache();
		oVal = stepVal();
		oType = stepType();
		oMod = stepMod();
		cacheIndex++;
		c = getCache();
		cVal = stepVal();
		cType = stepType();
		cMod = stepMod();
		cacheIndex++;
		if (cacheIndex == ((cache.length) - 1)) {
			UPIImpl ce = (UPIImpl) cache[cacheIndex];
			long cx = ce.getCode();
			if (cx > 0 && source != null)
				nextp = true;
			else {
				nextp = false;
				if (cx < 0)
					truncated = true;
			}
			setCache(null, false);
		} else
			nextp = true;
		return true;
	}

	/**
	 * Update the parts cache
	 * 
	 * @throws AllegroGraphException
	 * 
	 */
	synchronized void getTripleComponents() throws AllegroGraphException {

		// This actually never gets called if all fetching is done with
		// nextCursorAndParts().

		if (cache == null)
			return;
		// if ( cacheIndex<0 ) return;
		if (cTypes != null)
			return;
		int ln = (cache.length);

		int sln = 0; // Length of the shorter trimmed cache.
		int cc = 0;

		for (int i = 0; i < ln - 1; i++) {
			if (cc == 0)
				cc = CACHE_CYCLE; // Skip the triple ids in the cache.
			else {
				UPIImpl cu = (UPIImpl) cache[i];
				if (cu!=null && cu.withLabel())
					sln++;
			}
			cc--;
		}

		// Make a short copy of cache with duplicates eliminated.
		UPI[] shortCache = new UPI[sln];

		// -1=ignored -2=single 
		int[] cachePos = new int[ln];
		cc = 0;
		int sx = 0;
		for (int i = 0; i < ln - 1; i++) {
			if (cc == 0) {
				cc = CACHE_CYCLE; // Skip the triple ids in the cache.
				cachePos[i] = -1;
			}

			else {
				UPIImpl cu = (UPIImpl) cache[i];
				if (cu!=null && cu.withLabel()) {
					shortCache[sx] = cu;
					sx++;
					cachePos[i] = -2;
				} else
					cachePos[i] = -1;
			}
			cc--;
		}
		int[] stp = new int[sln];
		String[] svl = new String[sln];
		String[] smd = new String[sln];
		ag.verifyEnabled().getParts(ag, shortCache, stp, svl, smd);

		int[] tp = new int[ln];
		String[] vl = new String[ln];
		String[] md = new String[ln];

		sx = 0;
		for (int i = 0; i < ln - 1; i++) {
			switch (cachePos[i]) {
			case -1:
				break;
			case -2:
				tp[i] = stp[sx];
				vl[i] = svl[sx];
				md[i] = smd[sx];
				sx++;
				break;
			}
		}

		cTypes = tp;
		cVals = vl;
		cMods = md;
	}

	void getCachedAll(int partIndex) throws AllegroGraphException {
		switch (partIndex) {
		case 1:
			sType = getCachedType(partIndex);
			sVal = getCachedValue(partIndex);
			sMod = getCachedModifier(partIndex);
			break;
		case 2:
			pType = getCachedType(partIndex);
			pVal = getCachedValue(partIndex);
			pMod = getCachedModifier(partIndex);
			break;
		case 3:
			oType = getCachedType(partIndex);
			oVal = getCachedValue(partIndex);
			oMod = getCachedModifier(partIndex);
			break;
		case 4:
			cType = getCachedType(partIndex);
			cVal = getCachedValue(partIndex);
			cMod = getCachedModifier(partIndex);
			break;
		}
	}

	int getCachedType(int tpart) throws AllegroGraphException {
		if (cache == null)
			return 0;
		if (cacheIndex < 0)
			return 0;
		if (cTypes == null)
			getTripleComponents();
		return cTypes[cacheIndex - CACHE_CYCLE + tpart]; // -5 if quad-store
	}

	String getCachedValue(int tpart) throws AllegroGraphException {
		if (cache == null)
			return null;
		if (cacheIndex < 0)
			return null;
		if (cTypes == null)
			getTripleComponents();
		return cVals[cacheIndex - CACHE_CYCLE + tpart]; // -5 if quad-store
	}

	String getCachedModifier(int tpart) throws AllegroGraphException {
		if (cache == null)
			return null;
		if (cacheIndex < 0)
			return null;
		if (cTypes == null)
			getTripleComponents();
		return cMods[cacheIndex - CACHE_CYCLE + tpart]; // -5 if quad-store
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#step(int)
	 */
	synchronized public Triple[] step(int n) throws AllegroGraphException {
		Triple[] r;
		int have = 0;
		if (isCacheAvailable())
			have = ((cache.length - cacheIndex) - 1) / CACHE_CYCLE;
		boolean didFetch = false;
		UPIImpl[] newa = null;
		int newl = 0;
		String[] newd = null;
		
		//System.out.println("have " + have);
		if ((have < n) // we need more than we have
				&& (cache != null) // there is possibly more
				&& ((((UPIImpl) cache[cache.length - 1]).getCode()) > 0) // next-p flag
																// is set
				&& (source != null)) // we have a cursor
		{
			UPIImpl[] ra = null;
			int get = n - have;
			if ( get<lookAhead ) get = lookAhead;
			if ( withParts ) 
			{
				Object[] v = ag.verifyEnabled().nextCursorAndParts(ag, source, get);
				ra = (UPIImpl[]) v[0];
				newd = (String[]) v[1];
				didFetch = true;

			}
			else
			{
				ra = ag.verifyEnabled().nextCursor(ag, source, get);
				didFetch = true;
			}
			if (ra != null)
			{
				newa = ra;
				newl = (((newa.length) - 1) / CACHE_CYCLE);
			}
		}
		else if ( have>=n ) have = n;
		r = new Triple[have + newl];
		for (int i = 0; i < have; i++) {
			stepCache();
			r[i] = new TripleImpl(ag, id, s, p, o, c);
		}
		if ( didFetch ) {
			if (newa == null) {
				setCache(null, true);
				return r;
			}
			else if ( newd ==null )
				setCache(newa, true);
			else
				setCache(newa, newd);
			for (int i = 0; i < newl; i++) {
				stepCache();
				r[have + i] = new TripleImpl(ag, id, s, p, o, c);
			}
		}
		return r;
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#toString()
	 */
	public synchronized String toString() {
		String triple = "empty";
		String ns;
		if (atTriple())
			triple = "" + id + ": " + showPart(sVal, sType, sMod, s) + " "
					+ showPart(pVal, pType, pMod, p) + " "
					+ showPart(oVal, oType, oMod, o) + " "
					+ showPart(cVal, cType, cMod, c);
		if (nextp)
			ns = ", with";
		else
			ns = ", no";
		return "<Cursor " + triple + " " + ns + " next>";
	}

	String showPart(String val, int type, String mod, UPI upi) {
		switch (type) {
		case 1: // anon
			return "_:blank" + val;
		case 2: // node
			return "<" + val + ">";
		case 3: // literal
			return "\"" + val + "\"";
		case 4: // literal/lang
			return "\"" + val + "@" + mod + "\"";
		case 5: // typed-literal
			return "\"" + val + "^^<" + mod + ">\"";
		default:
			return "" + upi;
		}
	}

	/* (non-Javadoc)
	 * @see com.franz.agbase.TriplesIterator#remove()
	 */
	public synchronized void remove() {
		if (!atTriple())
			throw new IllegalStateException("Nothing to remove");
		setTriple();
	}

}
