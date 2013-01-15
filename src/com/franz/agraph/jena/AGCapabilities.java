/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.impl.AllCapabilities;

/**
 * Implements the Jena Capabilities interface for AllegroGraph.
 * 
 */
public class AGCapabilities extends AllCapabilities implements Capabilities {

	@Override
	// TODO: "true" would require support for D-entailment 
    public boolean handlesLiteralTyping() { return false; }

}
