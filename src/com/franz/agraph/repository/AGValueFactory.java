/**
 * 
 */
package com.franz.agraph.repository;

import java.io.IOException;

import org.openrdf.http.protocol.UnauthorizedException;
import org.openrdf.model.BNode;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.ntriples.NTriplesUtil;

import com.franz.agraph.http.AGHTTPClient;

/**
 *
 */
public class AGValueFactory extends ValueFactoryImpl {

	private final AGRepository repository;
	
	private int blankNodeAmount = 100;
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
	
	public void initialize() throws RepositoryException {
		getBlankNodeIds();
	}
	
	@Override
	public synchronized BNode createBNode() {
		String id;
		try {
			id = getNextBNodeId();
		} catch (RepositoryException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
		return NTriplesUtil.parseBNode(id, this);
	}

	@Override
	public synchronized BNode createBNode(String id) {
		return super.createBNode(id);
	}

	String getNextBNodeId() throws RepositoryException {
		if (index==-1) {
			throw new RepositoryException("Value Factory has no blank node ids.");
		}
		String id = blankNodeIds[index];
		index++;
		if (index==blankNodeAmount) {
			getBlankNodeIds();
		}
		return id; 
	}
	
	private void getBlankNodeIds() throws RepositoryException {
		try {
			blankNodeIds = getHTTPClient().getBlankNodes(getRepository().getRepositoryURL(),blankNodeAmount);
			index = 0;
		} catch (UnauthorizedException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}		
	}
	
}
