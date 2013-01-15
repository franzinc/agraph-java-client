/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import com.franz.util.Closeable;

/**
 * 
 */
public interface AGAbstractRepository extends Repository, Closeable {

	public String getSpec();
	
	public AGValueFactory getValueFactory();
	
	public AGRepositoryConnection getConnection() throws RepositoryException;
	
	public AGCatalog getCatalog();

}
