/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/
package com.franz.openrdf.rio.nquads;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParserRegistry;

import java.nio.charset.Charset;

/**
 * An {@link RDFFormat} for N-Quads.
 *
 * @deprecated Use {@link org.openrdf.rio.RDFFormat#NQUADS} instead
 */
@Deprecated
public class NQuadsFormat extends RDFFormat {
    public static NQuadsFormat NQUADS = new NQuadsFormat();

	static {
		RDFParserRegistry.getInstance().add(new NQuadsParserFactory());
	}
	
    private NQuadsFormat() {
        super("N-Quads", "text/x-nquads", Charset.forName("US-ASCII"), "nq", true, true);
        
    }
}
