package com.franz.ag.repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.BindingImpl;

import com.franz.agbase.ValueNode;
import com.franz.agbase.ValueObject;
import com.franz.agbase.ValueSetIterator;
import com.franz.agsail.util.AGSInternal;

public class AGBindingSet implements BindingSet {

	AGSInternal ags = null;
	ValueObject[] vals = null;
	Map<String,Integer> nameIndex = new HashMap<String,Integer>();
	
	AGBindingSet(AGSInternal ags, ValueSetIterator iter) {
		this.ags = ags;
		String[] names = iter.getNames();
		for (int i = 0; i < names.length; i++) {
			nameIndex.put(names[i], i);
		}
		vals = iter.get();
	}
	

	// BindingSet API
	
	public Binding getBinding(String bindingName) {
		return new BindingImpl(bindingName,getValue(bindingName));
	}

	public Set<String> getBindingNames() {
		return nameIndex.keySet();
	}

	public Value getValue(String bindingName) {
		// TODO Check that this cast is safe
		return ags.coerceToSailValue((ValueNode)vals[nameIndex.get(bindingName)]);
	}

	public boolean hasBinding(String bindingName) {
		return nameIndex.containsKey(bindingName);
	}

	public Iterator<Binding> iterator() {
		Set<Binding> bindings = new HashSet<Binding>(); 
		Set<String> bn = getBindingNames();
		for (String name: bn) {
//			System.out.println("    name=" + name);
			Value bv = getValue(name);
			if ( bv!=null)   // Interface Binding requires value to be non-null.
				bindings.add(new BindingImpl(name, bv));
		}
		return bindings.iterator();
	}

	public int size() {
		return nameIndex.size();
	}

}
