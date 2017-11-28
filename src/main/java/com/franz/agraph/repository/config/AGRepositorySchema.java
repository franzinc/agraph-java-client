/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository.config;

import com.franz.agraph.repository.AGRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;

/**
 * Defines constants for the AGRepository schema which is used by
 * {@link AGRepositoryFactory}s to initialize {@link AGRepository}s.
 */
public class AGRepositorySchema {

    /**
     * The AGRepository schema namespace (<tt>http://franz.com/agraph/repository/config#</tt>).
     */
    public static final String NAMESPACE = "http://franz.com/agraph/repository/config#";

    /**
     * <tt>http://franz.com/agraph/repository/config#serverUrl</tt>
     */
    public final static IRI SERVERURL;

    /**
     * <tt>http://franz.com/agraph/repository/config#username</tt>
     */
    public final static IRI USERNAME;

    /**
     * <tt>http://franz.com/agraph/repository/config#password</tt>
     */
    public final static IRI PASSWORD;

    /**
     * <tt>http://franz.com/agraph/repository/config#catalogId</tt>
     */
    public final static IRI CATALOGID;

    /**
     * <tt>http://franz.com/agraph/repository/config#repositoryId</tt>
     */
    public final static IRI REPOSITORYID;

    static {
        ValueFactory factory = ValueFactoryImpl.getInstance();
        SERVERURL = factory.createIRI(NAMESPACE, "serverUrl");
        USERNAME = factory.createIRI(NAMESPACE, "username");
        PASSWORD = factory.createIRI(NAMESPACE, "password");
        CATALOGID = factory.createIRI(NAMESPACE, "catalogId");
        REPOSITORYID = factory.createIRI(NAMESPACE, "repositoryId");
    }
}
