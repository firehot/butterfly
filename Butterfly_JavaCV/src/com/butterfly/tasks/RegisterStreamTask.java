package com.butterfly.tasks;

import java.util.Locale;

import android.app.Activity;

import com.butterfly.listeners.IAsyncTaskListener;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class RegisterStreamTask extends
		AbstractAsyncTask<String, Void, Boolean> {


	public RegisterStreamTask(IAsyncTaskListener taskListener, Activity context) {
		super(taskListener, context);
	}


	/**
	 * params[0] streamName
	 * params[1] streamURL
	 * params[2] mailsToBeNotified
	 * params[3] mail addres
	 * params[4] is_video_public
	 */
	@Override
	protected Boolean doInBackground(String... params) {
		boolean result = false;
		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {
			System.out.println(params[0]);

			boolean isVideoPublic = Boolean.parseBoolean(params[4]);
			amfConnection.connect(HTTP_GATEWAY_URL);
			result = (Boolean) amfConnection.call("registerLiveStream",
					params[0], params[1], params[2],
					params[3], isVideoPublic, Locale.getDefault()
							.getISO3Language());

		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		amfConnection.close();
		return result;
	}

}
