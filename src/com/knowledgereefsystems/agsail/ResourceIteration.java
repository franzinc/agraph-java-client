package com.knowledgereefsystems.agsail;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.model.Resource;
import org.openrdf.sail.SailException;

import com.franz.agbase.ValueNode;
import com.franz.agbase.ValueSetIterator;
import com.franz.agsail.AGForSail;
import com.franz.agsail.AGSailValueObject;

/**
 * Author: josh
 * Date: Feb 15, 2008
 * Time: 11:05:52 AM
 */
public class ResourceIteration implements CloseableIteration<Resource, SailException> {
    private AGSailValueObject[] values;
    private int index = 0;

    public ResourceIteration ( final AGSailValueObject[] values) {
        this.values = values;
    }
    
    private boolean withValit = false;
    private AGForSail ags;
    private ValueSetIterator valit;
    public ResourceIteration (  final AGForSail ags, final ValueSetIterator valit ) {
    	if ( 1!=valit.width() )
    		throw new IllegalArgumentException("");
    	this.valit = valit;   this.ags = ags;
    	withValit = true;
    }

    public void close() throws SailException {
        values = null; valit = null; withValit = false;
    }

    public boolean hasNext() throws SailException {
    	if ( withValit ) {
    		return valit.hasNext();
    	}
        return index < values.length;
    }

    public Resource next() throws SailException {
    	if ( withValit )   	
    		return (Resource) ags.coerceToSailValue((ValueNode) valit.next(0));
    
        Resource r = (Resource) values[index];
        index++;
        return r;
    }

    public void remove() throws SailException {
        // TODO Auto-generated method stu
    	if ( withValit ) valit.remove();
    }
}
