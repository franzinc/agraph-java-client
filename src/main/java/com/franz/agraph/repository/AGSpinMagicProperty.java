/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.repository;

import org.eclipse.rdf4j.query.BindingSet;

/**
 * Represents a SPIN magic property.
 *
 * @since v4.4
 */
public class AGSpinMagicProperty extends AGSpinFunction {

    /**
     * @param uri       spin function identifier
     * @param arguments name of arguments in the sparqlQuery, must include question mark
     * @param query     spin function query text
     */
    public AGSpinMagicProperty(String uri, String[] arguments, String query) {
        super(uri, arguments, query);
    }

    public AGSpinMagicProperty(BindingSet bindings) {
        super(bindings);
    }

}
