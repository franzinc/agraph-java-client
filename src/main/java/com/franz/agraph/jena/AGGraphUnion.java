/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

import org.eclipse.rdf4j.model.Resource;

public class AGGraphUnion extends AGGraph {

    AGGraphUnion(AGGraphMaker maker, Resource context, Resource... contexts) {
        super(maker, context, contexts);
    }

}
