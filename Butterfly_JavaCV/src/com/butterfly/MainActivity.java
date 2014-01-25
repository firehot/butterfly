package com.butterfly;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.adapter.AppSectionsPagerAdapter;
import com.butterfly.debug.BugSense;
import com.butterfly.fragment.ContactsListFragment;
import com.butterfly.fragment.ContactsListFragment.RemoveContactListener;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.listeners.IStreamListUpdateListener;
import com.butterfly.message.CloudMessaging;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class MainActivity extends FragmentActivity implements
		OnPageChangeListener {

	AppSectionsPagerAdapter mAppSectionsPagerAdapter;
	ViewPager mViewPager;
	ArrayList<Stream> streamList = new ArrayList<Stream>();
	ArrayList<IStreamListUpdateListener> streamUpdateListenerList = new ArrayList<IStreamListUpdateListener>();
	private String httpGatewayURL;

	private static final String SHARED_PREFERENCE_FIRST_INSTALLATION = "firstInstallation";
	private static final String APP_SHARED_PREFERENCES = "applicationDetails";
	private int batteryLevel = 0;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getBatteryPercentage();
		BugSenseHandler.initAndStartSession(this, BugSense.API_KEY);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		final SharedPreferences applicationPrefs = getSharedPreferences(
				APP_SHARED_PREFERENCES, MODE_PRIVATE);
		Boolean firstInstallation = applicationPrefs.getBoolean(
				SHARED_PREFERENCE_FIRST_INSTALLATION, false);
		if (!firstInstallation) {
			showTermsOfUse(applicationPrefs);
		}

		setContentView(R.layout.activity_main);

		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(
				getSupportFragmentManager(), this);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		// Specify that the Home/Up button should not be enabled, since there is
		// no hierarchical
		// parent.

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);
		mViewPager.setOnPageChangeListener(this);
		httpGatewayURL = getString(R.string.http_gateway_url);
		new GetStreamListTask().execute(httpGatewayURL);
		// Check device for Play Services APK.
		if (checkPlayServices(this)) {
			CloudMessaging msg = new CloudMessaging(
					this.getApplicationContext(), this, httpGatewayURL);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_client, menu);
		return true;
	}

	public ArrayList<Stream> getStreamList() {
		return streamList;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			new GetStreamListTask().execute(httpGatewayURL);
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		checkPlayServices(this);
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		showHideKeyboard(false, mViewPager);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		BugSenseHandler.closeSession(this);
	}

	public void registerStreamListListener(IStreamListUpdateListener listener) {
		streamUpdateListenerList.add(listener);
	}

	public void removeStreamListListener(IStreamListUpdateListener listener) {
		streamUpdateListenerList.remove(listener);
	}

	private void updateStreamListeners() {
		for (IStreamListUpdateListener listener : streamUpdateListenerList) {
			listener.streamListUpdated(streamList);
		}
	}

	private void showTermsOfUse(final SharedPreferences applicationPrefs) {
		AlertDialog.Builder termsDialog = new AlertDialog.Builder(this);
		termsDialog.setTitle(R.string.terms_of_service_title);
		termsDialog.setCancelable(false);
		final View dialog_view = getLayoutInflater().inflate(
				R.layout.dialog_privacy_and_terms, null);
		View acceptingPrivacyTextView = dialog_view
				.findViewById(R.id.accepting_privacy_and_terms);
		acceptingPrivacyTextView.setOnClickListener(new OnClickListener() {
			boolean termsLoadedOnce = false;

			@Override
			public void onClick(View v) {
				if (termsLoadedOnce == false) {
					termsLoadedOnce = true;

					final ProgressBar progressBar = (ProgressBar) dialog_view
							.findViewById(R.id.web_view_load_progress);
					progressBar.setVisibility(View.VISIBLE);

					WebView view = (WebView) dialog_view
							.findViewById(R.id.privacy_and_terms_view);
					view.setWebChromeClient(new WebChromeClient() {

						@Override
						public void onProgressChanged(WebView view,
								int newProgress) {
							super.onProgressChanged(view, newProgress);
							if (newProgress == 100) {
								view.setVisibility(View.VISIBLE);
								progressBar.setVisibility(View.GONE);
							}
						}
					});
					view.loadUrl(getString(R.string.terms_of_service_url));
				}
			}
		});
		termsDialog.setView(dialog_view);
		termsDialog.setPositiveButton(R.string.accept,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface arg0, int arg1) {
						// do something when the OK button is clicked
						SharedPreferences.Editor mInstallationEditor = applicationPrefs
								.edit();
						mInstallationEditor.putBoolean(
								SHARED_PREFERENCE_FIRST_INSTALLATION, true);
						mInstallationEditor.commit();
					}
				});
		termsDialog.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface arg0, int arg1) {
						// do something when the Cancel button is clicked
						MainActivity.this.finish();
					}
				});
		termsDialog.show();
	}

	private void getBatteryPercentage() {

		BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {

			public void onReceive(Context context, Intent intent) {
				context.unregisterReceiver(this);
				int currentLevel = intent.getIntExtra(
						BatteryManager.EXTRA_LEVEL, -1);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				int level = -1;
				if (currentLevel >= 0 && scale > 0) {
					level = (currentLevel * 100) / scale;
				}
				batteryLevel = level;
			}
		};
		IntentFilter batteryLevelFilter = new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(batteryLevelReceiver, batteryLevelFilter);
	}

	public int getBatteryLevel() {
		return batteryLevel;
	}

	public static boolean checkPlayServices(Activity activity) {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(activity);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
						CloudMessaging.PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				activity.finish();
			}
			return false;
		}

		return true;
	}

	public class GetStreamListTask extends AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {
			String streams = null;
			AMFConnection amfConnection = new AMFConnection();
			amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
			try {
				System.out.println(params[0]);
				amfConnection.connect(params[0]);
				streams = (String) amfConnection.call("getLiveStreams");
			} catch (ClientStatusException e) {
				e.printStackTrace();
			} catch (ServerStatusException e) {
				e.printStackTrace();
			}
			amfConnection.close();

			return streams;
		}

		@Override
		protected void onPostExecute(String streams) {
			streamList.clear();

			if (streams != null) {
				// JSONObject jsonObject = new JSONObject(streams);
				JSONArray jsonArray;
				try {
					jsonArray = new JSONArray(streams);
					int length = jsonArray.length();
					if (length > 0) {

						JSONObject jsonObject;

						for (int i = 0; i < length; i++) {
							jsonObject = (JSONObject) jsonArray.get(i);
							streamList.add(new Stream(jsonObject
									.getString("name"), jsonObject
									.getString("url"), Integer
									.parseInt(jsonObject
											.getString("viewerCount")), Double
									.parseDouble(jsonObject
											.getString("latitude")), Double
									.parseDouble(jsonObject
											.getString("longitude")), Double
									.parseDouble(jsonObject
											.getString("altitude")), Boolean
									.parseBoolean(jsonObject
											.getString("isLive"))));

						}
					} else {
						Toast.makeText(getApplicationContext(),
								getString(R.string.noLiveStream),
								Toast.LENGTH_LONG).show();
					}

					updateStreamListeners();

				} catch (JSONException e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(),
							getString(R.string.connectivityProblem),
							Toast.LENGTH_LONG).show();
				}

			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.connectivityProblem),
						Toast.LENGTH_LONG).show();

			}
			setProgressBarIndeterminateVisibility(false);
			super.onPostExecute(streams);
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {

		if (state == ViewPager.SCROLL_STATE_IDLE) {
			
			//Camera var ise item count 3 oluyor(contactlist,streamlist ve mapfragment)
			if(mAppSectionsPagerAdapter.getCount() == 3)
			{
				if(mViewPager.getCurrentItem() == 0)
				{
					//Contactlistfragment secili ise klavye goster
					showHideKeyboard(true, mViewPager);
				}
				else
				{
					showHideKeyboard(false, mViewPager);
				}
			}

		}
	}
	
	/**
	 * Klavyenin gosterilip gosterilmemesini saglayan metod
	 * @param toShow
	 * @param view
	 */
	public void showHideKeyboard(boolean toShow,View view)
	{
		final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		
		if(toShow)
		{
			inputMethodManager.toggleSoftInputFromWindow(
					view
							.getApplicationWindowToken(),
					InputMethodManager.SHOW_FORCED, 0);
		}
		else
		{
			inputMethodManager.hideSoftInputFromWindow(
					view.getWindowToken(), 0);
		}
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageSelected(int position) {

	}
}
