/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository.config;

import static com.franz.agraph.repository.config.AGRepositorySchema.CATALOGID;
import static com.franz.agraph.repository.config.AGRepositorySchema.PASSWORD;
import static com.franz.agraph.repository.config.AGRepositorySchema.REPOSITORYID;
import static com.franz.agraph.repository.config.AGRepositorySchema.SERVERURL;
import static com.franz.agraph.repository.config.AGRepositorySchema.USERNAME;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfigBase;

/**
 * Configuration for an AllegroGraph Repository.
 * 
 */
public class AGRepositoryConfig extends RepositoryImplConfigBase {

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
		throws RepositoryConfigException
	{
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
	public Resource export(Graph graph) {
		Resource implNode = super.export(graph);

		if (serverUrl != null) {
			graph.add(implNode, SERVERURL, graph.getValueFactory().createURI(serverUrl));
		}
		if (username != null) {
			graph.add(implNode, USERNAME, graph.getValueFactory().createLiteral(username));
		}
		if (password != null) {
			graph.add(implNode, PASSWORD, graph.getValueFactory().createLiteral(password));
		}
		if (catalogId != null) {
			graph.add(implNode, CATALOGID, graph.getValueFactory().createLiteral(catalogId));
		}
		if (repositoryId != null) {
			graph.add(implNode, REPOSITORYID, graph.getValueFactory().createLiteral(repositoryId));
		}

		return implNode;
	}

	@Override
	public void parse(Graph graph, Resource implNode)
		throws RepositoryConfigException
	{
		super.parse(graph, implNode);

		try {
			URI uri = GraphUtil.getOptionalObjectURI(graph, implNode, SERVERURL);
			if (uri != null) {
				setServerUrl(uri.toString());
			}
			Literal username = GraphUtil.getOptionalObjectLiteral(graph, implNode, USERNAME);
			if (username != null) {
				setUsername(username.getLabel());
			}
			Literal password = GraphUtil.getOptionalObjectLiteral(graph, implNode, PASSWORD);
			if (password != null) {
				setPassword(password.getLabel());
			}
			Literal catalogId = GraphUtil.getOptionalObjectLiteral(graph, implNode, CATALOGID);
			if (catalogId != null) {
				setCatalogId(catalogId.getLabel());
			}
			Literal repositoryId = GraphUtil.getOptionalObjectLiteral(graph, implNode, REPOSITORYID);
			if (repositoryId != null) {
				setRepositoryId(repositoryId.getLabel());
			}
		}
		catch (GraphUtilException e) {
			throw new RepositoryConfigException(e.getMessage(), e);
		}
	}
}
