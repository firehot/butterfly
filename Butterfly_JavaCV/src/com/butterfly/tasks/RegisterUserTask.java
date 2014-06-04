package com.butterfly.tasks;

import java.io.IOException;

import android.app.Activity;

import com.butterfly.listeners.IAsyncTaskListener;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class RegisterUserTask extends AbstractAsyncTask<String, Void, String> implements IRegistrationStatus{

	private boolean isRegistered = false;
	private String registerId = null;

	public RegisterUserTask(IAsyncTaskListener taskListener, Activity context) {
		super(taskListener, context);
	}

	public void setRegisterId(String registerId) {
		this.registerId = registerId;
	}

	/*
	 * params[0] SENDER_ID
	 * params[1] Server ADDR
	 * params[2] Comma separated mail addresses
	 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
	 */
	@Override
	protected String doInBackground(String... params) {
		AMFConnection amfConnection = null;
		try {
			if (registerId == null) {
				GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
				registerId = gcm.register(params[0]);
			}

			amfConnection = new AMFConnection();
			amfConnection.setObjectEncoding(MessageIOConstants.AMF0);

			System.out.println(registerId);
			amfConnection.connect(params[1]);
			isRegistered = (Boolean) amfConnection.call("registerUser",registerId, params[2]);


		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (amfConnection != null) {
				amfConnection.close();
			}
		}
		return registerId;
	}

	public boolean isRegistered() {
		return isRegistered;
	}

}
