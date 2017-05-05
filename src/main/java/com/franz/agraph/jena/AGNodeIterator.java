/******************************************************************************
** See the file LICENSE for the full license governing this code.
******************************************************************************/

package com.franz.agraph.jena;

import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;

import com.franz.util.Closeable;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
 * 
 * A utility class for iterating over Jena Nodes.
 *
 */
public class AGNodeIterator extends NiceIterator<Node>
implements Closeable {
	
	private final TupleQueryResult result;
	
	/**
	 * Constructs an AGNodeIterator from a TupleQueryResult.
	 *  
	 * @param result a TupleQueryResult with binding var "st".
	 */
	public AGNodeIterator(TupleQueryResult result) {
		this.result = result;
	}

	@Override
	public void close() {
		try {
			result.close();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);		
		}
	}

	@Override
	public boolean hasNext() {
		try {
			return result.hasNext();
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);		
		}
	}

	@Override
	public Node next() {
		try {
			return AGNodeFactory.asNode(result.next().getValue("st"));
		} catch (QueryEvaluationException e) {
			throw new RuntimeException(e);		
		}
	}

}
