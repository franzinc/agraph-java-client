/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import org.eclipse.rdf4j.rio.RDFFormat;

import java.nio.charset.Charset;

public class AGRDFFormat extends RDFFormat {

    // Added because RDFFormat.NQUADS is outdated and has a default charset of "latin1"
    public static RDFFormat NQUADS =
            new AGRDFFormat("N-Quads", "text/x-nquads", Charset.forName("UTF8"),
                    "nq", NO_NAMESPACES, SUPPORTS_CONTEXTS);
    // Added because RDFFormat.TRIG defaults to a content type of "applicaton/x-trig"
    // which, paradoxically is not supported in pre-6.2.2 versions of AG, despite
    // being a deprecated content-type for TRIG.
    public static RDFFormat TRIG =
            new AGRDFFormat("TRIG", "application/trig", Charset.forName("UTF8"), "trig",
                    SUPPORTS_NAMESPACES, SUPPORTS_CONTEXTS);
    // Extended NQUADS format. An extension of supported RDF formats in order to support
    // triple attributes.
    public static RDFFormat NQX =
            new AGRDFFormat("NQX", "application/x-extended-nquads",
                    Charset.forName("UTF8"), "nqx",
                    NO_NAMESPACES, SUPPORTS_CONTEXTS);

    public AGRDFFormat(String name, String mimeType, Charset charset,
                       String fileExtension, boolean supportsNamespaces,
                       boolean supportsContexts) {
        super(name, mimeType, charset, fileExtension, supportsNamespaces,
                supportsContexts);
    }

}
