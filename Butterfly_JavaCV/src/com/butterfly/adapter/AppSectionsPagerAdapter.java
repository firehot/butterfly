package com.butterfly.adapter;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.butterfly.R;
import com.butterfly.fragment.ContactsListFragment;
import com.butterfly.fragment.MapFragment;
import com.butterfly.fragment.StreamListFragment;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one
 * of the primary sections of the app.
 */
public class AppSectionsPagerAdapter extends FragmentStatePagerAdapter {

	private int itemCount = 3;
	private Activity activity;
	StreamListFragment streamFragment;
	MapFragment mapFragment;
	ContactsListFragment contactListFragment;

	public AppSectionsPagerAdapter(FragmentManager fm, Activity activity) {
		super(fm);
		this.activity = activity;
		PackageManager packageManager = this.activity.getPackageManager();
		boolean backCamera = packageManager
				.hasSystemFeature(PackageManager.FEATURE_CAMERA);
		boolean frontCamera = packageManager
				.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
		if (backCamera == false && frontCamera == false) {
			itemCount = 2;
		}

	}

	@Override
	public Fragment getItem(int i) {
		if (itemCount == 2) {
			switch (i) {
			case 0:
				if (streamFragment == null)
					streamFragment = new StreamListFragment();
				return streamFragment;
			case 1:
				if(mapFragment == null)
					mapFragment = new MapFragment();
				return mapFragment;
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
				if (streamFragment == null)
					streamFragment = new StreamListFragment();
				return streamFragment;
			case 2:
				if(mapFragment == null)
					mapFragment = new MapFragment();
				return mapFragment;
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

		switch (position) {
		case 0:
			return this.activity.getString(R.string.contactListTitle);
		case 1:
			return this.activity.getString(R.string.streamListTitle);
		case 2:
			return this.activity.getString(R.string.mapTitle);
		default:
			break;
		}
		return super.getPageTitle(position);
	}
}