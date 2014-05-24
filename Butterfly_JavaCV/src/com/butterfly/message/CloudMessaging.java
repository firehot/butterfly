package com.butterfly.message;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;
import io.vov.utils.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;

import com.butterfly.utils.Utils;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class CloudMessaging {

	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	public final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final String REGISTRATION_COMPLETED = "REGISTRATION_COMPLETED";

	public static String SENDER_ID = "451796524204";

	public static GoogleCloudMessaging gcm;
	public static AtomicInteger msgId = new AtomicInteger();

	public String regid;
	private Context context;
	private Activity mActivity;
	private String backendServer;

	public CloudMessaging(Context context, Activity activity, String backendServer) {
		this.context = context;
		this.mActivity = activity;
		gcm = GoogleCloudMessaging.getInstance(this.context);
		regid = getRegistrationId(this.context);
		this.backendServer = backendServer;

		if (regid.isEmpty()) {
			registerInBackground();
		}
		else if (isAppUpdated(this.context)) {
			new UpdateRegIDTask().execute(regid);
		}
		else if (isRegisrationCompleted() == false) {
			new Thread() {
				public void run() {
					boolean result = sendRegistrationIdToBackend();
		            
					storeRegistrationCompleted(result);
				};
			}.start();
		}

	}

	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGCMPreferences(context,
				mActivity.getClass());
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {

			return "";
		}
		
		return registrationId;
	}
	
	private boolean isRegisrationCompleted() {
		final SharedPreferences prefs = getGCMPreferences(context,
				mActivity.getClass());
		return prefs.getBoolean(REGISTRATION_COMPLETED, false);
	}
	
	private boolean isAppUpdated(Context context) {
		final SharedPreferences prefs = getGCMPreferences(context,
				mActivity.getClass());
		
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {

			return true;
		}
		return false;
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
	
	private boolean sendRegistrationIdToBackend() 
	{
		boolean result = false;
		String mails = Utils.getMailList(this.mActivity);
		if(mails != null)
		{
			Log.e("butterfly", mails);	
			result = registerUser(backendServer, regid, mails);
		}
		return result;
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
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		}
		amfConnection.close();

		return isRegistered;
	}
	
	protected Boolean updateUser(String backendServer, String registerId, String mail,String oldRegID) {
		Boolean result = false;
		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {
			System.out.println(registerId);
			amfConnection.connect(backendServer);
			result = (Boolean) amfConnection
					.call("updateUser",registerId, mail,oldRegID);

		} catch (ClientStatusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServerStatusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		amfConnection.close();

		return result;
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
	                boolean result = sendRegistrationIdToBackend();
	                
	                // For this demo: we don't need to send it because the device
	                // will send upstream messages to a server that echo back the
	                // message using the 'from' address in the message.

	                // Persist the regID - no need to register again.
	                storeRegistrationId(context, regid, result);
	            } catch (IOException ex) {
	
	                System.out.println("register hata:"+ex.getMessage());
	            }
	            return regid;
	     }
	 }
	
	private boolean updateRegistrationId(String oldRegID) 
	{
		boolean result = false;
		String mails =  Utils.getMailList(this.mActivity);
		if(mails != null)
		{
			Log.e("butterfly", mails);	
			result = updateUser(backendServer, regid, mails, oldRegID);
		}
		return result;
	}
	
	private class UpdateRegIDTask extends AsyncTask<String, Void, String> {
	     protected String doInBackground(String... params) {

	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(context);
	                }
	                regid = gcm.register(SENDER_ID);


	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.
	                // The request to your server should be authenticated if your app
	                // is using accounts.
	                boolean completed = updateRegistrationId(params[0]);

	                // For this demo: we don't need to send it because the device
	                // will send upstream messages to a server that echo back the
	                // message using the 'from' address in the message.

	                // Persist the regID - no need to register again.
	                storeRegistrationId(context, regid, completed);
	            } catch (IOException ex) {
	
	                System.out.println("register hata:"+ex.getMessage());
	            }
	            return regid;
	     }

	     protected void onPostExecute(String regid) {
	         
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
	private void storeRegistrationId(Context context, String regId, boolean registrationCompleted) {
		final SharedPreferences prefs = getGCMPreferences(context,
				mActivity.getClass());
		int appVersion = getAppVersion(context);

		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.putBoolean(REGISTRATION_COMPLETED, registrationCompleted);
		editor.commit();
	}
	
	/**
	 * Stores registation status in backend server
	 * @param registrationCompleted
	 */
	private void storeRegistrationCompleted(boolean registrationCompleted) {
		final SharedPreferences prefs = getGCMPreferences(context,
				mActivity.getClass());

		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(REGISTRATION_COMPLETED, registrationCompleted);
		editor.commit();
	}

}
