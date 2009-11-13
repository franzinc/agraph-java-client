package com.franz.agbase;


import com.franz.agbase.AllegroGraphException;
import com.franz.ag.UPI;
import com.franz.agbase.impl.TriplesIteratorImpl;
import com.franz.agbase.util.AGInternals;

/**
 * This abstract class is the superclass of the AllegroGraph serializer classes.
 * @author mm
 *
 */
public abstract class AllegroGraphSerializer {
	

	protected AllegroGraph ag = null;
	protected Object sourceId = null;
	protected Object sourceType = null;
	

	protected Object source = null;
	
	protected String destination = null;
	protected Object result = null;
	
	/**
	 * Serialize all the triples in a triple store.
	 * @param source The AllegroGraph instance to be serialized.
	 * @return The string containing the serialization if destination is null,
	 *    or null if the destination is a file.
	 * @throws AllegroGraphException
	 */
	public Object run( AllegroGraph source) throws AllegroGraphException {
		sourceType = "store";
		sourceId = new Integer(source.tsx);
		ag = source;
		this.source = source;
		result = run();
		if ( null==destination ) return result;
		return null;
	}
	
	/**
	 * Serialize all the triples collected by a cursor.
	 * @param source The Cursor instance to be serialized.
	 * @return The string containing the serialization if destination is null,
	 *    or null if the destination is a file.
	 * @throws AllegroGraphException
	 */
	public Object run( TriplesIterator source) throws AllegroGraphException {
		sourceType = "cursor";
		sourceId = ((TriplesIteratorImpl)source).getSource();
		this.source = source;
		ag = ((TriplesIteratorImpl)source).getAG();
		result = run();
		if ( null==destination ) return result;
		return null;
	}
	
	protected abstract Object run() throws AllegroGraphException;
	
	
	/**
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}
	/**
	 * @param destination Null to serialize to a string, or a string containing the pathname
	 *    of the destination file or folder.
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}
	/**
	 * @return the result
	 */
	public Object getResult() {
		return result;
	}
	/**
	 * @return the source
	 */
	public Object getSource() {
		return source;
	}
	
	Object verifiedBase = null;
	Object userBase = null;
	void setBase ( Object base ) {

		
		
		if ( base==null )
		{
			userBase = null;  verifiedBase = null; return;
		}
		if ( base instanceof String )
		{   // assume base is a string URI
			userBase = base;
			verifiedBase = AGInternals.refNodeToString((String) base);
			return;
		}
		if ( base instanceof UPI )
		{   
			userBase = base; verifiedBase = base; return;
		}
		if ( base instanceof ValueNode )
		{
			userBase = base;
			verifiedBase = AGInternals.validRefOb(base);
			return;
		}
		if ( base instanceof AGInternals )
		{
			userBase = base;
			verifiedBase = ((AGInternals)base).tsx;
			return;
		}
		if ( (base instanceof Boolean) && ((Boolean)base).booleanValue() )
		{
			userBase = base;
			verifiedBase = 1;
			return;
		}
		throw new IllegalArgumentException("Not a valid base URI value: " + base);			
	
	}
	

}
