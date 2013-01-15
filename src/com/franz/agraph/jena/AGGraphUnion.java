/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import org.openrdf.model.Resource;

public class AGGraphUnion extends AGGraph {

	AGGraphUnion(AGGraphMaker maker, Resource context, Resource... contexts) {
		super(maker,context,contexts);
	}
	
}
