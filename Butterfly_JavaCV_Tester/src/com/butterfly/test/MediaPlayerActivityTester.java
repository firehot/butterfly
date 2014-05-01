package com.butterfly.test;

import java.util.List;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.butterfly.MainActivity;
import com.butterfly.MediaPlayerActivity;
import com.butterfly.R;
import com.butterfly.RecordActivity;
import com.butterfly.adapter.AppSectionsPagerAdapter;
import com.butterfly.fragment.ContactsListFragment;

public class MediaPlayerActivityTester extends ActivityInstrumentationTestCase2 {

	private MediaPlayerActivity playerActivity;


	public MediaPlayerActivityTester() {
		super(MediaPlayerActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		playerActivity = (MediaPlayerActivity)getActivity();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		playerActivity = null;
	}
	
	public void testWebIntent() {
		
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.addCategory(Intent.CATEGORY_BROWSABLE);
		i.addCategory(Intent.CATEGORY_DEFAULT);
		Uri uri = Uri.parse("http://www.butterflytv.net/player.html?videoId=123131241"); 
		i.setData(uri);
		
		List<ResolveInfo> activities = playerActivity.getPackageManager().queryIntentActivities(i, 0);
		
		boolean found = false;
		for (int j = 0; j < activities.size(); j++) {
			if (activities.get(j).activityInfo.name.equals(playerActivity.getClass().getName())) {
				found = true;
				break;
			}
		}
		assertTrue(found);
		
		
		uri = Uri.parse("http://butterflytv.net/player.html?videoId=123131241"); 
		i.setData(uri);
		
		activities = playerActivity.getPackageManager().queryIntentActivities(i, 0);
		
		found = false;
		for (int j = 0; j < activities.size(); j++) {
			if (activities.get(j).activityInfo.name.equals(playerActivity.getClass().getName())) {
				found = true;
				break;
			}
		}
		assertTrue(found);
		
		
		
		
		uri = Uri.parse("http://www.butterflytv.net/"); 
		i.setData(uri);
		
		activities = playerActivity.getPackageManager().queryIntentActivities(i, 0);
		
		found = false;
		for (int j = 0; j < activities.size(); j++) {
			if (activities.get(j).activityInfo.name.equals(playerActivity.getClass().getName())) {
				found = true;
				break;
			}
		}
		assertFalse(found);
		
		uri = Uri.parse("http://butterflytv.net/"); 
		i.setData(uri);
		
		activities = playerActivity.getPackageManager().queryIntentActivities(i, 0);
		
		found = false;
		for (int j = 0; j < activities.size(); j++) {
			if (activities.get(j).activityInfo.name.equals(playerActivity.getClass().getName())) {
				found = true;
				break;
			}
		}
		assertFalse(found);

		
	}


}
