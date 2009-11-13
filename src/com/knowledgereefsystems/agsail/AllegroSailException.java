/* Copyright (C) 2008 Knowledge Reef Systems.  All rights reserved. */

package com.knowledgereefsystems.agsail;

import org.openrdf.sail.SailException;

public class AllegroSailException extends SailException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 7874336557187279899L;

	public AllegroSailException() {
        super();
    }

    public AllegroSailException(final String message) {
        super(message);
    }

    public AllegroSailException(final Throwable cause) {
        super(cause);
    }

    public AllegroSailException(final String message,
                                final Throwable cause) {
        super(message, cause);
    }

}
