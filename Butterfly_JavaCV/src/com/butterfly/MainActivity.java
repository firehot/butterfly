package com.butterfly;

import java.util.ArrayList;

import org.json.JSONException;

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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.adapter.AppSectionsPagerAdapter;
import com.butterfly.debug.BugSense;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.listeners.IStreamListUpdateListener;
import com.butterfly.message.CloudMessaging;
import com.butterfly.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class MainActivity extends FragmentActivity implements
		OnPageChangeListener {

	public static boolean mainActivityCompleted=false;
	AppSectionsPagerAdapter mAppSectionsPagerAdapter;
	ViewPager mViewPager;
	ArrayList<Stream> streamList = new ArrayList<Stream>();
	ArrayList<IStreamListUpdateListener> streamUpdateListenerList = new ArrayList<IStreamListUpdateListener>();
	public String httpGatewayURL;

	private static final String SHARED_PREFERENCE_FIRST_INSTALLATION = "firstInstallation";
	private static final String APP_SHARED_PREFERENCES = "applicationDetails";
	private int batteryLevel = 0;
	public GetStreamListTask getStreamListTask;

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
		
		// Check device for Play Services APK.
		if (checkPlayServices(this)) {
			CloudMessaging msg = new CloudMessaging(
					this.getApplicationContext(), this, httpGatewayURL);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main_menu, menu);
		return true;
	}

	public ArrayList<Stream> getStreamList() {
		return streamList;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.record:
			Intent intent = new Intent(this, RecordActivity.class);
			startActivity(intent);
			return true;
		case R.id.group_broadcast:
			startActivity(new Intent(this, ContactsGroupActivity.class));
		case R.id.settings:
			startActivity(new Intent(this, SettingsActivity.class));
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		checkPlayServices(this);
		new GetStreamListTask().execute(httpGatewayURL,"0","10");
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		mainActivityCompleted = false;
		showHideKeyboard(false, mViewPager);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (getStreamListTask != null && getStreamListTask.getStatus() == AsyncTask.Status.RUNNING) {
			getStreamListTask.cancel(true);
		}
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
			
			String streams = Utils.getLiveStreams(MainActivity.this, params);

			return streams;
		}

		@Override
		protected void onPostExecute(String streams) {
			

			streamList.clear();

			if (streams != null) {

				try {
					streamList.addAll(Utils.parseStreams(streams, getApplicationContext()));

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
			
			MainActivity.mainActivityCompleted = true;
		}
		
		
	}

	@Override
	public void onPageScrollStateChanged(int state) {

		if (state == ViewPager.SCROLL_STATE_IDLE) {
			
			//Camera var ise item count 2 oluyor(contactlist,streamlist ve mapfragment)
			if(mAppSectionsPagerAdapter.getCount() == 2)
			{
				if(mViewPager.getCurrentItem() == 1)
				{
					//hide keyboard when showing the video list
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
