package com.franz.agbase.util;

import java.util.ArrayList;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.NamespaceRegistry;
import com.franz.agbase.UPI;
import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.BlankNode;
import com.franz.agbase.DefaultGraph;
import com.franz.agbase.TriplesIterator;
import com.franz.agbase.URINode;
import com.franz.agbase.ValueNode;
import com.franz.agbase.ValueObject;
import com.franz.agbase.impl.AGFactory;
import com.franz.agbase.impl.DefaultGraphImpl;
import com.franz.agbase.impl.EncodedLiteralImpl;
import com.franz.agbase.impl.LiteralNodeImpl;
import com.franz.agbase.impl.NamedAttributeList;
import com.franz.agbase.impl.TriplesIteratorImpl;
import com.franz.agbase.impl.UPIImpl;
import com.franz.agbase.impl.URINodeImpl;
import com.franz.agbase.impl.ValueNodeImpl;
import com.franz.agbase.transport.AGConnector;

public class AGInternals extends AGBase {


	
	
	public AllegroGraphConnection ags = null;
	
	private static final Object[] accessOptions = new Object[] {
		"expected-unique-resources", Long.class,
		"with-indices", String[].class,
		"include-standard-parts", Boolean.class,
		"read-only", Boolean.class, 
		"indirect-host", String.class,
		"indirect-port", Integer.class,
		"read-only-p", Boolean.class
	};
	
	protected static class valueMapEntry {
    	Object savedToken; public int savedMore; AllegroGraph savedAG; Object savedVal = null;
    	int savedPlimit;  boolean savedNullOk;
    	public valueMapEntry ( AllegroGraph ag, Object token, int more, int pl, Object sv, boolean nullOk ) {
			savedToken = token; savedMore = more; 
			savedAG = ag; savedPlimit = pl; savedVal = sv;
			savedNullOk = nullOk;
		}
    	protected synchronized void finalize() throws Throwable {
    		if ( (savedAG.ags)==null ) return;
    		if ( null!=savedToken ) savedAG.ags.oldTokens.add(savedToken);
    	}
    }
	
	public NamedAttributeList accOpts = new NamedAttributeList(accessOptions);
	
protected NamespaceRegistry nsregs = null;
	
	protected NamespaceRegistry nsregsInit() {
	    	if ( nsregs==null ) nsregs = new com.franz.agbase.NamespaceRegistry();
	    	return nsregs;
	    }
	
	
	public int defaultLookAhead = 0;   // use the default in Cursor class
	
	public TriplesIterator emptyCursor () { return TriplesIteratorImpl.emptyCursor(); }
	
	// CREATED GETTER FOR THIS PUBLIC FIELD - RMM
	public AllegroGraphConnection getConnection () {return this.ags;}

	public AGConnector verifyEnabled() { 
		if ( ags==null ) 
			throw new IllegalStateException("AllegroGraph server is not set.");
		return ags.getServer();
		}
	

	public void discardCursor(Object ref)
		throws AllegroGraphException {
		verifyEnabled().discardCursor(this, ref);
	}
	
	public ValueObject newValue(UPI id, int type, String val, String mod) {
		// id is a valid reference checked by caller
		ValueNodeImpl newInstance;
		switch (type) {
		case AGC.AGU_ANON:
			return AGFactory.makeBlankNode((AllegroGraph)this, id, val);
		case AGC.AGU_NODE:
			if ( mod!=null ) val = val + mod;
			return AGFactory.makeNode((AllegroGraph) this, id, val);
		case AGC.AGU_LITERAL:
			return AGFactory.makeLiteral((AllegroGraph) this, id, val, null, null, LiteralNodeImpl.LANG_NONE, null);
		case AGC.AGU_LITERAL_LANG:
			return AGFactory.makeLiteral((AllegroGraph) this, id, val, null, null, LiteralNodeImpl.LANG_KNOWN, mod);
		case AGC.AGU_TYPED_LITERAL:
			return AGFactory.makeLiteral((AllegroGraph) this, id, val, null, mod, LiteralNodeImpl.LANG_NONE, null);
		case AGC.AGU_TRIPLE:
			if ( id instanceof UPIImpl )
			{
				long idn = ((UPIImpl) id).getCode();
				if ( idn>-1 ) 
					return AGFactory.makeTriple((AllegroGraph)this, idn, null, null, null);
			}
			
		case AGC.AGU_DEFAULT_GRAPH:
			return getDefaultGraph(id);
		case AGC.AGU_ENCODED_STRING:
			newInstance = (ValueNodeImpl) AGFactory.makeEncodedLiteral((AllegroGraph)this, val, mod);
			newInstance.nodeUPI = id;
			return newInstance;
		case AGC.AGU_ENCODED_INTEGER:
			newInstance = (ValueNodeImpl) AGFactory.makeEncodedLiteral((AllegroGraph)this, Long.parseLong(val), mod);
			newInstance.nodeUPI = id;
			return newInstance;
		case AGC.AGU_ENCODED_FLOAT:
			newInstance = (ValueNodeImpl) AGFactory.makeEncodedLiteral((AllegroGraph)this, Double.parseDouble(val), mod);
			newInstance.nodeUPI = id;
			return newInstance;
			// case 11:    // encoded triple id
		default:
			throw new IllegalArgumentException
			      ("Unknown node type " + type + "  " + val + "  " + mod);
		}
	}
	
