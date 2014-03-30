package com.butterfly.test;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.butterfly.MainActivity;
import com.butterfly.R;
import com.butterfly.RecordActivity;
import com.butterfly.adapter.AppSectionsPagerAdapter;
import com.butterfly.fragment.ContactsListFragment;

public class MainActivityTester extends ActivityInstrumentationTestCase2 {

	private MainActivity mainActivity;
	protected boolean mainActivityOnPausedCalled = false;
	protected boolean recordActivityStarted = false;


	public MainActivityTester() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mainActivity = (MainActivity) getActivity();
		mainActivityOnPausedCalled = false;
		recordActivityStarted = false;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		mainActivity = null;
	}

	
	public void testContactSearch() {
		ViewPager viewPager = (ViewPager)mainActivity.findViewById(R.id.pager);
		AppSectionsPagerAdapter adapter = (AppSectionsPagerAdapter) viewPager.getAdapter();
		int count = adapter.getCount();
		assertEquals(2, count);
		
		ContactsListFragment contactFragment = (ContactsListFragment) adapter.getItem(0);
		
		View contactView = contactFragment.getView();
		final EditText searchEditText = (EditText)contactView.findViewById(R.id.searchText);
		
		mainActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				searchEditText.setText("gmail.com");
			}
		});
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		final ListView lv = (ListView) contactView.findViewById(R.id.selectedContactList);
		
		int childCount = lv.getChildCount();
		assertTrue(childCount > 0);
		
		mainActivity.getApplication().registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			
			@Override
			public void onActivityStopped(Activity arg0) {}
			
			@Override
			public void onActivityStarted(Activity activity) {
				if (activity.getClass().equals(RecordActivity.class)) {
					recordActivityStarted = true;
				}
			}
			
			@Override
			public void onActivitySaveInstanceState(Activity arg0, Bundle arg1) {}
			
			@Override
			public void onActivityResumed(Activity arg0) {}
			
			@Override
			public void onActivityPaused(Activity activity) {
				if (activity.getClass().equals(MainActivity.class)) {
					mainActivityOnPausedCalled  = true;
				}
			}
			
			@Override
			public void onActivityDestroyed(Activity arg0) {}
			
			@Override
			public void onActivityCreated(Activity arg0, Bundle arg1) {}
		});
		 
		
		mainActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				lv.performItemClick(lv.getAdapter().getView(0, null, null), 0, lv.getAdapter().getItemId(0));
				searchEditText.setText("gmail.co");
			}
		});
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		 assertTrue(mainActivityOnPausedCalled);
		 assertTrue(recordActivityStarted);
		
		
		
		

	
	}


}
