package com.franz.agbase.impl;

import java.util.ArrayList;

public class NamedAttributeList {
	
	Object[] attributeDefs ;
//	new Object[] {
//		"name", String[].class
//	};
	// Elements line up with elements in accessOptions
	//   state, value    state -> null (unset)   "" (set)
	Object[] attributeStates;
	ArrayList<Object> attributeList = new ArrayList<Object>();
	
	public NamedAttributeList ( Object[] defs ) {
		attributeDefs = defs;
		attributeStates = new Object[attributeDefs.length]; 
		for (int i = 0; i < attributeStates.length; i++) {
			attributeStates[i] = null;
		}
	}
	
	int attributeIndex ( String name ) {
		int index = -1;
		for (int i = 0; i < attributeDefs.length; i=i+2) {
			if ( name.equalsIgnoreCase((String) attributeDefs[i]) )
				index = i;
		}
		if ( index==-1 ) throw new IllegalArgumentException
				( "Not an attribute name: " + name );
		return index;
	}
	
	public Object getAttribute ( String name ) {
		return attributeStates[attributeIndex(name)+1];
	}
	
	boolean queryAttribute ( String name ) {
		return null!=attributeStates[attributeIndex(name)];
	}
	
	public synchronized void setAttribute ( String name, Object value ) {
		
		int index = attributeIndex(name);
		Class<? extends Object> vclass = null;
		if ( value!=null ) vclass = value.getClass(); 
		if ( (vclass!=null) && vclass!=attributeDefs[index+1] )
			throw new IllegalArgumentException
			("Value of attribute " + name + " is not of class " +
					((Class<?>)attributeDefs[index+1]).getName() + 
					": " + value);
		boolean found = false;
		for (int i = 0; i < attributeList.size(); i=i+2) {
			if ( name.equalsIgnoreCase((String) attributeList.get(i)) )
			{
				attributeList.set(i+1, value); found = true;  break;
			}		
		}
		if ( !found ) {
			attributeList.add(name);
			attributeList.add(value);
		}
		attributeStates[index] = "";
		attributeStates[index+1] = value;
	}

	public Object[] getList () { return attributeList.toArray(); }


}
