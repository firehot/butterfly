package com.butterfly.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.butterfly.R;
import com.butterfly.listeners.IAsyncTaskListener;

public abstract class AbstractAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

	private IAsyncTaskListener taskListener;
	protected Activity context;
	protected static String HTTP_GATEWAY_URL = null;

	public AbstractAsyncTask(IAsyncTaskListener taskListener, Activity context) {
		this.taskListener = taskListener;
		this.context = context;
		if (HTTP_GATEWAY_URL == null) {
			HTTP_GATEWAY_URL = context.getString(R.string.http_gateway_url);
		}
	}
	
	@Override
	protected void onPreExecute() {
		if (this.taskListener != null) {
			this.taskListener.onPreExecute();
		}
		super.onPreExecute();
	}
	
	@Override
	protected void onPostExecute(Result result) {
		if (this.taskListener != null) {
			this.taskListener.onPostExecute(result);
		}
		super.onPostExecute(result);
	}
	
	@Override
	protected void onProgressUpdate(Progress... values) {
		if (this.taskListener != null) {
			this.taskListener.onProgressUpdate(values);
		}
		super.onProgressUpdate(values);
	}
}
