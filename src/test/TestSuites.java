/******************************************************************************
** Copyright (c) 2008-2016 Franz Inc.
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

import test.callimachus.AGCallimachusTest;
import test.pool.AGConnPoolClosingTest;
import test.pool.AGConnPoolSessionTest;
import test.stress.StreamingTest;
import test.stress.TransactionStressTest;
import test.util.CljTest;

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
    @SuiteClasses( { AGTripleAttributesTest.class,
    	TutorialTests.class,
        QuickTests.class,
        AGRepositoryConnectionTests.class,
        JenaTests.class,
        StreamingTest.class,
        ServerCodeTests.class,
        AGConnPoolSessionTest.class,
        AGConnPoolClosingTest.class,
        SpinTest.class,
        CljTest.class
    })
    public static class Temp {}
    
    /**
     * Suite for 'ant test-prepush' and 'ant prepush'.
     * Expected to pass.
     */
    @RunWith(Categories.class)
    @ExcludeCategory(NonPrepushTest.class)
    @SuiteClasses( { AGCallimachusTest.class,
        AGMaterializerTests.class,
    	MasqueradeAsUserTests.class,
    	UserManagementTests.class,
    	TutorialTests.class,
    	LiteralAndResourceResultsTest.class,
        QuickTests.class,
        AGRepositoryConnectionTests.class,
        RDFTransactionTest.class,
        JenaTests.class,
        BulkModeTests.class,
        IndexManagementTests.class,
        EncodableNamespaceTests.class,
        FederationTests.class,
        SessionTests.class,
    	ServerCodeTests.class,
        SpogiTripleCacheTests.class,
        UploadCommitPeriodTests.class,
        NQuadsTests.class,
        AGGraphQueryTests.class,
        QueryLimitOffsetTests.class,
        UntypedLiteralMatchingTest.class,
        AGConnPoolSessionTest.class,
        BlankNodeTests.class,
        MappingsTests.class,
        DynamicCatalogTests.class,
        SpinTest.class,
        FreetextTests.class,
        DeleteDuplicatesTests.class,
        SparqlUpdateTests.class,
        SparqlDefaultTest.class,
        SparqlDefaultDatasetTest.class,
        AGRepositoryFactoryTest.class,
        CljTest.class,
        AGQueryExecutionTest.class,
        AGTripleAttributesTest.class
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
        AGConnPoolClosingTest.class,
        AGRepositoryConnectionTests.class})
    public static class Broken implements NonPrepushTest {}
    
    /**
     * Category marker for stress tests.
     * 
     * Suite for 'ant test-stress'
     */
    @RunWith(Categories.class)
    @IncludeCategory(Stress.class)
    @ExcludeCategory(Prepush.class)
    @SuiteClasses( { QuickTests.class,
    	TutorialRunTests.class,
    	TransactionStressTest.class,
    	AGConnPoolClosingTest.class,
        AGConnPoolSessionTest.class
    })
    public static class Stress implements NonPrepushTest {}
    
}
