package org.openrdf.repository.sail;

import java.util.ArrayList;
import java.util.List;

import miniclient.Server;
import franz.exceptions.ServerException;

/**
 * The 'AllegroSail' class holds the information needed to connect to an
 * AllegroGraph HTTPD endpoint.  It enables the creation of Catalogs, which
 * are catalogs of available triple stores/repositories.
 */
public class AllegroSail {
	
	private String host;
	private int port;
	private String username = null;
	private String password = null;
	private List<Catalog> openCatalogs = new ArrayList<Catalog>();

	/**
	 * Constructor.  Records a hostname and port that points to an AllegroGraph HTTPD endpoint.
	 */
	public AllegroSail(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	/**
	 * Constructor.  Records a hostname and port that points to an AllegroGraph HTTPD endpoint.
	 * Also records a username and password.
	 */
	public AllegroSail(String host, int port, String username, String password) {
		this(host, port);
		this.username = username;
		this.password = password;
	}
	
	private String getAddress() {
        return this.host + ":" + this.port;
	}
	
	protected List<Catalog> getOpenCatalogs() {return this.openCatalogs;}
	
	/**
	 * List the (short) names of available catalogs.
	 */
	public List<String> listCatalogs() {
		List<String> catNames = new ArrayList<String>();
		for (String name : Server.listCatalogs(this.getAddress())) {
			int pos = name.lastIndexOf("/");
			if (pos >= 0) name = name.substring(pos + 1);
			catNames.add(name);
		}
		return catNames;
	}
	
	/**
	 * Open a catalog named 'catalogName'.
	 */
	public Catalog openCatalog(String catalogName) {
		if (!this.listCatalogs().contains(catalogName))
			throw new ServerException("There is no catalog named '" + catalogName + "'");
		for (Catalog cat : this.openCatalogs) {
			if (cat.getName().equals(catalogName)) return cat;
		}
        String longName = "/catalogs/" + catalogName;
        miniclient.Catalog miniCatalog = Server.openCatalog(this.getAddress(), longName, this.username, this.password);
        Catalog catalog = new Catalog(catalogName, miniCatalog, this);
        return catalog;
	}


}
