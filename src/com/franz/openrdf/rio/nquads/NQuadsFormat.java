/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/
package com.franz.openrdf.rio.nquads;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParserRegistry;

import java.nio.charset.Charset;

/**
 * An {@link RDFFormat} for N-Quads.
 *
 */
public class NQuadsFormat extends RDFFormat {
    public static NQuadsFormat NQUADS = new NQuadsFormat();

	static {
		RDFParserRegistry.getInstance().add(new NQuadsParserFactory());
	}
	
    private NQuadsFormat() {
        super("N-Quads", "text/x-nquads", Charset.forName("US-ASCII"), "nq", true, true);
        
    }
}
