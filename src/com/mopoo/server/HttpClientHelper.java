package com.mopoo.server;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.mopoo.MyApplication;

public class HttpClientHelper {

	private static HttpClientHelper helper = null;
	public static final HttpClientHelper getInstance(MyApplication myApplication) {
		if(helper == null){
			helper = new HttpClientHelper(myApplication);
		}
		return helper;
	}
	private MyApplication myApplication;
	private HttpClientHelper(MyApplication myApplication){
		this.myApplication = myApplication;
		this.setHttpClient();
		this.addCookies(this.myApplication.getRememberMeCookie());
	}
	public HttpResponse execute(HttpRequestBase request) throws Exception {
		try{
			return httpClient.execute(request, localContext);
		}catch(Exception e){
			String msg = e.getMessage();
			if(com.mopoo.utils.StringUtils.isNotBlank(msg)&&msg.indexOf(RemoteServer.REAL_MOPOO_IP)>=0){ //清除IP提示
				throw new Exception(msg.replaceAll(java.util.regex.Pattern.quote(RemoteServer.REAL_MOPOO_IP), ""));
			}else{
				throw e;
			}
		}
	}

	CookieStore cookieStore = new BasicCookieStore();
	HttpContext localContext = new BasicHttpContext();
	DefaultHttpClient httpClient = null;

	public String getCurrentCookie() {
		String cookie = "";
		List<Cookie> cookies = cookieStore.getCookies();
		int size = cookies.size();
		for (int i = 0; i < size; i++) {
			cookie += cookies.get(i).getName() + "="
					+ cookies.get(i).getValue();
			if (i != size - 1) {
				cookie += ";";
			}
		}
		return cookie;
	}

	public void addCookies(String value) {
		if (value == null){
			return;
		}
		String[] arr = value.split(";");
		for (String curr : arr) {
			int posi = curr.indexOf("=");
			if(posi>=0){
				String key = curr.substring(0,posi);
				Cookie oldCookie = getFromCookieStore(key);
				if(oldCookie==null){
					String cookieValue = curr.substring(posi+1);
					Cookie cookie = new BasicClientCookie(key, cookieValue);
					cookieStore.addCookie(cookie);
				}
			}

		}
	}
	public Cookie getFromCookieStore(String key){
		for(Cookie cookie : cookieStore.getCookies()){
			if(cookie.getName().equals(key)){
				return cookie;
			}
		}
		return null;
	}
	private class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public MySSLSocketFactory(KeyStore truststore)
				throws NoSuchAlgorithmException, KeyManagementException,
				KeyStoreException, UnrecoverableKeyException {
			super(truststore);
			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port,
				boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host,
					port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

	private void setHttpClient() {
		try {
			localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);
			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			HttpParams params = new BasicHttpParams();
			ConnPerRoute connPerRoute = new ConnPerRouteBean(50);
			ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
			ConnManagerParams.setMaxTotalConnections(params, 200);

			HttpConnectionParams.setConnectionTimeout(params, 1000 * 60);
			HttpConnectionParams.setSoTimeout(params, 1000 * 60);
			HttpConnectionParams.setSocketBufferSize(params, 8192);
//			params.setBooleanParameter(HttpProtocolParams.USE_EXPECT_CONTINUE,
//					false);
//		params.setBooleanParameter(
//					HttpConnectionParams.STALE_CONNECTION_CHECK, false);	

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));
			ThreadSafeClientConnManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			httpClient = new DefaultHttpClient(ccm, params);		
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
}
