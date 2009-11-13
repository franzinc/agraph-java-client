package com.knowledgereefsystems.agsail;

import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailChangedEvent;
import org.openrdf.sail.SailChangedListener;
import org.openrdf.model.URI;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Author: josh
 * Date: Jun 27, 2008
 * Time: 6:39:39 PM
 */
public class TestListeners extends AllegroSailTestCase {
    public TestListeners(final String name) throws Exception {
        super(name);
    }

//    public void testSailConnectionListeners() throws Exception {
//        SimpleListener listener1 = new SimpleListener(),
//                listener2 = new SimpleListener();
//
//        SailConnection sc = sail.getConnection();
////        sc.addConnectionListener(listener1);
//
//        URI ctxA = sail.getValueFactory().createURI("http://example.org/ctxA");
//
//        sc.removeStatements(null, null, null, ctxA);
////        sc.addConnectionListener(listener2);
//        sc.addStatement(ctxA, ctxA, ctxA, ctxA);
//
////        assertEquals(1, listener1.getRemoved());
////        assertEquals(0, listener2.getRemoved());
////        assertEquals(1, listener1.getAdded());
////        assertEquals(1, listener2.getAdded());
//
//        sc.close();
//    }

    public void testSailChangedListeners() throws Exception {
        final Collection<SailChangedEvent> events = new LinkedList<SailChangedEvent>();

        SailChangedListener listener = new SailChangedListener() {

            public void sailChanged(final SailChangedEvent event) {
                events.add(event);
            }
        };

        sail.addSailChangedListener(listener);

        URI uriA = sail.getValueFactory().createURI("http://example.org/uriA");
        SailConnection sc = sail.getConnection();

        assertEquals(0, events.size());
        sc.addStatement(uriA, uriA, uriA, uriA);
        sc.commit();
        assertEquals(1, events.size());
        SailChangedEvent event = events.iterator().next();
        assertTrue(event.statementsAdded());
        assertFalse(event.statementsRemoved());

        events.clear();
        assertEquals(0, events.size());
        sc.removeStatements(uriA, uriA, uriA, uriA);
        sc.commit();
        assertEquals(1, events.size());
        event = events.iterator().next();
        assertFalse(event.statementsAdded());
        assertTrue(event.statementsRemoved());

        sc.close();
    }
}