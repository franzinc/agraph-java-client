/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.query.view.actions;

import org.topbraidcomposer.ui.views.AbstractViewAction;

import com.franz.agraph.query.view.SPARQLView;


public abstract class AbstractSPARQLViewAction extends AbstractViewAction {

	protected SPARQLView getSparqlView() {
		return (SPARQLView) getViewPart();
	}
}
