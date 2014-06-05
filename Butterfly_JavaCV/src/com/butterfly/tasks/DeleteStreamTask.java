package com.butterfly.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.butterfly.R;
import com.butterfly.RecordActivity;
import com.butterfly.fragment.StreamListFragment;
import com.butterfly.listeners.IAsyncTaskListener;
import com.butterfly.utils.Utils;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class DeleteStreamTask extends AbstractAsyncTask<String, Void, Boolean> {

	public DeleteStreamTask(IAsyncTaskListener taskListener, Activity context) {
		super(taskListener, context);
	}

	@Override
	protected Boolean doInBackground(String... params) {
		Boolean result = false;

		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);

		try {

			amfConnection.connect(HTTP_GATEWAY_URL);
			result = (Boolean) amfConnection.call("deleteStream", params[0]);

		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {

			e.printStackTrace();
		}
		amfConnection.close();

		return result;
	}
}
