package com.franz.ag.db_import;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.franz.ag.AllegroGraph;
import com.franz.ag.db_import.MappingBuilder.MapObject;
import com.franz.ag.db_import.RDFConstants.MAP;
import com.franz.ag.db_import.RDFConstants.RDF;
import com.franz.ag.db_import.RDFConstants.RDFS;
import com.franz.ag.db_import.RDFConstants.XSD;
import com.franz.ag.exceptions.NiceException;
import com.franz.ag.jena.AllegroGraphGraphMaker;
import com.franz.ag.jena.AllegroGraphModel;
import com.franz.ag.jena.StartUp;
import com.franz.ag.jena.Utils;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Simple exporter that may have some real code in it, but is also partly just for
 * debugging the DB Import code.
 *  
 * @author bmacgregor
 *
 */
public class RDFExporter {
	
	private static Logger logger = Logger.getLogger(RDFExporter.class);
	
	private List<Triple> triples = new ArrayList<Triple>();
	private Map<String, String> namespaceNicknames = new HashMap<String, String>();
	private Map<Object, String> objectToURIMap = new HashMap<Object, String>();
	private String baseNamespace = null;
	// target-specific parameters
	private ExportTargetType exportTargetType;
	private String[] executionArgs; // passed to AllegroGraph startup
	private boolean abbreviateNamespaces = false;
	// target file for N3, XML_RDF, or NTRIPLES:
	private String outputFile = null;
	// AllegroGraph parameters:
	private String allegroGraphHost = null;
	private String allegroGraphDBName = null;
	private String allegroGraphDBDirectory = null;

		
	String[] exportTypes = {"NTRIPLES", "ALLEGRO_GRAPH"}; //, "RDF_XML", "NTRIPLES"
	private enum ExportTargetType {ALLEGRO_GRAPH, N3, RDF_XML, NTRIPLES}
	
	private ExportTargetType parseExportType (String eType) {
		eType = eType.toLowerCase();
		if ("n3".equals(eType)) return ExportTargetType.N3;
		else if ("ntriples".equals(eType)) return ExportTargetType.NTRIPLES;
		else if ("allegro_graph".equals(eType)) return ExportTargetType.ALLEGRO_GRAPH;
		else if ("allegrograph".equals(eType)) return ExportTargetType.ALLEGRO_GRAPH;
		else {
			throw new NiceException("Unsupported type of export '" + eType + "'\n" +
					"   Supported types are 'ALLEGRO_GRAPH' and 'NTRIPLES'.");
		}
		
	}
	
	/** Constructor */
	public RDFExporter (String exportType, String[] args)  {
		this.exportTargetType = parseExportType(exportType);		
		this.executionArgs = args;
		this.namespaceNicknames.put(MAP.NS, "map");
		this.namespaceNicknames.put(RDF.NS, "rdf");
		this.namespaceNicknames.put(RDFS.NS, "rdfs");
		this.namespaceNicknames.put(XSD.NS, "xsd");
	}
	
	//-----------------------------------------------------------------------------------------------
	// Accessors
	//-----------------------------------------------------------------------------------------------
	
	public void setOutputFile(String file) {this.outputFile = file;}
	public void setAllegroGraphHost(String host) {this.allegroGraphHost = host;}
	public void setAllegroGraphDBName(String dbName) {this.allegroGraphDBName = dbName;}
	public void setAllegroGraphDBDirectory(String directory) {this.allegroGraphDBDirectory = directory;}
	
	public void addNamespacePrefix(String namespace, String nickname) {
		this.namespaceNicknames.put(namespace, nickname);
	}
	
	public void setBaseNamespace(String baseNS) {
		this.baseNamespace = baseNS;
	}
	
	//-----------------------------------------------------------------------------------------------
	// 
	//-----------------------------------------------------------------------------------------------
	
	// TODO: REPLACE THIS BY String[4] OR Object[4] AFTER ITS ALL DEBUGGED:
	public class Triple {
		
		private String subject;
		private String predicate;
		private String object;
		private String xsdDatatype;
		
		public Triple(String s, String p, String o, String xsdDatatype) {
			this.subject = s;
			this.predicate = p;
			this.object = o;
			this.xsdDatatype = xsdDatatype;
		}
		
		public String toString () {
			return "<" + subject + ", " + predicate + ", " + object + ">";
		}
	}
	
	// THIS IS A KLUDGE
	private int uriCounter = 0;
	
	private String makeNewURI(String bareName) {
		if (bareName == null) bareName = "thing";
		return this.baseNamespace + bareName + "_" + (++this.uriCounter);
	}
	
	private String uriToTerm (String uri) {
		if (this.abbreviateNamespaces) {
			for (Map.Entry<String, String> entry : this.namespaceNicknames.entrySet()) {
				String ns = entry.getKey();
				if (uri.startsWith(ns)) {
					// return qname:
					return entry.getValue() + ":" + uri.substring(ns.length());
				}
			}
		}
		// return full URI, suitable bracketed
		return "<" + uri + ">";
	}
	
	/**
	 * 'term' may be a string of an object.  If its
	 */
	private String getURI (Object term) {
		if (term instanceof MapObject) {
			String uri = this.objectToURIMap.get(term);
			if (uri == null) {
				String bareName = ((MapObject)term).getName();
				uri = makeNewURI(bareName);
				this.objectToURIMap.put(term, uri);
			}
			return uri;
		} else {
			return (String)term;
		}
	}
	
