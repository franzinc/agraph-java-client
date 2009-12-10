package com.franz.agraph.jena;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.rdf.model.Alt;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelChangedListener;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.NsIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.RSIterator;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceF;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Seq;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Derivation;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.shared.Command;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.ReificationStyle;

public class AGInfModel implements InfModel {

	public AGInfModel(AGReasoner reasoner, AGModel model) {
		// TODO Auto-generated constructor stub
	}

	public AGInfModel(AGInfGraph inferencer2) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Model getDeductionsModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Derivation> getDerivation(Statement statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model getRawModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Reasoner getReasoner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listStatements(Resource subject, Property predicate,
			RDFNode object, Model posit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void prepare() {
		// TODO Auto-generated method stub

	}

	@Override
	public void rebind() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDerivationLogging(boolean logOn) {
		// TODO Auto-generated method stub

	}

	@Override
	public ValidityReport validate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model abort() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(Statement s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(Statement[] statements) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(List<Statement> statements) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(StmtIterator iter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(Model m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(Model m, boolean suppressReifications) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model begin() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public Model commit() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(Statement s) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(Resource s, Property p) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(Resource s, Property p, RDFNode o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(StmtIterator iter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Model model) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAny(StmtIterator iter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAny(Model model) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsResource(RDFNode r) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RDFList createList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RDFList createList(Iterator<? extends RDFNode> members) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RDFList createList(RDFNode[] members) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createLiteral(String v, String language) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createLiteral(String v, boolean wellFormed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Property createProperty(String nameSpace, String localName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReifiedStatement createReifiedStatement(Statement s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReifiedStatement createReifiedStatement(String uri, Statement s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource createResource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource createResource(AnonId id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource createResource(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createStatement(Resource s, Property p, RDFNode o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(String lex, RDFDatatype dtype) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(Object value, RDFDatatype dtype) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model difference(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object executeInTransaction(Command cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource getAnyReifiedStatement(Statement s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Lock getLock() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Property getProperty(String nameSpace, String localName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement getProperty(Resource s, Property p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReificationStyle getReificationStyle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement getRequiredProperty(Resource s, Property p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource getResource(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean independent() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Model intersection(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isIsomorphicWith(Model g) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReified(Statement s) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public NsIterator listNameSpaces() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NodeIterator listObjects() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NodeIterator listObjectsOfProperty(Property p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NodeIterator listObjectsOfProperty(Resource s, Property p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RSIterator listReifiedStatements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RSIterator listReifiedStatements(Statement st) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listResourcesWithProperty(Property p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listResourcesWithProperty(Property p, RDFNode o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listStatements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listStatements(Selector s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listStatements(Resource s, Property p, RDFNode o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listSubjects() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listSubjectsWithProperty(Property p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listSubjectsWithProperty(Property p, RDFNode o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model notifyEvent(Object e) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model query(Selector s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model read(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model read(InputStream in, String base) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model read(Reader reader, String base) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model read(String url, String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model read(InputStream in, String base, String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model read(Reader reader, String base, String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model read(String url, String base, String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model register(ModelChangedListener listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model remove(Statement[] statements) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model remove(List<Statement> statements) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model remove(Statement s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model removeAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model removeAll(Resource s, Property p, RDFNode r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeAllReifications(Statement s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeReification(ReifiedStatement rs) {
		// TODO Auto-generated method stub

	}

	@Override
	public long size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean supportsSetOperations() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsTransactions() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Model union(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model unregister(ModelChangedListener listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model write(Writer writer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model write(OutputStream out) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model write(Writer writer, String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model write(OutputStream out, String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model write(Writer writer, String lang, String base) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model write(OutputStream out, String lang, String base) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(Resource s, Property p, RDFNode o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(Resource s, Property p, String o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(Resource s, Property p, String lex, RDFDatatype datatype) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(Resource s, Property p, String o, boolean wellFormed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model add(Resource s, Property p, String o, String l) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model addLiteral(Resource s, Property p, boolean o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model addLiteral(Resource s, Property p, long o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model addLiteral(Resource s, Property p, int o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model addLiteral(Resource s, Property p, char o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model addLiteral(Resource s, Property p, float o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model addLiteral(Resource s, Property p, double o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model addLiteral(Resource s, Property p, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(Resource s, Property p, String o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(Resource s, Property p, String o, String l) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsLiteral(Resource s, Property p, boolean o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsLiteral(Resource s, Property p, long o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsLiteral(Resource s, Property p, int o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsLiteral(Resource s, Property p, char o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsLiteral(Resource s, Property p, float o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsLiteral(Resource s, Property p, double o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsLiteral(Resource s, Property p, Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Alt createAlt() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Alt createAlt(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bag createBag() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bag createBag(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createLiteral(String v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createLiteralStatement(Resource s, Property p, boolean o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createLiteralStatement(Resource s, Property p, float o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createLiteralStatement(Resource s, Property p, double o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createLiteralStatement(Resource s, Property p, long o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createLiteralStatement(Resource s, Property p, int o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createLiteralStatement(Resource s, Property p, char o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createLiteralStatement(Resource s, Property p, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Property createProperty(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource createResource(Resource type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource createResource(ResourceF f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource createResource(String uri, Resource type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource createResource(String uri, ResourceF f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Seq createSeq() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Seq createSeq(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createStatement(Resource s, Property p, String o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createStatement(Resource s, Property p, String o, String l) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createStatement(Resource s, Property p, String o,
			boolean wellFormed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement createStatement(Resource s, Property p, String o,
			String l, boolean wellFormed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(boolean v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(int v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(long v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(Calendar d) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(char v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(float v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(double v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(String v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(String lex, String typeURI) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal createTypedLiteral(Object value, String typeURI) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Alt getAlt(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Alt getAlt(Resource r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bag getBag(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bag getBag(Resource r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Property getProperty(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RDFNode getRDFNode(Node n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource getResource(String uri, ResourceF f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Seq getSeq(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Seq getSeq(Resource r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listLiteralStatements(Resource subject,
			Property predicate, boolean object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listLiteralStatements(Resource subject,
			Property predicate, char object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listLiteralStatements(Resource subject,
			Property predicate, long object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listLiteralStatements(Resource subject,
			Property predicate, float object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listLiteralStatements(Resource subject,
			Property predicate, double object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listResourcesWithProperty(Property p, boolean o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listResourcesWithProperty(Property p, long o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listResourcesWithProperty(Property p, char o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listResourcesWithProperty(Property p, float o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listResourcesWithProperty(Property p, double o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listResourcesWithProperty(Property p, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listStatements(Resource subject, Property predicate,
			String object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StmtIterator listStatements(Resource subject, Property predicate,
			String object, String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listSubjectsWithProperty(Property p, String o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResIterator listSubjectsWithProperty(Property p, String o, String l) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model remove(StmtIterator iter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model remove(Model m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model remove(Model m, boolean suppressReifications) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Model remove(Resource s, Property p, RDFNode o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RDFNode asRDFNode(Node n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Statement asStatement(Triple t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Graph getGraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryHandler queryHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RDFReader getReader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RDFReader getReader(String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String setReaderClassName(String lang, String className) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RDFWriter getWriter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RDFWriter getWriter(String lang) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String setWriterClassName(String lang, String className) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String expandPrefix(String prefixed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getNsPrefixMap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNsPrefixURI(String prefix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNsURIPrefix(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrefixMapping lock() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String qnameFor(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrefixMapping removeNsPrefix(String prefix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean samePrefixMappingAs(PrefixMapping other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public PrefixMapping setNsPrefix(String prefix, String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrefixMapping setNsPrefixes(PrefixMapping other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrefixMapping setNsPrefixes(Map<String, String> map) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String shortForm(String uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrefixMapping withDefaultMappings(PrefixMapping map) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void enterCriticalSection(boolean readLockRequested) {
		// TODO Auto-generated method stub

	}

	@Override
	public void leaveCriticalSection() {
		// TODO Auto-generated method stub

	}

}
