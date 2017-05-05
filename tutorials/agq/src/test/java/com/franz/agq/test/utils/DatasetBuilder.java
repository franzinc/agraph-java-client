package com.franz.agq.test.utils;

import com.franz.agraph.repository.AGRepositoryConnection;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A fluent interface for creating datasets.
 *
 * Example:
 * <pre>{@code
 * try (final DatasetBuilder builder = new DatasetBuilder(conn)) {
 *     // Note this 'base' applies to all URIs - absolute URIs are not detected
 *     builder.base("ex://")
 *            // Note how URIs and literals are created from raw values
 *            .s("six").p("divisibleBy").o(1)
 *                                      .o(2)
 *                                      .o(3)
 *                     .p("divides").o(12)
 *            // 'u' is the same as 'o', but coerces strings to uris
 *            // rather than string literals
 *            .s("eight").p("divisibleBy").u("two")
 *                                        .u("four")
 *            // s() means 'take the last object as the subject.
 *            .s().p("divisibleBy").u("two")
 *            // 'b' variants are for blank nodes
 *            .sb("b1").p("something").ob("b1")
 *            // Statements can be quads, not just triples
 *            .g("g1").s("s").p("p").o("o")
 *            // We can define builder-scoped namespaces
 *            .ns("ns1", "http://my.namespace.com/")
 *            .s("ns1", "x").p("p").o("o")
 *            // Built-in namespaces can also be used
 *            // ot creates a typed literal.
 *            .s("s").p("p").ot("1", "xsd", "integer")
 * }
 * }</pre>
 */
public class DatasetBuilder implements AutoCloseable {
    /** Commit all statements when the builder is closed. */
    public static final int COMMIT_WHEN_DONE = -1;
    /** Never call commit. */
    public static final int DO_NOT_COMMIT = 0;

    private final AGRepositoryConnection conn;
    private final int commitSize;
    private final ValueFactory valueFactory;
    private final Map<String, Resource> blankNodes;
    private final Map<String, String> namespaces;

    private int statementsToCommit;
    private String base;
    private Resource graph;
    private Resource subject;
    private URI predicate;
    private Value object;

