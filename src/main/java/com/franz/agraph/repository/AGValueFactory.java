/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGHttpException;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import java.util.regex.Pattern;

/**
 * Implements the Sesame ValueFactory interface for AllegroGraph.
 */
public class AGValueFactory extends ValueFactoryImpl {
    private static final Pattern AG_BNODE_ID_PATTERN = Pattern.compile("\\Ab[0-9A-Fa-f]{8}x\\d+\\z");
    // Expected type of tagged literals
    private static final String RDF_LANG_STRING = RDF.langString.toString();

    // Jena sometimes creates empty urls - for instance in RDF/XML writer
    // it tries to create an URI representing the file containing the data,
    // when that is not known an empty string is used.
    // But RDF4J will not allow URIs without ':', so we convert these
    // to the URI specified below.
    private static final String JENA_EMPTY_URL = "http://franz.com/jena-empty-uri";

    private final AGRepository repository;
    private final AGRepositoryConnection conn;
    public String PREFIX_FOR_EXTERNAL_BNODES = "urn:x-bnode:";
    private int blankNodesPerRequest = Integer.parseInt(System.getProperty("com.franz.agraph.repository.blankNodesPerRequest", "100"));
    private String[] blankNodeIds;
    private int index = -1;

    public AGValueFactory(AGRepository repository) {
        super();
        this.repository = repository;
        this.conn = null;
    }

    public AGValueFactory(AGRepository repository, AGRepositoryConnection conn) {
        super();
        this.repository = repository;
        this.conn = conn;
    }

    public AGRepository getRepository() {
        return repository;
    }

    public AGHTTPClient getHTTPClient() {
        return getRepository().getHTTPClient();
    }

