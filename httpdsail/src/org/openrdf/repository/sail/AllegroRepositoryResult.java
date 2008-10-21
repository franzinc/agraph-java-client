package org.openrdf.repository.sail;

import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.repository.RepositoryResult;

import franz.exceptions.UnimplementedMethodException;

public class AllegroRepositoryResult extends RepositoryResult {
	
	private List<List<String>> stringTuples;
	private int cursor = 0;

    protected AllegroRepositoryResult(List<List<String>> stringTuples) {
    	super(null);
        this.stringTuples = stringTuples;
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
    	return this.cursor < this.stringTuples.size();
    }

    /**
     * Return the next Statement in the answer, if there is one.
     */
    public Statement next() {
        if (this.hasNext()) {
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
        throw new UnimplementedMethodException("enableDuplicateFilterthis");
    }



}
