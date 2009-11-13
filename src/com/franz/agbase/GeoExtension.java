package com.franz.agbase;

import com.franz.agbase.AllegroGraphException;
import com.franz.agbase.UPI;
import com.franz.agbase.impl.AGFactory;
import com.franz.agbase.impl.GeospatialSubtypeImpl;
import com.franz.agbase.impl.TriplesIteratorImpl;
import com.franz.agbase.transport.AGConnector;
import com.franz.agbase.util.AGC;

/**
 * This class implements Geospatial Analysis tools.
 * <p>
 * An AllegroGraph triple store can be used to store geographic locations
 * in a compact and efficiently searchable representation.
 * The methods in this class implement search operations on locations.
 * <p>
 * This class is instantiated from an AllegroGraph instance with a call to
 * {@link AllegroGraph#getGeoExtension()}.
 *  
 * @author mm
 *
 */
public class GeoExtension {
	
	private AllegroGraph ag = null;
	
	GeoExtension ( AllegroGraph ag ) {
		this.ag = ag;
	}

	/**
	 * Register a cartesian striping subtype.
	 * 
	 * @param lonMin
	 * @param lonMax
	 * @param latMin
	 * @param latMax
	 * @param stripWidth
	 * @return a GeospatialSubtype instance
	 * @throws AllegroGraphException
	 */
	public GeospatialSubtype registerCartesianStriping ( double lonMin, double lonMax, double latMin, double latMax, 
			double stripWidth) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_STRIPING, new Object[] {
				"cartesian",
				new Double(lonMin), new Double(lonMax), new Double(latMin), new Double(latMax),
				new Double(stripWidth) });
		return registeredStriping(v);
	}
	
	GeospatialSubtype registeredStriping ( Object[] v ) {
		if ( v==null ) return null;
		if ( 1>v.length ) return null;
		GeospatialSubtypeImpl s = (GeospatialSubtypeImpl) GeospatialSubtypeImpl.get(AGConnector.longValue(v[0]), ag);
		if ( 3==v.length ) {
			s.setUUID((String) v[1]);
			s.setXSDType((String) v[2]);
		}
		return s;
	}
	
	
	/**
	 * Query all the geospatial subtypes defined in the triple store.
	 * @return an array of GeospatialSubtype instances.
	 * @throws AllegroGraphException
	 */
	public GeospatialSubtype[] getSubtypes () throws AllegroGraphException {
		
		Object[] v = null;
		try {
			v = ag.verifyEnabled().applyAGFn(ag, AGC.AGJ_GEOSUBTYPES, new Object[] {1});
		} catch (IllegalArgumentException e) {
			// Try again, maybe talking to an old server - pre-bug18543 patch
			v = ag.verifyEnabled().applyAGFn(ag, AGC.AGJ_GEOSUBTYPES, new Object[0]);
		}
		if ( v==null ) return new GeospatialSubtype[0];
		if ( 0==v.length ) return new GeospatialSubtype[0];
		Object w = v[0];
		GeospatialSubtype[] r;
		
		if ( 1==v.length && w instanceof String[] ) {
			if ( !(w instanceof String[]) ) return new GeospatialSubtype[0];
			String[] y = (String[]) w;
			r = new GeospatialSubtype[(y.length)/2];
			for (int i = 0; i < y.length; i=i+2) {
				long n = Long.parseLong(y[i]);
				GeospatialSubtypeImpl s = (GeospatialSubtypeImpl) GeospatialSubtypeImpl.get(n, ag);
				s.setUUID(y[i+1]);
				r[i/2] = s;
			}
			return r;
		}
		
		// [bug18543] returning more info from the server
		//System.out.println("getSubtypes " + v.length + "  " + w + "  " + isLongArray(w));
		if ( 3==v.length && isLongArray(w) ) {
			int[] ids = AGConnector.intArray(w);
			String[] uuids = (String[]) v[1]; 
			String[] xsdts = (String[]) v[2]; 
			r = new GeospatialSubtype[ids.length];
			for (int i = 0; i < ids.length; i++) {
				GeospatialSubtypeImpl s = (GeospatialSubtypeImpl) GeospatialSubtypeImpl.get(ids[i], ag);
				s.setUUID(uuids[i]);
				s.setXSDType(xsdts[i]);
				r[i] = s;
			}
			return r;
		}
		return new GeospatialSubtype[0];
	}
	
	static boolean isLongArray ( Object x ) {
		if ( x==null ) return false;
		if ( x instanceof Object[] ) return true;
		if ( x instanceof long[] ) return true;
		if ( x instanceof int[] ) return true;
		if ( x instanceof short[] ) return true;
		if ( x instanceof byte[] ) return true;
		return false;
	}
	
	
	/**
	 * Register a latitude striping subtype.
	 * @param width in degrees
	 * @return a GeospatialSubtype instance
	 * @throws AllegroGraphException
	 */
	public GeospatialSubtype registerLatitudeStripingInDegrees ( double width ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_STRIPING, 
				new Object[] { "degrees", new Double(width),  });
		return registeredStriping(v);
	}
	
	/**
	 * Register a latitude striping subtype.
	 * @param width width in degrees
	 * @param latMin the minimum latitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param latMax the maximum latitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param lonMin the minimum longitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param lonMax the maximum longitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @return
	 * @throws AllegroGraphException
	 */
	public GeospatialSubtype registerLatitudeStripingInDegrees ( double width, Double latMin,
			Double latMax, Double lonMin, Double lonMax ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_STRIPING, 
				new Object[] { "degrees", new Double(width), 
								"lat-min", latMin,
								"lat-max", latMax,
								"lon-min", lonMin,
								"lon-max", lonMax
		});
		return registeredStriping(v);
	}
	
	
	
	/**
	 * Register a latitude striping subtype.
	 * @param width in miles
	 * @return a GeospatialSubtype instance
	 * @throws AllegroGraphException
	 */
	public GeospatialSubtype registerLatitudeStripingInMiles ( double width ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_STRIPING, 
				new Object[] { "miles", new Double(width),  });
		return registeredStriping(v);
	}
	
	/**
	 * Register a latitude striping subtype.
	 * @param width width in miles
	 * @param latMin the minimum latitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param latMax the maximum latitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param lonMin the minimum longitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param lonMax the maximum longitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @return
	 * @throws AllegroGraphException
	 */
	public GeospatialSubtype registerLatitudeStripingInMiles ( double width, Double latMin,
			Double latMax, Double lonMin, Double lonMax ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_STRIPING, 
				new Object[] { "miles", new Double(width), 
								"lat-min", latMin,
								"lat-max", latMax,
								"lon-min", lonMin,
								"lon-max", lonMax
		});
		return registeredStriping(v);
	}
	
	/**
	 * Register a latitude striping subtype.
	 * @param width in kilometers
	 * @return a GeospatialSubtype instance
	 * @throws AllegroGraphException
	 */
	public GeospatialSubtype registerLatitudeStripingInKm ( double width ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_STRIPING, 
				new Object[] { "km", new Double(width),  });
		return registeredStriping(v);
	}
	
	/**
	 * Register a latitude striping subtype.
	 * @param width width in kilometers
	 * @param latMin the minimum latitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param latMax the maximum latitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param lonMin the minimum longitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param lonMax the maximum longitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @return
	 * @throws AllegroGraphException
	 */
	public GeospatialSubtype registerLatitudeStripingInKm ( double width, Double latMin,
			Double latMax, Double lonMin, Double lonMax ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_STRIPING, 
				new Object[] { "km", new Double(width), 
								"lat-min", latMin,
								"lat-max", latMax,
								"lon-min", lonMin,
								"lon-max", lonMax
		});
		return registeredStriping(v);
	}
	
	/**
	 * Register a latitude striping subtype.
	 * @param width in radians
	 * @return a GeospatialSubtype instance
	 * @throws AllegroGraphException
	 */
	public GeospatialSubtype registerLatitudeStripingInRadians ( double width ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_STRIPING, 
				new Object[] { "radians", new Double(width),  });
		return registeredStriping(v);
	}
	
	/**
	 * Register a latitude striping subtype.
	 * @param width width in radians
	 * @param latMin the minimum latitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param latMax the maximum latitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param lonMin the minimum longitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @param lonMax the maximum longitude included in the strip. A null value specifies the
	 *          built-in default of the server.
	 * @return
	 * @throws AllegroGraphException
	 */
	public GeospatialSubtype registerLatitudeStripingInRadians ( double width, Double latMin,
			Double latMax, Double lonMin, Double lonMax ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_REGISTER_STRIPING, 
				new Object[] { "radians", new Double(width), 
								"lat-min", latMin,
								"lat-max", latMax,
								"lon-min", lonMin,
								"lon-max", lonMax
		});
		return registeredStriping(v);
	}
	
	/**
	 * Associate a GeospatialSubtype with a triple store.
	 * @param subtype the GeospatialSubtype instance
	 * @throws AllegroGraphException
	 */
	public void addSubtype ( GeospatialSubtype subtype ) throws AllegroGraphException {
		ag.verifyEnabled().applyAGFn(ag, AGC.AG_ADD_SUBTYPE, 
				new Object[] { ((GeospatialSubtypeImpl) subtype).getLocal(ag.ags) });
		
	}
	
	/**
	 * Create a geospatial UPI instance encoding a location.
	 * @param subtype the GeospatialSubtype instance that defines the encoding
	 * @param longitude
	 * @param latitude
	 * @return
	 * @throws AllegroGraphException
	 */
	public UPI encodeUPI ( GeospatialSubtype subtype, double longitude, double latitude ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_ENCODE_GEOUPI, 
				new Object[] { ((GeospatialSubtypeImpl) subtype).getLocal(ag.ags), new Double(longitude), new Double(latitude)});
		return (UPI)v[0];
	}
	
	/**
	 * Create an array of geospatial UPI instances encoding locations.
	 * @param subtype the GeospatialSubtype instance that defines the encoding
	 * @param longitude an array of longitude values
	 * @param latitude an array of latitude values.  
	 * @return an array of UPI instances.  The length is the same as the length 
	 *      of the longer argument array.  The last value in the shorter array is repeated.
	 * @throws AllegroGraphException
	 */
	public UPI[] encodeUPIs ( GeospatialSubtype subtype, double[] longitude, double[] latitude )
	throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_ENCODE_GEOUPI, 
				new Object[] { ((GeospatialSubtypeImpl) subtype).getLocal(ag.ags), longitude, latitude });
		return (UPI[])v[0];
	}
	
	/**
	 * Create an array of geospatial UPI instances encoding locations.
	 * @param subtype the GeospatialSubtype instance that defines the encoding
	 * @param longitude a longitude value
	 * @param latitude an array of latitude values.  
	 * @return an array of UPI instances.  The length is the same as the length of the latitude array.
	 * @throws AllegroGraphException
	 */
	public UPI[] encodeUPIs ( GeospatialSubtype subtype, double longitude, double[] latitude )
	throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_ENCODE_GEOUPI, 
				new Object[] { ((GeospatialSubtypeImpl) subtype).getLocal(ag.ags), new Double(longitude), latitude });
		return (UPI[])v[0];
	}
	
	/**
	 * Create an array of geospatial UPI instances encoding locations.
	 * @param subtype the GeospatialSubtype instance that defines the encoding
	 * @param longitude an array of longitude values
	 * @param latitude a latitude value
	 * @return an array of UPI instances.  The length is the same as the length of the longitude array.
	 * @throws AllegroGraphException
	 */
	public UPI[] encodeUPIs ( GeospatialSubtype subtype, double[] longitude, double latitude )
	throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_ENCODE_GEOUPI, 
				new Object[] { ((GeospatialSubtypeImpl) subtype).getLocal(ag.ags), longitude, new Double(latitude) });
		return (UPI[])v[0];
	}
	
	/**
	 * Create an array of geospatial UPI instances encoding locations.
	 * @param subtype the GeospatialSubtype instance that defines the encoding
	 * @param longlat an array of alternating longitude and latitude values
	 * @return an array of UPI instances.  The length is half the length of the argument array.
	 * @throws AllegroGraphException
	 */
	public UPI[] encodeUPIs ( GeospatialSubtype subtype, double[] longlat ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_ENCODE_GEOUPI, 
				new Object[] { ((GeospatialSubtypeImpl) subtype).getLocal(ag.ags), "longlat", longlat });
		return (UPI[])v[0];
	}

	/**
	 * Decode a geospatial UPI into subtype, longitude and latitude.
	 * @param upi
	 * @return an array of 3 Object instances:
	 *    <ul>
	 *    <li>the GeospatialSubtype instance that defines the encoding
	 *    <li>a Double instance containing the longitude value
	 *    <li>a Double instance containing the latitude value
	 *    </ul>
	 * @throws AllegroGraphException
	 */
	public Object[] decodeUPI ( UPI upi ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_DECODE_GEOUPI, 
				new Object[] { upi });
		v[0] = GeospatialSubtypeImpl.get(((Long)v[0]).longValue(), ag);
		return v;
	}
	
	/**
	 * Decode an array of geospatial UPIs into subtype, longitude and latitude arrays.
	 * @param upi an array of UPI instances
	 * @return an array of 3 Object instances:
	 *    <ul>
	 *    <li>an array of GeospatialSubtype instances 
	 *    <li>an array of Double instances containing the longitude values
	 *    <li>an array of Double instances containing the latitude values
	 *    </ul>
	 * @throws AllegroGraphException
	 */
	public Object[] decodeUPIs ( UPI[] upi ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_DECODE_GEOUPI, 
				new Object[] { upi });
		Object w = v[0];
		if ( w instanceof byte[] ) 
		{
			byte[] gw = (byte[]) w;
			GeospatialSubtype[] gs = new GeospatialSubtype[gw.length];
			for (int i = 0; i < gs.length; i++) {
				gs[i] = GeospatialSubtypeImpl.get(gw[i], ag);
			}
			v[0] = gs;
		}
		else
			v[0] = GeospatialSubtypeImpl.get(AGConnector.longValue(v[0]), ag);
		return v;
	}
	
	private Integer lookAheadArg () {
		int lh = ag.defaultLookAhead;
		if (lh < 1) lh = TriplesIteratorImpl.defaultLookAhead;
		return new Integer(lh);
	}
	
	private TriplesIterator geoCursor ( Object[] v ) {
		if ( (v==null) || 2>v.length ) return ag.emptyCursor();
		return AGFactory.makeCursor(ag, v[0], AGConnector.toUPIArray(v[1]));
	}
	
	/**
	 * Find all the triples in some bounding box.
	 * @param subtype the GeospatialSubtype of interest
	 * @param predicate a string, UPI instance, ot Value instance that identifies
	 *    the predicate of the relevant triples
	 * @param xMin
	 * @param xMax
	 * @param yMin
	 * @param yMax
	 * @param indexedOnly if true, look only at indexed triples (this may be a lot faster
	 *         if there are many unindexed triples).
	 * @param includeDeleted if true, include deleted triples in the search
	 * @return a Cursor instance that can iterate over the search result
	 * @throws AllegroGraphException
	 */
	public TriplesIterator getStatementsInBoundingBox ( GeospatialSubtype subtype, Object predicate,
			double xMin, double xMax, double yMin, double yMax, 
			boolean indexedOnly, boolean includeDeleted ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_GET_GEO_IN_BOX,
				              new Object[]{ 
								((GeospatialSubtypeImpl) subtype).getLocal(ag.ags),
								ag.validRef(predicate),
								new Double(xMin), new Double(xMax),
								new Double(yMin), new Double(yMax),
								"indexed-only", indexedOnly?"":null,
								"include-deleted", includeDeleted?"":null,
								"look-ahead", lookAheadArg()
				              });
		return geoCursor(v);
	}
	
	private TriplesIterator getGeoStatements ( String units, GeospatialSubtype subtype, Object predicate,
			double x, double y, double radius, boolean indexedOnly, boolean includeDeleted ) throws AllegroGraphException {
		Object[] v = ag.verifyEnabled().applyAGFn(ag, AGC.AG_GET_GEO_TRIPLES,
				              new Object[]{ units,
								((GeospatialSubtypeImpl) subtype).getLocal(ag.ags),
								ag.validRef(predicate),
								new Double(x), new Double(y),
								new Double(radius),
								"indexed-only", indexedOnly?"":null,
								"include-deleted", includeDeleted?"":null,
								"look-ahead", lookAheadArg()
				              });
		return geoCursor(v);
	}
	
	/**
	 * Find all the triples in a circle.
	 * @param subtype the GeospatialSubtype of interest
	 * @param predicate a string, UPI instance, or Value instance that identifies
	 *    the predicate of the relevant triples
	 * @param x The position of the center in Cartesian coordinates.
	 * @param y The position of the center in Cartesian coordinates.
	 * @param radius
	 * @param indexedOnly if true, look only at indexed triples (this may be a lot faster
	 *         if there are many unindexed triples).
	 * @param includeDeleted if true, include deleted triples in the search
	 * @return a Cursor instance that can iterate over the search result
	 * @throws AllegroGraphException
	 */
	public TriplesIterator getStatementsInRadius ( GeospatialSubtype subtype, Object predicate,
			double x, double y, double radius, boolean indexedOnly, boolean includeDeleted ) throws AllegroGraphException {
		return getGeoStatements("radius", subtype, predicate, x, y, radius, indexedOnly, includeDeleted);
	}
	
	/**
	 * Find all the triples in a circle.
	 * @param subtype the GeospatialSubtype of interest
	 * @param predicate a string, UPI instance, ot Value instance that identifies
	 *    the predicate of the relevant triples
	 * @param lon The position of the center in geographic coordinates.
	 * @param lat The position of the center in geographic coordinates.
	 * @param radius the radius of the circle in miles
	 * @param indexedOnly if true, look only at indexed triples (this may be a lot faster
	 *         if there are many unindexed triples).
	 * @param includeDeleted if true, include deleted triples in the search
	 * @return a Cursor instance that can iterate over the search result
	 * @throws AllegroGraphException
	 */
	public TriplesIterator getStatementsInHMiles ( GeospatialSubtype subtype, Object predicate,
			double lon, double lat, double radius, boolean indexedOnly, boolean includeDeleted ) throws AllegroGraphException {
		return getGeoStatements("hmi", subtype, predicate, lon, lat, radius, indexedOnly, includeDeleted);
	}
	
	/**
	 * Find all the triples in a circle.
	 * @param subtype the GeospatialSubtype of interest
	 * @param predicate a string, UPI instance, ot Value instance that identifies
	 *    the predicate of the relevant triples
	 * @param lon The position of the center in geographic coordinates.
	 * @param lat The position of the center in geographic coordinates.
	 * @param radius the radius of the circle in kilometers
	 * @param indexedOnly if true, look only at indexed triples (this may be a lot faster
	 *         if there are many unindexed triples).
	 * @param includeDeleted if true, include deleted triples in the search
	 * @return a Cursor instance that can iterate over the search result
	 * @throws AllegroGraphException
	 */
	public TriplesIterator getStatementsInHKm ( GeospatialSubtype subtype, Object predicate,
			double lon, double lat, double radius, boolean indexedOnly, boolean includeDeleted ) throws AllegroGraphException {
		return getGeoStatements("hkm", subtype, predicate, lon, lat, radius, indexedOnly, includeDeleted);
	}
	
	/**
	 * Find all the triples in a circle.
	 * @param subtype the GeospatialSubtype of interest
	 * @param predicate a string, UPI instance, ot Value instance that identifies
	 *    the predicate of the relevant triples
	 * @param lon The position of the center in geographic coordinates.
	 * @param lat The position of the center in geographic coordinates.
	 * @param radius the radius of the circle in radians
	 * @param indexedOnly if true, look only at indexed triples (this may be a lot faster
	 *         if there are many unindexed triples).
	 * @param includeDeleted if true, include deleted triples in the search
	 * @return a Cursor instance that can iterate over the search result
	 * @throws AllegroGraphException
	 */
	public TriplesIterator getStatementsInHRadians ( GeospatialSubtype subtype, Object predicate,
			double lon, double lat, double radius, boolean indexedOnly, boolean includeDeleted ) throws AllegroGraphException {
		return getGeoStatements("hrad", subtype, predicate, lon, lat, radius, indexedOnly, includeDeleted);
	}

	

	
	/**
	 * Add a predicate mapping for a geospatial subtype encoding.
	 * @param label the URL that identifies the predicate
	 * @param subtype the GeospatialSubtype of the encoded literal
	 * @throws AllegroGraphException
	 */
	public void addSphericalPredicateMapping ( String label, GeospatialSubtype subtype ) throws AllegroGraphException {
		ag.verifyEnabled().applyAGFn(ag, AGC.AG_ADD_GEO_MAPPING,
				new Object[] { "sph-pred", label, ((GeospatialSubtypeImpl) subtype).getLocal(ag.ags) });
		
	}

	/**
	 * Add a predicate mapping for a geospatial subtype encoding.
	 * @param label the URL that identifies the datatype
	 * @param subtype the GeospatialSubtype of the encoded literal
	 * @throws AllegroGraphException
	 */
	public void addSphericalDatatypeMapping ( String label, GeospatialSubtype subtype ) throws AllegroGraphException {
		ag.verifyEnabled().applyAGFn(ag, AGC.AG_ADD_GEO_MAPPING,
				new Object[] { "sph-type", label, ((GeospatialSubtypeImpl) subtype).getLocal(ag.ags) });
		
	}
	
	/**
	 * Add a predicate mapping for a geospatial subtype encoding.
	 * @param label the URL that identifies the predicate
	 * @param subtype the GeospatialSubtype of the encoded literal
	 * @throws AllegroGraphException
	 */
	public void addCartesianPredicateMapping ( String label, GeospatialSubtype subtype ) throws AllegroGraphException {
		ag.verifyEnabled().applyAGFn(ag, AGC.AG_ADD_GEO_MAPPING,
				new Object[] { "cart-pred", label, ((GeospatialSubtypeImpl) subtype).getLocal(ag.ags) });
		
	}

	/**
	 * Add a predicate mapping for a geospatial subtype encoding.
	 * @param label the URI that identifies the datatype
	 * @param subtype the GeospatialSubtype of the encoded literal
	 * @throws AllegroGraphException
	 */
	public void addCartesianDatatypeMapping ( String label, GeospatialSubtype subtype ) throws AllegroGraphException {
		ag.verifyEnabled().applyAGFn(ag, AGC.AG_ADD_GEO_MAPPING,
				new Object[] { "cart-type", label, ((GeospatialSubtypeImpl) subtype).getLocal(ag.ags) });
		
	}
	
}
