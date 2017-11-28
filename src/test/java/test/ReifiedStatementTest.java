/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package test;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RSIterator;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.IRI;
import org.junit.Test;

public class ReifiedStatementTest extends AGAbstractTest {

    @Test
    public void test1() throws Exception {

        AGGraphMaker maker = new AGGraphMaker(conn);

        AGGraph graph = maker.getGraph();
        AGModel model = new AGModel(graph);
        String exns = "http://example.org/people/";
        model.setNsPrefix("ex", exns);
        // Create index1
        Property fullname = model.createProperty(exns + "fullname");

        conn.createFreetextIndex("index1", new IRI[] {conn.getValueFactory()
                .createIRI(exns + "fullname")});
        // Create parts of person resources.
        Resource alice = model.createResource(exns + "alice1");
        Resource carroll = model.createResource(exns + "carroll");
        Resource persontype = model.createResource(exns + "Person");
        Literal alicename = model.createLiteral("Alice B. Toklas");
        Literal lewisCarroll = model.createLiteral("Lewis Carroll");
        // Create parts of book resources.
        Resource book = model.createResource(exns + "book1");
        Resource booktype = model.createResource(exns + "Book");
        Property booktitle = model.createProperty(exns + "title");
        Property author = model.createProperty(exns + "author");
        Literal wonderland = model.createLiteral("Alice in Wonderland");
        // Add Alice B. Toklas triples
        model.add(alice, RDF.type, persontype);
        Statement stmt = model.createStatement(alice, fullname, alicename);
        //model.add(stmt);

        // reify statement [http://example.org/people/alice1,
        // http://example.org/people/fullname, "Alice B. Toklas"]
        ReifiedStatement rs = stmt.createReifiedStatement();
        rs.addProperty(ResourceFactory.createProperty("urn:foo:fooProperty"),
                "FOOBAR");
        // Add Alice in Wonderland triples
        model.add(book, RDF.type, booktype);
        model.add(book, booktitle, wonderland);
        model.add(book, author, carroll);
        // Add Lewis Carroll triples
        model.add(carroll, RDF.type, persontype);
        model.add(carroll, fullname, lewisCarroll);
        // Check triples

        StmtIterator statements = model.listStatements();
        try {
            while (statements.hasNext()) {
                System.out.println(statements.next());
            }
        } finally {
            statements.close();
        }

        System.out.println("\nN-TRIPLE output:");
        model.write(System.out, "N-TRIPLE");
        long l = model.size();
        System.out.println("Number of stmts: " + l);
    }

    @Test
    public void test2() throws Exception {

        AGGraphMaker maker = new AGGraphMaker(conn);

        AGGraph graph = maker.getGraph();
        AGModel model = new AGModel(graph);
        String exns = "http://example.org/people/";
        model.setNsPrefix("ex", exns);
        // Create index1
        Property fullname = model.createProperty(exns + "fullname");

        conn.createFreetextIndex("index1", new IRI[] {conn.getValueFactory()
                .createIRI(exns + "fullname")});
        // Create parts of person resources.
        Resource alice = model.createResource(exns + "alice1");
        Resource carroll = model.createResource(exns + "carroll");
        Resource persontype = model.createResource(exns + "Person");
        Literal alicename = model.createLiteral("Alice B. Toklas");
        Literal lewisCarroll = model.createLiteral("Lewis Carroll");
        // Create parts of book resources.
        Resource book = model.createResource(exns + "book1");
        Resource booktype = model.createResource(exns + "Book");
        Property booktitle = model.createProperty(exns + "title");
        Property author = model.createProperty(exns + "author");
        Literal wonderland = model.createLiteral("Alice in Wonderland");
        // Add Alice B. Toklas triples
        model.add(alice, RDF.type, persontype);
        Statement stmt = model.createStatement(alice, fullname, alicename);
        //model.add(stmt);

        // reify statement [http://example.org/people/alice1,
        // http://example.org/people/fullname, "Alice B. Toklas"]
        ReifiedStatement rs = stmt.createReifiedStatement();
        rs.addProperty(ResourceFactory.createProperty("urn:foo:fooProperty"),
                "FOOBAR");
        // Add Alice in Wonderland triples
        model.add(book, RDF.type, booktype);
        model.add(book, booktitle, wonderland);
        model.add(book, author, carroll);
        // Add Lewis Carroll triples
        model.add(carroll, RDF.type, persontype);
        model.add(carroll, fullname, lewisCarroll);

        StmtIterator statements = model.listStatements();
        try {
            while (statements.hasNext()) {
                System.out.println(statements.next());
            }
        } finally {
            statements.close();
        }

        // try to get the reification triple:
        System.out.println("Reified Statements:");
        RSIterator iter = stmt.listReifiedStatements();
        while (iter.hasNext()) {
            ReifiedStatement rs1 = iter.next();
            System.out.println(rs1);

            System.out.println("listProperties:");
            StmtIterator iter1 = rs1.listProperties();
            while (iter1.hasNext()) {
                System.out.println(iter1.next());
            }
        }
    }
}
