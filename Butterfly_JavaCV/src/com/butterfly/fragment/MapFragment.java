package com.butterfly.fragment;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.butterfly.ClientActivity;
import com.butterfly.MainActivity;
import com.butterfly.R;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.listeners.IStreamListUpdateListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements IStreamListUpdateListener  {

	private GoogleMap mMap;
	private HashMap<String, Stream> hashMap = new HashMap<String, StreamListFragment.Stream>();
	private Fragment fragment;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.activity_map, container, false);
		fragment = getFragmentManager().findFragmentById(R.id.map);
		mMap = ((SupportMapFragment) fragment).getMap();
		mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
			
			@Override
			public boolean onMarkerClick(Marker marker) {
				String id = marker.getId();
				if (hashMap.containsKey(id)) {
					Stream stream = hashMap.get(id);

					Intent intent = new Intent(getActivity().getApplicationContext(),
							ClientActivity.class);
					intent.putExtra(StreamListFragment.STREAM_PUBLISHED_NAME,
							stream.url);

					startActivity(intent);
				}
				return false;
			}
		});
		return rootView;
	}

	public Marker addMarker(double latitude, double longitude, String title) {
		if (latitude != 0 || longitude != 0) {
			Marker mapMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude))
				.title(title));
		
			return mapMarker;
		}
		return null;
	}

	@Override
	public void onResume() {
		super.onResume();
		ArrayList<Stream> streamList = ((MainActivity)getActivity()).getStreamList();
		streamListUpdated(streamList);
	}
	
	@Override
	public void onStart() {
		((MainActivity)getActivity()).registerStreamListListener(this);
		super.onStart();
	}
	
	@Override
	public void onStop() {
		((MainActivity)getActivity()).removeStreamListListener(this);
		super.onStop();
	}

	public void streamListUpdated(ArrayList<Stream> streamList) {
		if (mMap != null) {
			mMap.clear();
			hashMap.clear();
			for (final Stream stream : streamList) {
				Marker marker = addMarker(stream.latitude, stream.longitude, stream.name);
				if (marker != null) {
					hashMap.put(marker.getId(), stream);
				}
			}
		}

	}

}