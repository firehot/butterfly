package com.butterfly.tasks;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.butterfly.MainActivity;
import com.butterfly.R;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class GetVideoListTask extends AsyncTask<String, Void, String> {

	List<String> videoList;
	Activity activity;
	
	public GetVideoListTask(List<String> videoList,Activity activity)
	{
		this.videoList = videoList;
		this.activity = activity;	
	}
	@Override
	protected void onPreExecute() {
		this.activity.setProgressBarIndeterminateVisibility(true);
		super.onPreExecute();
	}

	@Override
	protected String doInBackground(String... params) {
		String videos = null;
		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {
			amfConnection.connect(params[0]);
			videos = (String) amfConnection.call("getRecordedVideoFileList");
		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		}
		amfConnection.close();

		return videos;
	}

	@Override
	protected void onPostExecute(String videos) {
		videoList.clear();

		if (videos != null) {
			
			JSONArray jsonArray;
			try {
				jsonArray = new JSONArray(videos);
				int length = jsonArray.length();
				if (length > 0) {

					JSONObject jsonObject;

					for (int i = 0; i < length; i++) {
						jsonObject = (JSONObject) jsonArray.get(i);
						videoList.add(jsonObject.getString("streamName"));

					}
				} else {
					Toast.makeText(this.activity.getApplicationContext(),
							"No videos",
							Toast.LENGTH_LONG).show();
				}

//				((MainActivity)this.activity).updateVideoListeners();
				

			} catch (JSONException e) {
				e.printStackTrace();
				Toast.makeText(this.activity.getApplicationContext(),
						this.activity.getString(R.string.connectivityProblem),
						Toast.LENGTH_LONG).show();
			}

		} else {
			Toast.makeText(this.activity.getApplicationContext(),
					this.activity.getString(R.string.connectivityProblem),
					Toast.LENGTH_LONG).show();

		}
		this.activity.setProgressBarIndeterminateVisibility(false);
		super.onPostExecute(videos);
	}
}
