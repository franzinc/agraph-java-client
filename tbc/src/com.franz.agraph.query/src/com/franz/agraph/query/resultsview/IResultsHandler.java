/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.resultsview;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.topbraid.sparql.update.UpdateChange;

import com.franz.agraph.jena.AGQuery;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public abstract interface IResultsHandler {
		  
	  public abstract void handleAsk(List<Integer> arg0);
		  
	  public abstract void handleConstructedModel(Model arg0);
		  
	  public abstract void handleResultSet(AGQuery arg0, QueryExecution arg1, ResultSet arg2);
		  
	  public abstract void handleResultSet(QueryExecution arg0, ResultSet arg1, IProgressMonitor arg2);
		  
	  public abstract void handleUpdate(UpdateChange arg0);
}
