package com.franz.agbase.impl;

import com.franz.agbase.UPI;
import com.franz.agbase.AllegroGraphConnection;
import com.franz.agbase.AllegroGraph;
import com.franz.agbase.DefaultGraph;

/**
 * Instances of this class represent the marker that identifies 
 * the default graph, or the null context, of a triple store.
 * <p>
 * The label of any DefaultGraph is the string "default graph".
 * The toString() method returns a string that is different for different triple stores.
 * <p>
 * Instances are created by the system as needed to represent the triples 
 * returned from the store.  There are no public constructors and no methods 
 * to create instances from an application.  When the default graph is mentioned in
 * a method call, it should be mentioned with a null argument or the empty string,
 * as specified in the method description.  
 * In general, it is not advisable for applications to manipulate
 * DefaultGraph instances in any way. 
 * <p>
 * See the note in the description of 
 * {@link AllegroGraphConnection#federate(String, AllegroGraph[], boolean)}
 * for additional issues about default graphs.
 * <p>
 * It is likely (but not guaranteed) 
 * that two instances representing the default graph of the same store will be
 * identical.  They will necessarily return true from equals().
 *  
 * @author mm
 *
 */
public class DefaultGraphImpl extends ResourceNodeImpl implements DefaultGraph {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7722435639903332584L;

//	 Use package access here because only use should be in AGFactory
    DefaultGraphImpl ( AllegroGraph ag, UPI id ) {
		nodeUPI = id;
		owner = ag;
	}
    
    private String str = null;
    
    /**
     * This method provides a string representation of the instance as the string
     * <p><code>&lt;DefaultGraph aaa bbbbbb&gt;</code>
     * <p>
     * where <code>aaa</code> is the triple store index (shown in the string
     * form of the AllegroGraph instance), and <code>bbbbbb</code> is a hexadecimal
     * string that identifies the instance uniquely.
     */
    public String toString () {
    	if ( str!=null ) return str;
    	String agx;
    	if ( owner==null )
    		agx = "";
    	else
    		agx = "" + owner.tsx;
    	if ( null!=nodeUPI ) 
    		agx = agx + " " + ((UPIImpl) nodeUPI).getStoreBytes();
    	str = "<DefaultGraph " + agx + ">";
    	return str;
    }
}
