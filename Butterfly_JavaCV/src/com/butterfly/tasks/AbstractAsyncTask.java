package com.butterfly.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.butterfly.listeners.IAsyncTaskListener;

public abstract class AbstractAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

	private IAsyncTaskListener taskListener;
	protected Activity context;

	public AbstractAsyncTask(IAsyncTaskListener taskListener, Activity context) {
		this.taskListener = taskListener;
		this.context = context;
	}
	
	@Override
	protected void onPreExecute() {
		this.taskListener.onPreExecute();
		super.onPreExecute();
	}
	
	@Override
	protected void onPostExecute(Result result) {
		this.taskListener.onPostExecute(result);
		super.onPostExecute(result);
	}
	
	@Override
	protected void onProgressUpdate(Progress... values) {
		this.taskListener.onProgressUpdate(values);
		super.onProgressUpdate(values);
	}
}
