package com.butterfly.message;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;
import io.vov.utils.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Patterns;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class CloudMessaging {

	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	public final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	public static String SENDER_ID = "451796524204";

	public static GoogleCloudMessaging gcm;
	public static AtomicInteger msgId = new AtomicInteger();

	public String regid;
	private Context context;
	private Activity activity;
	private String backendServer;

	public CloudMessaging(Context context, Activity activity, String backendServer) {
		this.context = context;
		this.activity = activity;
		gcm = GoogleCloudMessaging.getInstance(this.context);
		regid = getRegistrationId(this.context);
		this.backendServer = backendServer;

		if (regid.isEmpty()) {
			registerInBackground();
		}
	}

	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGCMPreferences(context,
				activity.getClass());
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {

			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {

			return "";
		}
		return registrationId;
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences(Context context, Class className) {
		// This sample app persists the registration ID in shared preferences,
		// but
		// how you store the regID in your app is up to you.
		return context.getSharedPreferences(className.getSimpleName(),
				Context.MODE_PRIVATE);
	}

	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground() {

		new RegisterTask().execute(null,null);
	}
	
	private void sendRegistrationIdToBackend() 
	{
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(activity).getAccounts();
		for (Account account : accounts) {
			if (emailPattern.matcher(account.name).matches()) {
				String possibleEmail = account.name;
				if (possibleEmail.contains("gmail.com"))
				{
					Log.e("butterfly", possibleEmail);	
					registerUser(backendServer, regid, possibleEmail);
					break;
				}

			}
		}
	}
	
	protected Boolean registerUser(String backendServer, String registerId, String mail) {
		Boolean isRegistered = false;
		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {
			System.out.println(registerId);
			amfConnection.connect(backendServer);
			isRegistered = (Boolean) amfConnection
					.call("registerUser",registerId, mail);

		} catch (ClientStatusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServerStatusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		amfConnection.close();

		return isRegistered;
	}
	
	
	private class RegisterTask extends AsyncTask<Void, Void, String> {
	     protected String doInBackground(Void... params) {

	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(context);
	                }
	                regid = gcm.register(SENDER_ID);


	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.
	                // The request to your server should be authenticated if your app
	                // is using accounts.
	                sendRegistrationIdToBackend();

	                // For this demo: we don't need to send it because the device
	                // will send upstream messages to a server that echo back the
	                // message using the 'from' address in the message.

	                // Persist the regID - no need to register again.
	                storeRegistrationId(context, regid);
	            } catch (IOException ex) {
	
	                System.out.println("register hata:"+ex.getMessage());
	            }
	            return regid;
	     }

	     protected void onPostExecute(String regid) {
	         
	     }
	 }

	
	private class SendMessageTask extends AsyncTask<String, Void, String> {
	     protected String doInBackground(String... params) {
	    	 String msg = "";
             try {
                 Bundle data = new Bundle();
                     data.putString("my_message", params[0]);
                     data.putString("my_action",
                             "com.google.android.gcm.demo.app.ECHO_NOW");
                     String id = Integer.toString(msgId.incrementAndGet());
                     gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
                     msg = "Sent message";
             } catch (IOException ex) {
                 msg = "Error :" + ex.getMessage();
             }
             return msg;
	     }

	     protected void onPostExecute(String msg) {
	         
	     }
	 }
	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 * 
	 * @param context
	 *            application's context.
	 * @param regId
	 *            registration ID
	 */
	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getGCMPreferences(context,
				activity.getClass());
		int appVersion = getAppVersion(context);

		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

}
