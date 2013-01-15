/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import java.nio.charset.Charset;

import org.openrdf.rio.RDFFormat;

public class AGRDFFormat extends RDFFormat {

	public static RDFFormat NQUADS = new AGRDFFormat("N-Quads","text/x-nquads",Charset.forName("US-ASCII"),"nq",false,true);
	
	public AGRDFFormat(String name, String mimeType, Charset charset,
			String fileExtension, boolean supportsNamespaces,
			boolean supportsContexts) {
		super(name, mimeType, charset, fileExtension, supportsNamespaces,
				supportsContexts);
	}

}