	/**
	 * Write out a file of NTriples triples.
	 */
	private void writeNTriplesTriples () {
		writeNSomethingTriples(false);
	}
	
	/*
	 * TODO: IF WE REALLY MEAN TO IMPLEMENT THIS, WE NEED TO ADD 
	 * PREFIX DECLARATIONS.  RIGHT NOW ITS BOGUS
	 */
	private void writeN3Triples () {
		writeNSomethingTriples(true);		
	}

	
	private void writeNSomethingTriples (boolean abbreviateNamespaces) {
		this.abbreviateNamespaces = abbreviateNamespaces;
		StringBuffer buf = new StringBuffer();
		for (Triple t : this.triples) {
			buf.append(uriToTerm(t.subject));
			buf.append("   ");
			buf.append(uriToTerm(t.predicate));
			buf.append("   ");
			if (t.xsdDatatype == null) {
				buf.append(uriToTerm(t.object));
			} else {				
				String xsdType = t.xsdDatatype;
				buf.append("\"");
				buf.append(t.object);
				buf.append("\"");
				if (!xsdType.equals(XSD.STRING)) {
					buf.append("^^" + xsdType);
				}
			}
			buf.append(" .\n");			
		}
		File file = new File(this.outputFile);
		try {
			FileWriter writer = new FileWriter(file);
			writer.write(buf.toString());
			writer.flush();
			writer.close();
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			else throw new NiceException(e);
		}
	}
	
	/**
	 * Write triples in 'this.triples' into an AllegroGraph store.
	 * Using the Jena interface; replace this with OpenRDF when that's ready.
	 */
	private void writeTriplesToAllegroGraph () {
//		AllegroGraphModelFactory factory = AllegroGraphModelFactory.getInstance();
//    	AllegroGraph agStore = factory.startUpTripleStore(
//    			AllegroGraphModelFactory.AccessTripleStore.RENEW,
//    			this.allegroGraphHost, this.allegroGraphDBName, this.allegroGraphDBDirectory, this.executionArgs);       	
    	AllegroGraph agStore = StartUp.startUpTripleStore(
       			StartUp.AccessTripleStore.RENEW,
       			this.allegroGraphHost, this.allegroGraphDBName, this.allegroGraphDBDirectory, this.executionArgs);   
    	   AllegroGraphGraphMaker.setDefaultMaker(agStore);
       	try {
    		if (agStore != null) {
    			System.out.println("Uploading " + this.triples.size() + " to AllegroGraph triple store.");
    			//Model model = AllegroGraphModelFactory.createDefaultModel();
    			Model model = new AllegroGraphModel(AllegroGraphGraphMaker.getInstance().getGraph());
    			System.out.println("Model size before: " + model.size());
    			List<com.hp.hpl.jena.rdf.model.Statement> statements = new ArrayList<com.hp.hpl.jena.rdf.model.Statement>();
    			for (Triple t : this.triples) {
    				Resource subject = model.createResource(t.subject);
    				Property predicate = model.createProperty(t.predicate);
    				RDFNode object = null;
    				if (t.xsdDatatype == null)
    					object = model.createResource(t.object);
    				else if (t.xsdDatatype.equals(XSD.STRING))
    					object = model.createLiteral(t.object);
    				else if (t.xsdDatatype.equals(XSD.NUMBER)) {
    					boolean hasDot = t.object.indexOf('.') >= 0;
    					String localName = hasDot ? "float" : "int"; // winging it a bit here
    					RDFDatatype dt = new XSDDatatype(localName);
						object = model.createTypedLiteral(t.object, dt);
    				}
    				else {
    					try {
    						String localName = Utils.uriToLocalName(t.xsdDatatype);
    						RDFDatatype dt = new XSDDatatype(localName);
    						object = model.createTypedLiteral(t.object, dt);
    					} catch (Exception e) {
    						System.out.println("Failed to write triple " + t + " because " + e.getMessage() + ", " + e.toString());
    						System.out.println("Offending object datatype " + t.xsdDatatype);
    						continue;
    					}
    				}
    				com.hp.hpl.jena.rdf.model.Statement stmt = model.createStatement(subject, predicate, object);
    				statements.add(stmt);
    			}
    			model.add(statements);
    			System.out.println("Model size after: " + model.size());
    			System.out.println("Finished uploading triples.");
    		}
    	} catch (Exception ex) {
    		System.out.println("Failure in upload to AllegroGraph \n" + ex.toString() + ex.getMessage());
    		ex.printStackTrace();
    	} finally {
    		StartUp.shutDownTripleStore(agStore);
    	}

	}
	
	//---------------------------------------------------------------------------
	// Entries
	//---------------------------------------------------------------------------
	
	public void exportTriple(Object s, Object p, Object o, String sqlDatatype) {
		String subject = this.getURI(s);
		String predicate = this.getURI(p);
		String object;
		if (sqlDatatype == null) {
			object = this.getURI(o);
		} else {
			object = ((String)o).trim();
		}
		this.triples.add(new Triple(subject, predicate, object, sqlDatatype));
	}
	
	public void writeTriples () {
		if (this.baseNamespace == null) {
			throw new NiceException("RDF exporter can't write triples because base namespace has not been set.");
		}
		switch (this.exportTargetType) {
		case N3:
			this.writeN3Triples();
			break;
		case ALLEGRO_GRAPH:
			this.writeTriplesToAllegroGraph();
			break;
		case RDF_XML:
			throw new NiceException("RDF_XML output not yet implemented.");
		case NTRIPLES:
			this.writeNTriplesTriples();
		}
	}

}
