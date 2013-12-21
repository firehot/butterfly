package com.butterfly.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.butterfly.R;
import com.butterfly.R.layout;
import com.google.android.gms.maps.SupportMapFragment;


public class MapFragment extends Fragment {

	public static final String FRAGMENT_NAME = "MAP";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.activity_map, container, false);

		return rootView;
	}

}
