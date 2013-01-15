/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ReifiedStatementImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;

public class AGStatement extends StatementImpl {

	public AGStatement(Resource subject, Property predicate, RDFNode object) {
		super(subject, predicate, object);
	}
	
	public AGStatement(Resource subject, Property predicate, RDFNode object,
			AGModel model) {
		super(subject, predicate, object, model);
	}
	
	@Override
	public AGModel getModel() {
		return (AGModel)super.getModel();
	}
	
    /**
    	create a ReifiedStatement corresponding to this Statement
     */
	public ReifiedStatement createReifiedStatement() {
		Resource bnode = getModel().createResource();
		return ReifiedStatementImpl.create( this.getModel(), bnode.asNode(), this ); 
	}
    
    /**
	 * create a Statement from the triple _t_ in the enhanced graph _eg_. The
	 * Statement has subject, predicate, and object corresponding to those of
	 * _t_.
	 */
    public static Statement toStatement( Triple t, AGModel eg )
        {
        Resource s = new ResourceImpl( t.getSubject(), eg );
        Property p = new PropertyImpl( t.getPredicate(), eg );
        RDFNode o = createObject( t.getObject(), eg );
        return new AGStatement( s, p, o, eg );
        }
    
}
