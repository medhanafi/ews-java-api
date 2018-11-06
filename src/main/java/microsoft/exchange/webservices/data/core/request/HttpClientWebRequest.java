/*
 * The MIT License
 * Copyright (c) 2012 Microsoft Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package microsoft.exchange.webservices.data.core.request;

import microsoft.exchange.webservices.data.core.WebProxy;
import microsoft.exchange.webservices.data.core.exception.http.EWSHttpException;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * HttpClientWebRequest is used for making request to the server through NTLM
 * Authentication by using Apache HttpClient 3.1 and JCIFS Library.
 */
public class HttpClientWebRequest extends HttpWebRequest {

	/**
	 * The Http Method.
	 */
	private HttpGet httpGet = null;
	private CloseableHttpResponse response = null;
	private final CloseableHttpClient httpClient;
	private final HttpClientContext httpContext;

	/**
	 * Instantiates a new http native web request.
	 */
	public HttpClientWebRequest(CloseableHttpClient httpClient, HttpClientContext httpContext) {
		this.httpContext = httpContext;
		this.httpClient = httpClient;
	}

	/**
	 * Releases the connection by Closing.
	 */
	@Override
	public void close() throws IOException {
		// First check if we can close the response, by consuming the complete
		// response
		// This releases the connection but keeps it alive for future request
		// If that is not possible, we simply cleanup the whole connection
		if (response != null && response.getEntity() != null) {
			EntityUtils.consume(response.getEntity());
		} else if (httpGet != null) {
			httpGet.releaseConnection();
		}

		// We set httpPost to null to prevent the connection from being closed
		// again by an accidental
		// second call to close()
		// The response is kept, in case something in the library still wants to
		// read something from it,
		// like response code or headers
		httpGet = null;
	}

	/**
	 * Prepares the request by setting appropriate headers, authentication,
	 * timeouts, etc.
	 */
	@Override
	public void prepareConnection() {
		httpGet = this.httpGetPrepareHead("https://webmail.emea.netgrs.com/EWS/Services.wsdl");

		// Build request configuration.
		// Disable Kerberos in the preferred auth schemes - EWS should usually
		// allow NTLM or Basic auth
		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom().setAuthenticationEnabled(true)
				.setConnectionRequestTimeout(getTimeout()).setConnectTimeout(getTimeout())
				.setRedirectsEnabled(isAllowAutoRedirect()).setSocketTimeout(getTimeout())
				.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM))
				.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC));

		// CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

		// Add proxy credential if necessary.
//		WebProxy proxy = getProxy();
//		if (proxy != null) {
//			HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort());
//			requestConfigBuilder.setProxy(proxyHost);
//
//			if (proxy.hasCredentials()) {
//				NTCredentials proxyCredentials = new NTCredentials(proxy.getCredentials().getUsername(),
//						proxy.getCredentials().getPassword(), "", proxy.getCredentials().getDomain());
//
//				credentialsProvider.setCredentials(new AuthScope(proxyHost), proxyCredentials);
//			}
//		}

		// Add web service credential if necessary.
//		if (isAllowAuthentication() && getUsername() != null) {
//			NTCredentials webServiceCredentials = new NTCredentials(getUsername(), getPassword(), "", getDomain());
//			credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY), webServiceCredentials);
//		}

		// httpContext.setCredentialsProvider(credentialsProvider);

		// httpContext.setCredentialsProvider(getSwiziCredentialProvider());

		httpGet.setConfig(requestConfigBuilder.build());
	}

//	private RequestConfig getSwiziRequestConfig() {
//		// HttpHost proxy = new HttpHost("sl-ams-01-guido.statica.io", 9293);
//		return RequestConfig.custom()
//				// .setProxy(proxy)
//				.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM))
//				.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).build();
//	}

