/* Copyright (C) 2008 Knowledge Reef Systems.  All rights reserved. */

package com.knowledgereefsystems.agsail;

import org.openrdf.model.Resource;
import org.openrdf.sail.SailException;

import com.franz.agbase.ValueNode;
import com.franz.agbase.ValueObject;
import com.franz.agbase.ValueSetIterator;
import com.franz.agsail.util.AGSInternal;

import info.aduna.iteration.CloseableIteration;

public class ContextIteration implements CloseableIteration<Resource, SailException> {
	
	AGSInternal ags = null;
	ValueSetIterator vsi = null;
	
	ContextIteration(AGSInternal ags, ValueSetIterator vsi) {
		int numNames = vsi.getNames().length;
		if (vsi.hasNext() && numNames!=1) {
			throw new IllegalArgumentException("Expecting exactly 1 name in ValueSetIterator, got: " + numNames);
		}
		this.ags = ags;
		this.vsi = vsi;
	}
	
    public void close() throws SailException {
    }

    public boolean hasNext() throws SailException {
        return vsi.hasNext();
    }

    public Resource next() throws SailException {
    	ValueObject[] vals = vsi.next();
    	return (Resource) ags.coerceToSailValue((ValueNode)vals[0]);
    }

    public void remove() throws SailException {
    }
}
