package com.stavfx.quiktweet.util;

import android.content.Context;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;

/**
 * Use this class for ANY Http communication.
 * Created by Stav on 11/14/13.
 */
public class HttpConnector {
	public static final int DEFAULT_CONN_TIMEOUT = 15 * 1000;// seconds * 1000...
	public static final int DEFAULT_MAX_RETRY = 1;
	public static int GLOBAL_CONNECTION_TIMEOUT = DEFAULT_CONN_TIMEOUT;
	public static int GLOBAL_MAX_RETRY = DEFAULT_MAX_RETRY;
	private String mAddress;
	private int mTimeout;
	private boolean isFollowingRedirects;
	private HashMap<String, String> mHeaders;
	private boolean isUsingGzip;
	private RequestProgressListener mProgressListener;
	private Context mContext;
	private String mCookies;
	private Object mStateObject;
	private int mMaxRetry = DEFAULT_MAX_RETRY;
	private boolean mEncodeUrl;

	private HttpConnector(String address, Context context) {
		this.mContext = context;
		this.mAddress = address;
		this.mTimeout = GLOBAL_CONNECTION_TIMEOUT;
		this.isFollowingRedirects = true;
		this.mHeaders = new HashMap<String, String>();
		this.isUsingGzip = false;
		this.mMaxRetry = GLOBAL_MAX_RETRY;
		this.mEncodeUrl = false;
	}

	public static HttpConnector newRequest(String address, Context context) {
		return new HttpConnector(address, context.getApplicationContext());
	}

	public static void setGlobalTimeout(int timeout) {
		GLOBAL_CONNECTION_TIMEOUT = timeout;
	}

	public static void setGLOBAL_MAX_RETRY(int maxRetry) {
		GLOBAL_MAX_RETRY = maxRetry;
	}

	/**
	 * @param postData may be <code>null</code>
	 * @return
	 */
	public Response post(byte[] postData) {
		return execute(true, postData);
	}

	/**
	 * Don't forget to set a #RequestProgressListener if you want to know when the request has finished and get the {@link HttpConnector.Response}
	 *
	 * @param postData may be <code>null</code>
	 */
	public void postAsync(final byte[] postData) {
		executeAsync(true, postData);
	}

	public Response get() {
		return execute(false, null);
	}

	/**
	 * Don't forget to set a #RequestProgressListener if you want to know when the request has finished and get the {@link HttpConnector.Response}
	 */
	public void getAsync() {
		executeAsync(false, null);
	}

	private Response execute(boolean isPost, byte[] postData) {
		if (mEncodeUrl)
			try {
				mAddress = URLEncoder.encode(mAddress, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

		Response response = new Response(mContext, mStateObject);
		int responseCode = -1;
		try {
			// send data
			URL url = new URL(mAddress);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(mTimeout);
			conn.setReadTimeout(mTimeout);
			conn.setInstanceFollowRedirects(isFollowingRedirects);

			//Set Headers
			for (Map.Entry<String, String> header : mHeaders.entrySet())
				conn.setRequestProperty(header.getKey(), header.getValue());

			//Upload content for POST requests
			if (isPost) {
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				if (postData != null) {
					//Will override Manual setting of "Content-Length" to the real size sent
					conn.setRequestProperty("Content-Length", String.valueOf(postData.length));
					conn.getOutputStream().write(postData);
				}
			} else
				conn.setRequestMethod("GET");

			// get the response
			InputStream is;
			if (isUsingGzip)
				is = new BufferedInputStream(new GZIPInputStream(conn.getInputStream()), 512);
			else
				is = new BufferedInputStream(conn.getInputStream(), 512);

			responseCode = conn.getResponseCode();

			ByteArrayOutputStream buff = new ByteArrayOutputStream();
			byte[] chunk = new byte[256];
			int contentLength = conn.getContentLength();
			int totalRead = 0;
			int currentRead;
			while ((currentRead = is.read(chunk)) != -1) {
				totalRead += currentRead;
				if (mProgressListener != null)
					mProgressListener.downloadProgress(totalRead, contentLength, mStateObject);
				buff.write(chunk, 0, currentRead);
			}

			response.mResponseBytes = buff.toByteArray();
			response.mResponseCode = responseCode;
			response.mHeaders = conn.getHeaderFields();
			response.isSuccess = true;
		} catch (Exception e) {
			if (shouldRetry(e))
				return execute(isPost, postData);

			//            Logger.getLogger(Constants.LOGGER).logp(Level.WARNING, Util.class.getSimpleName(), String.format("openUrl(%s)", address), e.toString());
			//            Util.logStackTrace(e);
			//			e.printStackTrace();
			//			mResponse.append(e.getMessage());
			response.addError(e.getMessage());
			response.isSuccess = false;
		}

		return response;
	}

	private boolean shouldRetry(Exception e) {
		if (mMaxRetry-- < 1)
			return false;

		boolean io = e instanceof IOException;
		boolean timeout = e instanceof TimeoutException;
		return io || timeout;
	}

	private void executeAsync(final boolean isPost, final byte[] postData) {
		new ThreadedAsyncTask<Void, Void, Response>() {
			@Override
			protected Response doInBackground(Void... params) {
				if (isPost)
					return HttpConnector.this.post(postData);
				else
					return HttpConnector.this.get();
			}

			@Override
			protected void onPostExecute(Response response) {
				if (mProgressListener != null)
					mProgressListener.requestFinished(response);
			}
		}.executeStart();
	}

	public HttpConnector setEncodeUrl(boolean shouldEncode) {
		this.mEncodeUrl = shouldEncode;
		return this;
	}

	public HttpConnector setMaxRetry(int maxRetry) {
		this.mMaxRetry = maxRetry;
		return this;
	}

	public HttpConnector setStateObject(Object state) {
		this.mStateObject = state;
		return this;
	}

	public HttpConnector setRequestProgressListener(RequestProgressListener listener) {
		this.mProgressListener = listener;
		return this;
	}

	public HttpConnector setFollowRedirects(boolean followRedirects) {
		this.isFollowingRedirects = followRedirects;
		return this;
	}

	public HttpConnector setUseGzip(boolean useGzip) {
		this.isUsingGzip = useGzip;
		return this;
	}

	public HttpConnector addHeader(String name, String value) {
		if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value))
			this.mHeaders.put(name, value);
		return this;
	}

