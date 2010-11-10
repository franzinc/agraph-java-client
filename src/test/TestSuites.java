/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package test;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import test.stress.TransactionStressTest;

public class TestSuites {

    public interface NonPrepushTest {}

    /**
     * Takes too long to be included in prepush.
     */
    public interface Slow extends NonPrepushTest {}
    
    /**
     * Test is not applicable for AGraph for some reason.
     */
    public interface NotApplicableForAgraph extends NonPrepushTest {}
    
    /**
     * Temporary category for developer to run a single test.
     */
    @RunWith(Categories.class)
    @IncludeCategory(Temp.class)
    @SuiteClasses( { TutorialTests.class,
        QuickTests.class,
        AGRepositoryConnectionTests.class,
        JenaTests.class
    })
    public static class Temp {}
    
    /**
     * Suite for 'ant test-prepush' and 'ant prepush'.
     * Expected to pass.
     */
    @RunWith(Categories.class)
    @ExcludeCategory(NonPrepushTest.class)
    @SuiteClasses( { TutorialTests.class,
        QuickTests.class,
        AGRepositoryConnectionTests.class,
        RDFTransactionTest.class,
        JenaTests.class,
        IndexManagementTests.class,
        SpogiTripleCacheTests.class
    })
    public static class Prepush {}
    
    /**
     * Category marker for tests known to be broken.
     * Marker should be removed when test is fixed.
     * 
     * Suite for 'ant test-broken'.
     * Known to fail.
     * When they are fixed, should be categorized as PrepushTest.
     */
    @RunWith(Categories.class)
    @IncludeCategory(Broken.class)
    @ExcludeCategory(NotApplicableForAgraph.class)
    @SuiteClasses( { TutorialTests.class,
        QuickTests.class,
        AGRepositoryConnectionTests.class})
    public static class Broken implements NonPrepushTest {}
    
    /**
     * Category marker for stress tests.
     * 
     * Suite for 'ant test-stress'
     */
    @RunWith(Categories.class)
    @IncludeCategory(Stress.class)
    @SuiteClasses( { TransactionStressTest.class })
    public static class Stress implements NonPrepushTest {}
    
}
