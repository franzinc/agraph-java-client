
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

package com.franz.agbase.transport;

import java.io.IOException;
import java.util.ArrayList;

import com.franz.ag.UPI;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.Triple;
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.impl.AGFactory;
import com.franz.agbase.impl.TripleImpl;
import com.franz.agbase.impl.TriplesIteratorImpl;
import com.franz.agbase.impl.UPIImpl;
import com.franz.agbase.impl.ValueNodeImpl;
import com.franz.agbase.util.AGBase;
import com.franz.agbase.util.AGC;


/**
 * @author mm
 *
 * Created on Jul 21, 2006
 *
 * Direct socket interface to AllegroGraph server.
 * 
 */
public class AGDirectConnector 
	extends AGConnector
	{
	
	AGDirectLink trs = null;
	AGDirectLink verifyLink() {
		if ( trs==null )
			throw new IllegalStateException("Cannot use disabled link.");
		return trs;
	}
	
	public int transportVersion () { return AGC.AG_DIRECT_LEVEL; }
	
	/**
	 * Call a function and return only the first value
	 * @param fn function name
	 * @param args an array of argument
	 * @return the first, or only value returned, or null if zero values
	 * @throws AllegroGraphException
	 */
	Object tsApply0 ( AGBase ag, String fn, Object[] args)
		throws AllegroGraphException {
		testIndex(ag);
		try {
			return verifyLink().sendOp3n(AGDirectLink.OP_CALL, 1, 0, AG_APPLY,
								ag.tsx, fn, args);
		} catch (IOException e) {
			throw new AllegroGraphException(e);
		}
	}
	
	/**
	 * Call a function via ag-apply and return all the values
	 * @param fn function name
	 * @param args an array of argument
	 * @return the result array (includes 2extra leading entries)
	 * @throws AllegroGraphException
	 */
	Object[] tsApplyA ( AGBase ag, String fn, Object[] args)
		throws AllegroGraphException {
		testIndex(ag);
		try {
			return (Object[])verifyLink().sendOp3n(AGDirectLink.OP_CALL, 1, -1, AG_APPLY,
										  ag.tsx, fn, args);
		} catch (IOException e) {
			throw new AllegroGraphException(e);
		}
	}
	
	Object[] applyA( String fn, Object[] args)
		throws AllegroGraphException {
	try {
		return (Object[])verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, -1, fn,
									   args);
	} catch (IOException e) {
		throw new AllegroGraphException(e);
	}
	}
	
	int intValue ( Object r ) {
		if ( r instanceof Long ) return (int)(((Long)r).longValue());
		if ( r instanceof Integer ) return ((Integer)r).intValue();
		throw new IllegalArgumentException("Cannot convert to int " + r);
	}

	int access(String createCode, String name, String dir)
			throws AllegroGraphException {
		return access(createCode, name, dir, new Object[0]);
	}
	
	public int access(String createCode, String name, String dir, Object[] more)
	throws AllegroGraphException {
		Object r;
		Object[] args = new Object[3+more.length];
		args[0] = createCode;
		args[1] = name;
		args[2] = dir;
		if ( (0<more.length)  &&  !serverLevel(1) )
			throw new IllegalStateException("Server level < 1");
		for (int i = 0; i < more.length; i++) {
			args[3+i] = more[i];
		}
		try {
			r = verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, 0, AG_ACCESS_TRIPLE_STORE, 
					args );
		} catch (IOException e) {
			String m = e.getMessage();
			int p = m.indexOf("AGErr");
			if ( p<0 )
				throw new AllegroGraphException(e);
			throw new AllegroGraphException(m.substring(p));
		}
		return (int)AGConnector.longValue(r);
}


	public Object[] addTriple(AGBase ag, Object s, Object p, Object o, Object c )
		throws AllegroGraphException {
		Object[] r = tsApplyA(ag, AG_ADD_TRIPLE, new Object[]{ s, p, o, c,
									"with-parts", new Integer(1),
									"sync", (ag.sync)?"":null
									});
		// returns 5 values: triple id, subject UPI, pred UPI, object UPI, context UPI
		Object[] v = new Object[5];
		v[0] = new Long(AGConnector.longValue(r[2]));
		for (int i=1;i<5; i++)
			v[i] = r[i+2];
		return v;
	}

	public Object[] addTriples(AGBase ag, Object s, Object p, Object o, Object c )
		throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_ADD_TRIPLES,
				new Object[]{ s, p, o, c, "with-parts", new Integer(1),
						"sync", (ag.sync)?"":null });
