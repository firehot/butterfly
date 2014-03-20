package com.butterfly.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.butterfly.R;

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
		
		
		setTabTextColor();
		return tabHost;
	}

	private void setTabTextColor() {
		for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {

	        final TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i)
	                .findViewById(android.R.id.title);

	            // Look for the title view to ensure this is an indicator and not a divider.(I didn't know, it would return divider too, so I was getting an NPE)
	        if (tv == null)
	            continue;
	        else
	            tv.setTextColor(Color.parseColor("#000000"));
	}
	}
	
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tabHost = null;
    }

}
