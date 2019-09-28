/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository.config;

import com.franz.agraph.repository.AGRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Defines constants for the AGRepository schema which is used by
 * {@link AGRepositoryFactory}s to initialize {@link AGRepository}s.
 */
public class AGRepositorySchema {

    /**
     * The AGRepository schema namespace (<code>http://franz.com/agraph/repository/config#</code>).
     */
    public static final String NAMESPACE = "http://franz.com/agraph/repository/config#";

    /**
     * <code>http://franz.com/agraph/repository/config#serverUrl</code>
     */
    public final static IRI SERVERURL;

    /**
     * <code>http://franz.com/agraph/repository/config#username</code>
     */
    public final static IRI USERNAME;

    /**
     * <code>http://franz.com/agraph/repository/config#password</code>
     */
    public final static IRI PASSWORD;

    /**
     * <code>http://franz.com/agraph/repository/config#catalogId</code>
     */
    public final static IRI CATALOGID;

    /**
     * <code>http://franz.com/agraph/repository/config#repositoryId</code>
     */
    public final static IRI REPOSITORYID;

    static {
        ValueFactory factory = SimpleValueFactory.getInstance();
        SERVERURL = factory.createIRI(NAMESPACE, "serverUrl");
        USERNAME = factory.createIRI(NAMESPACE, "username");
        PASSWORD = factory.createIRI(NAMESPACE, "password");
        CATALOGID = factory.createIRI(NAMESPACE, "catalogId");
        REPOSITORYID = factory.createIRI(NAMESPACE, "repositoryId");
    }
}