	private DefaultGraph dgraph = null;  // if there is only one
	private ArrayList<Object> dgraphs = null;    // a federation has several
	
	public synchronized DefaultGraph getDefaultGraph ( UPI id ) {
		 if ( id==null ) return null;
		 DefaultGraph temp;
		 if ( null==dgraphs ) {
			 if ( null==dgraph ) {
				 // If none are saved, save this one as the only one.
				 dgraph = AGFactory.makeDefaultGraph((AllegroGraph) this, id);
				 return dgraph;
			 }
			 if ( id.equals(((DefaultGraphImpl)dgraph).nodeUPI) ) return dgraph;
			 // We now have two for sure.
			 dgraphs = new ArrayList<Object>();
			 dgraphs.add(dgraph);
			 dgraph = null;
			 temp = AGFactory.makeDefaultGraph((AllegroGraph) this, id);
			 dgraphs.add(temp);
			 return temp;
		 }
		 for (int i = 0; i < dgraphs.size(); i++) {
			 temp = (DefaultGraphImpl) dgraphs.get(i);
			 if ( id.equals(((DefaultGraphImpl)temp).nodeUPI) ) return temp;
		}
		 temp = AGFactory.makeDefaultGraph((AllegroGraph) this, id);
		 dgraphs.add(temp);
		 return temp;
	 }
	
	 
	 public synchronized boolean isDefaultGraph ( UPI id ) { 
		 if ( id==null ) return false;
		 DefaultGraphImpl temp;
		 if ( null==dgraphs ) {
			 if ( null==dgraph ) {
				 // If none are saved, return a fail-safe answer.
				 return false;
			 }
			 if ( id.equals(((DefaultGraphImpl)dgraph).nodeUPI) ) return true;
			 return false;
		 }
		 for (int i = 0; i < dgraphs.size(); i++) {
			 temp = (DefaultGraphImpl) dgraphs.get(i);
			 if ( id.equals(temp.nodeUPI) ) return true;
		}
		 return false;
	 }
	 public boolean isDefaultGraph ( ValueNode x ) { 
		 if ( x instanceof DefaultGraphImpl )
			 return isDefaultGraph(((ValueNodeImpl)x).nodeUPI);
		 return false;
	 }
	 
	 public boolean isDefaultGraph ( int type ) {
		 return type==AGC.AGU_DEFAULT_GRAPH;
	 }
	 
	
	
	public void getPartsInternal(UPI[] ids, int[] tnums, String[] vals, String[] mods)
		throws AllegroGraphException {
		verifyEnabled().getParts(this, ids, tnums, vals, mods);
}



	public static String refEncToString(EncodedLiteralImpl v) {
		UPI u = v.queryAGId();
		if ( (null!=u) && UPIImpl.canReference(u) )
			return refUPIToString(u);
		return "%E" + v.encoding + ";" + v.stringValue();
	}


	public UPI queryAGId(ValueNode x) {
		if (x == null)
			//throw new IllegalArgumentException("Null node reference not allowed");
			return null;
		return x.queryAGId();
	}

