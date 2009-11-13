package com.knowledgereefsystems.agsail;

import org.openrdf.sail.SailConnection;
import org.openrdf.model.URI;
import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.Statement;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.model.vocabulary.RDFS;

import javax.xml.datatype.XMLGregorianCalendar;

import java.io.File;
import java.util.Iterator;

/**
 * Author: josh
 * Date: Jun 27, 2008
 * Time: 6:39:39 PM
 */
public class TestValues extends AllegroSailTestCase {
    public TestValues(final String name) throws Exception {
        super(name);
    }

    public void testCreateLiteralsThroughValueFactory() throws Exception {
        Literal l;
        XMLGregorianCalendar calendar;
        ValueFactory vf = sail.getValueFactory();

        l = vf.createLiteral("a plain literal");
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("a plain literal", l.getLabel());
        assertNull(l.getDatatype());

        l = vf.createLiteral("auf Deutsch, bitte", "de");
        assertNotNull(l);
        assertEquals("de", l.getLanguage());
        assertEquals("auf Deutsch, bitte", l.getLabel());
        assertNull(l.getDatatype());

        // Test data-typed createLiteral methods
        l = vf.createLiteral("foo", XMLSchema.STRING);
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("foo", l.getLabel());
        assertEquals(XMLSchema.STRING, l.getDatatype());
        l = vf.createLiteral(42);
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("42", l.getLabel());
        assertEquals(42, l.intValue());
        assertEquals(XMLSchema.INT, l.getDatatype());
        l = vf.createLiteral(42l);
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("42", l.getLabel());
        assertEquals(42l, l.longValue());
        assertEquals(XMLSchema.LONG, l.getDatatype());
        l = vf.createLiteral((short) 42);
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("42", l.getLabel());
        assertEquals((short) 42, l.shortValue());
        assertEquals(XMLSchema.SHORT, l.getDatatype());
        l = vf.createLiteral(true);
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("true", l.getLabel());
        assertEquals(true, l.booleanValue());
        assertEquals(XMLSchema.BOOLEAN, l.getDatatype());
        l = vf.createLiteral((byte) 'c');
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("99", l.getLabel());
        assertEquals('c', l.byteValue());
        assertEquals(XMLSchema.BYTE, l.getDatatype());
        calendar = XMLDatatypeUtil.parseCalendar("2002-10-10T12:00:00-05:00");
        l = vf.createLiteral(calendar);
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("2002-10-10T12:00:00-05:00", l.getLabel());
        assertEquals(calendar, l.calendarValue());
        assertEquals(XMLSchema.DATETIME, l.getDatatype());
    }