	public HttpConnector setUserAgent(String userAgent) {
		return addHeader("User-Agent", userAgent);
	}

	public HttpConnector setCookies(String cookies) {
		this.mCookies = cookies;
		return addHeader("cookie", cookies);
	}

	public HttpConnector setTimeout(int timeout) {
		this.mTimeout = timeout;
		return this;
	}

	public HttpConnector setContentType(String contentType) {
		return addHeader("content-type", contentType);
	}

	public static class Response {
		private boolean isSuccess;
		private int mResponseCode;
		private String mErrorMessage;
		private byte[] mResponseBytes;
		private Map<String, List<String>> mHeaders;
		private Object mStateObject;

		private Response() {
		}

		private Response(Context context, Object state) {
			this.mStateObject = state;
		}

		public static Response dummyResponse(boolean isSuccess) {
			Response dummy = new Response();
			dummy.isSuccess = isSuccess;
			return dummy;
		}

		public static Response dummyResponse() {
			return dummyResponse(false);
		}

		public boolean isSuccess() {
			return isSuccess;
		}

		public int getResponseCode() {
			return mResponseCode;
		}

		public String getErrorMessage() {
			return mErrorMessage;
		}

		public byte[] getResponseBytes() {
			return mResponseBytes;
		}

		public String getResponseString() {
			return getResponseString("UTF-8");
		}

		public void addError(String error) {
			this.mErrorMessage += error + "; ";
		}

		public String getResponseString(String encoding) {
			if (mResponseBytes == null)
				return "";
			try {
				return new String(mResponseBytes, encoding);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return new String(mResponseBytes);
		}

		public Object getState() {
			return mStateObject;
		}

		public Map<String, List<String>> getHeaders() {
			return mHeaders;
		}

		public String getHeader(String key) {
			List<String> values = mHeaders.get(key);
			if (values == null)
				return null;
			String value = values.toString();
			return value.substring(1, value.length() - 2);
		}
	}

	public static abstract class RequestProgressListener {
		public void downloadProgress(long currentProgress, long totalSize, Object state) {
		}

		/**
		 * Upload not implemented yet
		 */
		public void uploadProgress(long currentProgress, long totalSize, Object state) {
		}

		/**
		 * Will only be called from Async requests ({@link #postAsync(byte[])} or {@link #getAsync()})
		 *
		 * @param response The response object as return by Synchronous calls ({@link #post(byte[])}  and {@link #get()}}
		 */
		public void requestFinished(Response response) {
		}
	}
}