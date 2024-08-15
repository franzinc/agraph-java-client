/******************************************************************************
 ** See the file LICENSE for the full license governing this code.
 ******************************************************************************/

package com.franz.agraph.jena;

/**
 * A utility class for operations not currently supported by AllegroGraph.
 */
class AGUnsupportedOperationException extends UnsupportedOperationException {
    private static final String message = "Operation is unsupported in AG Java client";

    public AGUnsupportedOperationException() {
        super(message);
    }
}
