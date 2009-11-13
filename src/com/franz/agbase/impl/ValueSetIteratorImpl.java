package com.franz.agbase.impl;

import java.util.NoSuchElementException;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.ValueObject;
import com.franz.agbase.ValueSetIterator;
import com.franz.agbase.util.AGBase;
import com.franz.agbase.util.AGInternals;


public class ValueSetIteratorImpl implements ValueSetIterator {
	
	public ValueObject[][] sets = null;
	// -3 never filled   -2 current array empty   -1 array ready   -4 exhausted
	private int index = -3;
	private ValueObject[] row = null;
	private AGInternals ag = null;
	private Object savedToken = null;
	private boolean nullOk = false;
	private int savedMore = 0;
	private int savedPlimit = 0;
	public Object savedExtra = null;
	
	public ValueSetIteratorImpl() {
	}
	
	
	
/**
 * 
 * @param ag
 * @param v An array holding the multiple values returned by ag-select-values
 *   and ag-twinql-select.
 */
	public ValueSetIteratorImpl ( AGBase ag, Object vv ) {
		this();
    	if ( vv==null ) return;
    	if ( !(vv instanceof Object[]) )
    		throw new IllegalArgumentException("Unexpected result " + vv);
    	Object[] v = (Object[]) vv;
    	UPIImpl[] ids = (UPIImpl[])v[0];
    	if ( ids==null ) return;
    	int[] types = (int[])v[1];
    	String[] labels = (String[]) v[2];
    	String[] mods = (String[])v[3];
    	int more = ((Integer)v[4]).intValue();
    	int width = ((Integer)v[5]).intValue();
    	Object token = v[6];
    	int plimit = ((Integer)v[7]).intValue();
    	Object sv = null;
    	if ( 8<v.length ) sv = v[8];
    	
    	int all = ids.length;
    	int n = all;
    	if ( width>0 )  n = all/width;
    	ValueObject[][] r = new ValueObject[n][width];
    	if ( width>0 )
    	{
    		int i = 0; int j = 0; 
    		while (i<all) {
    			for ( int k=0; k<width; k++ ) {
    				r[j][k] = ((AGInternals) ag).newSelectValue(true, ids[i], types[i], labels[i], mods[i]);
    				i++;
    			}
    			j++;
    		}
    	}
    	this.ag = (AGInternals) ag;
		this.sets = r;
		index = -1;
		row = null;
		this.nullOk = true;
		savedToken = token;
		savedMore = more;
		savedPlimit = plimit;
		savedExtra = sv;
    }
	


	public boolean hasNext() {
		if ( sets==null ) return false;
		if ( index<-2 ) return false;
		if ( index==-2 ) 
		{
			if ( savedMore>0 ) return true;
			return false;
		}
		if ( (index+1)<sets.length ) return true;
		if ( savedMore>0 ) return true;
		return false;
	}
	
	/**
	 * Test if positioned at a result.
	 * @return true if result is there to be fetched.
	 */
	private boolean canUseIndex () {
		if ( sets==null ) return false;
		if ( index<0 ) return false;
		if ( index<sets.length ) return true;
		return false;
	}
	
	private boolean stepIndex () {
		if ( sets==null ) return false;
		if ( index<-1 ) return false;
		index++; 
		if ( index<sets.length ) return true;
		index = -2;
		return false;
	}

	public ValueObject[] next() {
		row = null;
		if ( sets==null || index<-1 )
			throw new NoSuchElementException("Empty iterator.");
		if ( stepIndex() ) {
			row = sets[index]; return row;
		}
		if ( savedMore>0 )
			{
			try {
				sets = selectMoreInternal(sets);
			} catch (AllegroGraphException e) {
				throw new NoSuchElementException("Server error " + e);
			}
			index = 0;
			}
		if ( canUseIndex() ) {
			row = sets[index]; return row;
		}
		index = -4;
		throw new NoSuchElementException("Exhausted iterator.");
	}

