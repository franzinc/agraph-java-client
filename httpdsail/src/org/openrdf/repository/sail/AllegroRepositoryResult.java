package org.openrdf.repository.sail;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryResult;

/**
 * The inventors of OpenRDF 'RepositoryResult' should be shot for obstruction of
 * progress.  This version of the class tries to make it vaguely palatable.
 *
 */
public class AllegroRepositoryResult extends RepositoryResult {
	
	private List<List<String>> stringTuples = null;
	private List uninterpretedCollection = null;
	private Iterator uninterpretedCollectionIterator = null;
	private int cursor = 0;
	private Set<Statement> nonDuplicateSet = null;
	private Statement nextUniqueStatement = null;

    protected AllegroRepositoryResult(List<List<String>> stringTuples) {
    	super(null);
        this.stringTuples = stringTuples;
    }
   
    protected static AllegroRepositoryResult createUninterpretedRepositoryResult(List collection) {
    	AllegroRepositoryResult rr = new AllegroRepositoryResult(null);
        rr.uninterpretedCollection = collection;
        rr.uninterpretedCollectionIterator = collection.iterator();
        return rr;
    }

    /**
     * Allocate a Statement and fill it in from 'stringTuple'.
     */
    public Statement createStatement(List<String> stringTuple) {
    	AllegroStatement stmt = new AllegroStatement(null, null, null);
        stmt.setQuad(stringTuple);
        return stmt;
    }

    /**
     * Return 'true' if the iterator has additional statement(s).
     */
    public boolean hasNext() {
    	if (this.uninterpretedCollectionIterator != null)
    		return this.uninterpretedCollectionIterator.hasNext();
    	if (this.nonDuplicateSet != null) {
			// need to materialize the next non duplicate statement immediately, 
    		// which is slightly awkward:
    		Set<Statement> savedNonDuplicateSet = this.nonDuplicateSet;    		
    		try {
    			this.nonDuplicateSet = null;
    			while (this.hasNext()) {
    				Statement stmt = (Statement)this.next();
    				if (!savedNonDuplicateSet.contains(stmt)) {
    					savedNonDuplicateSet.add(stmt);
    					this.nextUniqueStatement = stmt;
    					return true;
    				}
    			}
    		} finally {
    			this.nonDuplicateSet = savedNonDuplicateSet;
    		}    		
    	}
    	return this.cursor < this.stringTuples.size();
    }

    /**
     * Return the next Statement in the answer, if there is one.
     * Else if this is an uninterpreted collection, return something.
     */
    public Object next() {
    	if (this.uninterpretedCollectionIterator != null)
    		return this.uninterpretedCollectionIterator.next();
    	if ((this.nonDuplicateSet != null) && this.hasNext()) {
    		return this.nextUniqueStatement;
    	} else if (this.hasNext()) {
            List<String>stringTuple = this.stringTuples.get(this.cursor);
            this.cursor++;
            return this.createStatement(stringTuple);
        } else {
        	return null;
        }
    }

//    /**
//     * Shut down the iterator, to insure that resources are free'd up.
//     */
//    public void close() {}

   /**
     * Switches on duplicate filtering while iterating over objects. The
     * RepositoryResult will keep track of the previously returned objects in a
     * {@link java.util.Set} and on calling next() or hasNext() will ignore any
     * objects that already occur in this Set.
     * <P>
     * Caution: use of this filtering mechanism is potentially memory-intensive.
     */
    public void enableDuplicateFilter() {
    	this.nonDuplicateSet = new HashSet<Statement>();
    }



}
