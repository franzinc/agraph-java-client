package com.franz.agraph.jena;

import org.openrdf.model.Resource;

public class AGGraphUnion extends AGGraph {

	AGGraphUnion(AGGraphMaker maker, Resource context, Resource... contexts) {
		super(maker,context,contexts);
	}
	
}
