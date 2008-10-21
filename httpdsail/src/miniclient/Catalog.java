package miniclient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import franz.exceptions.SoftException;

public class Catalog {
	
	private String url;
	private String username = null;
	private String password = null;
	private Object curl = null;
	
	public Catalog(String url, String username, String password) {
		this.url = Server.toFullURL(url);
		this.username = username;
		this.password = password;
	}
	
	public String getURL() {return this.url;}
	
	/**
	 * Return a set of names of triple stores available in this catalog.
	 */
	public List<String> listTripleStores() {
		return (List<String>)Request.jsonRequest("GET", this.url + "/repositories", null, null, null);
	}
	
	/**
	 * Create a repository/triple store named 'name'.
	 */
	public void createTripleStore(String name) {
		try {
			Request.nullRequest("PUT", this.url + "/repositories/" + URLEncoder.encode(name, "UTF-8"),
				null, null);
		} catch (UnsupportedEncodingException ex) { throw new SoftException(ex); }
	}

	/**
	 * Delete the repository/triple store named 'name'.
	 */
	public void deleteTripleStore(String name) {
		try {
			Request.nullRequest("DELETE", this.url + "/repositories/" + URLEncoder.encode(name, "UTF-8"),
					null, null);
		} catch (UnsupportedEncodingException ex) { throw new SoftException(ex); }
	}
	
	/**
	 * Return a repository object connected to the triple store named 'name'.
	 */
	public Repository getRepository(String name) {
		try {
			return new Repository(this.url + "/repositories/" + URLEncoder.encode(name, "UTF-8"));
		} catch (UnsupportedEncodingException ex) { throw new SoftException(ex); }
	}

	//
//  def setAuth(self, user, password):
//      """Set a username and password to use when talking to this server."""
//      self.curl.setopt(pycurl.USERPWD, "%s:%s" % (user, password))
//      self.curl.setopt(pycurl.HTTPAUTH, pycurl.HTTPAUTH_BASIC)
	
}


