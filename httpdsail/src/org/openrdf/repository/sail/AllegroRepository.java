/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2006.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository.sail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.sail.Sail;

import franz.exceptions.ServerException;
import franz.exceptions.UnimplementedMethodException;

/**
 * An implementation of the {@link Repository} interface that operates on a
 * (stack of) {@link Sail Sail} object(s). The behaviour of the repository is
 * determined by the Sail stack that it operates on; for example, the repository
 * will only support RDF Schema or OWL semantics if the Sail stack includes an
 * inferencer for this.
 * <p>
 * Creating a repository object of this type is very easy. For example, the
 * following code creates and initializes a main-memory store with RDF Schema
 * semantics:
 * 
 * <pre>
 * Repository repository = new RepositoryImpl(new ForwardChainingRDFSInferencer(new MemoryStore()));
 * repository.initialize();
 * </pre>
 * 
 * Or, alternatively:
 * 
 * <pre>
 * Sail sailStack = new MemoryStore();
 * sailStack = new ForwardChainingRDFSInferencer(sailStack);
 * 
 * Repository repository = new Repository(sailStack);
 * repository.initialize();
 * </pre>
 * 
 */
public class AllegroRepository implements Repository {

	private String accessVerb = null;
	private miniclient.Catalog miniCatalog = null;
	private miniclient.Repository miniRepository;
	private String repositoryName = null; 
	//private AllegroRepositoryConnection repositoryConnection = null;
	private Map<String, String> inlinedPredicates = new HashMap<String, String>();
	private Map<String, String> inlinedDatatypes = new HashMap<String, String>();	

	private AllegroValueFactory valueFactory = null;
	
	public static String RENEW = "RENEW";
	public static String CREATE = "CREATE";
	public static String OPEN = "OPEN";
	public static String ACCESS = "ACCESS";	

	/**
	 * Constructor.  Create a new Sail that operates on the triple store that 'miniRepository'
	 * connects to.
	 */
	protected AllegroRepository(Catalog catalog, String repositoryName, String accessVerb) {
		this.miniCatalog = catalog.getMiniCatalog();
		this.repositoryName = repositoryName;
		this.accessVerb = accessVerb;		
	}
	
	protected miniclient.Repository getMiniRepository() {
		this.verify();
		return this.miniRepository;
	}
	
	public String getName () {return this.repositoryName;}
	
	public Map<String, String> getInlinedPredicates () {
		return this.inlinedPredicates;
	}

	public Map<String, String> getInlinedDatatypes () {
		return this.inlinedDatatypes;
	}

	


	public File getDataDir() {
		throw new UnimplementedMethodException("getDataDir");
	}

	public void setDataDir(File dataDir) {
		throw new UnimplementedMethodException("setDataDir");
	}

	public void initialize() {
        boolean clearIt = false;
        String quotedRepName = AllegroSail.quotePlus(this.repositoryName);
        miniclient.Catalog miniConn = this.miniCatalog;
        if (this.accessVerb == RENEW) {
            if (miniConn.listTripleStores().contains(quotedRepName)) {
                // not nice, since someone else probably has it open:
                clearIt = true;
            } else {
                miniConn.createTripleStore(quotedRepName);
            }
        } else if (this.accessVerb == CREATE) {
            if (miniConn.listTripleStores().contains(quotedRepName)) {
                throw new ServerException(
                    "Can't create triple store named '" + this.repositoryName + "' because a store with that name already exists.");
            }
            miniConn.createTripleStore(quotedRepName);
        } else if (this.accessVerb == OPEN) {
            if (!miniConn.listTripleStores().contains(quotedRepName)) {
                throw new ServerException(
                    "Can't open a triple store named '" + this.repositoryName + "' because there is none.");
            }
        } else if(this.accessVerb == ACCESS) {
            if (!miniConn.listTripleStores().contains(quotedRepName)) {
                miniConn.createTripleStore(quotedRepName) ;
            }
        }
        this.miniRepository = miniConn.getRepository(quotedRepName);
        // we are done unless a RENEW requires us to clear the store
        if (clearIt) {
            this.miniRepository.deleteMatchingStatements(null, null, null, null);
        }
	}
	
	private void verify () {
		if (this.miniCatalog != null && this.miniRepository != null) return;
		else if (this.miniCatalog == null)
			throw new ServerException("Attempt to use the repository after it has been closed.");
		else 
			throw new ServerException("Attempt to use the repository before it has been initialized.");
	}

	public void shutDown() {
		this.miniCatalog = null;
		this.miniRepository = null;
	}


	public boolean isWritable() {
		this.verify();
		return this.miniRepository.isWriteable();
	}

	public ValueFactory getValueFactory() {
		if (this.valueFactory == null) {
			this.valueFactory = new AllegroValueFactory(this);
		}
		return this.valueFactory;
	}

	public RepositoryConnection getConnection() {
		this.verify();
//		if (this.repositoryConnection == null) {
//			this.repositoryConnection = new AllegroRepositoryConnection(this);
//		}
//		return this.repositoryConnection;
		// Sesame JUnits test insists on supporting multiple connections to same repository:
		// Not clear that there is any value in that:
		return new AllegroRepositoryConnection(this);
	}
	
	//------------------------------------------------------------------------
	// Extensions to the Sesame API
	//------------------------------------------------------------------------

	/**
	    Index the newly-added triples in the store.  This should be done after every 
        significant-sized load of triples into the store.
        If 'all', re-index all triples in the store.  If 'asynchronous', spawn
        the indexing task as a separate thread, and don't wait for it to complete.
        Note. Upon version 4.0, calling this will no longer be necessary.        
	 */
   public void indexTriples(boolean all) {
        this.miniRepository.indexStatements(all);
   }

   /**
        Register a predicate 'uri' (or 'namespace'+'localname'), telling the RDF store to index
        text keywords belonging to strings in object position in the corresponding
        triples/statements.  This is needed to make the  fti:match  operator
        work properly.
    */
   public void registerFreeTextPredicate(String uri) {
        this.miniRepository.registerFreeTextPredicate("<" + uri + ">");
   }
   
   
//   private void translate_inlined_type(String type) {
//        if (type == "int") return "int";
//        else if (type == "datetime") return "date-time";
//        else if (type == "float") return "float";
//        else
//            throw new XXX("Unknown inlined type '%s'\n.  Legal types are " +
//                    "'int', 'float', and 'datetime'");
//   }
//        
//    def registerInlinedDatatype(self, predicate=None, datatype=None, inlinedType=None):
//        """
//        Register an inlined datatype.  If 'predicate', then object arguments to triples
//        with that predicate will use an inlined encoding of type 'inlinedType' in their 
//        internal representation.
//        If 'datatype', then typed literal objects with a datatype matching 'datatype' will
//        use an inlined encoding of type 'inlinedType'.
//        """
//        predicate = predicate.getURI() if isinstance(predicate, URI) else predicate
//        datatype = datatype.getURI() if isinstance(datatype, URI) else datatype
//        if predicate:
//            if not inlinedType:
//                raise IllegalArgumentException("Missing 'inlinedType' parameter in call to 'registerInlinedDatatype'")
//            lispType = self._translate_inlined_type(inlinedType)
//            mapping = [predicate, lispType, "predicate"]
//            self.inlined_predicates[predicate] = lispType
//        elif datatype:
//            lispType = self._translate_inlined_type(inlinedType or datatype)
//            mapping = [datatype, lispType, "datatype"]
//            self.inlined_datatypes[datatype] = lispType
//        ##self.internal_ag_store.addDataMapping(mapping)
//        raise UnimplementedMethodException("Inlined datatypes not yet implemented.")
	 
}
