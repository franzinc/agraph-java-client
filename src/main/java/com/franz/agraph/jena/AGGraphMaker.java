/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import com.franz.agraph.repository.AGRepositoryConnection;
import org.apache.jena.graph.GraphMaker;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.shared.AlreadyExistsException;
import org.apache.jena.shared.DoesNotExistException;
import org.apache.jena.util.CollectionFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NiceIterator;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.io.Closeable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements the Jena GraphMaker interface for AllegroGraph.
 */
public class AGGraphMaker implements GraphMaker, Closeable {
    // TODO make this persistent?
    protected Map<String, AGGraph> created = CollectionFactory.createHashedMap();
    private AGRepositoryConnection conn;
    private AGGraph defaultGraph;

    public AGGraphMaker(AGRepositoryConnection conn) {
        this.conn = conn;
        // It's common enough for Jena applications to use ResourceFactory to
        // create new blank nodes, so experimentally enable this by default
        conn.prepareHttpRepoClient().setAllowExternalBlankNodeIds(true);
    }

    public AGRepositoryConnection getRepositoryConnection() {
        return conn;
    }

    @Override
    public void close() {
    }

    @Override
    public AGGraph getGraph() {
        if (defaultGraph == null) {
            defaultGraph = new AGGraph(this, null);
        }
        return defaultGraph;
    }

    @Override
    public AGGraph createGraph() {
        // Get an unused blank node id from the server.  Note that using
        // createAnon() would generate an illegal id based on a uuid.
        String id = conn.getValueFactory().createBNode().getID();
        Node anon = NodeFactory.createBlankNode(id);
        return new AGGraph(this, anon);
    }

    @Override
    public AGGraph createGraph(String uri) {
        return createGraph(uri, false);
    }

    @Override
    public AGGraph createGraph(String uri, boolean strict) {
        AGGraph g = created.get(uri);
        if (g == null) {
            Node node = NodeFactory.createURI(absUriFromString(uri));
            g = new AGGraph(this, node);
            created.put(uri, g);
        } else if (strict) {
            throw new AlreadyExistsException(uri);
        }
        return g;
    }

    private String absUriFromString(String name) {
        String uri = name;
        if (name.indexOf(':') < 0) {
            // TODO: absolute uri's must contain a ':'
            // GraphMaker tests don't supply absolute URI's
            uri = "urn:x-franz:" + name;
        }
        return uri;
    }

    @Override
    public boolean hasGraph(String uri) {
        return null != created.get(uri);
    }

    @Override
    public ExtendedIterator<String> listGraphs() {
        return new NiceIterator<String>().andThen(created.keySet().iterator());
    }

    @Override
    public AGGraph openGraph() {
        return getGraph();
    }

    @Override
    public AGGraph openGraph(String name) {
        return openGraph(name, false);
    }

    @Override
    public AGGraph openGraph(String uri, boolean strict) {
        AGGraph g = created.get(uri);
        if (g == null) {
            if (strict) {
                throw new DoesNotExistException(uri);
            } else {
                Node node = NodeFactory.createURI(absUriFromString(uri));
                g = new AGGraph(this, node);
                created.put(uri, g);
            }
        }
        return g;
    }

    @Override
    public void removeGraph(String uri) {
        AGGraph g = created.get(uri);
        if (g == null) {
            throw new DoesNotExistException(uri);
        } else {
            try {
                g.getConnection().clear(g.getGraphContext());
                created.remove(uri);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Returns the union of all graphs (includes default graph).
     * <p>
     * Add operations on this graph are housed in the
     * default graph.
     *
     * @return the union of all graphs.
     */
    public AGGraph getUnionOfAllGraphs() {
        return createUnion();
    }

    /**
     * Returns a graph that is the union of specified graphs.
     * By convention, the first graph mentioned will be used
     * for add operations on the union.  All other operations
     * will apply to all graphs in the union.  If no graphs
     * are supplied, the union of all graphs is assumed, and
     * add operations apply to the default graph.
     *
     * @param graphs the graphs in the union
     * @return the union of the specified graphs
     */
    public AGGraphUnion createUnion(AGGraph... graphs) {
        Set<Resource> contexts = new HashSet<>();
        for (AGGraph g : graphs) {
            contexts.addAll(Arrays.asList(g.getGraphContexts()));
        }
        Resource context = null;
        if (graphs.length > 0) {
            context = graphs[0].getGraphContext();
        }
        return new AGGraphUnion(this, context, contexts.toArray(new Resource[contexts.size()]));
    }

}
