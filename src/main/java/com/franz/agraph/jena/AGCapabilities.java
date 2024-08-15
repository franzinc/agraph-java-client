/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.graph.Capabilities;
import org.apache.jena.graph.impl.AllCapabilities;

public class AGCapabilities {

    private static final Capabilities instance = AllCapabilities.create(true, true, true, false);;

    public static Capabilities getInstance() {
        return instance;
    }
}
