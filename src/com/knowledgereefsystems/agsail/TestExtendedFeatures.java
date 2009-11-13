package com.knowledgereefsystems.agsail;

import java.io.File;

/**
 * Author: josh
 * Date: Jun 27, 2008
 * Time: 6:39:39 PM
 */
public class TestExtendedFeatures extends AllegroSailTestCase {
    public TestExtendedFeatures(final String name) throws Exception {
        super(name);
    }

    public void testFreetext() throws Exception {
        //loadTrig(TestExtendedFeatures.class.getResource("freetextTest.trig"));
        loadTrig(new File("resources/com/knowledgereefsystems/agsail/freetextTest.trig"));

        /*
    	ValueFactory vf = sail.getValueFactory();
    	URI conquest = vf.createURI("http://knowledgereefsystems.com/agsail/test/conquest");
    	URI war = vf.createURI("http://knowledgereefsystems.com/agsail/test/war");
    	URI pestilence = vf.createURI("http://knowledgereefsystems.com/agsail/test/pestilence");
    	URI death = vf.createURI("http://knowledgereefsystems.com/agsail/test/death");
    	URI freetextContext = vf.createURI("http://knowledgereefsystems.com/agsail/test/freetext");

    	AllegroSailConnection sc = sail.getConnection();
    	sc.registerFreetextPredicate(RDFS.LABEL);
    	sc.registerFreetextPredicate(RDFS.COMMENT);

    	Collection<Statement> st;

    	// Complete match in the default context
    	st = asCollection(sc.getFreetextStatements("Conquest"));
    	assertEquals(1, st.size());
    	assertEquals(conquest, st.iterator().next().getSubject());
    	assertEquals(null, st.iterator().next().getContext());
    	// Complete match in a specific context
    	st = asCollection(sc.getFreetextStatements("Pestilence"));
    	assertEquals(1, st.size());
    	assertEquals(pestilence, st.iterator().next().getSubject());
    	assertEquals(freetextContext, st.iterator().next().getContext());

    	// Wildcard match in the default context
    	st = asCollection(sc.getFreetextStatements("?ar"));
    	assertEquals(1, st.size());
    	assertEquals(war, st.iterator().next().getSubject());
    	assertEquals(null, st.iterator().next().getContext());
    	// Wildcard match in a specific context
    	st = asCollection(sc.getFreetextStatements("D?a??"));
    	assertEquals(1, st.size());
    	assertEquals(death, st.iterator().next().getSubject());
    	assertEquals(freetextContext, st.iterator().next().getContext());
    	sc.close();

    	// Wildcard sequence match
    	st = asCollection(sc.getFreetextStatements("carries *"));
    	assertEquals(4, st.size());
    	sc.close();

    	// Lazy wildcard sequence match
    	st = asCollection(sc.getFreetextStatements("rides a * horse"));
    	assertEquals(4, st.size());
    	sc.close();*/

        // TODO: test AllegroSailConnetion.getFreetextUniqueSubjects
    }
}