	/**
	 * Convert a Value instance to a String
	 * containing the object id or an unambiguous representation
	 * of the object.
	 * @param node or literal
	 * @return a string
	 */
	public static String refValueToString(ValueNode node) {
		if ( node instanceof ValueNodeImpl )
		{
			ValueNodeImpl nd = (ValueNodeImpl)node;
			UPI n = nd.queryAGId();
			if ( UPIImpl.canReference(n) )
				{
				UPIImpl nn = (UPIImpl) n;
				if ( UPIImpl.isNullContext(n) ) return ( "" + AGC.AGU_NULL_CONTEXT );
				if ( nn.isWild() ) return ( "" + AGC.AGU_WILD );
				return ( "" + nn.asChars("%Z") );
				}
			if ( nd instanceof URINodeImpl )
				return refNodeToString(((URINode)nd).queryURI());
			else if ( nd instanceof BlankNode )
				// A BlankNode without a UPI turns into a reference to a NEW blank node in the sotre.
				return refAnonToString(((BlankNode)nd).getID());
			else if ( node instanceof LiteralNodeImpl ) 
			{	
				LiteralNodeImpl lt = (LiteralNodeImpl)nd;
				String label = lt.queryLabel();
				if ( label==null ) notValRef(node);
				String lang = lt.queryLanguage();
				if ( lang!=null )
					return refLitToString(label, lang, null);
				String type = lt.queryType();
				if ( type!=null )
					return refLitToString(label, null, type);
				UPI tid = lt.typeId;
				if ( tid==null ) return refLitToString(label, null, null);
				if ( UPIImpl.canReference(tid) ) return refLitToString(label, tid);
				notValRef(node);		
			}	
			else if ( node instanceof DefaultGraphImpl )
				// A DefaultGraph instance without a UPI denotes the default graph
				// in the store where the instance was created, but we do not know
				// at this point where it is going.  Caller needs to supplu destination
				// but in federated store this is still ambiguous. 
				return "";
			else notValRef(node);
		}
		notValRef(node);
		return "";  // dummy return to avoid compiler error
	}


	/**
	 * Map to string decoded by ag-intern-thing
	 * @param node ntriple string, ValueObject, UPI.
	 * @return encoded string
	 */
	public String refToString(Object node) {
		if ( node instanceof UPIImpl ) return refUPIToString((UPIImpl)node);
		if ( node instanceof EncodedLiteralImpl ) return refEncToString((EncodedLiteralImpl) node);
		if ( node instanceof ValueNode ) return refValueToString((ValueNode)node);
		if ( node instanceof String ) return refNtripleString((String) node);
		throw new IllegalArgumentException
		("Cannot map this object to a Value ref" + node);
	}


	private static Object validRefEnc(EncodedLiteralImpl x) {
		UPI u = x.queryAGId();
		if ( (null!=u) && UPIImpl.canReference(u) ) 
			return validUPI(u);
		return refEncToString(x);
	}

	public static Object validRefOb ( Object x ) {
		if ( x instanceof UPIImpl ) return validUPI((UPIImpl)x);
		if ( x instanceof EncodedLiteralImpl ) return validRefEnc((EncodedLiteralImpl) x);
		if ( x instanceof ValueNode ) return refValueToString((ValueNode)x); 
		if ( x instanceof String ) return refNtripleString((String)x);
		throw new IllegalArgumentException("Not a valid part reference: " + x);
	}

	// This has a lot of refs as a class method.
	public Object validRef(Object x) { return validRefOb(x); }

    public static Object validRefOb ( Object x, Object ifNull ) {
    	if ( x==null ) return ifNull;
		if ( (x instanceof UPIImpl ) && ((UPIImpl)x).isWild() ) return ifNull;
		return validRefOb(x);
    }
	
    // This has a lot of refs as a class method.
	public Object validRef(Object x, Object ifNull) { return validRefOb(x, ifNull); }


	public static Object validRefOrWild(Object x) { return validRefOb(x, UPIImpl.wildUPI()); }


	private String[] validRefObjects(Object[] nodes) {
		// Turn all into strings to make a homegeneous array.
		// Direct encoder likes that better.
		String[] v = new String[nodes.length];
		// look at elements with agjRef to detect EncodedLiteral
		//  instances [bug17163]
		for (int i=0; i<v.length; i++) v[i] = refToString(nodes[i]);
		return v;
	}



	public static Object validRangeRef(Object ref) {
		Object v = minMaxRef(ref);
		if ( v!=null ) return v;
		return validRefOrWild(ref);
	}
	


	private String[] validRefValues(ValueNode[] nodes) {
		String[] v = new String[nodes.length];
		// look at elements with agjRef to detect EncodedLiteral
		//  instances [bug17163]
		for (int i=0; i<v.length; i++) v[i] = refToString(nodes[i]);
		return v;
	}

	
	public Object validRefsOrWild ( Object nodes ) {
		if ( nodes==null ) return null;
		return validRefs(nodes);
	}

	public Object validRefs(Object nodes) {
		if ( nodes instanceof String ) return refToString(nodes);
		if ( nodes instanceof ValueNode ) return refToString(nodes);
		if ( nodes instanceof UPIImpl ) return nodes;
		if ( nodes instanceof ValueNode[] ) return validRefValues((ValueNode[])nodes);
		if ( nodes instanceof String[] ) return validRefStrings((String[])nodes);
		if ( nodes instanceof UPI[] ) return nodes;
		if ( nodes instanceof Object[] ) return validRefObjects((Object[])nodes);
		throw new IllegalArgumentException
		("Cannot map this object to a Value ref or array " + nodes);
	}


