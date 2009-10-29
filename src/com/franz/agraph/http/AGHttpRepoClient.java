package com.franz.agraph.http;

import static com.franz.agraph.http.AGProtocol.getSessionCloseURL;
import static com.franz.agraph.http.AGProtocol.getSessionURL;
import static org.openrdf.http.protocol.Protocol.ACCEPT_PARAM_NAME;
import info.aduna.io.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.openrdf.OpenRDFUtil;
import org.openrdf.http.protocol.Protocol;
import org.openrdf.http.protocol.UnauthorizedException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.franz.agraph.repository.AGRepositoryConnection;

/**
 * TODO: rename this class.
 */
public class AGHttpRepoClient {

	private final AGRepositoryConnection repoconnection;
	private final String sessionRoot;
	final Logger logger = LoggerFactory.getLogger(this.getClass());

	// TODO: choose proper defaults
	private long lifetimeInSeconds = 3600;
	private TupleQueryResultFormat preferredTQRFormat = TupleQueryResultFormat.SPARQL;
	private BooleanQueryResultFormat preferredBQRFormat = BooleanQueryResultFormat.TEXT;
	private RDFFormat preferredRDFFormat = RDFFormat.TRIX;

	public AGHttpRepoClient(AGRepositoryConnection repoconnection)
			throws RepositoryException {
		this.repoconnection = repoconnection;
		try {
			/*if (AGCatalog.FEDERATED_CATALOG == repoconnection.getRepository().getCatalog().getCatalogType()) {
				// use the shared session
				sessionRoot = getRepositoryURL();
			} else {
				// use a dedicated session in autoCommit mode
				sessionRoot = openSession(lifetimeInSeconds, true);
			}*/
			// use a dedicated session with appropriate autoCommit mode
			sessionRoot = openSession(lifetimeInSeconds, repoconnection.isAutoCommit());
		} catch (UnauthorizedException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public AGRepositoryConnection getRepositoryConnection() {
		return repoconnection;
	}

	public String getSessionRoot() {
		return sessionRoot;
	}

	public TupleQueryResultFormat getPreferredTQRFormat() {
		return preferredTQRFormat;
	}

	public void setPreferredTQRFormat(TupleQueryResultFormat preferredTQRFormat) {
		this.preferredTQRFormat = preferredTQRFormat;
	}

	public BooleanQueryResultFormat getPreferredBQRFormat() {
		return preferredBQRFormat;
	}

	public void setPreferredBQRFormat(
			BooleanQueryResultFormat preferredBQRFormat) {
		this.preferredBQRFormat = preferredBQRFormat;
	}

	public RDFFormat getPreferredRDFFormat() {
		return preferredRDFFormat;
	}

	public void setPreferredRDFFormat(RDFFormat preferredRDFFormat) {
		this.preferredRDFFormat = preferredRDFFormat;
	}

	public String getServerURL() {
		return getRepositoryConnection().getRepository().getCatalog()
				.getServer().getServerURL();
	}

	public String getRepositoryURL() {
		return getRepositoryConnection().getRepository().getRepositoryURL();
	}

	public AGHTTPClient getHTTPClient() {
		return getRepositoryConnection().getRepository().getHTTPClient();
	}

	private String openSession(long lifetime, boolean autoCommit)
			throws IOException, RepositoryException, UnauthorizedException {
		String url = getSessionURL(getRepositoryURL());
		Header[] headers = new Header[0];
		NameValuePair[] data = {
				new NameValuePair(AGProtocol.LIFETIME_PARAM_NAME, Long
						.toString(lifetime)),
				new NameValuePair(AGProtocol.AUTOCOMMIT_PARAM_NAME, Boolean
						.toString(autoCommit)) };
		AGResponseHandler handler = new AGResponseHandler("");
		try {
			getHTTPClient().post(url, headers, data, null, handler);
		} catch (RDFParseException e) {
			// bug.
			throw new RuntimeException(e);
		}
		if (logger.isDebugEnabled())
			logger.debug("openSession: {}", sessionRoot);
		return handler.getString();
	}

	private void closeSession(String sessionRoot) throws IOException,
			RepositoryException, UnauthorizedException {
		String url = getSessionCloseURL(getSessionURL(sessionRoot));
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		try {
			getHTTPClient().post(url, headers, params, null, null);
			if (logger.isDebugEnabled())
				logger.debug("closeSession: {}", url);
		} catch (RDFParseException e) {
			// bug.
			throw new RuntimeException(e);
		}
	}

	public void getStatements(Resource subj, URI pred, Value obj,
			boolean includeInferred, RDFHandler handler, Resource... contexts)
			throws IOException, RDFHandlerException, RepositoryException,
			UnauthorizedException {
		String uri = Protocol.getStatementsLocation(getSessionRoot());
		Header[] headers = { new Header(ACCEPT_PARAM_NAME,
				getPreferredRDFFormat().getDefaultMIMEType()) };

		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		if (subj != null) {
			params.add(new NameValuePair(Protocol.SUBJECT_PARAM_NAME, Protocol
					.encodeValue(subj)));
		}
		if (pred != null) {
			params.add(new NameValuePair(Protocol.PREDICATE_PARAM_NAME,
					Protocol.encodeValue(pred)));
		}
		if (obj != null) {
			params.add(new NameValuePair(Protocol.OBJECT_PARAM_NAME, Protocol
					.encodeValue(obj)));
		}
		for (String encodedContext : Protocol.encodeContexts(contexts)) {
			params.add(new NameValuePair(Protocol.CONTEXT_PARAM_NAME,
					encodedContext));
		}
		params.add(new NameValuePair(Protocol.INCLUDE_INFERRED_PARAM_NAME,
				Boolean.toString(includeInferred)));

		try {
			getHTTPClient()
					.get(
							uri,
							headers,
							params.toArray(new NameValuePair[params.size()]),
							new AGResponseHandler(getRepositoryConnection()
									.getRepository(), handler,
									getPreferredRDFFormat()));
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}

	public void deleteStatements(Resource subj, URI pred, Value obj,
			Resource... contexts) throws RepositoryException {
		String url = Protocol.getStatementsLocation(getSessionRoot());
		Header[] headers = {};

		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		if (subj != null) {
			params.add(new NameValuePair(Protocol.SUBJECT_PARAM_NAME, Protocol
					.encodeValue(subj)));
		}
		if (pred != null) {
			params.add(new NameValuePair(Protocol.PREDICATE_PARAM_NAME,
					Protocol.encodeValue(pred)));
		}
		if (obj != null) {
			params.add(new NameValuePair(Protocol.OBJECT_PARAM_NAME, Protocol
					.encodeValue(obj)));
		}
		for (String encodedContext : Protocol.encodeContexts(contexts)) {
			params.add(new NameValuePair(Protocol.CONTEXT_PARAM_NAME,
					encodedContext));
		}
		try {
			getHTTPClient().delete(url, headers,
					params.toArray(new NameValuePair[params.size()]));
		} catch (HttpException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		String url = AGProtocol.getAutoCommitLocation(getSessionRoot());
		Header[] headers = {};
		NameValuePair[] params = { new NameValuePair(AGProtocol.ON_PARAM_NAME,
				Boolean.toString(autoCommit)) };
		try {
			getHTTPClient().post(url, headers, params, null, null);
		} catch (HttpException e) {
			throw new RepositoryException(e);
		} catch (RDFParseException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public void commit() throws RepositoryException {
		String url = getSessionRoot() + "/" + AGProtocol.COMMIT;
		Header[] headers = {};
		try {
			getHTTPClient().post(url, headers, new NameValuePair[0],
					(RequestEntity) null, null);
		} catch (HttpException e) {
			throw new RepositoryException(e);
		} catch (RDFParseException e) {
			// bug.
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public void rollback() throws RepositoryException {
		String url = getSessionRoot() + "/" + AGProtocol.ROLLBACK;
		Header[] headers = {};
		try {
			getHTTPClient().post(url, headers, new NameValuePair[0],
					(RequestEntity) null, null);
		} catch (HttpException e) {
			throw new RepositoryException(e);
		} catch (RDFParseException e) {
			// bug.
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public void clearNamespaces() throws RepositoryException {
		String url = Protocol.getNamespacesLocation(getSessionRoot());
		Header[] headers = {};
		try {
			getHTTPClient().delete(url, headers, new NameValuePair[0]);
		} catch (HttpException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public void upload(final Reader contents, String baseURI,
			final RDFFormat dataFormat, boolean overwrite, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException,
			UnauthorizedException {
		final Charset charset = dataFormat.hasCharset() ? dataFormat
				.getCharset() : Charset.forName("UTF-8");

		RequestEntity entity = new RequestEntity() {

			public long getContentLength() {
				return -1; // don't know
			}

			public String getContentType() {
				return dataFormat.getDefaultMIMEType() + "; charset="
						+ charset.name();
			}

			public boolean isRepeatable() {
				return false;
			}

			public void writeRequest(OutputStream out) throws IOException {
				OutputStreamWriter writer = new OutputStreamWriter(out, charset);
				IOUtil.transfer(contents, writer);
				writer.flush();
			}
		};

		upload(entity, baseURI, overwrite, contexts);
	}

	public void upload(InputStream contents, String baseURI,
			RDFFormat dataFormat, boolean overwrite, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException,
			UnauthorizedException {
		// TODO: research the ramifications of using CONTENT_LENGTH_AUTO here.
		// Formerly we did this: "Set Content-Length to -1 as we don't know it and we also don't want
		// to cache" per the original Sesame client, but this yielded "ran out of input" exceptions
		// from the AG server for some users, but not for others.  TODO: understand why and explain.
		RequestEntity entity = new InputStreamRequestEntity(contents, InputStreamRequestEntity.CONTENT_LENGTH_AUTO,
				dataFormat.getDefaultMIMEType());
		upload(entity, baseURI, overwrite, contexts);
	}

	protected void upload(RequestEntity reqEntity, String baseURI,
			boolean overwrite, Resource... contexts) throws IOException,
			RDFParseException, RepositoryException, UnauthorizedException {
		OpenRDFUtil.verifyContextNotNull(contexts);
		String url = Protocol.getStatementsLocation(getSessionRoot());
		Header[] headers = {};
		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		for (String encodedContext : Protocol.encodeContexts(contexts)) {
			params.add(new NameValuePair(Protocol.CONTEXT_PARAM_NAME,
					encodedContext));
		}
		if (baseURI != null && baseURI.trim().length() != 0) {
			String encodedBaseURI = Protocol.encodeValue(new URIImpl(baseURI));
			params.add(new NameValuePair(Protocol.BASEURI_PARAM_NAME,
					encodedBaseURI
					// baseURI // the workaround no longer needed.
					));
		}
		if (overwrite == false) {
			getHTTPClient().post(url, headers,
					params.toArray(new NameValuePair[params.size()]),
					reqEntity, null);
		} else {
			// TODO: overwrite==true
			throw new UnsupportedOperationException();
		}
	}

	public TupleQueryResult getContextIDs() throws IOException,
			RepositoryException, UnauthorizedException {
		try {
			TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
			getContextIDs(builder);
			return builder.getQueryResult();
		} catch (TupleQueryResultHandlerException e) {
			// Found a bug in TupleQueryResultBuilder?
			throw new RuntimeException(e);
		}
	}

	public void getContextIDs(TupleQueryResultHandler handler)
			throws IOException, TupleQueryResultHandlerException,
			RepositoryException, UnauthorizedException {
		String url = Protocol.getContextsLocation(getSessionRoot());
		Header[] headers = { new Header(ACCEPT_PARAM_NAME,
				getPreferredTQRFormat().getDefaultMIMEType()) };
		try {
			getHTTPClient().get(
					url,
					headers,
					new NameValuePair[0],
					new AGResponseHandler(getRepositoryConnection()
							.getRepository(), handler));
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}

	public long size(Resource... contexts) throws IOException,
			RepositoryException, UnauthorizedException {
		String url = Protocol.getSizeLocation(getSessionRoot());
		Header[] headers = {};
		String[] encodedContexts = Protocol.encodeContexts(contexts);
		NameValuePair[] contextParams = new NameValuePair[encodedContexts.length];
		for (int i = 0; i < encodedContexts.length; i++) {
			contextParams[i] = new NameValuePair(Protocol.CONTEXT_PARAM_NAME,
					encodedContexts[i]);
		}
		AGResponseHandler handler = new AGResponseHandler(0L);
		try {
			getHTTPClient().get(url, headers, contextParams, handler);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
		return handler.getLong();
	}

	public TupleQueryResult getNamespaces() throws IOException,
			RepositoryException, UnauthorizedException {
		try {
			TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
			getNamespaces(builder);
			return builder.getQueryResult();
		} catch (TupleQueryResultHandlerException e) {
			// Found a bug in TupleQueryResultBuilder?
			throw new RuntimeException(e);
		}
	}

	public void getNamespaces(TupleQueryResultHandler handler)
			throws IOException, TupleQueryResultHandlerException,
			RepositoryException, UnauthorizedException {
		String url = Protocol.getNamespacesLocation(getSessionRoot());
		Header[] headers = { new Header(ACCEPT_PARAM_NAME,
				getPreferredTQRFormat().getDefaultMIMEType()) };
		try {
			getHTTPClient().get(
					url,
					headers,
					new NameValuePair[0],
					new AGResponseHandler(getRepositoryConnection()
							.getRepository(), handler));
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}

	public String getNamespace(String prefix) throws IOException,
			RepositoryException, UnauthorizedException {
		String url = Protocol.getNamespacePrefixLocation(getSessionRoot(),
				prefix);
		Header[] headers = {};
		AGResponseHandler handler = new AGResponseHandler("");
		try {
			getHTTPClient().get(url, headers, new NameValuePair[0], handler);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
		return handler.getString();
	}

	public void setNamespacePrefix(String prefix, String name)
			throws RepositoryException {
		String url = Protocol.getNamespacePrefixLocation(getSessionRoot(),
				prefix);
		Header[] headers = {};
		NameValuePair[] params = {};
		try {
			getHTTPClient().put(url, headers, params,
					new StringRequestEntity(name, "text/plain", "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
	}

	public void removeNamespacePrefix(String prefix) throws RepositoryException {
		String url = Protocol.getNamespacePrefixLocation(getSessionRoot(),
				prefix);
		Header[] headers = {};
		try {
			getHTTPClient().delete(url, headers, new NameValuePair[0]);
		} catch (HttpException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public void query(AGResponseHandler handler, QueryLanguage ql,
			String query, Dataset dataset, boolean includeInferred,
			Binding... bindings) throws HttpException, RepositoryException,
			RDFParseException, IOException {
		String url = getSessionRoot();
		List<Header> headers = new ArrayList<Header>(5);
		headers.add(new Header("Content-Type", Protocol.FORM_MIME_TYPE
				+ "; charset=utf-8"));
		if (handler.getRequestMIMEType() != null) {
			headers.add(new Header(ACCEPT_PARAM_NAME, handler
					.getRequestMIMEType()));
		}
		List<NameValuePair> queryParams = getQueryMethodParameters(ql, query,
				dataset, includeInferred, bindings);
		getHTTPClient().post(url, headers.toArray(new Header[headers.size()]),
				queryParams.toArray(new NameValuePair[queryParams.size()]),
				null, handler);
	}

	protected List<NameValuePair> getQueryMethodParameters(QueryLanguage ql,
			String query, Dataset dataset, boolean includeInferred,
			Binding... bindings) {
		List<NameValuePair> queryParams = new ArrayList<NameValuePair>(
				bindings.length + 10);

		queryParams.add(new NameValuePair(Protocol.QUERY_LANGUAGE_PARAM_NAME,
				ql.getName()));
		queryParams.add(new NameValuePair(Protocol.QUERY_PARAM_NAME, query));
		queryParams.add(new NameValuePair(Protocol.INCLUDE_INFERRED_PARAM_NAME,
				Boolean.toString(includeInferred)));

		if (dataset != null) {
			for (URI defaultGraphURI : dataset.getDefaultGraphs()) {
				String param=Protocol.NULL_PARAM_VALUE;
				if (defaultGraphURI==null) {
					queryParams.add(new NameValuePair(Protocol.CONTEXT_PARAM_NAME, param));
				} else {
					param = defaultGraphURI.toString();
					queryParams.add(new NameValuePair(Protocol.DEFAULT_GRAPH_PARAM_NAME, param));
				}
			}
			for (URI namedGraphURI : dataset.getNamedGraphs()) {
				queryParams.add(new NameValuePair(
						Protocol.NAMED_GRAPH_PARAM_NAME, namedGraphURI
								.toString()));
			}
		} // TODO: no else clause here assumes AG's default dataset matches Sesame's, confirm this.

		for (int i = 0; i < bindings.length; i++) {
			String paramName = Protocol.BINDING_PREFIX + bindings[i].getName();
			String paramValue = Protocol.encodeValue(bindings[i].getValue());
			queryParams.add(new NameValuePair(paramName, paramValue));
		}

		return queryParams;
	}

	public void close() throws RepositoryException {
		if (sessionRoot != null) {
			try {
				closeSession(sessionRoot);
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}
	}

	public void registerFreetextPredicate(URI predicate)
			throws RepositoryException {
		String url = AGProtocol.getFreetextPredicatesLocation(getSessionRoot());
		String pred_nt = NTriplesUtil.toNTriplesString(predicate);
		Header[] headers = {};
		NameValuePair[] params = { new NameValuePair(
				AGProtocol.FTI_PREDICATE_PARAM_NAME, pred_nt) };
		try {
			getHTTPClient().post(url, headers, params, null, null);
		} catch (HttpException e) {
			throw new RepositoryException(e);
		} catch (RDFParseException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public String[] getFreetextPredicates() throws RepositoryException {
		String url = AGProtocol.getFreetextPredicatesLocation(getSessionRoot());
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		AGResponseHandler handler = new AGResponseHandler("");
		try {
			getHTTPClient().get(url, headers, params, handler);
		} catch (IOException e) {
			throw new RepositoryException(e);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
		return handler.getString().split("\n");
	}

	public void registerPredicateMapping(URI predicate, URI primitiveType)
			throws RepositoryException {
		String url = AGProtocol.getPredicateMappingLocation(getSessionRoot());
		String pred_nt = NTriplesUtil.toNTriplesString(predicate);
		String primtype_nt = NTriplesUtil.toNTriplesString(primitiveType);
		Header[] headers = {};
		NameValuePair[] params = { new NameValuePair(
				AGProtocol.FTI_PREDICATE_PARAM_NAME, pred_nt),
				new NameValuePair(AGProtocol.ENCODED_TYPE_PARAM_NAME,
						primtype_nt) };
		try {
			getHTTPClient().post(url, headers, params, null, null);
		} catch (HttpException e) {
			throw new RepositoryException(e);
		} catch (RDFParseException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public String[] getPredicateMappings() throws RepositoryException {
		String url = AGProtocol.getPredicateMappingLocation(getSessionRoot());
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		AGResponseHandler handler = new AGResponseHandler("");
		try {
			getHTTPClient().get(url, headers, params, handler);
		} catch (IOException e) {
			throw new RepositoryException(e);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
		return handler.getString().split("\n");
	}

	public void registerDatatypeMapping(URI datatype, URI primitiveType)
			throws RepositoryException {
		String url = AGProtocol.getDatatypeMappingLocation(getSessionRoot());
		String datatype_nt = NTriplesUtil.toNTriplesString(datatype);
		String primtype_nt = NTriplesUtil.toNTriplesString(primitiveType);
		Header[] headers = {};
		NameValuePair[] params = {
				new NameValuePair(AGProtocol.TYPE_PARAM_NAME, datatype_nt),
				new NameValuePair(AGProtocol.ENCODED_TYPE_PARAM_NAME,
						primtype_nt) };
		try {
			getHTTPClient().post(url, headers, params, null, null);
		} catch (HttpException e) {
			throw new RepositoryException(e);
		} catch (RDFParseException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public String[] getDatatypeMappings() throws RepositoryException {
		String url = AGProtocol.getPredicateMappingLocation(getSessionRoot());
		Header[] headers = new Header[0];
		NameValuePair[] params = new NameValuePair[0];
		AGResponseHandler handler = new AGResponseHandler("");
		try {
			getHTTPClient().get(url, headers, params, handler);
		} catch (IOException e) {
			throw new RepositoryException(e);
		} catch (AGHttpException e) {
			throw new RepositoryException(e);
		}
		return handler.getString().split("\n");
	}

	public void addRules(String rules) throws RepositoryException {
		try {
			InputStream rulestream = new ByteArrayInputStream(rules.getBytes("UTF-8"));
			addRules(rulestream);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}		
	}
	
	public void addRules(InputStream rulestream) throws RepositoryException {
		String url = AGProtocol.getFunctorLocation(getSessionRoot());
		Header[] headers = {};
		NameValuePair[] params = {};
		try {
			RequestEntity entity = new InputStreamRequestEntity(rulestream, -1, null);
			getHTTPClient().post(url, headers, params, entity, null);
		} catch (HttpException e) {
			throw new RepositoryException(e);
		} catch (RDFParseException e) {
			throw new RepositoryException(e);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}		
	}
		
}
