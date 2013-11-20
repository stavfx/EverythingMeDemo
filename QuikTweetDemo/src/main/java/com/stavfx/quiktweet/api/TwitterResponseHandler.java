package com.stavfx.quiktweet.api;

import com.stavfx.quiktweet.app.SearchResult;
import com.stavfx.quiktweet.model.TwitterStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Stav on 11/20/13.
 */
public class TwitterResponseHandler {
	public static SearchResult handleSearchResponse(String response) {
		SearchResult result = new SearchResult();
		try {
			JSONObject json = new JSONObject(response);
			if (json.has("search_metadata")) {
				JSONObject meta = json.getJSONObject("search_metadata");
				if (meta.has("next_results"))
					result.next_results = meta.getString("next_results");
			}
			if (json.has("statuses")) {
				final JSONArray rawStatuses = json.getJSONArray("statuses");
				TwitterStatus[] statuses = new TwitterStatus[rawStatuses.length()];
				for (int i = 0; i < statuses.length; i++) {
					JSONObject status = rawStatuses.getJSONObject(i);
					long id = status.getLong("id");
					String text = status.getString("text");
					if (status.getBoolean("truncated"))
						text += "...";
					String link = getLinkForStatus(status);
					statuses[i] = new TwitterStatus(id, text, link);
				}
				result.statuses = statuses;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}

	private static String getLinkForStatus(JSONObject status) {
		String link = "http://twitter.com/";
		try {
			link += status.getJSONObject("user").getString("screen_name");
			link += "/status/";
			link += status.getString("id_str");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return link;
	}
}
