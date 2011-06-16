/******************************************************************************
** Copyright (c) 2008-2011 Franz Inc.
** All rights reserved. This program and the accompanying materials
** are made available under the terms of the Eclipse Public License v1.0
** which accompanies this distribution, and is available at
** http://www.eclipse.org/legal/epl-v10.html
******************************************************************************/

package com.franz.agraph.http.handler;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpMethod;
import org.json.JSONException;
import org.json.JSONObject;

import com.franz.agraph.http.AGHttpException;

public class AGJSONHandler extends AGResponseHandler {

	private JSONObject result;
	
	public AGJSONHandler() {
		super("application/json");
	}

	@Override
	public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
		/* TODO: server responds with text/plain here, not application/json
		String mimeType = getResponseMIMEType(method);
		if (!mimeType.equals(getRequestMIMEType())) {
			throw new AGHttpException("unexpected response MIME type: " + mimeType);
		}*/
		InputStream response = getInputStream(method);
		try {
			result = new JSONObject(streamToString(response));
		} catch (JSONException e) {
			throw new AGHttpException(e.getLocalizedMessage());
		}
	}
	
	public JSONObject getResult() {
		return result;
	}
	
}