	/**
	 * Verify a context reference
	 * @param x any thing
	 * @param wildok 0 - only explicit context allowed,
	 *               1 - null or "" denotes the null context
	 *               2 - null is wild, "" not allowed
	 *               3 - null is wild, "" is null context
	 *               4 - null is null, "" not allowed
	 *               5 - like 3 + min/max
	 *               6 - like 4 + min/max
	 * @return
	 */
	public static Object anyContextRef(Object x, int wildok) throws AllegroGraphException {
		if ( x==null )
			switch (wildok) {
			case 1: return UPIImpl.nullUPI();
			case 2: 
			case 3: case 5: return UPIImpl.wildUPI();
			case 4: case 6: return null;
			default: throw new AllegroGraphException("Null is not a valid context");
			}			
		if ( "".equals(x) ) 
			switch (wildok) {
			case 1:
			case 3: case 5: return UPIImpl.nullUPI();
			default: throw new AllegroGraphException("\"\" is not a valid context");
			}
		switch (wildok) {
		case 5:
		case 6:
			Object v = minMaxRef(x);
			if ( v!=null ) return v;
		}
		return validRefOb(x);
	}


	private UPI[] anyContextUPIs(UPI[] x, int wildok) throws AllegroGraphException {
		UPI[] y = new UPI[x.length];
		for (int i = 0; i < x.length; i++) {
			y[i] = (UPI)anyContextRef(x[i], wildok);
		}
		return y;
	}


	private String[] anyContextStrings(String[] x, int wildok) throws AllegroGraphException {
		String[] y = new String[x.length];
		for (int i = 0; i < x.length; i++) {
			y[i] = (String)anyContextRef(x[i], wildok);
		}
		return y;
	}


	private String[] anyContextValues(ValueNode[] x, int wildok) throws AllegroGraphException {
		String[] y = new String[x.length];
		for (int i = 0; i < x.length; i++) {
			y[i] = (String)anyContextRef(x[i], wildok);
		}
		return y;
	}


	public Object anyContextRefs(Object x, int wildok) throws AllegroGraphException {
		if ( x instanceof UPI[] ) return anyContextUPIs((UPI[])x, wildok);
		if ( x instanceof String[] ) return anyContextStrings((String[])x, wildok);
		if ( x instanceof ValueNode[] ) return anyContextValues((ValueNode[])x, wildok);
		//if ( x instanceof Object[] ) return anyContextObjects((Object[])x, wildok);
		return anyContextRef(x, wildok);
	}


	public synchronized void discardOldTokens(boolean force) throws AllegroGraphException {
		if ( ags==null ) return;
		if ( ags.oldTokens.size()<ags.oldBatch ) return;
		int n = ags.oldTokens.size();
		if ( n>=ags.oldBatch )
			n = ags.oldBatch;
		else if ( !force )
			return;
		Object[] r = new Object[n];
		for (int i = 0; i < n; i++) 
			r[i] = ags.oldTokens.remove(0);
		verifyEnabled().discardCursors(this, r);
	}

	 public Object selectNull ( boolean one ) {
	    	if ( one ) return new ValueObject[0];
	    	return new ValueObject[0][0];
	    }
	 
	 public ValueObject newSelectValue ( boolean nullOk, UPIImpl id, int type, String label, String mod ) {
	    	if ( type>0 )
	    		return newValue(id, type, label, mod);
	    	if ( nullOk )
	    		return null;
	    	throw new IllegalArgumentException
		      ("Unknown node type " + type + "  " + label + "  " + mod);
	    }
	 
	 
	 public void connect ( AllegroGraphConnection conn, String key ) throws AllegroGraphException {
		 if ( ags!=null ) throw new IllegalStateException ("Allready connected");
		 ags = conn;
		 connect(key);
	 }

	public void connect(String key) throws AllegroGraphException {
		if ( tsx>-1 ) throw new IllegalStateException ("Allready connected");
		if ( tsx<-1 ) throw new IllegalStateException("Closed triple store");
		tsx = verifyEnabled().access(key, storeName, storeDirectory,
										accOpts.getList());
		ags.addTS(this);
		initNamespaces();
	}
	
	public void initNamespaces () throws AllegroGraphException {
		if ( nsregs==null ) nsregs = ags.nsregs;
		if ( nsregs!=null )
			verifyEnabled().namespaces(this, nsregs.toArray());
	}
	
	
}
