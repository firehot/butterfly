package com.butterfly.tasks;

import java.io.IOException;

import android.app.Activity;

import com.butterfly.listeners.IAsyncTaskListener;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class UpdateUserRegisterIdTask extends AbstractAsyncTask<String, Void, String> implements IRegistrationStatus{

	private boolean isRegistered = false;

	public UpdateUserRegisterIdTask(IAsyncTaskListener taskListener,
			Activity context) {
		super(taskListener, context);
	}

	/*
	 * params[0] SENDER_ID
	 * params[1] mail addresses
	 * params[2] old reg id
	 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
	 */
	@Override
	protected String doInBackground(String... params) {
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
		AMFConnection amfConnection = null;
		String registerId = null;				
		try {
			registerId = gcm.register(params[0]);
			
			isRegistered  = false;
			amfConnection = new AMFConnection();
			amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
			
				System.out.println(registerId);
				amfConnection.connect(HTTP_GATEWAY_URL);
				isRegistered = (Boolean) amfConnection.call("updateUser",registerId, params[1],params[2]);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		}
		finally {
			if (amfConnection != null) {
				amfConnection.close();
			}
		}
		
		return registerId;
	}
	
	@Override
	public boolean isRegistered() {
		return isRegistered;
	}

}
