package com.stavfx.quiktweet.util;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

/**
 * @author Kevin Kowalewski
 */
public abstract class ThreadedAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
	/**
	 * Call this instead of execute.
	 *
	 * @param params
	 * @return
	 */
	public AsyncTask<Params, Progress, Result> executeStart(Params... params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return executePostHoneycomb(params);
		} else {
			return super.execute(params);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private AsyncTask<Params, Progress, Result> executePostHoneycomb(Params... params) {
		return super.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
	}
}