//		returns 5 arrays of equal length:  triple ids,
//		subject ids, pred ids, object ids, context ids
		long[] ids = longArray(v[2]);
		Object[] rr = new Object[] { ids, null, null, null, null };
		rr[1] = v[3];
		rr[2] = v[4];
		rr[3] = v[5];
		rr[4] = v[6];
		return rr;
	}
	

	public boolean closeTripleStore(AGBase ag, boolean doClose) throws AllegroGraphException {
		return 1==longValue(tsApply0(ag, AG_CLOSE, new Object[] { doClose?"":null }));
	}

	public void delete(AGBase ag, Object s, Object p, Object o, Object c, boolean wildOk )
		throws AllegroGraphException {
	tsApply0(ag, AG_DELETE, new Object[]{ s, p, o, c, new Integer(wildOk?1:0) });
}

	public void disable() {
		try {
			verifyLink().disconnect();
			trs = null;
		} catch (IOException e) {
		}
	}

	public void discardCursor(AGBase ag, Object ref)
		throws AllegroGraphException {
		tsApply0(ag, AG_DISCARD_CURSOR, new Object[] { ref });
	}

	public void discardCursors(AGBase ag, Object[] refs)
		throws AllegroGraphException {
		if ( 16000<refs.length )   // cl:call-arguments-limit == 16384
			throw new AllegroGraphException("Too many arguments in call.");
		tsApply0(ag, AG_DISCARD_CURSOR, refs);
	}

	public void enable() throws IOException {
		if ( trs!=null ) return;
		AGDirectLink.debug(debug);
		if ( debug>0 )
			trs = new AGDirectLinkDebug(host, port, pollCount, pollInterval, timeout);
		else
			trs = new AGDirectLink(host, port, pollCount, pollInterval, timeout);
	}



	
	public boolean exists(String name, String directory)
		throws AllegroGraphException {
		Object r;
		try {
			r = verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, 0, AG_EXISTS_P, 
							 new Object[]{ name, directory } );
		} catch (IOException e) {
			throw new AllegroGraphException(e);
		}
		if ( 1==AGConnector.longValue(r) ) return true;
		return false;
	}

	public String getLangPart(AGBase ag, UPI id)
		throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_GET_NODE_PARTS, new Object[]{ id });
		// returns 3 values: part type num, label, mod
		switch ( (int)AGConnector.longValue(v[2]) ) {
		case 4: if ( 4<v.length ) return (String)v[4];
		}
		return null;
	}

	public Object[] getParts(AGBase ag, UPI id)
		throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_GET_NODE_PARTS,
				           new Object[]{ id });
		// returns 3 values: part type num, label, mod
		return new Object[]{ new Integer( (int)AGConnector.longValue(v[2]) ),
							 (String)v[3], (String)v[4] };
	}

	
	
	public void getParts(AGBase ag, UPI[] ids, int[] types,
			             String[] vals, String[] mods)
		throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_GET_NODE_PARTS,
		                   new Object[]{ ids });
		// returns 3 values: part type num array, label array, mod array
		copy( intArray(v[2]), types );
		copy( stringArray(v[3]), vals );
		copy( stringArray(v[4]), mods );
	}

	public String getTextPart(AGBase ag, UPI id)
		throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_GET_NODE_PARTS, new Object[]{ id });
		// returns 3 values: part type num, label, mod
		switch ( (int)AGConnector.longValue(v[2]) ) {
		case 2:
		case 3:
		case 4:
		case 5:
		case 8:
			if ( 3<v.length ) return (String)v[3];
		}
		return null;
	}

	public TriplesIterator getTriples(AllegroGraph ag, Object s, Object p, Object o,
			  Object c, int lh)
		throws AllegroGraphException {
		if (lh < 1) lh = TriplesIteratorImpl.defaultLookAhead;
		Object[] v = tsApplyA(ag, AG_GET_TRIPLES,
				              new Object[]{ s, p, o, c, 
								new Integer(lh), new Integer(1) });
		//	returned 2 values: Lisp source object, array of lh results
		//       or 3 values: source, ids, parts
		if ( 4>v.length ) return TriplesIteratorImpl.emptyCursor;
		if ( 4 ==v.length ) AGFactory.makeCursor(ag, v[2], toUPIArray(v[3]));
		return AGFactory.makeCursor(ag, v[2], toUPIArray(v[3]), (String[])v[4]);
	}
	
	public TriplesIterator getInfTriples(AllegroGraph ag, Object s, Object p, Object o,
			  Object c, int lh, boolean infer)
		throws AllegroGraphException {
		if (lh < 1) lh = TriplesIteratorImpl.defaultLookAhead;
		Object[] v = tsApplyA(ag, AG_INFER_TRIPLES,
				              new Object[]{ s, p, o, c, 
								new Integer(lh),
								"infer", new Integer(infer?1:0) 
				              //, "with-parts", new Integer(1)   ???with-parts not yet implemented
				              });
		//		returned values are like selectTriples: 
		//                 array of lh results, Lisp source object.
		if ( 4>v.length ) return TriplesIteratorImpl.emptyCursor;
		return AGFactory.makeCursor(ag, v[3], toUPIArray(v[2]));
	}
	
	public TriplesIterator getTriples(AllegroGraph ag, Object s, Object p, Object o,
			Object c, Object subend, Object predend, 
			Object obend, Object cxend, int lh)
	throws AllegroGraphException {
		if (lh < 1) lh = TriplesIteratorImpl.defaultLookAhead;
		Object[] v = tsApplyA(ag, AG_GET_TRIPLE_RANGE,
				new Object[]{ s, p, o, c, subend, predend, obend, cxend,
				new Integer(lh), new Integer(1) });
		//	returned 2 values: Lisp source object, array of lh results
		//       or 3 values: source, ids, parts
		if ( 4>v.length ) return TriplesIteratorImpl.emptyCursor;
		if ( 4 ==v.length ) return AGFactory.makeCursor(ag, v[2], toUPIArray(v[3]));
		return AGFactory.makeCursor(ag, v[2], toUPIArray(v[3]), (String[])v[4]);
	}
	
	public Object getTriples(AllegroGraph ag, Object s, Object p, Object o,
			Object c, Object subend, Object predend, 
			Object obend, Object cxend, int lh, Object[] options)
	throws AllegroGraphException {
		
		if ( !serverLevel(8) )
			throw new UnsupportedOperationException("AllegroGraph server is too old.");
		
		/*
		 * lh:
		 * A positive number specifies the maximum number of triples to be
	      returned in the reply.
	      A lookahead value of zero requests a cursor that is not advanced at all.
	      A lookahead value of -1 specifies that only a yes/no reply is 
	      needed. 
	     A lookahead value of -2 requests a single triple or null reply.
	     A lookahead value of -3 requests an estimated result count.
	     A lookahead value of -4 requests an closer estimateed result count.
	     A lookahead value of -5 requests an exact result count.
		 */
		
		Object[] args = new Object[9 + options.length];
		args[0] = s;  args[1] = p;  args[2] = o;  args[3] = c;
		args[4] = subend;  args[5] = predend;  args[6] = obend;  args[7] = cxend;
		args[8] = lh;
		for (int i = 0; i < options.length; i++) {
			args[9+i] = options[i];
		}
		Object[] v = tsApplyA(ag, AG_GET_TRIPLE_RANGE, args);
		switch (lh) {
		case -1:  
			if ( 3>v.length ) return false;
			return true;
		case -3:
		case -4:
		case -5:
			if ( 3>v.length ) return 0;
			return longValue(v[2]);
		}
		// Otherwise return a Cursor.
		//	returned 2 values: Lisp source object, array of lh results
		//       or 3 values: source, ids, parts
		if ( 4>v.length ) return TriplesIteratorImpl.emptyCursor;
		if ( 4 ==v.length ) return AGFactory.makeCursor(ag, v[2], toUPIArray(v[3]));
		return AGFactory.makeCursor(ag, v[2], toUPIArray(v[3]), (String[])v[4]);
	}

	
	public TriplesIterator getInfTriples(AllegroGraph ag, Object s, Object p, Object o,
			Object c, Object subend, Object predend, 
			Object obend, Object cxend, int lh, boolean infer)
	throws AllegroGraphException {
		if (lh < 1) lh = TriplesIteratorImpl.defaultLookAhead;
		Object[] v = tsApplyA(ag, AG_INFER_TRIPLE_RANGE,
				new Object[]{ s, p, o, c, subend, predend, obend, cxend,
				new Integer(lh), "infer", new Integer(infer?1:0)
				//, new Integer(1)  ???with-parts not yet implemented
		});
		//	returned values are like selectTriples: 
		//                 array of lh results, Lisp source object.
		if ( 4>v.length ) return TriplesIteratorImpl.emptyCursor;
		return AGFactory.makeCursor(ag, v[3], toUPIArray(v[2]));
	}
	
	
	
	


	public String getTypePart(AGBase ag, UPI id) throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_GET_NODE_PARTS, new Object[]{ id });
		// returns 3 values: part type num, label, mod
		switch ( (int)AGConnector.longValue(v[2]) ) {
		case 5: if ( 4<v.length ) return (String)v[4];
		}
		return null;
	}

	
	public boolean hasTriple(AGBase ag, Object s, Object p, Object o, Object c ) throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_GET_TRIPLES,
	              new Object[]{ s, p, o, c, new Integer(-1) });
		// return zero or one values
		if ( 3>v.length ) return false;
		return true;
	}
	
	public boolean hasInfTriple(AGBase ag, Object s, Object p, Object o, Object c, boolean infer ) throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_INFER_TRIPLES,
	              new Object[]{ s, p, o, c, new Integer(-1), "infer", new Integer(infer?1:0)  });
		// return zero or one values
		if ( 3>v.length ) return false;
		return true;
	}

	public void indexAll(AGBase ag, boolean wait) throws AllegroGraphException {
		tsApply0(ag, AG_ALL,  new Object[] { new Integer(wait?1:0) });
	}

	public void indexTriples(AGBase ag, boolean wait) throws AllegroGraphException {
		tsApply0(ag, AG_INDEX,  new Object[] { new Integer(wait?1:0) });
	}

	public void indexStore(AGBase ag, boolean wait, Boolean all) throws AllegroGraphException {
		ArrayList<Object> args = new ArrayList<Object>();
		args.add("wait");
		args.add(wait?1:0);
		args.add("index");
		args.add((all==null)?"merge":(all?"all":"new"));
		tsApply0(ag, AG_INDEX,  args.toArray());
	}
	
	
	public long loadNTriples(AGBase ag, Object name, Object context,
			String from, Object[] place, Boolean save, String ext) throws AllegroGraphException {
		return loadNTriples(ag, name, context, from, place, save, ext, false);
	}
	public long loadNTriples(AGBase ag, Object name, Object context,
			String from, Object[] place, Boolean save, String ext, boolean unzip) throws AllegroGraphException {
		
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(name);
		args.add(context);
		args.add(from);
		
		Object gt = null;
		if ( null!=save ) {
			if ( save )
				gt = 8;
			else
				gt = 4;
		}
		args.add(gt);
		if ( null !=ext ) {
			args.add("external-format");   args.add(ext); 
		}
		if ( unzip ) {
			args.add("data-format");   args.add("gzip"); 
		}
		Object[] r = tsApplyA(ag, AG_LOAD, args.toArray());
		if ( r==null ) return 0;
		if ( 4>r.length ) return longValue(r[2]);
		if ( (place!=null) && 0<place.length )
			place[0] = r[3];
		return longValue(r[2]);	
	}

	public long loadRDF(AGBase ag, Object filePath, Object context,
			String baseURI, Object useRapper) throws AllegroGraphException {
		return loadRDF(ag, filePath, context, baseURI, useRapper, null, null);
	}
	
	public long loadRDF(AGBase ag, Object filePath, Object context,
			String baseURI, Object useRapper, String from, Boolean save) throws AllegroGraphException {
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(filePath);
		args.add(context);
		if ( (useRapper instanceof Boolean) && !((Boolean)useRapper) ) useRapper = null;
		int gt = ((null==useRapper)?0:1);  // send the bit for older(<13) servers
		if ( null!=save )
			gt = gt + (save?8:4);
		args.add(gt);
		if ( null!=useRapper )
			{ 
				args.add("use-rapper-p");   // older(<13) servers will ignore this
				if ( useRapper instanceof String )
					args.add(useRapper);
				else
					args.add("");
				}
		if ( null!=baseURI )
			{ args.add("base-uri"); args.add(baseURI); }
		if ( null!=from )
			{ args.add("from-string"); args.add(from); }
		
		Object[] r = tsApplyA(ag, AG_RDF, args.toArray());
		if ( r==null ) return 0;
		if ( 2<r.length ) return longValue(r[2]);
		return -1;	
	}

	public UPIImpl newBlankNode(AGBase ag) throws AllegroGraphException {
		Object v = tsApply0(ag, AG_NEW_NODE,  new Object[0]);
		return toUPI(v);
	}

	public UPIImpl newBlankNode(AGBase ag, String name) throws AllegroGraphException {
		Object v = tsApply0(ag, AG_NEW_NODE,  new Object[]{ name });
		return toUPI(v);
	}

	public UPIImpl[] newBlankNodes(AGBase ag, int n) throws AllegroGraphException {
		Object v = tsApply0(ag, AG_NEW_NODE,  new Object[]{ new Integer(n) });
		return toUPIArray(v);
	}

	public UPIImpl newLiteral(AGBase ag, String text, String type, String lang)
		throws AllegroGraphException {
		Object v = tsApply0(ag, AG_INTERN_LIT,  new Object[]{ text, type, lang });
		return toUPI(v);
	}

	public UPIImpl newLiteral(AGBase ag, String text, UPI type, String lang)
		throws AllegroGraphException {
		Object v = tsApply0(ag, AG_INTERN_LIT,  new Object[]{ text, type, lang });
		return toUPI(v);
	}

	public UPIImpl[] newLiteral(AGBase ag, String[] text, String[] type, String[] lang) throws AllegroGraphException {
		Object v = tsApply0(ag, AG_INTERN_LIT,  new Object[]{ text, type, lang });
		return toUPIArray(v);
	}

	public UPIImpl newResource(AGBase ag, String uri) throws AllegroGraphException {
		Object v = tsApply0(ag, AG_INTERN_RES,  new Object[]{ uri });
		return toUPI(v);
	}

	public UPIImpl[] newResources(AGBase ag, String[] uri) throws AllegroGraphException {
		Object v = tsApply0(ag, AG_INTERN_RES,  new Object[]{ uri });
		return toUPIArray(v);
	}

	public UPIImpl[] nextCursor(AGBase ag, Object source, int lh) throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_NEXT,  new Object[]{ source, new Integer(lh) });
		if (3>v.length) return null;
		return toUPIArray(v[2]);
	}
	

	public Object[] nextCursorAndParts(AGBase ag, Object source, int lh) throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_NEXT_WITH_PARTS,  new Object[]{ source, new Integer(lh) });
		if (4>v.length) return null;
		return new Object[]{ v[2], v[3] };
	}	
	
	

	public long numberOfTriples(AGBase ag) throws AllegroGraphException {
		Object v = tsApply0(ag, AG_NUMBER, new Object[0]);
		return AGConnector.longValue(v);
	}

	public int query() {
		if ( trs==null )
			return -1;
		if ( null==trs.softLock ) return 0;
		return 1;
	}

	public void syncTripleStore(AGBase ag) throws AllegroGraphException {
		tsApplyA(ag, AG_SYNC, new Object[0]);		
	}
	
	
	static String[] stringArray ( Object x ) {
		if ( x==null ) return new String[0];
		if ( x instanceof String[] ) return (String[])x;
		if ( x instanceof String ) return new String[]{ (String)x };
		if ( x instanceof Object[] ) {
			Object[] y = (Object[]) x;
			String[] r = new String[y.length];
			for (int i=0; i<y.length; i++) {
				Object e = y[i];
				if ( e==null ) r[i] = null;
				else if ( e instanceof String ) r[i] = (String)e;
				else r[i] = "Coerced " + e.toString() + " to String";
			}
			return r;
		}
		if ( x.getClass().isArray() ) {
			int ln = java.lang.reflect.Array.getLength(x);
			String[] r = new String[ln];
			for (int i=0; i<ln; i++) {
				Object e = java.lang.reflect.Array.get(x, i);
				if ( e==null ) r[i] = null;
				else if ( e instanceof String ) r[i] = (String)e;
				else r[i] = "Coerced " + e.toString() + " to String";
			}
			return r;
		}
		return new String[]{ "Coerced " + x.toString() + " to String" };
	}
	
	
	public TriplesIterator selectTriples ( AllegroGraph ag, String query, Object preset, String pvar, int limit, boolean infer, boolean distinct )
		throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_SELECT_TRIPLES, 
				new Object[]{ query, new Integer(limit), "presets", preset, "pvars", pvar,
				"use-reasoner", new Boolean(infer), "distinct", new Boolean(distinct)
				});
		return AGFactory.makeCursor(ag, v[3], toUPIArray(v[2]));
	}

	public TriplesIterator selectTriples ( AllegroGraph ag, String query, Object[] presets, String pvars, int limit, boolean infer, boolean distinct )
		throws AllegroGraphException {
		Object[] v = tsApplyA(ag, AG_SELECT_TRIPLES, 
				new Object[]{ query, new Integer(limit), "presets", presets, "pvars", pvars,
				"use-reasoner", new Boolean(infer), "distinct", new Boolean(distinct) });
		return AGFactory.makeCursor(ag, v[3], toUPIArray(v[2]));
	}
	
	
	public Object[] selectValues ( AGBase ag, String query, Object[] presets, String pvars, boolean infer, boolean distinct )
		throws AllegroGraphException {
		return selectValues(ag, query, presets, pvars, infer, distinct, null);
	}
	
	public Object[] selectValues ( AGBase ag, String query, Object[] presets, String pvars, boolean infer, boolean distinct, Object[] more )
		throws AllegroGraphException {
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(pvars);
		if ( presets== null ) args.add(new String[0]);
		else  if ( 0==presets.length ) args.add(new String[0]);
		else if ( presets[0] instanceof Long )
		{
			int[] iv = new int[presets.length];
			for (int i = 0; i < iv.length; i++) {
				iv[i] = (int)((Long)(presets[i])).longValue();
			}
			args.add(iv);
		}
		else if ( presets[0] instanceof String )
		{
			String[] iv = new String[presets.length];
			for (int i=0; i<iv.length; i++) {
				Object inval = presets[i];
				iv[i] = AGBase.refNtripleString((String)inval);
			}
			args.add(iv);
		}
		else if ( presets[0] instanceof ValueNodeImpl )
		{
			UPI[] iv = new UPI[presets.length];
			for (int i=0; i<iv.length; i++) {
				ValueNodeImpl inval = (ValueNodeImpl)presets[i];
				iv[i] = inval.getAGId();
			}
			args.add(iv);
		}
		else if ( presets[0] instanceof TripleImpl )
		{
			int[] iv = new int[presets.length];
			for (int i=0; i<iv.length; i++) {
				Triple inval = (Triple)presets[i];
				iv[i] = (int) inval.getAGId();
			}
			args.add(iv);
		}
		else
			throw new IllegalArgumentException
			("presets object contains unsuitable type");

		args.add(query);
		args.add(ag.selectLimit);
		args.add("use-reasoner");
		args.add(infer);
		args.add("distinct");
		args.add(distinct);
		if ( more!=null )
			for (int i = 0; i < more.length; i++) {
				args.add(more[i]);
			}
		
		Object[] r = tsApplyA(ag, AG_SELECT_VALUES, args.toArray());
		return valuesResults(r);
	}
	
	public Object[] nextValuesArray ( AGBase ag, Object source, int lh )
		throws AllegroGraphException {
		Object[] r = tsApplyA(ag, AG_NEXT,  new Object[]{ source, new Integer(lh) });
		return valuesResults(r);
	}
	
	/**
	 * 
	 * @param r  Array returned by tsApplyA() 2+values.
	 * @return Array of 10 values.
	 */
	Object[] valuesResults ( Object[] r ) {
		if ( 10>r.length ) {
			// In case of a :count-only query, there will only be one value returned.
			if ( (3==r.length) && hasLongValue(r[2]) )
				return new Object[] { r[2] };
			return null;
		}
		Object ex = null;
		if ( 10<r.length ) ex = r[10];   // extra data is var names from SPARQL
		Object ex2 = null;
		if ( 11<r.length ) ex2 = r[11];  // second extra is plan token from Prolog planner
		UPI[] ids = toUPIArray(r[2]);
		int[] types = intArray(r[3]);
		String[] labels = stringArray(r[4]);
		String[] mods = stringArray(r[5]);
		int more = (int)AGConnector.longValue(r[6]);
		int width = (int)AGConnector.longValue(r[7]);
		Object token = r[8];
		int plimit = (int)AGConnector.longValue(r[9]);
		return new Object[]{ ids, types, labels, mods, 
				new Integer(more), new Integer(width),
				token, new Integer(plimit), ex, ex2 };
	}
	
	
	

	public void serverTrace ( boolean onoff, String outFile )
		throws AllegroGraphException {
		Object arg = (outFile!=null)?((Object)outFile):((onoff)?new Integer(1):new Integer(0));
		try {
			verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, -1, AGJ_TRACE_INT, 
							 new Object[]{ arg } );
		} catch (IOException e) {
			throw new AllegroGraphException(e);
		}
	}
	
	public void serverTrace ( AGBase ag, boolean onoff, String outFile )
		throws AllegroGraphException {
		Object arg = (outFile!=null)?((Object)outFile):((onoff)?new Integer(1):new Integer(0));
		tsApply0(ag, AGJ_TRACE_INT_A, new Object[]{ arg } );
	}
	
	public void  serverTimers ( AGBase ag, boolean onoff, boolean perStore )
		throws AllegroGraphException {
		int arg = onoff?(perStore?3:2):0;
		if ( ag==null )
		{
			try {
				verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, -1, AGJ_TRACE_INT, 
								 new Object[]{ arg } );
			} catch (IOException e) {
				throw new AllegroGraphException(e);
			}
			return;
		}
		tsApply0(ag, AGJ_TRACE_INT_A, new Object[]{ arg } );
	}
	
	public long[] getTimers ( AGBase ag ) throws AllegroGraphException {
		return longArray(serverOptionAll("timers", (ag==null)?null:ag.tsx));
	}
	
	public long serverId ( ) throws AllegroGraphException {
		try {
			Object r = verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, 0, AGJ_TRACE_INT, 
							 new Object[]{ new Integer(-2) } );
			return AGConnector.longValue(r);
		} catch (IOException e) {
			throw new AllegroGraphException(e);
		}
	}
	
