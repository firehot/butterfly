package com.butterfly.tasks;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.butterfly.R;
import com.butterfly.RecordActivity;
import com.butterfly.fragment.StreamListFragment;
import com.butterfly.utils.Utils;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class DeleteStreamTask extends AsyncTask<String, Void, Boolean> {

	private StreamListFragment fragment;
	private String streamUrl;
	
	public DeleteStreamTask(StreamListFragment fragment)
	{
		this.fragment = fragment;
	}
	@Override
	protected Boolean doInBackground(String... params) {
		Boolean result = false;

		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);

		try {

			amfConnection.connect(params[0]);
			result = (Boolean) amfConnection.call("deleteStream", params[1]);
			streamUrl = params[1];

		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {

			e.printStackTrace();
		}
		amfConnection.close();

		return result;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if(result)
		{
			fragment.removeStream(streamUrl);
			Toast.makeText(fragment.getActivity(), "Stream deleted succefully.", Toast.LENGTH_LONG).show();
		}
		else
		{
			Toast.makeText(fragment.getActivity(), "Stream deletion failed :(", Toast.LENGTH_LONG).show();

		}

		super.onPostExecute(result);

	}
}