//	public CredentialsProvider getSwiziProxyCredentialProvider() {
//		CredentialsProvider credsProvider = new BasicCredentialsProvider();
//        credsProvider.setCredentials(new AuthScope("sl-ams-01-guido.statica.io", 9293, null, AuthSchemes.BASIC),
//                new UsernamePasswordCredentials("statica4488", "6a7d0a87b22fc5c0"));
//		return credsProvider;
//	}

//	public CredentialsProvider getSwiziHotCredentialProvider(CredentialsProvider credsProvider) {
//		NTCredentials proxyCredentials = new NTCredentials("indoormapping@servier.com", "Password456!", null, null);
//		credsProvider.setCredentials(new AuthScope("webmail.emea.netgrs.com", 443, null, AuthSchemes.NTLM),
//				proxyCredentials);
//		return credsProvider;
//	}
//
//	public CredentialsProvider getSwiziCredentialProvider() {
//		return this.getSwiziHotCredentialProvider(getSwiziProxyCredentialProvider());
//	}

	/**
	 * Gets the input stream.
	 *
	 * @return the input stream
	 * @throws EWSHttpException the EWS http exception
	 */
	@Override
	public InputStream getInputStream() throws EWSHttpException, IOException {
		throwIfResponseIsNull();
		String stringContent = IOUtils.toString(response.getEntity().getContent());
		InputStream inputStream =new ByteArrayInputStream(stringContent.getBytes());	
		return inputStream;
	}

	/**
	 * Gets the error stream.
	 *
	 * @return the error stream
	 * @throws EWSHttpException the EWS http exception
	 */
	@Override
	public InputStream getErrorStream() throws EWSHttpException {
		throwIfResponseIsNull();
		BufferedInputStream bufferedInputStream = null;
		try {
			bufferedInputStream = new BufferedInputStream(response.getEntity().getContent());
		} catch (Exception e) {
			throw new EWSHttpException("Connection Error " + e);
		}
		return bufferedInputStream;
	}

	/**
	 * Gets the output stream.
	 *
	 * @return the output stream
	 * @throws EWSHttpException the EWS http exception
	 */
	@Override
	public OutputStream getOutputStream() throws EWSHttpException {
		OutputStream os = null;
		throwIfRequestIsNull();
		os = new ByteArrayOutputStream();

		// httpPost.setEntity(new ByteArrayOSRequestEntity(os));
		return os;
	}

	/**
	 * Gets the response headers.
	 *
	 * @return the response headers
	 * @throws EWSHttpException the EWS http exception
	 */
	@Override
	public Map<String, String> getResponseHeaders() throws EWSHttpException {
		throwIfResponseIsNull();
		Map<String, String> map = new HashMap<String, String>();

		Header[] hM = response.getAllHeaders();
		for (Header header : hM) {
			// RFC2109: Servers may return multiple Set-Cookie headers
			// Need to append the cookies before they are added to the map
			if (header.getName().equals("Set-Cookie")) {
				String cookieValue = "";
				if (map.containsKey("Set-Cookie")) {
					cookieValue += map.get("Set-Cookie");
					cookieValue += ",";
				}
				cookieValue += header.getValue();
				map.put("Set-Cookie", cookieValue);
			} else {
				map.put(header.getName(), header.getValue());
			}
		}

		return map;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see microsoft.exchange.webservices.HttpWebRequest#getResponseHeaderField(
	 * java.lang.String)
	 */
	@Override
	public String getResponseHeaderField(String headerName) throws EWSHttpException {
		throwIfResponseIsNull();
		Header hM = response.getFirstHeader(headerName);
		return hM != null ? hM.getValue() : null;
	}

	/**
	 * Gets the content encoding.
	 *
	 * @return the content encoding
	 * @throws EWSHttpException the EWS http exception
	 */
	@Override
	public String getContentEncoding() throws EWSHttpException {
		throwIfResponseIsNull();
		return response.getFirstHeader("content-encoding") != null
				? response.getFirstHeader("content-encoding").getValue()
				: null;
	}

	/**
	 * Gets the response content type.
	 *
	 * @return the response content type
	 * @throws EWSHttpException the EWS http exception
	 */
	@Override
	public String getResponseContentType() throws EWSHttpException {
		throwIfResponseIsNull();
		return response.getFirstHeader("Content-type") != null ? response.getFirstHeader("Content-type").getValue()
				: null;
	}

	/**
	 * Executes Request by sending request xml data to server.
	 *
	 * @throws EWSHttpException the EWS http exception
	 * @throws                  java.io.IOException the IO Exception
	 */
	@Override
	public int executeRequest() throws EWSHttpException, IOException {
		throwIfRequestIsNull();
		System.err.println(response + " " + httpGet + " " + httpContext + " " + httpClient);
		response = httpClient.execute(httpGet);
		return response.getStatusLine().getStatusCode(); // ?? don't know what
															// is wanted in return
	}

