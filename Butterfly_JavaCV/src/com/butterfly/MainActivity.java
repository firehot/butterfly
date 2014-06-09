package com.butterfly;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.adapter.AppSectionsPagerAdapter;
import com.butterfly.debug.BugSense;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.listeners.IAsyncTaskListener;
import com.butterfly.listeners.IStreamListUpdateListener;
import com.butterfly.message.CloudMessaging;
import com.butterfly.tasks.GetStreamListTask;
import com.butterfly.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class MainActivity extends FragmentActivity implements OnPageChangeListener, OnClickListener {

	public static boolean mainActivityCompleted = false;
	AppSectionsPagerAdapter mAppSectionsPagerAdapter;
	ViewPager mViewPager;
	ArrayList<Stream> streamList = new ArrayList<Stream>();
	ArrayList<IStreamListUpdateListener> streamUpdateListenerList = new ArrayList<IStreamListUpdateListener>();

	private static final String SHARED_PREFERENCE_FIRST_INSTALLATION = "first_opening";
	public static final String APP_SHARED_PREFERENCES = "applicationDetails";
	public static final String REGISTERED_MAIL_ADDRESS = "REGISTERED_MAIL_ADDRESS";
	private int batteryLevel = 0;
	public GetStreamListTask getStreamListTask;
	private CloudMessaging gcmMessenger;
	private AlertDialog termsOfUseDialog;
	private SharedPreferences applicationPrefs;
	Button nextButton;
	Button okButton;
	ImageView image;
	ImageView image2;

	private IAsyncTaskListener mAsyncTaskListener = new IAsyncTaskListener() {

		@Override
		public void onProgressUpdate(Object... progress) {}

		@Override
		public void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
		}

		@Override
		public void onPostExecute(Object streams) {
			streamList.clear();

			if (streams != null) {
				List<Stream> localStreamList = (List<Stream>)streams;
				if (localStreamList.size()>0) {
					streamList.addAll(localStreamList);
				}
				else {
					Toast.makeText(MainActivity.this,
							MainActivity.this.getString(R.string.noLiveStream),
							Toast.LENGTH_LONG).show();
				}
			} else {
				Crouton.showText(MainActivity.this,
						R.string.connectivityProblem, Style.ALERT);

			}
			updateStreamListeners(streamList);
			setProgressBarIndeterminateVisibility(false);
			MainActivity.mainActivityCompleted = true;

		}
	};
	

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getBatteryPercentage();
		BugSenseHandler.initAndStartSession(this, BugSense.API_KEY);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_main);
		nextButton = (Button) findViewById(R.id.buttonNext);
		nextButton.setOnClickListener(this);
		okButton = (Button) findViewById(R.id.buttonOk);
		okButton.setOnClickListener(this);
		image = (ImageView) findViewById(R.id.help1ImageView);
		image2 = (ImageView) findViewById(R.id.help2ImageView);

		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager(), this);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		// Specify that the Home/Up button should not be enabled, since there is
		// no hierarchical
		// parent.

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);
		mViewPager.setOnPageChangeListener(this);

		// Check device for Play Services APK.
		if (checkPlayServices(this)) {
			gcmMessenger = new CloudMessaging(
					this.getApplicationContext(), this);
		}


		applicationPrefs = getSharedPreferences(
				APP_SHARED_PREFERENCES, MODE_PRIVATE);
		Boolean firstInstallation = getApplicationPrefs().getBoolean(
				SHARED_PREFERENCE_FIRST_INSTALLATION, false);
		if (!firstInstallation) {
			showTermsOfUse(getApplicationPrefs());
			mViewPager.setVisibility(View.GONE);
			nextButton.setVisibility(View.VISIBLE);
			okButton.setVisibility(View.GONE);
			image.setVisibility(View.VISIBLE);
			image2.setVisibility(View.GONE);
			setFullScreen(true);
		}
		else {
			mViewPager.setVisibility(View.VISIBLE);
			nextButton.setVisibility(View.GONE);
			okButton.setVisibility(View.GONE);
			image.setVisibility(View.GONE);
			image2.setVisibility(View.GONE);

			setFullScreen(false);

			// if the app is opened for the first time, check registation id is called
			// in dialog positive button click.
			if (getGcmMessenger() != null) {
				String registeredMailAddress = getApplicationPrefs().getString(REGISTERED_MAIL_ADDRESS, null);
				if (registeredMailAddress != null && registeredMailAddress.length()>0) {
					getGcmMessenger().checkRegistrationId(registeredMailAddress);
				}
			}
			getStreamListTask = new GetStreamListTask(this.getTaskListener(), this);
			getStreamListTask.execute("0","10");
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
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
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
		if (getStreamListTask != null
				&& getStreamListTask.getStatus() == AsyncTask.Status.RUNNING) {
			getStreamListTask.cancel(true);
		}
		BugSenseHandler.closeSession(this);
		Crouton.cancelAllCroutons();
	}

	public void registerStreamListListener(IStreamListUpdateListener listener) {
		streamUpdateListenerList.add(listener);
	}

	public void removeStreamListListener(IStreamListUpdateListener listener) {
		streamUpdateListenerList.remove(listener);
	}

	private void updateStreamListeners(ArrayList<Stream> streamList) {
		for (IStreamListUpdateListener listener : streamUpdateListenerList) {
			listener.streamListUpdated(streamList);
		}
	}

	private void showTermsOfUse(final SharedPreferences applicationPrefs) {
		AlertDialog.Builder termsDialog = new AlertDialog.Builder(this);
		termsDialog.setTitle(R.string.associate_addresses);
		termsDialog.setCancelable(false);
		final View dialog_view = getLayoutInflater().inflate(
				R.layout.dialog_privacy_and_terms, null);
		View acceptingPrivacyTextView = dialog_view
				.findViewById(R.id.accepting_privacy_and_terms);

		final ListView mailAddressesLv = (ListView)dialog_view.findViewById(R.id.mail_addresses);

		mailAddressesLv.setAdapter(new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_list_item_multiple_choice, Utils.getFullMailArrayList(MainActivity.this)));


		mailAddressesLv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
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

		termsDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				// we need to add this to compatibility. 
				// OK button doesnt close the dialog if no email address are selected.
			}
		});
		termsDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				MainActivity.this.finish();
			}
		});
		termsOfUseDialog = termsDialog.show();
		termsOfUseDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				int itemCount = mailAddressesLv.getCheckedItemCount();
				if (itemCount > 0) {
					SparseBooleanArray checkedItemIds = mailAddressesLv.getCheckedItemPositions();

					String mails = new String();
					for (int i = 0; i < mailAddressesLv.getCount(); i++) {
						if (checkedItemIds.get(i)) {
							if (i>0) {
								mails += ",";
							}
							mails += mailAddressesLv.getAdapter().getItem(i);
						}
					}
					Editor editor = applicationPrefs.edit();
					editor.putString(REGISTERED_MAIL_ADDRESS, mails);
					editor.commit();
					if (getGcmMessenger() != null) {
						getGcmMessenger().checkRegistrationId(mails);
					}
					termsOfUseDialog.dismiss();
					
					SharedPreferences.Editor mInstallationEditor = applicationPrefs
							.edit();
					mInstallationEditor.putBoolean(SHARED_PREFERENCE_FIRST_INSTALLATION, true);
					mInstallationEditor.commit();

					getStreamListTask = new GetStreamListTask(MainActivity.this.getTaskListener(), MainActivity.this);
					getStreamListTask.execute("0","10");

				}
				else {
					Toast.makeText(MainActivity.this, getString(R.string.choose_a_mail_address), Toast.LENGTH_SHORT).show();
				}
			}
		});
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
	 * 
	 * @param toShow
	 * @param view
	 */
	public void showHideKeyboard(boolean toShow, View view) {
		final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if(toShow)
		{
			inputMethodManager.toggleSoftInputFromWindow(
					view
					.getApplicationWindowToken(),
					InputMethodManager.SHOW_FORCED, 0);
		} else {
			inputMethodManager
			.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageSelected(int position) {

	}

	@Override
	public void onClick(View v) {
		if (v == nextButton) {
			nextButton.setVisibility(View.GONE);
			okButton.setVisibility(View.VISIBLE);
			image.setVisibility(View.GONE);
			image2.setVisibility(View.VISIBLE);
		} else {
			image.setVisibility(View.GONE);
			image2.setVisibility(View.GONE);
			setFullScreen(false);
			nextButton.setVisibility(View.GONE);
			okButton.setVisibility(View.GONE);

			mViewPager.setVisibility(View.VISIBLE);

		}

	}

	private void setFullScreen(boolean bFullScreen) {
		if (bFullScreen) {
			getActionBar().hide();
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getActionBar().show();
		}

		getWindow().getDecorView().requestLayout();

	}

	public IAsyncTaskListener getTaskListener() {
		return mAsyncTaskListener;
	}

	public CloudMessaging getGcmMessenger() {
		return gcmMessenger;
	}
	
	public AlertDialog getTermsOfUseDialog() {
		return termsOfUseDialog;
	}

	public SharedPreferences getApplicationPrefs() {
		return applicationPrefs;
	}

}