	public void remove() {
		row = null;
	}
	
	
	private ValueObject[][] selectMoreInternal ( ValueObject[][] sets ) throws AllegroGraphException {
    	// get more results from server
    	// re-use the old array if size is the same
		
    	Object[] v = ag.verifyEnabled().nextValuesArray(ag, savedToken, ag.selectLimit);
    	if ( v==null ) return (ValueObject[][]) ag.selectNull(false);
    	UPIImpl[] ids = (UPIImpl[])v[0];
    	int[] types = (int[])v[1];
    	String[] labels = (String[]) v[2];
    	String[] mods = (String[])v[3];
    	int more = ((Integer)v[4]).intValue();
    	int width = ((Integer)v[5]).intValue();
    	String token = (String) v[6];
    	int plimit = ((Integer)v[7]).intValue();
    	int all = ids.length;
    	
    	int i = 0;
    	savedMore = more;
    	if ( more>0 ) 
    	{
    		savedToken = token;
			savedPlimit = plimit;
    		}
    	else
    		{
    		ag.ags.oldTokens.add(token);
    		token = null;
    		}
   
    	//System.out.println("all=" + all + "   x.length=" + x.length);
    	if ( all==(width*sets.length) )
    	{
    		// We can recycle the same result array.
    		for (int j = 0; j < sets.length; j++) {
    			ValueObject[] row = new ValueObject[width];
    			for ( int k=0; k<width; k++ ) {
    				row[k] = ag.newSelectValue(nullOk, ids[i], types[i], labels[i], mods[i]);
    				i++;
    			}
				sets[j] = row;
			}
			return sets;
    	}
    	if ( width==0 ) sets = new ValueObject[all][0];
    	else
    	{    	
    		int n = 0;
    		n = all/width;
    		i = 0; int j = 0; sets = new ValueObject[n][width];
    		while (i<all) {
    			for ( int k=0; k<width; k++ ) {
    				//System.out.println("mods="+mods[i]);
    				sets[j][k] = ag.newSelectValue(nullOk, ids[i], types[i], labels[i], mods[i]);
    				i++;
    			}
    			j++;
    		}
    	}
    	//System.out.println("New results array ");
    	
    	ag.discardOldTokens(false);
    	return sets;    	
    }

	public ValueObject[] get() {
		return row;
	}

	public ValueObject get(int i) {
		if ( row==null ) return null;
		if ( i<0 ) return null;
		if ( i<row.length ) return row[i];
		return null;
	}

	public ValueObject next(int i) {
		next();
		return get(i);
	}

	
	public int width() {
		if ( canUseIndex() ) return sets[index].length;
		if ( savedExtra instanceof String[] ) return ((String[]) savedExtra).length;
		if ( (sets!=null) && 0<sets.length ) return sets[0].length;
		return -1;
	}

	
	public long getCount() {
		long sum = 0;
		if ( canUseIndex() )
			sum = sets.length - index;
		else if ( sets!=null )
			sum = sets.length;
		sum = sum + savedMore;
    	if ( savedMore>savedPlimit ) return -sum;
    	return sum;
	}

	
	public String[] getNames() {
		if ( savedExtra instanceof String[] ) return ((String[]) savedExtra).clone();
		int w = width();
		if ( w<0 ) throw new IllegalStateException("There are no values or names.");
		String[] nm = new String[w];
		for (int i = 0; i < nm.length; i++) {
			nm[i] = "v" + i;
		}
		savedExtra = nm;
		return nm;
	}

	public ValueObject get(String name) {
		return get(getIndex(name));
	}
	
	public static boolean sameVar ( String var, String name ) {
		if ( name.equals(var) ) return true;
		int p = var.indexOf("?");
		int q = name.indexOf("?");
		if ( p==q ) return false;
		if ( (p==-1) && (q==0) &&
				name.startsWith(var, 1) &&
				(1+var.length())==name.length() )
			return true;
		if ( (p==0) && (q==-1) &&
				var.startsWith(name, 1) &&
				(1+name.length())==var.length() )
			return true;
		return false;
	}

	public int getIndex(String var) {
		String[] names = getNames();
		for (int i = 0; i < names.length; i++) {
			if ( sameVar(var, names[i]) ) return i;
		}
		return -1;
	}
    	

}