    private void requestBlankNodeIds() {
        try {
            if (conn == null) {
                blankNodeIds = getHTTPClient().getBlankNodes(getRepository().getRepositoryURL(), blankNodesPerRequest);
            } else {
                blankNodeIds = conn.prepareHttpRepoClient().getBlankNodes(blankNodesPerRequest);
            }
            index = blankNodeIds.length - 1;
        } catch (AGHttpException e) {
            // TODO: server's out of blank nodes?
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the number of blank nodes fetched per request.
     *
     * @return int  the number of blank nodes fetched per request
     */
    public int getBlankNodesPerRequest() {
        return blankNodesPerRequest;
    }

    /**
     * Sets the number of blank nodes to fetch per request.
     * <p>
     * This can be used to control the number and frequency of
     * HTTP requests made when automatically obtaining new sets
     * of blank node ids from the server.
     * <p>
     * Defaults to the value of System property
     * com.franz.agraph.repository.blankNodesPerRequest
     * or to 100 if that property has not been set.
     *
     * @param amount a positive integer
     */
    public void setBlankNodesPerRequest(int amount) {
        blankNodesPerRequest = amount;
    }

    /**
     * Returns the array of fetched blank node ids
     * <p>
     * Primarily for testing purposes, not for use in apps.
     *
     * @return the array of fetched blank node ids
     */
    public String[] getBlankNodeIds() {
        return blankNodeIds;
    }

    String getNextBNodeId() {
        if (index == -1) {
            requestBlankNodeIds();
        }
        String id = blankNodeIds[index];
        index--;
        // TODO: parse using NTriplesUtil here to create BNode?
        return id.substring(2);   // strip off leading '_:';
    }

    /**
     * Returns a new blank node with the given id.
     * <p>
     * Consider using createBNode() instead to get an AG-allocated id,
     * it is safer (avoids unintended blank node conflicts) and can be
     * stored more efficiently.
     * <p>
     * If id is null or empty, returns a unique BNode with AG-allocated
     * id; otherwise, returns a BNode with the given (a.k.a. "external")
     * id (careful to avoid blank node conflicts).  See the javadoc for
     * allowing external blank nodes for more discussion.
     *
     * @see AGHttpRepoClient#setAllowExternalBlankNodeIds(boolean)
     * @see AGRepositoryConnection#prepareHttpRepoClient()
     */
    @Override
    public BNode createBNode(String nodeID) {
        if (nodeID == null || "".equals(nodeID)) {
            nodeID = getNextBNodeId();
        }
        return super.createBNode(nodeID);
    }

    /**
     * Returns a new blank node.
     * <p>
     * If this value factory is for an AGRepository, returns a new BNode
     * with an AG-allocated id; otherwise, returns a new BNode with an
     * "external" id (using ValueFactoryImpl).  See also the javadoc for
     * allowing external blank nodes for more discussion.
     *
     * @see AGHttpRepoClient#setAllowExternalBlankNodeIds(boolean)
     * @see AGRepositoryConnection#prepareHttpRepoClient()
     */
    @Override
    public BNode createBNode() {
        if (repository != null) {
            return createBNode(null);
        } else {
            return super.createBNode();
        }
    }

    /**
     * Creates an OpenRDF Value from a concrete Jena Node.
     *
     * @param node a concrete Jena node
     * @return the corresponding Value
     */
    public Value asValue(Node node) {
        Value val;
        if (node == null || node == Node.ANY) {
            val = null;
        } else if (node.isURI()) {
            val = createIRI(node.getURI());
        } else if (node.isBlank()) {
            String id = node.getBlankNodeLabel();
            val = createBNode(id);
        } else if (node.isLiteral()) {
            String lang = node.getLiteralLanguage();
            String datatypeURI = node.getLiteralDatatypeURI();
            if (lang != null && !lang.equals("")) {
                if (datatypeURI != null
                        && !datatypeURI.equals(RDF_LANG_STRING)) {
                    String msg = String.format(
                            "Wrong tagged literal type: %s, should be: %s",
                            datatypeURI, RDF_LANG_STRING);
                    throw new IllegalArgumentException(msg);
                }
                val = createLiteral(node.getLiteralLexicalForm(), lang);
            } else if (datatypeURI != null) {
                IRI datatype = createIRI(datatypeURI);
                val = createLiteral(node.getLiteralLexicalForm(), datatype);
            } else {
                // TODO
                val = createLiteral(node.getLiteralLexicalForm());
            }
        } else {
            throw new IllegalArgumentException("Cannot convert Node to Value: " + node);
        }
        return val;
    }

    /**
     * Returns true iff id looks like an AG blank node id.
     * <p>
     * AG blank node ids currently have the following form:
     * <p>
     * b[store ID in hex]x[blank node number].
     * <p>
     * There is nothing sacrosanct about this but it is unlikely
     * to change.
     *
     * @param id the string to be tested
     * @return true iff id looks like an AG blank node id
     */
    public boolean isAGBlankNodeId(String id) {
        return AG_BNODE_ID_PATTERN.matcher(id).matches();
    }

    public boolean isURIForExternalBlankNode(Value v) {
        return v.stringValue().startsWith(PREFIX_FOR_EXTERNAL_BNODES);
    }

    public Resource asResource(Node node) {
        Resource res;
        if (node == null || node == Node.ANY) {
            res = null;
        } else if (node.isURI()) {
            String uri = node.getURI();
            // See comment for JENA_EMPTY_URL
            if (uri.isEmpty()) {
                uri = JENA_EMPTY_URL;
            }
            res = createIRI(uri);
        } else if (node.isBlank()) {
            res = createBNode(node.getBlankNodeLabel());
        } else {
            throw new IllegalArgumentException("Cannot convert Node to Resource: " + node);
        }
        return res;
    }

    public IRI asURI(Node node) {
        IRI uri;
        if (node == null || node == Node.ANY) {
            uri = null;
        } else if (node.isURI()) {
            uri = createIRI(node.getURI());
        } else if (node.isBlank()) {
            // TODO: research this more, seems to be needed for the test
            // suite, as blank nodes appear in the predicate position
            uri = createIRI("http://anon/" + node.getBlankNodeLabel());
        } else {
            throw new IllegalArgumentException("Cannot convert Node to URI: " + node);
        }
        return uri;
    }


    /***********************
     *
     * Encodable Namespaces
     *
     ***********************/

    /**
     * Returns unique URIs within the specified encodable namespace.
     * <p>The generated URIs will conform to the format that was specified
     * when the encodable namespace was registered, and are guaranteed
     * to be unique for this namespace generator.  Note that this does
     * not prevent other parties from independently using URIs that
     * involve this namespace, however.</p>
     * <p>If amount cannot be generated, up to amount URIs will be returned,
     * or an exception will be thrown if none are available.</p>
     *
     * @param namespace encodable namespace from which the URIs will be generated
     * @param amount    the number of URIs to generate
     * @return a unique URI within the specified namespace.
     * @throws RepositoryException if there is an error with this request
     * @see AGRepositoryConnection#registerEncodableNamespace(String, String)
     * @see #generateURI(String)
     */
    public IRI[] generateURIs(String namespace, int amount) throws RepositoryException {
        String[] uri_strs;
        IRI[] uris;
        try {
            uri_strs = getHTTPClient().generateURIs(getRepository().getRepositoryURL(), namespace, amount);
            uris = new IRI[uri_strs.length];
            for (int i = 0; i < uri_strs.length; i++) {
                uris[i] = NTriplesUtil.parseURI(uri_strs[i], this);
            }
        } catch (AGHttpException e) {
            throw new RepositoryException(e);
        }
        return uris;
    }

    /**
     * Returns a unique URI within the specified encodable namespace.
     * <p>The generated URI will conform to the format that was specified
     * when the encodable namespace was registered, and is guaranteed
     * to be unique for this namespace generator.  Note that this does
     * not prevent other parties from independently using URIs that
     * involve this namespace, however.</p>
     *
     * @param registeredEncodableNamespace encodable namespace from which the URI will be generated
     * @return a unique URI within the specified namespace.
     * @throws RepositoryException if there is an error with this request
     * @see AGRepositoryConnection#registerEncodableNamespace(String, String)
     * @see #generateURIs(String, int)
     */
    public IRI generateURI(String registeredEncodableNamespace) throws RepositoryException {
        return generateURIs(registeredEncodableNamespace, 1)[0];
    }

}
