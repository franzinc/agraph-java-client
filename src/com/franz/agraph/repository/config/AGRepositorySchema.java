/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.repository.config;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

import com.franz.agraph.repository.AGRepository;

/**
 * Defines constants for the AGRepository schema which is used by
 * {@link AGRepositoryFactory}s to initialize {@link AGRepository}s.
 * 
 */
public class AGRepositorySchema {

	/** The AGRepository schema namespace (<tt>http://franz.com/agraph/repository/config#</tt>). */
	public static final String NAMESPACE = "http://franz.com/agraph/repository/config#";

	/** <tt>http://franz.com/agraph/repository/config#serverUrl</tt> */
	public final static URI SERVERURL;

	/** <tt>http://franz.com/agraph/repository/config#username</tt> */
	public final static URI USERNAME;

	/** <tt>http://franz.com/agraph/repository/config#password</tt> */
	public final static URI PASSWORD;

	/** <tt>http://franz.com/agraph/repository/config#catalogId</tt> */
	public final static URI CATALOGID;
	
	/** <tt>http://franz.com/agraph/repository/config#repositoryId</tt> */
	public final static URI REPOSITORYID;
	
	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		SERVERURL = factory.createURI(NAMESPACE, "serverUrl");
		USERNAME = factory.createURI(NAMESPACE, "username");
		PASSWORD = factory.createURI(NAMESPACE, "password");
		CATALOGID = factory.createURI(NAMESPACE, "catalogId");
		REPOSITORYID = factory.createURI(NAMESPACE, "repositoryId");
	}
}
