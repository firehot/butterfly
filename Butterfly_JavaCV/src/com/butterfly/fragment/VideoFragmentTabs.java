package com.butterfly.fragment;

import com.butterfly.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class VideoFragmentTabs extends Fragment {
	
	private FragmentTabHost tabHost;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		tabHost = new FragmentTabHost(getActivity());
		
		tabHost.setup(getActivity(), getChildFragmentManager(), R.id.fragmenttab);
		
		
		
		
		tabHost.addTab(tabHost.newTabSpec("list").setIndicator(getString(R.string.list)),
	                StreamListFragment.class, null);
		tabHost.addTab(tabHost.newTabSpec("map").setIndicator(getString(R.string.map)), 
					VideoMapFragment.class, null);
		
		return tabHost;
	}
	
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tabHost = null;
    }

}
