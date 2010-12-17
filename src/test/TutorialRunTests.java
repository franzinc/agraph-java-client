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
