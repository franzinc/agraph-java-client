package miniclient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

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
		List<Map> stores =  (List<Map>)Request.jsonRequest("GET", this.url + "/repositories", null, null, null);		
		List<String> names = new ArrayList<String>();
		for (Map<String, String> rep : stores) {
			// I have no idea why this is coming back with an extra layer of quotes on the
			// repository names:
			String titleWithQuotes = rep.get("title");
			names.add(trimDoubleQuotes(titleWithQuotes, true));
		}
		return names;
	}
	
	/**
	 * Create a repository/triple store named 'name'.
	 */
	public void createTripleStore(String name) {
		Request.nullRequest("PUT", this.url + "/repositories/" + legalizeName(name), null, null);
	}
	
	/**
	 * Create a federated store.
	 */
	public void federateTripleStores(String name, List<String>storeNames) {
		try {
			JSONObject options = new JSONObject().put("federate", (Object)storeNames);
			Request.nullRequest("PUT", this.url + "/repositories/" + legalizeName(name), options, null);
		} catch (JSONException ex) { throw new SoftException(ex); }	
	}

	/**
	 * Delete the repository/triple store named 'name'.
	 */
	public void deleteTripleStore(String name) {
		Request.nullRequest("DELETE", this.url + "/repositories/" + legalizeName(name), null, null);
	}
	
	/**
	 * Return a repository object connected to the triple store named 'name'.
	 */
	public Repository getRepository(String name) {
		return new Repository(this.url + "/repositories/" + legalizeName(name));
	}

	/**
	 * We aren't sure if we want to change the name to make it legal
	 * or just break.  For now, we just break.
	 */
	public static String legalizeName(String name) {
		return name;
	}

	//
//  def setAuth(self, user, password):
//      """Set a username and password to use when talking to this server."""
//      self.curl.setopt(pycurl.USERPWD, "%s:%s" % (user, password))
//      self.curl.setopt(pycurl.HTTPAUTH, pycurl.HTTPAUTH_BASIC)
	
	//-----------------------------------------------------------------------------------------
	// Utility function
	//-----------------------------------------------------------------------------------------
	
	
	/**
	 * Remove double quotes that wrap 's'.
	 */
	public static String trimDoubleQuotes(String s, boolean verify) {
		if (verify) {
			//System.out.print(" v ");
			if (!(s.startsWith("\"") && s.endsWith("\"")))
				throw new SoftException("Double-quotes missing on double-quoted string " + s);
		}
		return s.substring(1, s.length() - 1);
	}
	
}


