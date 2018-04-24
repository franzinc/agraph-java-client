/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGValueFactory;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.nquads.NQuadsWriter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implements the Jena Model interface for AllegroGraph.
 */
public class AGModel extends ModelCom implements Model, Closeable {

    public AGModel(AGGraph base) {
        super(base);
    }

    @Override
    public AGGraph getGraph() {
        return (AGGraph) graph;
    }

    @Override
    public AGModel read(InputStream reader, String base) {
        return read(reader, base, "RDF/XML");
    }

    @Override
    public AGModel read(InputStream reader, String base, String lang) {
        RDFFormat format;
        if (lang.contains("TRIPLE")) {
            format = RDFFormat.NTRIPLES;
        } else if (lang.contains("RDF")) {
            format = RDFFormat.RDFXML;
        } else if (lang.contains("TURTLE")) {
            format = RDFFormat.TURTLE;
        } else if (lang.contains("QUADS")) {
            format = RDFFormat.NQUADS;
        } else {
            // TODO: add other supported formats and improve this error message
            throw new IllegalArgumentException("Unsupported format: " + lang + " (expected RDF/XML, N-TRIPLE, TURTLE, or NQUADS).");
        }
        try {
            getGraph().getConnection().add(reader, base, format, getGraph().getGraphContext());
        } catch (RDFParseException | RepositoryException | IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * <p>Write a serialized representation of this model in a specified language.
     * </p>
     * <p>The language in which to write the model is specified by the
     * <code>lang</code> argument.  Predefined values are "RDF/XML",
     * "RDF/XML-ABBREV", "N-TRIPLE", "N-QUADS", "TURTLE", (and "TTL") and "N3".  The default value,
     * represented by <code>null</code>, is "RDF/XML".</p>
     *
     * @param out  The output stream to which the RDF is written
     * @param lang The output language
     * @return This model
     */
    @Override
    public Model write(OutputStream out, String lang) {
        return write(out, lang, "");
    }

    /**
     * <p>Write a serialized representation of a model in a specified language.
     * </p>
     * <p>The language in which to write the model is specified by the
     * <code>lang</code> argument.  Predefined values are "RDF/XML",
     * "RDF/XML-ABBREV", "N-TRIPLE", "N-QUADS", "TURTLE", (and "TTL") and "N3".  The default value,
     * represented by <code>null</code>, is "RDF/XML".</p>
     *
     * @param out  The output stream to which the RDF is written
     * @param base The base uri to use when writing relative URI's. <code>null</code>
     *             means use only absolute URI's. This is used for relative
     *             URIs that would be resolved against the document retrieval URL.
     *             For some values of <code>lang</code>, this value may be included in the output.
     * @param lang The language in which the RDF should be written
     * @return This model
     */
    @Override
    public Model write(OutputStream out, String lang, String base) {
        RDFWriter writer;
        if (lang.contains("QUADS")) {
            writer = new NQuadsWriter(out);
        } else {
            return super.write(out, lang, base);
        }
        try {
            getGraph().getConnection().exportStatements(null, null, null, false, writer, getGraph().getGraphContexts());
        } catch (RDFHandlerException | RepositoryException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Returns a new blank node with an AG-allocated id.
     * <p>
     * See also the javadoc for allowing external blank nodes for more discussion.
     *
     * @see AGHttpRepoClient#setAllowExternalBlankNodeIds(boolean)
     * @see AGRepositoryConnection#prepareHttpRepoClient()
     */
    @Override
    public Resource createResource() {
        AGValueFactory vf = getGraph().getConnection().getValueFactory();
        BNode blank = vf.createBNode();
        return createResource(new AnonId(blank.stringValue()));
    }

    /**
     * Returns a new blank node with the given (a.k.a "external") id.
     * <p>
     * Consider using createResource() instead to get an AG-allocated
     * blank node id, as it is safer (avoids unintended blank node
     * conflicts) and can be stored more efficiently in AllegroGraph.
     * <p>
     * See also the javadoc for allowing external blank nodes for more
     * discussion.
     *
     * @see AGHttpRepoClient#setAllowExternalBlankNodeIds(boolean)
     * @see AGRepositoryConnection#prepareHttpRepoClient()
     */
    @Override
    public Resource createResource(AnonId id) {
        return super.createResource(id);
    }


    /*
     * Override methods involving StatementImpls,
     * instead using AGStatements.
     */

    @Override
    public AGModel add(Statement[] statements) {
        GraphUtil.add(getGraph(), StatementImpl.asTriples(statements));
        return this;
    }

    @Override
    public AGModel remove(Statement[] statements) {
        GraphUtil.delete(getGraph(), StatementImpl.asTriples(statements));
        return this;
    }

    @Override
    public AGStatement createStatement(Resource r, Property p, RDFNode o) {
        return new AGStatement(r, p, o, this);
    }

    @Override
    public Statement asStatement(Triple t) {
        return AGStatement.toStatement(t, this);
    }
}
