/**
 * 
 */
package com.franz.agbase.impl;

import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.GeoExtension;
import com.franz.agbase.GeospatialSubtype;
import com.franz.agbase.util.AGInternals;


/**
 * Instances of this class represent the attributes of a particular geospatial encoding
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
public class GeospatialSubtypeImpl implements GeospatialSubtype {
	
	private long local = -1;
	private GeospatialSubtype[] home = null;
	private String uuid = null;
	private String xsd = null;
	
	public static GeospatialSubtype get ( long local, AGInternals ag ) {
		if ( (local<0) || 255<local )
			throw new IllegalArgumentException( "Not a GeospatialSubtype index " + local);
		GeospatialSubtype[] all = ag.ags.geoSubs;
		GeospatialSubtype old = all[(int) local];
		if ( old==null ) 
			old = new GeospatialSubtypeImpl(local, all);
		return old;
	}
	
	protected GeospatialSubtypeImpl ( long local, GeospatialSubtype[] all ) {
		this.local = local;
		home = all;
		all[(int) local] = this;
	}
	
	public void setUUID ( String id ) { uuid = id; }
	public String getUUID () { return uuid; }
	
	public void setXSDType ( String id ) { xsd = id; }
	public String getXSDType () { return xsd; }
	
	public long getLocal( AllegroGraphConnection conn ) {
		if ( home==conn.geoSubs )
			return local;
		throw new IllegalStateException("Obsolete instance " + this);
	}
	
	public String toString () {
		return "<GeospatialSubtype " + local + ":" + uuid + ">";
	}
	
}