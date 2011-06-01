/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import tutorial.TutorialExamples;

public class TutorialRunTests {
	
    @Test
    @Category(TestSuites.Stress.class)
    public void sesameTutorial() throws Exception {
    	TutorialExamples.SERVER_URL = AGAbstractTest.findServerUrl();
    	TutorialExamples.CATALOG_ID = AGAbstractTest.CATALOG_ID;
    	TutorialExamples.main(new String[] {"all"});
    }
    
    @Test
    @Category(TestSuites.Stress.class)
    public void jenaTutorial() throws Exception {
    	TutorialExamples.SERVER_URL = AGAbstractTest.findServerUrl();
    	TutorialExamples.CATALOG_ID = AGAbstractTest.CATALOG_ID;
    	TutorialExamples.main(new String[] {"all"});
    }
    
}
