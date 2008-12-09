package org.openrdf.repository.sail;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.MapBindingSet;

import franz.exceptions.UnimplementedMethodException;

public class AllegroTupleQueryResult implements TupleQueryResult {
	
	private List<String> columnNames;
	private List<List<String>> stringTuples;
	private int cursor = 0;
	private boolean skipIllegalTuples = false;
	private List<List<String>> illegalTuples = new ArrayList<List<String>>();
	
	public static boolean STRIP_QUESTION_MARKS = true;
	
	public AllegroTupleQueryResult(List<String> columnNames, List<List<String>> stringTuples) {
		this.columnNames = columnNames;
		this.stringTuples = stringTuples;
		if (STRIP_QUESTION_MARKS) {
			for (int i = 0; i < this.columnNames.size(); i++) {
				String name = this.columnNames.get(i);
				if ((name != null) && name.startsWith("?")) {
					name = name.substring(1);
					this.columnNames.set(i, name);
				}
			}
		}
	}

	public List<String> getBindingNames() {
		return this.columnNames;
	}
	
	/**
	 * Return 'true' if query tuples with illegal syntax should be collected
	 * silently.  Otherwise, an exception will be thrown if illegal syntax is detected. 
	 */
	public boolean skipIllegalTuples() {return this.skipIllegalTuples;}
	
	/**
	 * A setting of 'true' indicates that query tuples with illegal syntax should be collected
	 * silently.  Otherwise, an exception will be thrown if illegal syntax is detected. 
	 */
	public void setSkipIllegalTuples(boolean setting) {this.skipIllegalTuples = setting;}

	/**
	 * Return a list of illegal tuples returned by the last query.
	 */
	public List<List<String>> getIllegalTuples () {return this.illegalTuples;}
	
	public void close() {
	}
	
	/**
	 * Return a count of the number of tuples retrieved into the query result.
	 */
	public int getTupleCount() {
		return this.stringTuples.size();
	}

    /**
     * Return 'true' if the iterator has additional statement(s).
     */
    public boolean hasNext() {
    	return this.cursor < this.stringTuples.size();
    }
    
    private BindingSet createBindingSet(List<String> stringTuple) {
    	MapBindingSet bs = new MapBindingSet(stringTuple.size());
    	for (int i = 0; i < stringTuple.size(); i++) {
    		String stringTerm = stringTuple.get(i);
    		Value term = AllegroStatement.stringTermToTerm(stringTerm);
    		System.out.println("BS " + stringTerm + "  " + term);
    		bs.addBinding(this.getBindingNames().get(i), term);
    	}
    	return bs;
    }

    /**
     * Return the next Statement in the answer, if there is one.
     *   TODO: WHOOOA.  WHAT IF WE HAVE TUPLES INSTEAD OF STATEMENTS; HOW DOES THAT WORK???
     */
    public BindingSet next() {
        if (this.hasNext()) {
            List<String>stringTuple = this.stringTuples.get(this.cursor);
            this.cursor++;
            try {
            	return createBindingSet(stringTuple);
            } catch (IllegalArgumentException ex) {
            	if (this.skipIllegalTuples()) {
            		if (this.illegalTuples == null) this.illegalTuples = new ArrayList<List<String>>();
            		this.illegalTuples.add(stringTuple);
            		return this.next();
            	}
            	throw ex;
            }
        } else {
        	return null;
        }
    }

	public void remove() {
		throw new UnimplementedMethodException("remove");
	}

}
