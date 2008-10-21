package miniclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import franz.exceptions.SoftException;

public class Server {
	
	/**
	 * Make sure that 'url' has an HTTP protocol prepended.
	 */
	protected static String toFullURL(String url) {
		if (!url.toLowerCase().startsWith("http"))
			url = "http://" + url;
		return url;
	}
	
	public static List<String> listCatalogs(String serverURL) {
		 return (List)Request.jsonRequest("GET", toFullURL(serverURL) + "/catalogs", null, null, null);
	}
	
	public static Catalog openCatalog(String serverURL, String catalogName, String username, String password) {
		return new Catalog(serverURL + catalogName, username, password);
	}

	
	//----------------------------------------------------------------------------------------------------
	// Test code
	//----------------------------------------------------------------------------------------------------	
	
	
//	private static Catalog getCatalog(String serverURL) {
//	    List<String >cats = listCatalogs(serverURL);
//	    if (cats.size() == 0) {
//	        System.out.println("No catalogs on server.");
//	        return null;
//	    }
//	    else
//	    	System.out.println("Found catalog " + cats.get(0));
//	        return openCatalog(serverURL, cats.get(0), null, null);
//	}
//
//	private static Repository getRepository(String serverURL) {
//	    Catalog cat = getCatalog(serverURL);
//	    if (cat == null) return null;
//	    List<String> reps = cat.listTripleStores();
//	    if (reps.size() == 0) {
//	        System.out.println("No repositories in catalog " + cat.getURL() + ".  Creating one.");
//	        String name = "test2";
//	        cat.createTripleStore(name);
//	        return cat.getRepository("name");
//	    } else {
//	    	return cat.getRepository(reps.get(0));
//	    }
//	}
	
	
	private static long timeQuery(Repository rep, int n, int limit) {
	    long begin = System.currentTimeMillis();
	    for (int i = 0; i < n; i++) {
	        rep.evalSparqlQuery("select ?x ?y ?z {?x ?y ?z} limit " + limit, false, null, null);
	    }
	    long elapsed = System.currentTimeMillis() - begin;
	    System.out.println("Did " + n + " " + limit + "-row queries in " + elapsed + " milli seconds.");
	    return elapsed;
	}
	
	private static void test0() {
	    List<String> cats = listCatalogs("http://localhost:8080");
	    System.out.println("List of catalogs:" + cats);
	    Catalog cat = openCatalog("http://localhost:8080", cats.get(0), null, null);
	    System.out.println( "Found cat " + cat.getURL());
	    List<String> reps = cat.listTripleStores();
	    System.out.println( "Is 'test' there??:" + reps + (reps.contains("test")));
	    try {
	    	System.out.println( "Creating repository 'test'");
	        cat.createTripleStore("test");
	        reps = cat.listTripleStores();
	        System.out.println( "Now is 'test' there??:" + reps + (reps.contains("test")));
	    } catch (Exception ex) {}
	    Repository rep = cat.getRepository("test");
	    long size = rep.getSize(); 
	    System.out.println( "Size of 'test' repository " + size);
	    if (size == 0) {
	        rep.addStatement("<http://www.franz.com/example#ted>", "<http://www.franz.com/example#age>", 
	        		"\"55\"^^<http://www.w3.org/2001/XMLSchema#int>", "<http://foo.com>");
	    }
	    String query = "select ?x ?y ?z {?x ?y ?z} limit 5";
	    Map answer = (Map)rep.evalSparqlQuery(query, false, "<http://foo.com>", null);
	    System.out.println(answer.get("names"));
	    for (List v : (List<List>)answer.get("values")) {
	    	System.out.println("  " + v);
	    }
	    timeQuery(rep, 1000, 1);
	}	
	
	private static Repository openRep () {
	    List<String> cats = listCatalogs("http://localhost:8080");
	    System.out.println("List of catalogs:" + cats);
	    Catalog cat = openCatalog("http://localhost:8080", cats.get(0), null, null);
	    System.out.println( "Found cat " + cat.getURL());
	    List<String> reps = cat.listTripleStores();
	    System.out.println( "Is 'test' there??:" + reps + (reps.contains("test")));
	    try {
	    	System.out.println( "Creating repository 'test'");
	        cat.createTripleStore("test");
	        reps = cat.listTripleStores();
	        System.out.println( "Now is 'test' there??:" + reps + (reps.contains("test")));
	    } catch (Exception ex) {}
	    Repository rep = cat.getRepository("test");
	    long size = rep.getSize();
	    System.out.println("Repository size = " + rep.getSize());
	    return rep;
	}
	
	private static Object makeTerm(Object term, boolean isLiteral) {
		if (isLiteral) {
			return "\"" + term + "\"";
		} else if (term == null) {
			return null;
		} else if (term instanceof String) {
			return "<" + term + ">";
		} else if (term instanceof List) {
			List terms = new ArrayList();
			for (Object t : (List)term) {
				terms.add(makeTerm(t, false));
			}
			return terms;
		} else {
			throw new SoftException("makeTerm punts on " + term);
		}
		
	}
	
	private static List makeStatement(String subject, String predicate, String object, boolean isLiteral, String context) {
		List stmt = new ArrayList(4);
		stmt.add(makeTerm(subject, false));
		stmt.add(makeTerm(predicate, false));
		stmt.add(makeTerm(object, isLiteral));
		stmt.add(makeTerm(context, false));
		return stmt;
	}
	
	private static List makeList(Object... items) {
		List list = new ArrayList();
		for (Object i : items) list.add(i);
		return list;
	}
	
	private static void test1() {
	    Repository rep = openRep(); 
	    System.out.println("Adding statements ...");
	    String ns = "http:example#";
	    List<List> stmts = new ArrayList<List>();
	    stmts.add(makeStatement(ns + "alice", ns + "name", "alice", true, ns + "cxt"));
	    
	    stmts.add(makeStatement(ns + "bob", ns + "name", "bob", true, null));	    
	    rep.addStatements(stmts);
	    System.out.println("Repository size = " + rep.getSize());    
	    System.out.println("Statements " + rep.getStatements(null, null, null,  makeTerm(makeList(ns + "cxt", null), false), false, null));
	}

	    
	public static void main (String[] args) {
		int choice = 1;
		switch(choice) {
		case 0: test0(); break;
		case 1: test1(); break;
		default: System.out.println("No test for choice " + choice);
		}
	}
	
}

   


    
