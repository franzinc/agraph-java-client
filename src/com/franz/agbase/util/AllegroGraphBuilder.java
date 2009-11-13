package com.franz.agbase.util;

import com.franz.agbase.AllegroGraph;
import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.AllegroGraphException;

/**
 * This class is needed to perpetrate the two AG APIs.
 * Can delete when com.franz.ag package is removed.
 * @author mm
 *
 */
public class AllegroGraphBuilder extends AllegroGraph {
	
	public AllegroGraphBuilder ( AGInternals from, int ix, String name, String directory ) {
		super(from, ix, name, directory);
	}
	
	public AllegroGraphBuilder ( AllegroGraphConnection sv, String name, AGInternals[] parts, boolean supersede)
	throws AllegroGraphException {
		super(sv, name, parts, supersede);
	}
	
	public AllegroGraphBuilder ( AllegroGraphConnection sv, String name, String directory )
	throws AllegroGraphException {
		super(sv, name, directory);
	}
	
	public AllegroGraphBuilder ( AllegroGraphConnection sv, String access, String name, String directory)
	throws AllegroGraphException {
		super(sv, access, name, directory);
	}

}
