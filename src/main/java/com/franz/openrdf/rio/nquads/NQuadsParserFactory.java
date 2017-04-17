/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/
package com.franz.openrdf.rio.nquads;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} for N-Quads parsers.
 * 
 * @deprecated Use {@link org.openrdf.rio.nquads.NQuadsParserFactory} instead
 */
@Deprecated
public class NQuadsParserFactory implements RDFParserFactory {

	/**
	 * Returns {@link NQuadsFormat#NQUADS}.
	 */
	public RDFFormat getRDFFormat() {
		return NQuadsFormat.NQUADS;
	}

	/**
	 * Returns a new instance of NQuadsParser.
	 */
	public RDFParser getParser() {
		return new NQuadsParser();
	}
}
