/******************************************************************************
** Copyright (c) 2008-2014 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import com.hp.hpl.jena.graph.Graph;

public class AGAnonGraphTest extends AGGraphTest {

	public AGAnonGraphTest(String name) {
		super(name);
	}

	@Override
	public Graph getGraph() {
		return maker.createGraph();
	}

}
