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
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String qnameFor(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrefixMapping removeNsPrefix(String prefix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean samePrefixMappingAs(PrefixMapping other) {
		// TODO Auto-generated method stub
		return false;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrefixMapping setNsPrefixes(Map<String, String> map) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String shortForm(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrefixMapping withDefaultMappings(PrefixMapping map) {
		// TODO Auto-generated method stub
		return null;
	}

}
