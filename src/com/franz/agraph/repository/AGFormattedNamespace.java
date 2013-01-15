/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

/**
 * @see AGRepositoryConnection#registerEncodableNamespace(String, String)
 */
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
