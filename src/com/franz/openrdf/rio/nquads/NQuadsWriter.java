/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/
package com.franz.openrdf.rio.nquads;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.ntriples.NTriplesUtil;

/**
 * An implementation of the RDFWriter interface that writes RDF documents in
 * N-Quads format. The N-Quads format is defined 
 * <a href="http://sw.deri.org/2008/07/n-quads/">here</a>.
 */
public class NQuadsWriter implements RDFWriter {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Writer writer;

	private boolean writingStarted;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new NQuadsWriter that will write to the supplied OutputStream.
	 * 
	 * @param out
	 *        The OutputStream to write the N-Quads document to.
	 */
	public NQuadsWriter(OutputStream out) {
		this(new OutputStreamWriter(out, Charset.forName("US-ASCII")));
	}

	/**
	 * Creates a new NQuadsWriter that will write to the supplied Writer.
	 * 
	 * @param writer
	 *        The Writer to write the N-Quads document to.
	 */
	public NQuadsWriter(Writer writer) {
		this.writer = writer;
		writingStarted = false;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public RDFFormat getRDFFormat() {
		return NQuadsFormat.NQUADS;
	}

	public void startRDF()
		throws RDFHandlerException
	{
		if (writingStarted) {
			throw new RuntimeException("Document writing has already started");
		}

		writingStarted = true;
	}

	public void endRDF()
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet started");
		}

		try {
			writer.flush();
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
		finally {
			writingStarted = false;
		}
	}

	public void handleNamespace(String prefix, String name) {
		// N-Quads does not support namespace prefixes.
	}

	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet been started");
		}

		Resource subj = st.getSubject();
		URI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource context = st.getContext();

		try {
			// SUBJECT
			writeResource(subj);
			writer.write(" ");

			// PREDICATE
			writeURI(pred);
			writer.write(" ");

			// OBJECT
			if (obj instanceof Resource) {
				writeResource((Resource)obj);
			}
			else if (obj instanceof Literal) {
				writeLiteral((Literal)obj);
			}

			// CONTEXT
			if (context!=null) {
				writer.write(" ");
				writeResource(context);
			}
			
			writer.write(" .");
			writeNewLine();
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void handleComment(String comment)
		throws RDFHandlerException
	{
		try {
			writer.write("# ");
			writer.write(comment);
			writeNewLine();
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	private void writeResource(Resource res)
		throws IOException
	{
		if (res instanceof BNode) {
			writeBNode((BNode)res);
		}
		else {
			writeURI((URI)res);
		}
	}

	private void writeURI(URI uri)
		throws IOException
	{
		writer.write(NTriplesUtil.toNTriplesString(uri));
	}

	private void writeBNode(BNode bNode)
		throws IOException
	{
		writer.write(NTriplesUtil.toNTriplesString(bNode));
	}

	private void writeLiteral(Literal lit)
		throws IOException
	{
		writer.write(NTriplesUtil.toNTriplesString(lit));
	}

	private void writeNewLine()
		throws IOException
	{
		writer.write("\n");
	}
}
