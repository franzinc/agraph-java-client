/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package com.franz.agraph.query.view;


/**
 * A callback interface allowing the library components to update the buttons
 * of the surrounding SPARQLView without depending on the SPARQLView class.
 * 
 * @author Holger Knublauch
 */
public interface IStatusUpdater {

	void updateStatus();
}
