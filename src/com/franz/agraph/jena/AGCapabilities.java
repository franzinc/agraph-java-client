package com.franz.agraph.jena;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.impl.AllCapabilities;

public class AGCapabilities extends AllCapabilities implements Capabilities {

	@Override
	// TODO: "true" would require support for D-entailment 
    public boolean handlesLiteralTyping() { return false; }

}
