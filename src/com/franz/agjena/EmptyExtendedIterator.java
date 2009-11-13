package com.franz.agjena;

import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.util.iterator.Map1;

public class EmptyExtendedIterator implements ExtendedIterator {

	public ExtendedIterator andThen(ClosableIterator arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public ExtendedIterator filterDrop(Filter arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public ExtendedIterator filterKeep(Filter arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public ExtendedIterator mapWith(Map1 arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object removeNext() {
		// TODO Auto-generated method stub
		return null;
	}

	public List toList() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set toSet() {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() {
		// TODO Auto-generated method stub

	}

	/**
	 * Return false, indicating that this iterator is empty.
	 */
	public boolean hasNext() {
		return false;
	}

	public Object next() {
		// TODO Auto-generated method stub
		return null;
	}

	public void remove() {
		// TODO Auto-generated method stub

	}

}
