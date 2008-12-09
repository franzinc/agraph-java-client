package miniclient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import franz.exceptions.SoftException;

public class Request {
	
//	private static void enc(StringBuffer buf, String key, Object value) {
//		if (buf.length() > 0)
//			buf.append("&");
//		try {
//			// NOT SURE ABOUT THE 'toString':
//			buf.append(URLEncoder.encode(key, "UTF-8")).append("=").append(URLEncoder.encode(value.toString(), "UTF-8"));
//		} catch (UnsupportedEncodingException ex) {throw new SoftException(ex);}
//	}
//	
//	private static void encval(StringBuffer buf, String key, Object value) {
//		if ((value == null) || "".equals(value)) return;
//		if (value == Boolean.TRUE) enc(buf, key, "true");
//		else if (value == Boolean.FALSE) enc(buf, key, "false");
//		else if (value instanceof List) {
//			for (Object v : (List)value) enc(buf, key, v);
//		}
//		else enc(buf, key, value);
//	}
//	
//	protected static String urlenc(JSONObject options) {
//		StringBuffer buf = new StringBuffer();
//		for (Iterator it = options.keys(); it.hasNext();) {
//			String key = (String)it.next();
//			try {
//				Object value = options.get(key);
//				encval(buf, key, value);
//			} catch (JSONException ex) {throw new RuntimeException("JSON Exception that shouldn't occur", ex);}
//		}
//		return buf.toString();
//	}

	public static class RequestError extends RuntimeException {
		
		private int status;
		private String message;
		
		public RequestError(int status, String message) {
			this.status = status;
			this.message = message;
		}
		
		public String getMessage() {
			return "Server returned " + this.status + ": " + this.message; 
		}
	}

	private static void raiseError (int status, String message) {
		throw new RequestError(status, message);
	}
	
	private static HttpMethodBase makeHTTPMethod(String method, String url, JSONObject options) {
		HttpMethodBase httpMethod = null;
		if ("POST".equals(method))
			httpMethod = new PostMethod(url);
		else if ("DELETE".equals(method))
			httpMethod = new DeleteMethod(url);
		else if ("PUT".equals(method))
			httpMethod = new PutMethod(url);
		else
			httpMethod = new GetMethod(url);
		if (options != null) {
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			for (Iterator it = options.keys(); it.hasNext();) {
				String key = (String)it.next();
				try {
					Object value = options.get(key);
					if ("body".equals(key)) {
						RequestEntity body = new StringRequestEntity((value == null) ? "null" : value.toString());
						((PostMethod)httpMethod).setRequestEntity(body);
					} else if (value instanceof List) {
						for (Object v : (List)value) {
							NameValuePair nvp= new NameValuePair(key, (v == null) ? "null" : v.toString());
							pairs.add(nvp);
						}
					} else {
						NameValuePair nvp= new NameValuePair(key, (value == null) ? "null" : value.toString());
						pairs.add(nvp);
					}
				} catch (JSONException ex) {throw new RuntimeException("JSON Exception that shouldn't occur", ex);}
			}
			NameValuePair[] morePairs = new NameValuePair[pairs.size()];
			for (int i = 0; i < pairs.size(); i++) morePairs[i] = pairs.get(i);
			httpMethod.setQueryString(morePairs);
		}
		//method.addRequestHeader(USER_AGENT, "foo");
		// Provide custom retry handler if necessary
		httpMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
										new DefaultHttpMethodRetryHandler(3, false));
		return httpMethod;
	}

