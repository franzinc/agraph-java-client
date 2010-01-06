/******************************************************************************
** Copyright (c) 2008-2009 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.jena;

import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.Namespace;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import com.hp.hpl.jena.shared.PrefixMapping;

public class AGPrefixMapping implements PrefixMapping {

	AGGraph graph;
	
	public AGPrefixMapping(AGGraph graph) {
		this.graph = graph;
	}

	AGGraph getGraph() {
		return graph;
	}

	@Override
	public String expandPrefix(String prefixed) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public Map<String, String> getNsPrefixMap() {
		Map<String,String> map = new HashMap<String,String>();
		try {
			RepositoryResult<Namespace> result = getGraph().getConnection().getNamespaces();
			while (result.hasNext()) {
				Namespace ns = result.next();
				map.put(ns.getPrefix(), ns.getName());
			}
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
		return map;
	}

	@Override
	public String getNsPrefixURI(String prefix) {
		String uri = null;
		try {
			uri = getGraph().getConnection().getNamespace(prefix);
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
		return uri;
	}

	@Override
	public String getNsURIPrefix(String uri) {
		// TODO speed this up!
		String prefix = null;
		try {
			RepositoryResult<Namespace> result = getGraph().getConnection().getNamespaces();
			while (prefix==null && result.hasNext()) {
				Namespace ns = result.next();
				if (uri.equalsIgnoreCase(ns.getName())) {
					prefix = ns.getPrefix();
				}
			}
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
		return prefix;
	}

	@Override
	public PrefixMapping lock() {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public String qnameFor(String uri) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public PrefixMapping removeNsPrefix(String prefix) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public boolean samePrefixMappingAs(PrefixMapping other) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public PrefixMapping setNsPrefix(String prefix, String uri) {
		try {
			getGraph().getConnection().setNamespace(prefix, uri);
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	@Override
	public PrefixMapping setNsPrefixes(PrefixMapping other) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public PrefixMapping setNsPrefixes(Map<String, String> map) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public String shortForm(String uri) {
		throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

	@Override
	public PrefixMapping withDefaultMappings(PrefixMapping map) {
		return null;
		//throw new UnsupportedOperationException(AGUnsupportedOperation.message);
	}

}
