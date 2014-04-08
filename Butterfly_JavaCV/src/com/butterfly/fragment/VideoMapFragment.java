package com.butterfly.fragment;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.butterfly.MainActivity;
import com.butterfly.MediaPlayerActivity;
import com.butterfly.R;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.butterfly.listeners.IStreamListUpdateListener;
import com.butterfly.utils.LocationProvider;
import com.butterfly.view.BTVMarker;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class VideoMapFragment extends Fragment implements IStreamListUpdateListener,OnMarkerClickListener,OnInfoWindowClickListener  {

	private GoogleMap mMap;
	private HashMap<String, BTVMarker> hashMap = new HashMap<String, BTVMarker>();
	private SupportMapFragment fragment;
	private MapView mpView;
	private static View view;
	LocationProvider locationProvider;
	public static Location currentLocation = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (view != null) {
			ViewGroup parent = (ViewGroup) view.getParent();
			if (parent != null)
				parent.removeView(view);
		}
		try {
			view = inflater.inflate(R.layout.view_map, container, false);
		} catch (InflateException e) {
			/* map is already there, just return view as it is */
		}

		mpView = (MapView)view.findViewById(R.id.map);
		mpView.onCreate(savedInstanceState);

		try {
		     MapsInitializer.initialize(this.getActivity());
		 } catch (GooglePlayServicesNotAvailableException e) {
		     e.printStackTrace();
		 }
		return view;
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
		mpView.onResume();
		mMap = mpView.getMap();
		// Check if we were successful in obtaining the map.
		if (mMap != null) {
			// The Map is verified. It is now safe to manipulate the map.
			mMap.setOnMarkerClickListener(this);
			mMap.setOnInfoWindowClickListener(this);
			
			locationProvider = new LocationProvider(this.getActivity());
			locationProvider.getLocation(new LocationListener() {

					@Override
					public void onLocationChanged(Location location) {
						currentLocation = location;
						updateCameraPosition(location);
					}
				});

			if(currentLocation != null)
			{
				updateCameraPosition(currentLocation);
			}
			ArrayList<Stream> streamList = ((MainActivity)getActivity()).getStreamList();
			streamListUpdated(streamList);
		}

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
				BTVMarker btvMarker = new BTVMarker(marker, stream);
				if (marker != null) {
					hashMap.put(marker.getId(), btvMarker);
				}
			}
		}

	}
	
	public void updateCameraPosition(Location location)
	{
		mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(
				location.getLatitude(), location.getLongitude())));
		mMap.animateCamera(CameraUpdateFactory.zoomTo(6));

	}

	@Override
    public boolean onMarkerClick(Marker marker) {
		
		String id = marker.getId();
		if (hashMap.containsKey(id)) {
		
			BTVMarker btvmarker = hashMap.get(id);
			if(btvmarker.isVisible())
			{
				btvmarker.hideInfoWindow();
			}
			else
			{
				btvmarker.showInfoWindow();
			}
		}
        return true;
    }

	@Override
	public void onInfoWindowClick(Marker marker) {
		
		String id = marker.getId();
		if (hashMap.containsKey(id)) {
			BTVMarker btvmarker = hashMap.get(id);
			Stream stream = btvmarker.getStream();
			
			Intent intent = new Intent(getActivity().getApplicationContext(),
					MediaPlayerActivity.class);
			intent.putExtra(StreamListFragment.STREAM_PUBLISHED_NAME,
					stream.url);
			intent.putExtra(StreamListFragment.STREAM_IS_LIVE, stream.isLive);
			startActivity(intent);

		}
	}

}