    public void testGetValuesFromTripleStore() throws Exception {
        Literal l;
        XMLGregorianCalendar calendar;
        ValueFactory vf = sail.getValueFactory();

        //loadTrig(AllegroSail.class.getResource("queryTest.trig"));
        loadTrig(new File("resources/com/knowledgereefsystems/agsail/queryTest.trig"));

        SailConnection sc = sail.getConnection();

        // Get an actual plain literal from the triple store.
        URI ford = vf.createURI("http://knowledgereefsystems.com/agsail/test/ford");
        l = (Literal) toSet(sc.getStatements(ford, RDFS.COMMENT, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("he really knows where his towel is", l.getLabel());
        assertNull(l.getDatatype());

        // Get an actual language-tagged literal from the triple store.
        URI thor = vf.createURI("http://knowledgereefsystems.com/agsail/test/thor");
        URI foafName = vf.createURI("http://xmlns.com/foaf/0.1/name");
        Iterator<Statement> iter = toSet(sc.getStatements(thor, foafName, null, false)).iterator();
        boolean found = false;
        while (iter.hasNext()) {
            l = (Literal) iter.next().getObject();
            if (l.getLanguage().equals("en")) {
                found = true;
                assertEquals("Thor", l.getLabel());
                assertNull(l.getDatatype());
            }
        }
        assertTrue(found);

        // Get an actual data-typed literal from the triple-store.
        URI msnChatID = vf.createURI("http://xmlns.com/foaf/0.1/msnChatID");
        l = (Literal) toSet(sc.getStatements(thor, msnChatID, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("Thorster123", l.getLabel());
        assertEquals(XMLSchema.STRING, l.getDatatype());

        // Test Literal.xxxValue() methods for Literals read from the triple store
        URI valueUri, hasValueUri;
        hasValueUri = vf.createURI("http://knowledgereefsystems.com/agsail/test/hasValue");
        valueUri = vf.createURI("http://knowledgereefsystems.com/agsail/test/stringValue");
        l = (Literal) toSet(sc.getStatements(valueUri, hasValueUri, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("foo", l.getLabel());
        assertEquals(XMLSchema.STRING, l.getDatatype());
        valueUri = vf.createURI("http://knowledgereefsystems.com/agsail/test/byteValue");
        l = (Literal) toSet(sc.getStatements(valueUri, hasValueUri, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("99", l.getLabel());
        assertEquals(XMLSchema.BYTE, l.getDatatype());
        assertEquals('c', l.byteValue());
        valueUri = vf.createURI("http://knowledgereefsystems.com/agsail/test/booleanValue");
        l = (Literal) toSet(sc.getStatements(valueUri, hasValueUri, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("false", l.getLabel());
        assertEquals(XMLSchema.BOOLEAN, l.getDatatype());
        assertEquals(false, l.booleanValue());
        valueUri = vf.createURI("http://knowledgereefsystems.com/agsail/test/intValue");
        l = (Literal) toSet(sc.getStatements(valueUri, hasValueUri, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("42", l.getLabel());
        assertEquals(XMLSchema.INT, l.getDatatype());
        assertEquals(42, l.intValue());
        valueUri = vf.createURI("http://knowledgereefsystems.com/agsail/test/shortValue");
        l = (Literal) toSet(sc.getStatements(valueUri, hasValueUri, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("42", l.getLabel());
        assertEquals(XMLSchema.SHORT, l.getDatatype());
        assertEquals((short) 42, l.shortValue());
        valueUri = vf.createURI("http://knowledgereefsystems.com/agsail/test/longValue");
        l = (Literal) toSet(sc.getStatements(valueUri, hasValueUri, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("42", l.getLabel());
        assertEquals(XMLSchema.LONG, l.getDatatype());
        assertEquals(42l, l.longValue());
        valueUri = vf.createURI("http://knowledgereefsystems.com/agsail/test/floatValue");
        l = (Literal) toSet(sc.getStatements(valueUri, hasValueUri, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("3.1415926", l.getLabel());
        assertEquals(XMLSchema.FLOAT, l.getDatatype());
        assertEquals((float) 3.1415926, l.floatValue());
        valueUri = vf.createURI("http://knowledgereefsystems.com/agsail/test/doubleValue");
        l = (Literal) toSet(sc.getStatements(valueUri, hasValueUri, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("3.1415926", l.getLabel());
        assertEquals(XMLSchema.DOUBLE, l.getDatatype());
        assertEquals(3.1415926, l.doubleValue());
        valueUri = vf.createURI("http://knowledgereefsystems.com/agsail/test/dateTimeValue");
        calendar = XMLDatatypeUtil.parseCalendar("2002-10-10T12:00:00-05:00");
        l = (Literal) toSet(sc.getStatements(valueUri, hasValueUri, null, false)).iterator().next().getObject();
        assertNotNull(l);
        assertNull(l.getLanguage());
        assertEquals("2002-10-10T12:00:00-05:00", l.getLabel());
        assertEquals(XMLSchema.DATETIME, l.getDatatype());
        assertEquals(calendar, l.calendarValue());

        sc.close();
    }

    public void testBlankNodes() throws Exception {
        // TODO
        System.out.println("who needs blank nodes?");
    }
}