    /**
     * Calls {@code conn.isActive()}, but throws only {@code RuntimeException}s.
     *
     * @param conn A connection object.
     *
     * @return True if a transaction is active on the connection.
     */
    private static boolean isConnectionActive(final AGRepositoryConnection conn) {
        try {
            return conn.isActive();
        } catch (final RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new builder.
     *
     * @param conn Connection that will be used to create statements
     *             and retrieve namespace definitions.
     * @param commitSize How often should commit be called.
     *                   This can be a positive integer (number of statements),
     *                   {@link #COMMIT_WHEN_DONE} (commit when the builder
     *                   is closed) or {@link #DO_NOT_COMMIT}.
     */
    public DatasetBuilder(final AGRepositoryConnection conn,
                          final int commitSize) {
        this.conn = conn;
        this.commitSize = commitSize;
        this.valueFactory = conn.getValueFactory();
        this.blankNodes = new HashMap<>();
        this.namespaces = new HashMap<>();
        this.statementsToCommit = 0;
    }

    /**
     * Creates a new builder.
     *
     * The commit behavior depends on the state of {@code conn}.
     * If a transaction is active statements will be committed when
     * the builder is closed, otherwise a commit will never happen.
     *
     * @param conn Connection that will be used to create statements
     *             and retrieve namespace definitions.
     */
    public DatasetBuilder(final AGRepositoryConnection conn) {
        this(conn, isConnectionActive(conn) ? COMMIT_WHEN_DONE : DO_NOT_COMMIT);
    }

    /**
     * Creates or retrieves a blank node with given id.
     *
     * All calls with the same id will return the same node.
     *
     * @param blankNodeId Unique node identifier.
     *
     * @return A blank node.
     */
    private Resource getBlankNode(final String blankNodeId) {
        if (!blankNodes.containsKey(blankNodeId)) {
            blankNodes.put(blankNodeId, getBlankNode());
        }
        return blankNodes.get(blankNodeId);
    }

    /**
     * Creates a fresh blank node.
     *
     * @return Blank node.
     */
    private Resource getBlankNode() {
        return valueFactory.createBNode();
    }

    /**
     * Creates a URI object from a namespace name (optional) and a suffix.
     *
     * If namespace is not given uses the current base URI as the prefix.
     * If that is not set then the suffix must be an absolute URI.
     *
     * @param namespace Namespace name or {@code null}.
     * @param suffix Suffix or full absolute URI.
     *
     * @return An URI object.
     */
    private URI getURI(final String namespace, final String suffix) {
        final String prefix;
        if (namespace != null) {
            if (namespaces.containsKey(namespace)) {
                prefix = namespaces.get(namespace);
            } else {
                try {
                    prefix = conn.getNamespace(namespace);
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            prefix = base;
        }
        return prefix == null ?
                valueFactory.createURI(suffix) :
                valueFactory.createURI(prefix, suffix);
    }

    /**
     * Adds a namespace definition to the builder.
     *
     * @param name Namespace ID.
     * @param prefix Namespace URI prefix.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder namespace(final String name, final String prefix) {
        namespaces.put(name, prefix);
        return this;
    }

    /**
     * Adds a namespace definition to the builder.
     *
     * @param name Namespace ID.
     * @param prefix Namespace URI prefix.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder ns(final String name, final String prefix) {
        return namespace(name, prefix);
    }

    /**
     * Sets the base URI (it will be prepended to all URIs without namespaces).
     *
     * Set to {@code null} to remove the base prefix and use absolute URIs.
     *
     * @param base Base URI prefix or {@code null}.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder base(final String base) {
        this.base = base;
        return this;
    }

    /**
     * Sets the graph component and resets all other components.
     *
     * @param graph The graph URI.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder graph(final Resource graph) {
        this.graph = graph;
        this.subject = null;
        this.predicate = null;
        this.object = null;
        return this;
    }

    /**
     * Sets the graph component and resets all other components.
     *
     * @param graph The graph URI.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder g(final Resource graph) {
        return graph(graph);
    }

    /**
     * Sets the graph component and resets all other components.
     *
     * @param namespace The namespace name of the graph URI.
     * @param suffix The suffix of the graph URI.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder g(final String namespace, final String suffix) {
        return graph(getURI(namespace, suffix));
    }

    /**
     * Sets the graph component and resets all other components.
     *
     * @param graph The graph URI (relative to the base URI if that is set).
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder g(final String graph) {
        return g(null, graph);
    }

    /**
     * Sets the subject component.
     *
     * Also resets the predicate and object components.
     *
     * @param subject The subject value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder subject(final Resource subject) {
        this.subject = subject;
        this.predicate = null;
        this.object = null;
        return this;
    }

    /**
     * Sets the subject component to the last object used.
     *
     * Also resets the predicate and object components.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder lastObjectAsSubject() {
        if (object == null) {
           throw new RuntimeException("Need an object for chaining.");
        } if (!(object instanceof Resource)) {
           throw new RuntimeException("Last object is not a resource.");
        }
        return subject((Resource)object);
    }

    /**
     * Sets the subject component to a blank node.
     *
     * Also resets the predicate and object components.
     *
     * @param blankNodeId Blank node identifier.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder blankSubject(final String blankNodeId) {
        return subject(getBlankNode(blankNodeId));
    }

    /**
     * Sets the subject component to a blank node.
     *
     * Also resets the predicate and object components.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder blankSubject() {
        return subject(getBlankNode());
    }

    /**
     * Sets the subject component.
     *
     * Also resets the predicate and object components.
     *
     * @param subject The subject value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder s(final Resource subject) {
        return subject(subject);
    }

    /**
     * Sets the subject component.
     *
     * Also resets the predicate and object components.
     *
     * @param namespace The namespace name of the subject URI.
     * @param suffix The suffix of the subject URI.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder s(final String namespace, final String suffix) {
        return subject(getURI(namespace, suffix));
    }

    /**
     * Sets the subject component.
     *
     * Also resets the predicate and object components.
     *
     * @param subject The subject URI, relative to the base URI if that is set.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder s(final String subject) {
        return s(null, subject);
    }

    /**
     * Sets the subject component to a blank node.
     *
     * Also resets the predicate and object components.
     *
     * @param blankNodeId Blank node identifier.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder sb(final String blankNodeId) {
        return blankSubject(blankNodeId);
    }

    /**
     * Sets the subject component to a blank node.
     *
     * Also resets the predicate and object components.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder sb() {
        return blankSubject();
    }


    /**
     * Sets the subject component to the last object used.
     *
     * Also resets the predicate and object components.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder s() {
        return lastObjectAsSubject();
    }

    /**
     * Sets the predicate component.
     *
     * Also resets the object component.
     *
     * @param predicate The predicate value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder predicate(final URI predicate) {
        if (this.subject == null) {
            throw new RuntimeException("Missing subject.");
        }
        this.predicate = predicate;
        this.object = null;
        return this;
    }

    /**
     * Sets the predicate component.
     *
     * Also resets the object component.
     *
     * @param predicate The predicate value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder p(final URI predicate) {
        return predicate(predicate);
    }

    /**
     * Sets the predicate component.
     *
     * Also resets the object component.
     *
     * @param namespace The namespace name of the predicate URI.
     * @param suffix The suffix of the predicate URI.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder p(final String namespace, final String suffix) {
        return predicate(getURI(namespace, suffix));
    }

    /**
     * Sets the predicate component.
     *
     * Also resets the object component.
     *
     * @param predicate The predicate URI.
     *                  Relative to the base URI if that is set.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder p(final String predicate) {
        return p(null, predicate);
    }

    /**
     * Sets the object component and creates the statement.
     *
     * @param object Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder object(final Value object) {
        if (this.subject == null) {
            throw new RuntimeException("Missing subject.");
        }
        if (this.predicate == null) {
            throw new RuntimeException("Missing predicate.");
        }
        this.object = object;
        addCurrentStatement();
        return this;
    }

    /**
     * Sets the object component to a blank node and creates the statement.
     *
     * @param blankNodeId Blank node identifier.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder blankObject(final String blankNodeId) {
        return object(getBlankNode(blankNodeId));
    }

    /**
     * Sets the object component to a blank node and creates the statement.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder blankObject() {
        return object(getBlankNode());
    }

    /**
     * Sets the object component and creates the statement.
     *
     * @param object Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder o(final Value object) {
        return object(object);
    }

//    Need newer Sesame
//    public DatasetBuilder o(final BigDecimal value) {
//        return o(valueFactory.createLiteral(value));
//    }
//
//    public DatasetBuilder o(final BigInteger value) {
//        return o(valueFactory.createLiteral(value));
//    }

    /**
     * Sets the object to a boolean literal and creates the statement.
     *
     * @param value Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder o(final boolean value) {
        return o(valueFactory.createLiteral(value));
    }

    /**
     * Sets the object to a byte literal and creates the statement.
     *
     * @param value Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder o(final byte value) {
        return o(valueFactory.createLiteral(value));
    }

    /**
     * Sets the object to a datetime literal and creates the statement.
     *
     * @param value Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder o(final Date value) {
        return o(valueFactory.createLiteral(value));
    }

    /**
     * Sets the object to a double literal and creates the statement.
     *
     * @param value Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder o(final double value) {
        return o(valueFactory.createLiteral(value));
    }

    /**
     * Sets the object to a float literal and creates the statement.
     *
     * @param value Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder o(final float value) {
        return o(valueFactory.createLiteral(value));
    }

    /**
     * Sets the object to an integer literal and creates the statement.
     *
     * Note: this is different from the typical ValueFactory behavior
     * of converting ints to int literals.
     *
     * @param value Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder o(final int value) {
        return o(valueFactory.createLiteral(Integer.toString(value), XMLSchema.INTEGER));
    }

    /**
     * Sets the object to a long literal and creates the statement.
     *
     * @param value Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder o(final long value) {
        return o(valueFactory.createLiteral(value));
    }

    /**
     * Sets the object to a short literal and creates the statement.
     *
     * @param value Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder o(final short value) {
        return o(valueFactory.createLiteral(value));
    }

    /**
     * Sets the object to a string literal and creates the statement.
     *
     * @param value Object value.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder o(final String value) {
        return o(valueFactory.createLiteral(value));
    }

    /**
     * Sets the object to a typed literal and creates the statement.
     *
     * @param value Object value.
     * @param datatype Object type.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder ot(final String value, final URI datatype) {
        return o(valueFactory.createLiteral(value, datatype));
    }

    /**
     * Sets the object to a typed literal and creates the statement.
     *
     * @param value Object value.
     * @param datatype Object type (relative to the base URI).
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder ot(final String value, final String datatype) {
        return o(valueFactory.createLiteral(value, getURI(null, datatype)));
    }

    /**
     * Sets the object to a typed literal and creates the statement.
     *
     * @param value Object value.
     * @param datatypeNamespace Namespace name of the object type.
     * @param datatypeSuffix Suffix of the object type URI.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder ot(final String value,
                             final String datatypeNamespace,
                             final String datatypeSuffix) {
        return o(valueFactory.createLiteral(
                value, getURI(datatypeNamespace, datatypeSuffix)));
    }

    /**
     * Sets the object to a tagged literal and creates the statement.
     *
     * @param value Object value.
     * @param lang Language tag.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder ol(final String value, final String lang) {
        return o(valueFactory.createLiteral(value, lang));
    }

    /**
     * Sets the object component to a blank node and creates the statement.
     *
     * @param blankNodeId Blank node identifier.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder ob(final String blankNodeId) {
        return blankObject(blankNodeId);
    }

    /**
     * Sets the object component to a blank node and creates the statement.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder ob() {
        return blankObject();
    }

    /**
     * Sets the object component to an URI and creates the statement.
     *
     * @param object Object URI.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder u(final Resource object) {
        return object(object);
    }


    /**
     * Sets the object component to an URI and creates the statement.
     *
     * @param namespace Namespace name of the object URI.
     * @param suffix Suffix of the object URI.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder u(final String namespace, final String suffix) {
        return object(getURI(namespace, suffix));
    }

    /**
     * Sets the object component to an URI and creates the statement.
     *
     * @param object Object URI, relative to the base URI if that is set.
     *
     * @return The builder (for chaining).
     */
    public DatasetBuilder u(final String object) {
        return u(null, object);
    }

    /**
     * Sends the statement to the connection and commits if necessary.
     */
    private void addCurrentStatement() {
        final Resource[] graphs;
        if (graph == null) {
            graphs = new Resource[] {};
        } else {
            graphs = new Resource[] { graph };
        }
        try {
            conn.add(subject, predicate, object, graphs);
        } catch (final RepositoryException e) {
            throw new RuntimeException(e);
        }
        statementsToCommit += 1;
        if (commitSize > 0 && statementsToCommit >= commitSize) {
            try {
                conn.commit();
            } catch (final RepositoryException e) {
                throw new RuntimeException(e);
            }
            statementsToCommit = 0;
        }
    }

    /**
     * Commits the final batch of statements if necessary.
     *
     * Also checks if there are no dangling g()/s()/p() calls
     * (i.e. partially constructed statements).
     */
    @Override
    public void close() {
        if (this.object == null &&
                (this.subject != null || this.graph != null)) {
            throw new RuntimeException("Unfinished statement.");
        }
        if (commitSize != DO_NOT_COMMIT && statementsToCommit > 0) {
            try {
                conn.commit();
            } catch (final RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
