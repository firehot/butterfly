package com.butterfly.fragment;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.butterfly.ClientActivity;
import com.butterfly.MainActivity;
import com.butterfly.R;
import com.butterfly.fragment.StreamListFragment.Stream;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class MapFragment extends Fragment   {

	public static final String FRAGMENT_NAME = "MAP";
	private GoogleMap mMap;
	private Marker mapMarker;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.activity_map, container, false);
		return rootView;
	}
	public Marker addMarker(double latitude, double longitude, String title) {

		mMap = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		//mMap.setOnMarkerClickListener(this);
		mapMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude))
				.title(title));

		return mapMarker;
	}

	@Override
	public void onResume() {
		super.onResume();
		ArrayList<Stream> streamList = ((MainActivity)getActivity()).getStreamList();

		refreshStreamList(streamList);
	}


	public void refreshStreamList(ArrayList<Stream> streamList) {
		mMap.clear();
		for (final Stream stream : streamList) {
			Marker marker = addMarker(stream.latitude, stream.longitude, stream.name);

			mMap.setOnMarkerClickListener(new OnMarkerClickListener() {

				@Override
				public boolean onMarkerClick(Marker marker) {

					Intent intent = new Intent(getActivity().getApplicationContext(),
							ClientActivity.class);
					intent.putExtra(StreamListFragment.STREAM_PUBLISHED_NAME,
							stream.url);

					startActivity(intent);
					return false;
				}
			});
		}

	}
}