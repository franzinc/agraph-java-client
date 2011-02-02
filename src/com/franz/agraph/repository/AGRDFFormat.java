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
