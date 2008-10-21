package org.openrdf.repository.sail;

import java.util.List;

import org.openrdf.repository.Repository;

/**
 * Container of multiple repositories (triple stores).
 */
public class Catalog {
	private AllegroSail server;
	private miniclient.Catalog miniCatalog;
	private String shortName;
	private boolean isClosed = false;
	
	/**
	 * Constructor
	 */
	protected Catalog(String shortName, miniclient.Catalog miniCatalog, AllegroSail server) {
		this.server = server;
		this.miniCatalog = miniCatalog;
		this.shortName = shortName;
	}

	protected miniclient.Catalog getMiniCatalog () {return this.miniCatalog;}
	
	public String getName() {return this.shortName; }

	/**
	 * Return a list of names of repositories (triple stores) managed by
	 */
	public List<String> listRepositories () { return this.miniCatalog.listTripleStores();}
        
	public AllegroRepository getRepository(String name, String accessVerb) {
		return new AllegroRepository(this, name, accessVerb);
	}

	public void close () {
		if (this.isClosed) return;
		this.server.getOpenCatalogs().remove(this);
		this.isClosed = true;
	}

        

}
