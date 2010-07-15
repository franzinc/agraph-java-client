/******************************************************************************
** Copyright (c) 2008-2010 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.graphstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.topbraid.core.TB;
import org.topbraid.core.graph.BufferingGraph;
import org.topbraid.core.images.ImageMetadata;
import org.topbraid.core.io.AbstractGraphStore;
import org.topbraid.core.io.IOUtil;
import org.topbraid.core.model.Ontologies;
import org.topbraid.eclipsex.log.Log;

import com.franz.agraph.Activator;
import com.franz.agraph.AllegroConstants;
import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGServer;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.shared.PrefixMapping;


public class AllegroGraphStore extends AbstractGraphStore {
	
	private static Map<String,AGGraph> uri2Graph = new HashMap<String,AGGraph>();
	
	private final static String PREFIX_PREFIX = "Prefix-";

	public static AGGraph getAGGraph(URI baseURI) {
		return uri2Graph.get(baseURI.toString());
	}

	private Map<IFile,Graph> file2Graph = new HashMap<IFile,Graph>();
	
	private static AGGraphMaker graphMaker;
	
	// Copies prefix declarations from an existing file into a given Properties object
	@SuppressWarnings("unchecked")
	public static void addExistingPrefixes(File file, Properties properties) throws IOException {
		if(file.exists()) {
			Properties old = new Properties();
			InputStream is = new FileInputStream(file);
			old.load(is);
			is.close();
			Iterator it = old.keySet().iterator();
			while(it.hasNext()) {
				String key = (String) it.next();
				if(key.startsWith(PREFIX_PREFIX) && !properties.containsKey(key)) {
					String value = old.getProperty(key);
					properties.put(key, value);
				}
			}
		}
	}


	@SuppressWarnings("unchecked")
	public static void addPrefixesToProperties(Properties properties,
			PrefixMapping pm) {
		Iterator prefixes = pm.getNsPrefixMap().keySet().iterator();
		while(prefixes.hasNext()) {
			String prefix = (String) prefixes.next();
			String namespace = pm.getNsPrefixURI(prefix);
			if( namespace != null ){
				properties.put(PREFIX_PREFIX + prefix, namespace);
			}
		}
	}

	
	public void close(IFile file) throws Exception {
		AGGraph graph = (AGGraph)file2Graph.remove(file);
		if(graph != null) {
			try {
				graph.close();
			} catch(Throwable t) {
				Log.logWarning(Activator.PLUGIN_ID, "Could not close", t);
			}			
		} else {
			throw new IllegalStateException("we couldn't find a Graph for file " + file.getName());
		}
	}
	
	
	public AGGraph createGraph(Properties properties) throws Exception {
		if (graphMaker==null) {
			// TODO: get username password
			setGraphMaker(createGraphMaker(properties,"test","xyzzy"));
		}
		String baseURI = properties.getProperty(AllegroConstants.BASE_URI);
		AGGraph graph = graphMaker.createGraph(baseURI);
		uri2Graph.put(baseURI, graph);
		return graph;
	}
	
	public URI getBaseURI(IFile file, Set<URI> imports) throws Exception {
		Properties properties = IOUtil.loadProperties(file);
		String str = properties.getProperty(AllegroConstants.IMPORTS);
		if(str != null && str.length() > 0) {
			String[] ss = str.split(" ");
			for(int i = 0; i < ss.length; i++) {
				URI uri = URI.create(ss[i]);
				imports.add(uri);
			}
		}
		String value = properties.getProperty(AllegroConstants.BASE_URI);
		if(value != null) {
			return new URI(value);
		}
		else {
			return null;
		}
	}
	
	
	public ImageMetadata getImageMetadata() {
		return new ImageMetadata(Activator.PLUGIN_ID, "AllegroGraph");
	}
	
	
	@SuppressWarnings("unchecked")
	public static void initPrefixMapping(Graph graph, Properties p) {
		Enumeration e = p.keys();
		while(e.hasMoreElements()) {
			String pn = (String) e.nextElement();
			if(pn.startsWith(PREFIX_PREFIX)) {
				String prefix = pn.substring(PREFIX_PREFIX.length());
				String namespace = p.getProperty(pn);
				graph.getPrefixMapping().setNsPrefix(prefix, namespace);
			}
		}
	}

	
	public boolean isReadOnly(IFile file) {
		return false;
	}

	
	public Graph load(IFile file, IProgressMonitor monitor) throws Exception {
		Properties properties = IOUtil.loadProperties(file);
		Graph graph = createGraph(properties);
		file2Graph.put(file, graph);
		return graph;
	}

	
	public void save(IFile file, Graph graph, String baseURI) throws IOException {
		
		Properties properties = IOUtil.loadProperties(file);
		
		Set<URI> importedURIs = Ontologies.getImportedURIs(graph, baseURI);
		updateImportsProperty(properties, importedURIs);

		updateNamespaceProperties(graph, baseURI, properties);
		
		savePropertiesFile(file, properties);

		BufferingGraph bufferingGraph = ((BufferingGraph)graph);
		TB.getSession().getChangeEngine().commitBufferingGraph(bufferingGraph);
		// TODO check that graph and namespaces are committed
	}

	
	public boolean saveAs(IFile file, Graph graph, String baseURI) throws Exception {
		return false;
	}


	private void savePropertiesFile(IFile file, Properties properties) throws FileNotFoundException, IOException {
		File oFile = file.getRawLocation().toFile();
		FileOutputStream os = new FileOutputStream(oFile);
		properties.store(os, AllegroConstants.COMMENT);
		os.close();
	}


	private void updateImportsProperty(Properties properties, Set<URI> importedURIs) {
		StringBuffer sb = new StringBuffer();
		Iterator<URI> imports = importedURIs.iterator();
		while(imports.hasNext()) {
			URI uri = imports.next();
			sb.append(uri);
			if(imports.hasNext()) {
				sb.append(" ");
			}
		}
		properties.setProperty(AllegroConstants.IMPORTS, sb.toString());
	}


	@SuppressWarnings("unchecked")
	public static void updateNamespaceProperties(Graph graph, String baseURI, Properties properties) {
		properties.setProperty(AllegroConstants.BASE_URI, baseURI);
		Set entries = properties.entrySet();
		Iterator it = entries.iterator();
		while(it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String key = (String) entry.getKey();
			if(key.startsWith(PREFIX_PREFIX)) {
				it.remove();
			}
		}
		PrefixMapping pm = graph.getPrefixMapping();
		addPrefixesToProperties(properties, pm);
	}


	public static AGGraphMaker createGraphMaker(Properties properties, String username, String password) throws Exception {
		String serverURL = properties.getProperty(AllegroConstants.SERVER_URL);
		String catalogName = properties.getProperty(AllegroConstants.CATALOG);
		String repoName = properties.getProperty(AllegroConstants.REPOSITORY);
		AGServer server = new AGServer(serverURL, username, password);
		AGCatalog catalog = server.getCatalog(catalogName);
		AGRepository repo = catalog.createRepository(repoName);
		repo.initialize();
		AGGraphMaker maker = new AGGraphMaker(repo.getConnection());
		AllegroGraphStore.setGraphMaker(maker);
		return maker;
	}
	
	public static void setGraphMaker(AGGraphMaker maker) {
		graphMaker = maker;
	}

}
