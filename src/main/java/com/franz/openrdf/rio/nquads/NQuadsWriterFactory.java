/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/
package com.franz.openrdf.rio.nquads;

import java.io.OutputStream;
import java.io.Writer;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} for N-Quads writers.
 * 
 */
public class NQuadsWriterFactory implements RDFWriterFactory {

	/**
	 * Returns {@link NQuadsFormat#NQUADS}.
	 */
	public RDFFormat getRDFFormat() {
		return NQuadsFormat.NQUADS;
	}

	/**
	 * Returns a new instance of {@link NQuadsWriter}.
	 */
	public RDFWriter getWriter(OutputStream out) {
		return new NQuadsWriter(out);
	}

	/**
	 * Returns a new instance of {@link NQuadsWriter}.
	 */
	public RDFWriter getWriter(Writer writer) {
		return new NQuadsWriter(writer);
	}
}
