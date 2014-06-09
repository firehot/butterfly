package com.butterfly.tasks;

import android.app.Activity;

import com.butterfly.listeners.IAsyncTaskListener;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class RegisterLocationForStreamTask extends AbstractAsyncTask<Double, Void, Boolean> {

	public RegisterLocationForStreamTask(IAsyncTaskListener taskListener,
			Activity context) {
		super(taskListener, context);
	}

	private String streamURL;

	public void setParams(String url) {
		this.streamURL = url;
	}

	@Override
	protected Boolean doInBackground(Double... params) {
		Boolean result = false;

		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {
			amfConnection.connect(HTTP_GATEWAY_URL);
			result = (Boolean) amfConnection.call(
					"registerLocationForStream", this.streamURL, params[0],
					params[1], params[2]);

		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		}catch (Exception e) {
			e.printStackTrace();
		}
		amfConnection.close();
		return result;
	}
}
