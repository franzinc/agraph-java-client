/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.apache.jena.graph.Capabilities;
import org.apache.jena.graph.impl.AllCapabilities;

/**
 * Implements the Jena Capabilities interface for AllegroGraph.
 */
public class AGCapabilities extends AllCapabilities implements Capabilities {

    @Override
    // TODO: "true" would require support for D-entailment
    public boolean handlesLiteralTyping() {
        return false;
    }

}
