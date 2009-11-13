package com.franz.agbase;



/**
 * This interface defines the attributes of a particular geospatial encoding
 * in AllegroGraph.
 * <p>
 * In order to create encoded geospatial values in triples, the application must define
 * an encoding.  
 * The encoding must be registered with a call to one of the registration methods
 * in the {@link GeoExtension} class.
 * <p>
 * The registered encoding is associated with a triple store by calling
 * {@link GeoExtension#addSubtype(GeospatialSubtypeImpl)}.
 * 
 * <p>
 * @author mm
 *
 */
public interface GeospatialSubtype {
	
	/**
	 * Query the UUID string associated with this GeospatialSubtype.
	 * @return the UUID string
	 */
	public String getUUID();
	
	/**
	 * Query the XSD type URI associated with this GeospatialSubtype.
	 * @return the XSD type URI string
	 */
	public String getXSDType();

}