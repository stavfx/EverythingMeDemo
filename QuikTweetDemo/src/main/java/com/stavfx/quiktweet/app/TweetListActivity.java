package com.stavfx.quiktweet.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.stavfx.quiktweet.R;
import com.stavfx.quiktweet.api.TwitterAPI;
import com.stavfx.quiktweet.model.TwitterStatus;
import com.stavfx.quiktweet.ui.LoadMoreListView;

import java.util.ArrayList;
import java.util.Collections;

public class TweetListActivity extends Activity {

	private static final int MIN_CHAR_SEARCH = 3;
	private LoadMoreListView mList;
	private EditText mSearchBox;
	private TweetAdapter mAdapter;
	private int mLastCallId;
	private String mLastKeyword;
	private String mNextResults;
	private TextWatcher mTextWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {

		}

		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() < MIN_CHAR_SEARCH)
				return;

			setProgressBarIndeterminateVisibility(true);
			mLastKeyword = s.toString();
			TwitterAPI.Search(mLastKeyword, getApplicationContext(), ++mLastCallId, new TwitterAPI.ApiCallListener() {
				@Override
				public void apiCallFinished(int method, Object state, Object result) {
					if (state.equals(mLastCallId))
						setProgressBarIndeterminateVisibility(false);
					switch (method) {
						case SEARCH:
							if (result instanceof SearchResult) {
								SearchResult sResult = (SearchResult) result;
								mAdapter.setItems(sResult.statuses);
								mNextResults = (sResult.next_results);
							}
							break;
					}
				}
			});
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_tweet_list);

		mList = (LoadMoreListView) findViewById(R.id.list);
		mSearchBox = (EditText) findViewById(R.id.searchBox);

		mList.setAdapter(mAdapter = new TweetAdapter());
		mList.setOnLoadMoreListener(mAdapter);
		mList.setOnItemClickListener(mAdapter);

		mSearchBox.addTextChangedListener(mTextWatcher);

		if (!TwitterAPI.isAuthTokenStored(getApplicationContext()))
			TwitterAPI.Auth(getApplicationContext());
	}

	private static class ViewHolder {
		TextView textView;
	}

	private class TweetAdapter extends BaseAdapter implements LoadMoreListView.OnLoadMoreListener, AdapterView.OnItemClickListener {
		//I prefer to make sure there are no duplicates myself (rather than use HashSet) because it simplifies returning items by index
		private ArrayList<TwitterStatus> mItems = new ArrayList<TwitterStatus>();

		public void setItems(TwitterStatus... statuses) {
			mItems.clear();
			Collections.addAll(mItems, statuses);
			notifyDataSetChanged();
		}

		public void addItems(TwitterStatus... statuses) {
			for (TwitterStatus status : statuses)
				if (!mItems.contains(status))
					mItems.add(status);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public TwitterStatus getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			ViewHolder holder;
			if (v == null) {
				v = getLayoutInflater().inflate(R.layout.item_status, parent, false);

				holder = new ViewHolder();
				holder.textView = (TextView) v.findViewById(R.id.text);

				v.setTag(holder);
			} else
				holder = (ViewHolder) v.getTag();


			TwitterStatus status = getItem(position);
			String text = status.getText();
			Spannable span = new SpannableString(text);
			int start = text.toLowerCase().indexOf(mLastKeyword.toLowerCase());
			if (start > -1) {
				int end = start + mLastKeyword.length();
				end = Math.min(end, text.length());
				span.setSpan(new BackgroundColorSpan(Color.YELLOW), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (holder.textView != null)
				holder.textView.setText(span);

			return v;
		}

		@Override
		public void onLoadMore() {
			//get next page
			TwitterAPI.LoadMoreResults(mNextResults, getApplicationContext(), new TwitterAPI.ApiCallListener() {
				@Override
				public void apiCallFinished(int method, Object state, Object result) {
					if (result instanceof SearchResult) {
						SearchResult sResult = (SearchResult) result;
						addItems(sResult.statuses);
						mNextResults = sResult.next_results;
					}
					mList.onLoadMoreComplete();
				}
			});
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			String url = getItem(position).getLink();
			Intent browserIntent = new Intent(Intent.ACTION_VIEW);
			browserIntent.setData(Uri.parse(url));
			startActivity(browserIntent);
		}
	}
}
