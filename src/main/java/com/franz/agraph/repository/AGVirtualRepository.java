/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import com.franz.agraph.http.AGHTTPClient;
import com.franz.agraph.http.AGHttpRepoClient;
import com.franz.agraph.http.exception.AGHttpException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.AbstractRepository;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A class for virtual repositories, used for working with federations,
 * graph-filtered stores, reasoning stores, and compositions thereof.
 * <p>
 * Virtual repositories don't reside on disk (their component stores do);
 * they don't have a catalog.
 * <p>
 * Applications normally create a virtual repository via an AGServer instance.
 *
 * @see AGServer#virtualRepository(String)
 * @see AGServer#federate(AGAbstractRepository...)
 */
public class AGVirtualRepository extends AbstractRepository implements AGAbstractRepository {
    final AGRepository wrapped;
    private final AGServer server;
    private final String spec;
    private final AGValueFactory vf;

    /**
     * Applications normally create a virtual repository via an AGServer instance.
     *
     * @param server  the server hosting this virtual repository
     * @param spec    the store specification for this repository
     * @param wrapped the component repository of this virtual repository
     * @see AGServer#virtualRepository(String)
     * @see AGServer#federate(AGAbstractRepository...)
     */
    public AGVirtualRepository(AGServer server, String spec, AGRepository wrapped) {
        this.server = server;
        this.spec = spec;
        this.wrapped = wrapped;
        vf = new AGValueFactory(wrapped);
    }

    // string-mangling utilities for creating sessions
    public static String federatedSpec(String[] repoSpecs) {
        for (int i = 0; i < repoSpecs.length; i++) {
            if (!repoSpecs[i].startsWith("<")) {
                repoSpecs[i] = "<" + repoSpecs[i] + ">";
            }
        }
        return String.join("+", repoSpecs);
    }

    public static String reasoningSpec(String repoSpec, String reasoner) {
        return reasoningSpec(repoSpec, reasoner, null);
    }

    public static String reasoningSpec(String repoSpec, String reasoner, Resource inferredGraph) {
        String reasoningSpec = repoSpec + "[" + reasoner;
        if (inferredGraph != null) {
            reasoningSpec += ("#<" + inferredGraph.stringValue() + ">");
        }
        return reasoningSpec + "]";
    }

    // interface

    public static String filteredSpec(AGAbstractRepository repo, Resource[] contexts) {
        String[] graphs = new String[contexts.length];
        for (int i = 0; i < contexts.length; i++) {
            if (null == contexts[i]) {
                graphs[i] = null;
            } else {
                graphs[i] = "<" + contexts[i].stringValue() + ">";
            }
        }
        return filteredSpec(repo.getSpec(), graphs);
    }

    public static String filteredSpec(String repoSpec, String[] graphs) {
        repoSpec += "{";
        for (String graph : graphs) {
            repoSpec += " " + graph;
        }
        return repoSpec + "}";
    }

    public AGServer getServer() {
        return server;
    }

    /**
     * Virtual repositories don't reside on disk (their component stores do);
     * they don't have a catalog.
     *
     * @return always returns null
     */
    public AGCatalog getCatalog() {
        return null;
    }

    /**
     * Gets the store specification used to create this virtual repository.
     *
     * @see AGServer#virtualRepository(String)
     */
    public String getSpec() {
        return spec;
    }

    public boolean isWritable() {
        return wrapped != null;
    }

    public AGValueFactory getValueFactory() {
        return vf;
    }

    public AGRepositoryConnection getConnection(final ScheduledExecutorService executor)
            throws RepositoryException {
        AGHTTPClient client = server.getHTTPClient();
        AGHttpRepoClient repoclient;
        try {
            repoclient = new AGHttpRepoClient(
                    this, client, null, client.openSession(spec, true), executor);
        } catch (AGHttpException e) {
            throw new RepositoryException(e);
        }
        return new AGRepositoryConnection(this, repoclient);
    }

    public AGRepositoryConnection getConnection() throws RepositoryException {
        return this.getConnection(server.getExecutor());
    }

    /**
     * Calls Sesame method {@link #shutDown()}.
     */
    @Override
    public void close() throws RepositoryException {
        shutDown();
    }

    @Override
    protected void initializeInternal() throws RepositoryException {
    }

    @Override
    protected void shutDownInternal() throws RepositoryException {
    }

    /**
     * The dataDir is not currently applicable to AllegroGraph.
     *
     * @throws UnsupportedOperationException this operation is unsupported
     * @deprecated not applicable to AllegroGraph
     */
    public File getDataDir() {
        throw new UnsupportedOperationException("getDataDir is inapplicable for AG repositories");
    }

    /**
     * The dataDir is not currently applicable to AllegroGraph.
     *
     * @throws UnsupportedOperationException this operation is unsupported
     * @deprecated not applicable to AllegroGraph
     */
    public void setDataDir(File dataDir) {
        throw new UnsupportedOperationException("setDataDir is inapplicable for AG repositories");
    }
}
