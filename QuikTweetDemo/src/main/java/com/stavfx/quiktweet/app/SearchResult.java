package com.stavfx.quiktweet.app;

import com.stavfx.quiktweet.model.TwitterStatus;

/**
 * Created by Stav on 11/20/13.
 */
public class SearchResult {
	public TwitterStatus[] statuses;
	public String next_results;

	public SearchResult() {
		statuses = new TwitterStatus[0];
	}
}
