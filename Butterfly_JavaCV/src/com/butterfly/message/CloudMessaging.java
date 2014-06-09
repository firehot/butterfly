package com.butterfly.message;

import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.butterfly.listeners.IAsyncTaskListener;
import com.butterfly.tasks.AbstractAsyncTask;
import com.butterfly.tasks.IRegistrationStatus;
import com.butterfly.tasks.RegisterUserTask;
import com.butterfly.tasks.UpdateUserRegisterIdTask;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class CloudMessaging {

	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	public final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	public static final String REGISTRATION_COMPLETED = "REGISTRATION_COMPLETED";

	public static String SENDER_ID = "451796524204";

	public static GoogleCloudMessaging gcm;
	public static AtomicInteger msgId = new AtomicInteger();

	public String regid;
	private Context context;
	private Activity mActivity;
	private String commaSeparatedMails;
	private AbstractAsyncTask<String, Void, String> asyncTask;
	
	IAsyncTaskListener mRegisterUserTaskListener = new IAsyncTaskListener() {
		
		@Override
		public void onProgressUpdate(Object... progress) {}		
		@Override
		public void onPreExecute() {}		
		@Override
		public void onPostExecute(Object object) {
			String regId = (String) object;
			storeRegistrationId(context, regId, ((IRegistrationStatus)asyncTask).isRegistered());
		}
	};
	

	public CloudMessaging(Context context, Activity activity) {
		this.context = context;
		this.mActivity = activity;
		gcm = GoogleCloudMessaging.getInstance(this.context);
		regid = getRegistrationId(this.context);
	}
	
	public void checkRegistrationId(String mails) {
		this.commaSeparatedMails = mails;
		if (regid.isEmpty()) {
			asyncTask = new RegisterUserTask(mRegisterUserTaskListener, this.mActivity);
			asyncTask.execute(SENDER_ID, commaSeparatedMails);
		}
		else if (isAppUpdated(this.context)) {
			asyncTask = new UpdateUserRegisterIdTask(mRegisterUserTaskListener, mActivity);
			asyncTask.execute(SENDER_ID, commaSeparatedMails, regid);
		}
		else if (isRegisrationCompleted() == false) {
			asyncTask = new RegisterUserTask(mRegisterUserTaskListener, mActivity);
			((RegisterUserTask)asyncTask).setRegisterId(regid);
			asyncTask.execute(SENDER_ID, commaSeparatedMails);
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
	public SharedPreferences getGCMPreferences(Context context, Class className) {
		return context.getSharedPreferences(className.getSimpleName(), Context.MODE_PRIVATE);
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

	public AbstractAsyncTask<String, Void, String> getAsyncTask() {
		return asyncTask;
	}

}
