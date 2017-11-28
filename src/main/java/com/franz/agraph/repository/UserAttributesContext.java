package com.franz.agraph.repository;

import org.json.JSONObject;

/**
 * A nestable class for defining a scope over which user attributes are set
 * on an AGRepositoryConnection.
 * <p>
 * A UserAttributeContext can be used with a try-with-resources statement to automate
 * the saving, setting and restoring of userAttributes on an AGRepositoryConnection
 * object. It is intended as an aid to help ensure a specific set of user attributes
 * are not leaked outside the scope of their intended use.
 * <p>
 * Any request made between instantiation of this class and a call to the close()
 * method will automatically have an x-user-attributes header added to each request
 * sent to via the AGRepositoryConnection object passed as an argument to one of
 * the constructors. While the context instance is live, and before close() is called,
 * the AGRepositoryConnection can be use directly, or referenced via the `conn' field
 * of the context itself.
 * <p>
 * See example25() for sample code demonstrating its use.
 */
public class UserAttributesContext implements AutoCloseable {
    public AGRepositoryConnection conn;
    private String oldUserAttrs;

    /**
     * Instantiate a UserAttributesContext object. Saves any existing userAttributes
     * set on the argument connection object via connection.getUserAttributes() and then
     * calls connection.setUserAttributes(attrs).
     *
     * @param connection, an instance of AGRepositoryConnection
     * @param attrs,      a String representing a JSON object comprising a collection of
     *                    attribute/value pairs.
     */
    public UserAttributesContext(AGRepositoryConnection connection, String attrs) {
        conn = connection;
        oldUserAttrs = conn.getUserAttributes();
        conn.setUserAttributes(attrs);
    }

    /**
     * Instantiate a UserAttributesContext object. Saves any existing userAttributes
     * set on the argument connection object via connection.getUserAttributes() and then
     * calls connection.setUserAttributes(attrs).
     *
     * @param connection, an instance of AGRepositoryConnection
     * @param attrs,      a JSONObject comprising a collection of attribute/value pairs.
     */
    public UserAttributesContext(AGRepositoryConnection connection, JSONObject attrs) {
        conn = connection;
        oldUserAttrs = conn.getUserAttributes();
        conn.setUserAttributes(attrs);
    }

    /**
     * restore, via conn.setUserAttributes() the user attributes that were defined on
     * the AGRepositoryConnection object passed to the constructor of this instance.
     */
    public void close() {
        conn.setUserAttributes(oldUserAttrs);
    }
}
