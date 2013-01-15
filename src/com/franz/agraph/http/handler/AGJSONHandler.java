/******************************************************************************
** Copyright (c) 2008-2013 Franz Inc.
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

import com.franz.agraph.http.exception.AGHttpException;

public class AGJSONHandler extends AGResponseHandler {

	private JSONObject result;
	
	public AGJSONHandler() {
		super("application/json");
	}

	@Override
	public void handleResponse(HttpMethod method) throws IOException, AGHttpException {
		/* TODO: server sometimes responds with text/plain, not application/json
		String mimeType = getResponseMIMEType(method);
		if (!mimeType.equals(getRequestMIMEType())) {
			throw new AGHttpException("unexpected response MIME type: " + mimeType);
		}*/
		try {
			InputStream response = getInputStream(method);
			String resp = streamToString(response);
			// TODO: server sometimes returns the object in a JSONArray
			if (resp.startsWith("[{") && resp.endsWith("}]")) {
				resp = resp.substring(1, resp.lastIndexOf("]"));
			}
			result = new JSONObject(resp);
		} catch (JSONException e) {
			throw new AGHttpException(e);
		}
	}
	
	public JSONObject getResult() {
		return result;
	}
	
}