//	private static HttpMethodBase makePostMethod(String url, JSONObject options) {
//		PostMethod postMethod = new PostMethod(url);
//		if (options == null) return postMethod;
//		//System.out.println("OPTIONS " + options);
//
//		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
//		for (Iterator it = options.keys(); it.hasNext();) {
//			String key = (String)it.next();
//			try {
//				Object value = options.get(key);
//				if ("body".equals(key)) {
//					RequestEntity body = new StringRequestEntity((value == null) ? "null" : value.toString());
//					postMethod.setRequestEntity(body);
//				} else if (value instanceof List) {
//					for (Object v : (List)value) {
//						NameValuePair nvp= new NameValuePair(key, (v == null) ? "null" : v.toString());
//						pairs.add(nvp);
//					}
//				} else {
//					NameValuePair nvp= new NameValuePair(key, (value == null) ? "null" : value.toString());
//					pairs.add(nvp);
//				}
//			} catch (JSONException ex) {throw new RuntimeException("JSON Exception that shouldn't occur", ex);}
//		}
//		NameValuePair[] morePairs = new NameValuePair[pairs.size()];
//		for (int i = 0; i < pairs.size(); i++) morePairs[i] = pairs.get(i);
//		postMethod.setQueryString(morePairs);
//		for (NameValuePair p : morePairs) System.out.println("PPPPPAIR " + p);
//
////		for (Iterator it = options.keys(); it.hasNext();) {
////			String key = (String)it.next();
////			try {
////				Object value = options.get(key);
////				if ("body".equals(key)) {
////					RequestEntity body = new StringRequestEntity((value == null) ? "null" : value.toString());
////					postMethod.setRequestEntity(body);
////				} else if (value instanceof List) {
////					for (Object v : (List)value)  {
////						//System.out.println("   KEY " + key + ":'" + v.toString() + "'");
////						postMethod.addParameter(key, (v == null) ? "null" : v.toString());
////					}
////				} else {
////					//System.out.println("   KEY " + key + ":'" + value.toString() + "'");
////					postMethod.addParameter(key, value.toString());
////				}
////			} catch (JSONException ex) {throw new RuntimeException("JSON Exception that shouldn't occur", ex);}
////		}
//		
//		return postMethod;
//	}
	
	public static boolean TRACE_IT = true;

	/**
	 * Execute an HTTP request, and return the status and response in an array.
	 */
	private static Object[] makeRequest(String method, String url, JSONObject options, 
			String accept, String contentType, Object callback, Object errCallback) {
		//String body = urlenc(options);
		if ((accept == null) || "".equals(accept)) accept = "*/*";
		
		// TEMPORARY:
		//if (method.equals("GET") && url.equals("http://localhost:8080/catalogs/ag/repositories/agraph_test4/statements")) method = "POST";
		// END TEMPORA
		//System.out.println("METHOD " + method + " URL " + url + " OPTIONS " + options);
		HttpMethodBase httpMethod =  makeHTTPMethod(method, url, options);
		httpMethod.addRequestHeader("accept", accept);
		httpMethod.addRequestHeader("connection", "Keep-Alive");
		if ("POST".equals(method) && (contentType != null))
			httpMethod.addRequestHeader("Content-Type", contentType);
		HttpClient client = new HttpClient();
		try {
			if (TRACE_IT) {
				System.out.println("SEND REQUEST " + httpMethod.getName() + " " + httpMethod.getURI());
			}
			int statusCode = client.executeMethod(httpMethod);
            InputStream responseStream = httpMethod.getResponseBodyAsStream();	
            // EXPERIMENT:
            if (responseStream == null) {
            	System.out.println("   RESPONSE STREAM: " + responseStream);
            	return new Object[]{new Integer(statusCode), URLDecoder.decode("","UTF-8")};
            }
            InputStreamReader bufferedReader = new InputStreamReader(responseStream);
            // currently, we are not streaming our responses.  Instead, we read the
            // entire response into one possibly large string.  Given that JSON objects 
            // may be larger than the size of a 'chunk', this is not entirely unreasonable
            // for a first cut.
            StringBuffer buffer = new StringBuffer();
            while (true) {
            	// it appears that the HTTPD server sends chunks of size 4096.
            	// its harmless that we are accepting twice that, but if the 4096 is
            	// cast in stone, we should change this here to match:
	        	char c[] = new char[8192];
	        	int byteCount = bufferedReader.read(c);
	        	if (byteCount <= 0) break;
	        	String chunk = new String(c, 0, byteCount);
	        	buffer.append(chunk);
            }
        	bufferedReader.close();
        	String jsonString = buffer.toString();
        	if (TRACE_IT) {
        		System.out.println("JSON STRING REPONSE " + jsonString.substring(0, Math.min(200, jsonString.length())));
        	}
			//return new Object[]{new Integer(statusCode), URLDecoder.decode(jsonString,"UTF-8")};
			return new Object[]{new Integer(statusCode), jsonString};        	
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			else throw new SoftException("Error executing request " + url + " because: ", e);
		} finally {
			// NOT SURE ABOUT THIS:
			if (false) {
				System.out.println("Releasing http connection.");
				httpMethod.releaseConnection();
			}
		}
	}

	
	/**
	 * Decode 'value', which has already been converted from a JSON string into
	 * a JSON object of some sort.
	 */
	private static Object helpDecodeJSONResponse(Object value) throws JSONException {
		if (value instanceof String) return value;
		else if (value instanceof JSONArray) {
			JSONArray list = (JSONArray)value;
			List values = new ArrayList();
			for (int i = 0; i < list.length(); i++) {
				values.add(helpDecodeJSONResponse(list.get(i)));
			}
			return values;
		} else if (value instanceof JSONObject) {
			JSONObject dict = (JSONObject)value;
			Map map = new HashMap();			
			for (Iterator it = dict.keys(); it.hasNext();) {
				String key = (String)it.next();
				map.put(key, helpDecodeJSONResponse(dict.get(key)));
			}
			return map;
		} else {
			throw new SoftException("Don't know how to decode '" + value + "'");
		}
	}
	
	/**
	 * Convert JSON string into JSON objects, and then into Maps, Lists, and 
	 * strings.  Not sure about ints and booleans.
	 */
	private static Object decodeJSONResponse(String json) {
		if (TRACE_IT) System.out.println("HERE IS THE RESPONSE: \n   " + json.substring(0, Math.min(200, json.length())));
		if (json.length() == 0) return null;
		char c = json.charAt(0);
		try {
			if (c == '{') {
				JSONObject dict = new JSONObject(json);
				return helpDecodeJSONResponse(dict);
			} else if (c == '[') {
				JSONArray list = new JSONArray(json);
				return helpDecodeJSONResponse(list);
			} else {
				// NOT SURE IF THIS IS EVER AN INTEGER OR BOOLEAN:
				return json;
			}
		} catch (JSONException ex) {throw new SoftException(ex);}
	}
		
	public static Object jsonRequest(String method, String url, JSONObject options, 
			String contentType, Object callback) {
		if (callback == null) {
	        Object[] statusAndBody = makeRequest(method, url, options, "application/json", 
	        		contentType, callback, null);
	        int status = (int)(Integer)statusAndBody[0];
	        String body = (String)statusAndBody[1];
	        if (status == 200) {
	        	return decodeJSONResponse(body);
	        } else {
	        	raiseError(status, body);
	        }
		} else {
			// CALLBACKS NOT YET IMPLEMENTED:
//			RowReader rowreader = new RowReader(callback);
//	        makeRequest(method, url, body, "application/json", contentType, 
//	        		callback=rowreader.process, errCallback=raiseErr)
		}
		return null;
    }
		
	public static void nullRequest(String method, String url, JSONObject options, String contentType) {
		// I HAVE NO IDEA WHAT TO PASS HERE IF ITS NOT THIS:
		if (contentType == null) contentType = "application/x-www-form-urlencoded";
		//System.out.println("SENDING CONTENT TYPE: " + contentType);
		String optionsString = options != null ? options.toString() : "";
		//System.out.println("LENGTH " + optionsString.length());
		//System.out.println("   SENDING OPTIONS " + optionsString.substring(0, Math.min(200, optionsString.length())));
		//System.out.println("         OPTIONS TAIL " + optionsString.substring(Math.max(0, optionsString.length() - 100), optionsString.length()));
		Object[] statusAndBody = makeRequest(method, url, options, "application/json", contentType, null, null);
		int status = (int)(Integer)statusAndBody[0];
		String body = (String)statusAndBody[1];
		if (!(status >= 200 && status <= 204)) {
			raiseError(status, body);
		}
	}

}
