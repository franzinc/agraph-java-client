/**
 * 
 */
package com.franz.agraph.repository;

import java.io.IOException;

import org.openrdf.http.protocol.UnauthorizedException;
import org.openrdf.model.BNode;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryException;

import com.franz.agraph.http.AGHTTPClient;

/**
 *
 */
public class AGValueFactory extends ValueFactoryImpl {

	private final AGRepository repository;
	
	private int blankNodeAmount = 10;
	private String[] blankNodeIds;
	private int index = -1;
	
	public AGValueFactory(AGRepository repository) {
		super();
		this.repository = repository;
		blankNodeIds = new String[blankNodeAmount];
	}
	
	/*---------*
	 * Methods *
	 *---------*/

	public AGRepository getRepository() {
		return repository;
	}
	
	public AGHTTPClient getHTTPClient() {
		return getRepository().getHTTPClient();
	}
	

	private void getBlankNodeIds() {
		try {
			blankNodeIds = getHTTPClient().getBlankNodes(getRepository().getRepositoryURL(),blankNodeAmount);
			index = blankNodeIds.length - 1;
		} catch (UnauthorizedException e) {
			// TODO: check on the proper exceptions to throw here
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (RepositoryException e) {
			throw new IllegalStateException(e);
		}		
	}
	
	String getNextBNodeId() {
		if (index==-1) {
			getBlankNodeIds();
		}
		String id = blankNodeIds[index];
		index--;
		// TODO: parse using NTriplesUtil here to create BNode?
		return id.substring(2);   // strip off leading '_:'; 
	}
	
	@Override
	public BNode createBNode(String nodeID) {
		if (nodeID == null || "".equals(nodeID))
			nodeID = getNextBNodeId();
		return super.createBNode(nodeID);
	}

	@Override
	public BNode createBNode() {
		return createBNode(null);
	}

}
