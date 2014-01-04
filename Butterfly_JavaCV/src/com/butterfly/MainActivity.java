package com.butterfly;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.debug.BugSense;
import com.butterfly.fragment.IStreamListUpdateListener;
import com.butterfly.fragment.MapFragment;
import com.butterfly.fragment.StreamListFragment;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.message.CloudMessaging;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class MainActivity extends FragmentActivity  {

	AppSectionsPagerAdapter mAppSectionsPagerAdapter;
	ViewPager mViewPager;
	private static StreamListFragment streamListFragment;
	private static MapFragment mapFragment;
	ArrayList<Stream> streamList = new ArrayList<Stream>();
	ArrayList<IStreamListUpdateListener> streamUpdateListenerList = new ArrayList<IStreamListUpdateListener>();
	private String httpGatewayURL;

	private static final String SHARED_PREFERENCE_FIRST_INSTALLATION = "firstInstallation";
	private static final String APP_SHARED_PREFERENCES = "applicationDetails";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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

		// Create the adapter that will return a fragment for each of the three primary sections
		// of the app.
		mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();

		// Specify that the Home/Up button should not be enabled, since there is no hierarchical
		// parent.
	
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);
		
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
		PackageManager packageManager = getPackageManager();
		boolean backCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
		boolean frontCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
		if (backCamera == false && frontCamera == false) {
			menu.findItem(R.id.broadcast_live).setVisible(false);
		}
		return true;
	}
	
	public ArrayList<Stream> getStreamList() {
		return streamList;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.broadcast_live:
			Intent i = new Intent(this.getApplicationContext(), ContactsList.class);
			startActivity(i);
			return true;

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

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
	 * sections of the app.
	 */
	public class AppSectionsPagerAdapter extends FragmentStatePagerAdapter {

		public AppSectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			
			switch (i) {
			case 0:
				streamListFragment = new StreamListFragment();
				return streamListFragment;
			case 1:
				mapFragment = new MapFragment();
				return mapFragment;
				default:
					break;
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			
			switch (position) {
			case 0:
				return getString(R.string.streamListTitle);
			case 1:
				return getString(R.string.mapTitle);
				default:
					break;
			}
			return super.getPageTitle(position);
		}
	}

	private void showTermsOfUse(final SharedPreferences applicationPrefs) {
		AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
		myAlertDialog.setTitle(R.string.terms_of_service_title);
		myAlertDialog.setMessage(R.string.terms_of_service);
		myAlertDialog.setPositiveButton(R.string.accept,
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
		myAlertDialog.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface arg0, int arg1) {
				// do something when the Cancel button is clicked
				MainActivity.this.finish();
			}
		});
		myAlertDialog.show();
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
							streamList.add(new Stream(
									jsonObject.getString("name"), jsonObject
											.getString("url"), Integer
											.parseInt(jsonObject
													.getString("viewerCount")),
													Double.parseDouble(jsonObject.getString("latitude")),
													Double.parseDouble(jsonObject.getString("longitude")),
													Double.parseDouble(jsonObject.getString("altitude"))
													));

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

}
