package com.franz.agraph.repository;


public class AGFormattedNamespace {

	protected final String namespace;
	protected final String format;

	public AGFormattedNamespace(String namespace, String format) {
		this.namespace = namespace;
		this.format = format;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getFormat() {
		return format;
	}

}
