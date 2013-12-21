package com.butterfly;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.debug.BugSense;
import com.butterfly.fragment.MapFragment;
import com.butterfly.fragment.StreamListFragment;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {

	AppSectionsPagerAdapter mAppSectionsPagerAdapter;
	ViewPager mViewPager;
	private static StreamListFragment streamListFragment;
	private static MapFragment mapFragment;

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
		actionBar.setHomeButtonEnabled(false);

		// Specify that we will be displaying tabs in the action bar.
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Set up the ViewPager, attaching the adapter and setting up a listener for when the
		// user swipes between sections.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(
					actionBar.newTab()
					.setText(mAppSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_client, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.broadcast_live:
			Intent i = new Intent(this.getApplicationContext(), ContactsList.class);
			startActivity(i);
			return true;

		case R.id.refresh:
			streamListFragment.refreshStreamList();
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onDestroy() {
		super.onDestroy();

		BugSenseHandler.closeSession(this);
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
	 * sections of the app.
	 */
	public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

		public AppSectionsPagerAdapter(FragmentManager fm) {
			super(fm);
			streamListFragment = new StreamListFragment();
			mapFragment = new MapFragment();
		}

		@Override
		public Fragment getItem(int i) {
			System.out.print("i nin degeri");
			System.out.print(i);
			switch (i) {
			case 0:
				return streamListFragment;

			case 1:
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
				return streamListFragment.FRAGMENT_NAME;
			case 1:
				return mapFragment.FRAGMENT_NAME;
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


}
