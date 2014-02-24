package com.butterfly.adapter;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.butterfly.R;
import com.butterfly.fragment.ContactsListFragment;
import com.butterfly.fragment.VideoMapFragment;
import com.butterfly.fragment.StreamListFragment;
import com.butterfly.fragment.VideoFragmentTabs;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one
 * of the primary sections of the app.
 */
public class AppSectionsPagerAdapter extends FragmentStatePagerAdapter {

	private int itemCount = CAMERA_EXISTS_ITEM_COUNT;
	private Activity activity;
	VideoFragmentTabs videoFragmentTabs;
	VideoMapFragment mapFragment;
	ContactsListFragment contactListFragment;
	public static final int CAMERA_EXISTS_ITEM_COUNT = 2;
	public static final int CAMERA_NOT_EXISTS_ITEM_COUNT = CAMERA_EXISTS_ITEM_COUNT-1;
			

	public AppSectionsPagerAdapter(FragmentManager fm, Activity activity) {
		super(fm);
		this.activity = activity;
		PackageManager packageManager = this.activity.getPackageManager();
		boolean backCamera = packageManager
				.hasSystemFeature(PackageManager.FEATURE_CAMERA);
		boolean frontCamera = packageManager
				.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
		if (backCamera == false && frontCamera == false) {
			itemCount = CAMERA_NOT_EXISTS_ITEM_COUNT;
		}

	}

	@Override
	public Fragment getItem(int i) {
		if (itemCount == CAMERA_NOT_EXISTS_ITEM_COUNT) {
			switch (i) {
			case 0:
				if (videoFragmentTabs == null)
					videoFragmentTabs = new VideoFragmentTabs();
				return videoFragmentTabs;
	
			default:
				break;
			}
		} else {
			switch (i) {
			case 0:
				if(contactListFragment == null)
					contactListFragment = new ContactsListFragment();
				return contactListFragment;
			case 1:
				if (videoFragmentTabs == null)
					videoFragmentTabs = new VideoFragmentTabs();
				return videoFragmentTabs;
			default:
				break;
			}
		}
		return null;
	}

	@Override
	public int getCount() {
		return itemCount;
	}

	@Override
	public CharSequence getPageTitle(int position) {

		if (itemCount == CAMERA_NOT_EXISTS_ITEM_COUNT) {
			switch (position) {
			case 0:
				return this.activity.getString(R.string.streamListTitle);
			default:
				break;
			}
		} else {
			switch (position) {
			case 0:
				return this.activity.getString(R.string.contactListTitle);
			case 1:
				return this.activity.getString(R.string.streamListTitle);
			default:
				break;
			}
		}
		
		return super.getPageTitle(position);
	}
}