//	public CloseableHttpResponse execute() throws EWSHttpException, IOException {
//
//		SSLContextBuilder builder = new SSLContextBuilder();
//		try {
//			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
//
//			SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(builder.build(),
//					NoopHostnameVerifier.INSTANCE);
//			Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
//					.register("http", new PlainConnectionSocketFactory()).register("https", sslConnectionSocketFactory)
//					.build();
//
//			PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
//			cm.setMaxTotal(100);
//
//			CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(cm)
//					.setDefaultCredentialsProvider(this.getSwiziCredentialProvider()).build();
//			try {
//				HttpGet httpget = this.httpGetPrepareHead("https://webmail.emea.netgrs.com/EWS/Services.wsdl");
//				httpGet = httpget;
//				System.out.println("Executing request " + httpget.getRequestLine());
//				return httpclient.execute(httpget);
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				httpclient.close();
//			}
//		} catch (Exception e1) {
//			e1.printStackTrace();
//		}
//		return null;
//	}

	private HttpGet httpGetPrepareHead(String url) {
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Content-type", getContentType());
		httpGet.addHeader("User-Agent", getUserAgent());
		httpGet.addHeader("Accept", getAccept());
		httpGet.addHeader("Keep-Alive", "300");
		httpGet.addHeader("Connection", "Keep-Alive");

		if (isAcceptGzipEncoding()) {
			httpGet.addHeader("Accept-Encoding", "gzip,deflate");
		}

		if (getHeaders() != null) {
			for (Map.Entry<String, String> httpHeader : getHeaders().entrySet()) {
				httpGet.addHeader(httpHeader.getKey(), httpHeader.getValue());
			}
		}
		return httpGet;
	}

	/**
	 * Gets the response code.
	 *
	 * @return the response code
	 * @throws EWSHttpException the EWS http exception
	 */
	@Override
	public int getResponseCode() throws EWSHttpException {
		throwIfResponseIsNull();
		return response.getStatusLine().getStatusCode();
	}

	/**
	 * Gets the response message.
	 *
	 * @return the response message
	 * @throws EWSHttpException the EWS http exception
	 */
	public String getResponseText() throws EWSHttpException {
		throwIfResponseIsNull();
		return response.getStatusLine().getReasonPhrase();
	}

	/**
	 * Throw if conn is null.
	 *
	 * @throws EWSHttpException the EWS http exception
	 */
	private void throwIfRequestIsNull() throws EWSHttpException {
		if (null == httpGet) {
			throw new EWSHttpException("Connection not established");
		}
	}

	private void throwIfResponseIsNull() throws EWSHttpException {
		if (null == response) {
			throw new EWSHttpException("Connection not established");
		}
	}

	/**
	 * Gets the request property.
	 *
	 * @return the request property
	 * @throws EWSHttpException the EWS http exception
	 */
	public Map<String, String> getRequestProperty() throws EWSHttpException {
		throwIfRequestIsNull();
		Map<String, String> map = new HashMap<String, String>();

		Header[] hM = httpGet.getAllHeaders();
		for (Header header : hM) {
			map.put(header.getName(), header.getValue());
		}
		return map;
	}
}
