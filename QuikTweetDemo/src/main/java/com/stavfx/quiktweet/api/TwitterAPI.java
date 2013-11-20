package com.stavfx.quiktweet.api;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.stavfx.quiktweet.R;
import com.stavfx.quiktweet.app.SearchResult;
import com.stavfx.quiktweet.util.Base64;
import com.stavfx.quiktweet.util.HttpConnector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Calls are Async
 * Created by Stav on 11/20/13.
 */
public class TwitterAPI {
	private static final String BEARER_TOKEN_PREF = "com.stavfx.quiktwit.PREFS.BearerTokenForTwitter";
	private static final String BASE_API_URL = "https://api.twitter.com/";
	private static final String AUTH_URL = BASE_API_URL + "oauth2/token";
	private static final String SEARCH_URL = BASE_API_URL + "1.1/search/tweets.json";

	public static void Auth(final Context context, final ApiCallListener listener) {
		String key = context.getString(R.string.tweeter_consumer_key);
		String secret = context.getString(R.string.tweeter_consumer_secret);
		byte[] bytes = (key + ":" + secret).getBytes();
		//The call wil fail if we don't remove the line breaks
		String encodedData = Base64.encode(bytes).replace("\r", "").replace("\n", "");
		byte[] postBody = "grant_type=client_credentials".getBytes();

		HttpConnector.newRequest(AUTH_URL, context)
				.setRequestProgressListener(new HttpConnector.RequestProgressListener() {
					@Override
					public void requestFinished(HttpConnector.Response response) {
						if (response.isSuccess()) {
							try {
								JSONObject json = new JSONObject(response.getResponseString());
								if (json.has("access_token")) {
									PreferenceManager
											.getDefaultSharedPreferences(context)
											.edit()
											.putString(BEARER_TOKEN_PREF, json.getString("access_token"))
											.commit();
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
							if (listener != null)
								listener.apiCallFinished(ApiCallListener.AUTH, null, null);
						}
					}
				})
				.addHeader("Authorization", "Basic " + encodedData)
				.setContentType("application/x-www-form-urlencoded;charset=UTF-8")
				.postAsync(postBody);
	}

	public static void Auth(final Context context) {
		Auth(context, null);
	}

	public static boolean isAuthTokenStored(Context context) {
		return !TextUtils.isEmpty(getBearerTokenOrNull(context));
	}

	private static String getBearerTokenOrNull(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(BEARER_TOKEN_PREF, null);
	}

	public static void Search(final String keyword, final Context context, final Object state, final ApiCallListener listener) {
		String token = getBearerTokenOrNull(context);
		if (TextUtils.isEmpty(token)) {
			//Auth and retry
			Auth(context, new ApiCallListener() {
				@Override
				public void apiCallFinished(int method, Object state2, Object result) {
					Search(keyword, context, state, listener);
				}
			});
			return;
		}
		try {
			String query = URLEncoder.encode(keyword, "UTF-8");
			String url = SEARCH_URL + "?q=" + query;
			HttpConnector.newRequest(url, context)
					.addHeader("Authorization", "Bearer " + token)
					.setRequestProgressListener(new HttpConnector.RequestProgressListener() {
						@Override
						public void requestFinished(HttpConnector.Response response) {
							if (response.isSuccess()) {
								final SearchResult result = TwitterResponseHandler.handleSearchResponse(response.getResponseString());
								if (listener != null)
									listener.apiCallFinished(ApiCallListener.SEARCH, state, result);
							}
						}
					})
					.getAsync();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static void LoadMoreResults(final String next_results, final Context context, final ApiCallListener listener) {
		String token = getBearerTokenOrNull(context);
		if (TextUtils.isEmpty(token)) {
			//Auth and retry
			Auth(context, new ApiCallListener() {
				@Override
				public void apiCallFinished(int method, Object state2, Object result) {
					LoadMoreResults(next_results, context, listener);
				}
			});
			return;
		}

		String url = SEARCH_URL + next_results;
		HttpConnector.newRequest(url, context)
				.addHeader("Authorization", "Bearer " + token)
				.setRequestProgressListener(new HttpConnector.RequestProgressListener() {
					@Override
					public void requestFinished(HttpConnector.Response response) {
						if (response.isSuccess()) {
							final SearchResult result = TwitterResponseHandler.handleSearchResponse(response.getResponseString());
							if (listener != null)
								listener.apiCallFinished(ApiCallListener.LOAD_MORE, null, result);
						}
					}
				})
				.getAsync();
	}

	public interface ApiCallListener {
		int AUTH = 0;
		int SEARCH = 1;
		int LOAD_MORE = 2;

		void apiCallFinished(int method, Object state, Object result);
	}
}
