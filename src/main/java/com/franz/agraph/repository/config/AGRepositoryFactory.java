/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository.config;

import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGServer;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;

/**
 * A {@link RepositoryFactory} that creates {@link AGRepository}s based on
 * RDF configuration data.
 */
public class AGRepositoryFactory implements RepositoryFactory {

    /**
     * The type of repositories that are created by this factory.
     *
     * @see RepositoryFactory#getRepositoryType()
     */
    public static final String REPOSITORY_TYPE = "allegrograph:AGRepository";

    /**
     * Returns the repository's type: <tt>allegrograph:AGRepository</tt>.
     */
    public String getRepositoryType() {
        return REPOSITORY_TYPE;
    }

    public AGRepositoryConfig getConfig() {
        return new AGRepositoryConfig();
    }

    public Repository getRepository(RepositoryImplConfig config)
            throws RepositoryConfigException {
        AGRepository result;
        if (config instanceof AGRepositoryConfig) {
            AGRepositoryConfig agconfig = (AGRepositoryConfig) config;
            AGServer server = new AGServer(agconfig.getServerUrl(), agconfig.getUsername(), agconfig.getPassword());
            try {
                result = server.createCatalog(agconfig.getCatalogId()).createRepository(agconfig.getRepositoryId());
            } catch (RepositoryException e) {
                throw new RepositoryConfigException(e);
            }
        } else {
            throw new RepositoryConfigException("Invalid configuration class: " + config.getClass());
        }
        return result;
    }
}
