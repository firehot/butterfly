package com.butterfly.asynctasks;

import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;

import com.butterfly.MainActivity;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.listeners.IAsyncTaskListener;
import com.butterfly.utils.Utils;

public class GetStreamListTask extends	AbstractAsyncTask<String, Void, List<Stream>> {

	public GetStreamListTask(IAsyncTaskListener taskListener, Activity context) {
		super(taskListener, context);
	}

	@Override
	protected List<Stream> doInBackground(String... params) {
		String streams = Utils.getLiveStreams(this.context, params);
		if (streams != null) {
			try {
				return Utils.parseStreams((String)streams,	this.context);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	



}