//	long clientId ( long id ) throws AllegroGraphException {
//		try {
//			Object r = verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, 0, AGJ_TRACE_INT, 
//							 new Object[]{ new Long(id) } );
//			return AGConnector.longValue(r);
//		} catch (IOException e) {
//			throw new AllegroGraphException(e);
//		}
//	}
	
	public int interruptServer ( long id ) throws AllegroGraphException {
		if ( id<101 ) return -4;
		try {
			Object r = verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, 0, AGJ_TRACE_INT, 
							 new Object[]{ new Long(id) } );
			return AGConnector.toInt(r);
		} catch (IOException e) {
			throw new AllegroGraphException(e);
		}
	}
	
	public boolean serverLevel ( int level ) throws AllegroGraphException {
		if ( currentServerLevel < 0 ) getVersion();
		return level <= currentServerLevel;
	}
	
	public String getVersion () throws AllegroGraphException {
		if ( serverVersions==null )
			try {
				Object r = verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, -1, AGJ_TRACE_INT, 
								 new Object[]{ new Integer(100) } );
				if ( !(r instanceof Object[]) )
					throw new AllegroGraphException("Unknown server level."); 
				Object[] ra = (Object[]) r;
				serverVersions = new Object[(ra.length)-2];
				for (int i = 0; i < serverVersions.length; i++) {
					serverVersions[i] = ra[i+2];
				}
				currentServerLevel =  (int)AGConnector.longValue(ra[2]);
			} catch (IOException e) {
				throw new AllegroGraphException(e);
			}
			String s = "";
			for (int i = 0; i < serverVersions.length; i++) {
					s = s + serverVersions[i] + " ";
			}
			return s;
	}


	Object[] valuesOnly ( Object[] r ) {
		Object[] s = new Object[(r.length)-2];
		for (int i = 0; i < s.length; i++) {
			s[i] = r[i+2];
		}
		return s;
	}
	
	

	

	public Object[] evalInServer(AGBase ag, String expression)
		throws AllegroGraphException {
		Object[] r = tsApplyA(ag, AGJ_EVAL_A, 
					new Object[]{ expression } );
		return valuesOnly(r);
	}
	

	public Object[] evalInServer(String expression) throws AllegroGraphException {
		Object v;
		try {
			v =
			verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, -1, AGJ_EVAL, 
							 new Object[]{ expression } );
		} catch (IOException e) {
			throw new AllegroGraphException(e);
		}
		return valuesOnly((Object[])v);
	}
	

	public UPIImpl[] getTripleParts(AGBase ag, long id) throws AllegroGraphException {
		Object[] r = tsApplyA(ag, AG_GET_TRIPLE_PARTS,
				              new Object[]{ new Long(id) });
		if ( 6>r.length ) return null;
		return new UPIImpl[]{ toUPI(r[2]), toUPI(r[3]), toUPI(r[4]), toUPI(r[5]) };
	}

	public boolean twinqlAsk ( AGBase ag, String query, boolean infer ) throws AllegroGraphException {
		return twinqlAsk (ag, query, infer, null);
	}
	public boolean twinqlAsk ( AGBase ag, String query, boolean infer, 
			Object[] more ) throws AllegroGraphException {
		Object r; int ml = (null==more)?0:more.length;
		Object[] args = new Object[3+ml];
		args[0] = query;  
		args[1] = "use-reasoner"; args[2] =  new Boolean(infer);
		for (int i = 0; i < ml; i++) { args[3+i] = more[i]; }
		r = tsApply0(ag, AG_TWINQL_ASK, args);   // bug18181
		if ( 1==AGConnector.longValue(r) ) 
			return true;
		return false;
	}

	public Object[] twinqlSelect(AGBase ag, String query, String vars, int limit, int offset,
			int slimit, boolean infer ) throws AllegroGraphException {
		return twinqlSelect(ag, query, vars, limit, offset, slimit, infer, null);
	}
	public Object[] twinqlSelect(AGBase ag, String query, String vars, int limit, int offset,
			int slimit, boolean infer, Object[] more ) throws AllegroGraphException {
		Object[] r;  int ml = (null==more)?0:more.length;
		Object[] args = new Object[8+ml];
		args[0] = query; args[1] = vars; args[2] =  new Integer(limit); 
		args[3] = new Integer(offset);
		args[4] = "slimit";  args[5] = new Integer(slimit);
		args[6] = "use-reasoner";  args[7] = new Boolean(infer);
		for (int i = 0; i < ml; i++) { args[8+i] = more[i]; }
		r = tsApplyA(ag, AG_TWINQL_SELECT, args	);
		if ( slimit==-1 ) return valuesOnly(r);
		return valuesResults(r);
	}

	public TriplesIterator twinqlFind(AllegroGraph ag, String query, int limit, int offset, int slimit,
			boolean infer ) throws AllegroGraphException {
		return twinqlFind(ag, query, limit, offset, slimit,	infer, null );
	}
	public TriplesIterator twinqlFind(AllegroGraph ag, String query, int limit, int offset, int slimit,
			boolean infer, Object[] more ) throws AllegroGraphException {
		return twinqlFindTriples(AG_TWINQL_FIND, ag, query, limit, offset, slimit, infer, more);
	}
	public TriplesIterator twinqlConstruct(AllegroGraph ag, String query, int limit, int offset, int slimit,
			boolean infer, Object[] more ) throws AllegroGraphException {
		return twinqlFindTriples(AG_TWINQL_CONSTRUCT, ag, query, limit, offset, slimit, infer, more);
	}
	public TriplesIterator twinqlDescribe(AllegroGraph ag, String query, int limit, int offset, int slimit,
			boolean infer, Object[] more ) throws AllegroGraphException {
		return twinqlFindTriples(AG_TWINQL_DESCRIBE, ag, query, limit, offset, slimit, infer, more);
	}
	
	TriplesIterator twinqlFindTriples(String finder, AllegroGraph ag, String query, int limit, int offset, int slimit,
			boolean infer, Object[] more ) throws AllegroGraphException {
		Object[] v;  int ml = (null==more)?0:more.length;
			Object[] args = new Object[7+ml];
			args[0] = query;  args[1] = new Integer(limit);
			args[2] = new Integer(offset); 
			args[3] = "slimit";   args[4] =  new Integer(slimit);
			args[5] = "use-reasoner"; args[6] = new Boolean(infer);
			for (int i = 0; i < ml; i++) { args[7+i] = more[i]; }
			v = tsApplyA(ag, finder, args);
		String token = null;
		if ( 3<v.length ) token = (String)v[3];
		return AGFactory.makeCursor(ag, token, toUPIArray(v[2]));
	}

	public String twinqlQuery ( AGBase ag, String query, String format, int limit, int offset, 
			boolean infer ) throws AllegroGraphException {
		return twinqlQuery(ag, query, format, limit, offset, infer, null);
	}
	public String twinqlQuery ( AGBase ag, String query, String format, int limit, int offset, 
			boolean infer, Object[] more )
	throws AllegroGraphException {
		Object r;  int ml = (null==more)?0:more.length;
		Object[] args = new Object[6+ml];
		args[0] = query;  args[1] = format;  args[2] = new Integer(limit);
		args[3] = new Integer(offset);
		args[4] = "use-reasoner";  args[5] = new Boolean(infer);
		for (int i = 0; i < ml; i++) { args[6+i] = more[i]; }
		r = tsApply0(ag, AG_TWINQL_QUERY, args);
		return (String)r ;
	}

	public Object indexing ( AGBase ag, int mode, int value, String[] flavors) throws AllegroGraphException {
		return tsApply0(ag, AG_INDEXING, new Object[] { new Integer(mode),
				                        new Integer(value), flavors });
	}

	public Object mapping ( AGBase ag, int mode, String[] map) throws AllegroGraphException {
		return tsApply0(ag, AG_MAPPING, new Object[] { new Integer(mode), map });
	}


	public String[] namespaces ( AGBase ag, String[] map ) throws AllegroGraphException {
		Object r = tsApply0(ag, AGJ_NAMESPACES_A, new Object[] { map });
		String[] rs = stringArray(r);
		//System.out.println( "namespaces = " + rs + "  " + rs.length);
		return rs;
	}

	public Object[] addPart(AGBase ag, String part) throws AllegroGraphException {
		if ( serverLevel(2) )
		{
			Object[] v = tsApplyA(ag, AG_ADD_PART, new Object[] { part });
			return valuesOnly(v);
		}
		throw new IllegalStateException("Server does not support addPart()");
	}

	public String[] freetextPredicates(AGBase ag, Object defs)
		throws AllegroGraphException {
		Object v = tsApply0(ag, AG_FREETEXT_PREDICATES, new Object[] { defs });
		if ( v instanceof String[] ) return (String[]) v;
		return new String[0];  // allways return an array [rfe7522]
	}

	public TriplesIterator getFreetextStatements(AllegroGraph ag, String pattern, int lh)
		throws AllegroGraphException {
		if ( lh == 0 ) lh = TriplesIteratorImpl.defaultLookAhead;
		Object[] v = tsApplyA(ag, AG_FREETEXT_STATEMENTS,
				              new Object[]{ pattern, 
								new Integer(lh), new Integer(1) });
		//	returned 2 values: Lisp source object, array of lh results
		//       or 3 values: source, ids, parts
		//  expect zero values if lh==-1 and answer is no.
		if ( lh<0 ) 
		{
			if ( 2==v.length ) return null;
			return TriplesIteratorImpl.emptyCursor;
		}
		if ( 4>v.length ) return TriplesIteratorImpl.emptyCursor;
		if ( 4==v.length ) return AGFactory.makeCursor(ag, v[2], toUPIArray(v[3]));
		return AGFactory.makeCursor(ag, v[2], toUPIArray(v[3]), (String[])v[4]);
	}

	public Object[] getFreetextSubjects(AGBase ag, String pattern, int limit)
		throws AllegroGraphException {
		Object[] r = tsApplyA(ag, AG_FREETEXT_SUBJECTS, 
				 	new Object[] { pattern, new Integer(limit) } );
		return valuesResults(r);
	}


	// FEDERATION ADDITIONS

	public int federate ( String name, int[] parts, boolean supersede ) throws AllegroGraphException {
		Object r;
		try {
			r = verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, 0,
					AG_FEDERATE,
					new Object[]{ name, parts, "if-exists",
									(supersede?"supersede":null)
									} );
		} catch (IOException e) {
			throw new AllegroGraphException(e);
		}
		return (int)longValue(r);
	}

	public Object[] findStore(String name, String directory, Object delete) throws AllegroGraphException {
		Object r;
		try {
			r = verifyLink().sendOp1n(AGDirectLink.OP_CALL, 1, -1,
					AG_FIND_STORE,
					new Object[]{ name, directory, null,
									(delete==null)?"ignore":"delete",
									(delete==null)?null:"delete"
									} );
		} catch (IOException e) {
			throw new AllegroGraphException(e);
		}
		
		Object[] ra = (Object[]) r;
		if ( 5>ra.length ) return null;
		return new Object[] { 
				new Integer((int)longValue(ra[2])),
				ra[3], ra[4]  };
	}

	public Object[] getStores ( AGBase ag ) throws AllegroGraphException {
		Object[] r = tsApplyA(ag,
				AG_GET_STORES,
			 	new Object[0] );
		if ( 3==r.length ) return null;
		Object[] ra = new Object[r.length - 2];
		for (int i = 0; i < ra.length; i++) {
			if ( 0==i%3 ) 
				ra[i] = new Integer((int)longValue(r[i+2]));
			else
				ra[i] = r[i+2];
		}
		return ra;
	}

	public Object serializeTriples(AGBase ag, Object[] args) throws AllegroGraphException {
		Object[] r = tsApplyA(ag,
				AG_SERIALIZE_TRIPLES, args);
		if ( 2<r.length ) return r[2];
		return null;
	}

	// All-purpose function that calls ag-apply and returns only the Lisp values. 
	public Object[] applyAGFn(AGBase ag, String fn, Object[] args) throws AllegroGraphException {
		Object[] v = tsApplyA(ag, fn, args);
		if ( null==v ) return new Object[0];
		if ( 3>v.length ) return new Object[0];
		Object[] w = new Object[v.length - 2];
		for (int i = 0; i < w.length; i++) {
			w[i] = v[i+2];
		}
		return w;
	}
	
	public Object[] applyFn ( String fn, Object[] args) throws AllegroGraphException {
		Object[] v = applyA(fn, args);
		if ( null==v ) return new Object[0];
		if ( 3>v.length ) return new Object[0];
		Object[] w = new Object[v.length - 2];
		for (int i = 0; i < w.length; i++) {
			w[i] = v[i+2];
		}
		return w;
	}
	
	public Object serverOptionOne ( Object... args ) throws AllegroGraphException {
		Object[] v = applyA(AGJ_SERVER_OPTIONS, args);
		if ( null==v ) return null;
		if ( 3>v.length ) return null;
		return v[2];
	}
	
	public Object serverOptionAll ( Object... args ) throws AllegroGraphException {
		return applyFn(AGJ_SERVER_OPTIONS, args);
	}

	public Object[] clientOption(AGBase ag, Object... more) throws AllegroGraphException {
		return applyAGFn(ag, AG_CLIENT_OPTIONS, more);
	}

	
	
	}
