/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.config.AbstractRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

import java.util.Set;

import static com.franz.agraph.repository.config.AGRepositorySchema.CATALOGID;
import static com.franz.agraph.repository.config.AGRepositorySchema.PASSWORD;
import static com.franz.agraph.repository.config.AGRepositorySchema.REPOSITORYID;
import static com.franz.agraph.repository.config.AGRepositorySchema.SERVERURL;
import static com.franz.agraph.repository.config.AGRepositorySchema.USERNAME;

/**
 * Configuration for an AllegroGraph Repository.
 */
public class AGRepositoryConfig extends AbstractRepositoryImplConfig {

    private String serverUrl;
    private String username;
    private String password;
    private String catalogId;
    private String repositoryId;

    public AGRepositoryConfig() {
        super(AGRepositoryFactory.REPOSITORY_TYPE);
    }

    public AGRepositoryConfig(String url) {
        this();
        setServerUrl(url);
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String url) {
        this.serverUrl = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(String catalogId) {
        this.catalogId = catalogId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Override
    public void validate()
            throws RepositoryConfigException {
        super.validate();
        if (serverUrl == null) {
            throw new RepositoryConfigException("No serverUrl specified for AG repositoryId");
        }
        if (username == null) {
            throw new RepositoryConfigException("No username specified for AG repositoryId");
        }
        if (password == null) {
            throw new RepositoryConfigException("No password specified for AG repositoryId");
        }
        if (catalogId == null) {
            throw new RepositoryConfigException("No catalogId specified for AG repositoryId");
        }
        if (repositoryId == null) {
            throw new RepositoryConfigException("No repositoryId specified for AG repositoryId");
        }
    }

    @Override
    public Resource export(Model graph) {
        Resource implNode = super.export(graph);

        if (serverUrl != null) {
            graph.add(implNode, SERVERURL, getValueFactory().createIRI(serverUrl));
        }
        if (username != null) {
            graph.add(implNode, USERNAME, getValueFactory().createLiteral(username));
        }
        if (password != null) {
            graph.add(implNode, PASSWORD, getValueFactory().createLiteral(password));
        }
        if (catalogId != null) {
            graph.add(implNode, CATALOGID, getValueFactory().createLiteral(catalogId));
        }
        if (repositoryId != null) {
            graph.add(implNode, REPOSITORYID, getValueFactory().createLiteral(repositoryId));
        }
        return implNode;
    }


    private static Value getObjectValue(Model graph, Resource subject, IRI predicate) {
        Set<Value> objects = graph.filter(subject, predicate, null).objects();
        if (objects.size() == 1) {
            return objects.iterator().next();
        } else if (objects.isEmpty()) {
            throw new RepositoryConfigException(predicate + " not found");
        } else {
            throw new RepositoryConfigException("Multiple " + predicate + " properties found");
        }
    }

    private static String getIRIStringValue(Model graph, Resource subject, IRI predicate) {
        Value value = getObjectValue(graph, subject, predicate);
        if (value.isIRI()) {
            return value.stringValue();
        } else {
            throw new RepositoryConfigException(predicate + " is not an IRI");
        }
    }

    private static String getLiteralStringValue(Model graph, Resource subject, IRI predicate) {
        Value value = getObjectValue(graph, subject, predicate);
        if (value.isLiteral()) {
            return value.stringValue();
        } else {
            throw new RepositoryConfigException(predicate + " is not a literal");
        }
    }

    @Override
    public void parse(Model graph, Resource implNode) throws RepositoryConfigException {
        super.parse(graph, implNode);
        setServerUrl(getIRIStringValue(graph, implNode, SERVERURL));
        setUsername(getLiteralStringValue(graph, implNode, USERNAME));
        setPassword(getLiteralStringValue(graph, implNode, PASSWORD));
        setCatalogId(getLiteralStringValue(graph, implNode, CATALOGID));
        setRepositoryId(getLiteralStringValue(graph, implNode, REPOSITORYID));
    }

    private ValueFactory getValueFactory() {
        return SimpleValueFactory.getInstance();
    }
}
