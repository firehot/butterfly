package com.butterfly.fragment;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.butterfly.ClientActivity;
import com.butterfly.MainActivity;
import com.butterfly.MediaPlayerActivity;
import com.butterfly.R;
import com.butterfly.adapter.StreamListAdapter;
import com.butterfly.listeners.IStreamListUpdateListener;
import com.butterfly.message.CloudMessaging;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class StreamListFragment extends ListFragment implements IStreamListUpdateListener{

	public static final String STREAM_PUBLISHED_NAME = "stream-name";
	private StreamListAdapter adapter;
	public static CloudMessaging msg;

	public static class Stream {
		public String name;
		public String url;
		public int viewerCount;
		public double latitude;
		public double longitude;
		public double altitude;
		public boolean isLive;

		public Stream(String name, String url, int viewerCount, double latitude, double longitude, double altitude
				,boolean isLive) {
			super();
			this.name = name;
			this.url = url;
			this.viewerCount = viewerCount;
			this.latitude = latitude;
			this.longitude = longitude;
			this.altitude = altitude;
			this.isLive = isLive;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	private OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long id) {
			Toast.makeText(getActivity(),
					adapter.getItem(position).name, Toast.LENGTH_SHORT).show();
			
			Stream s = adapter.getItem(position);

			if(s.isLive)
			{
				Intent intent = new Intent(getActivity().getApplicationContext(),
						ClientActivity.class);
				intent.putExtra(STREAM_PUBLISHED_NAME,
						adapter.getItem(position).url);
				startActivity(intent);
			}
			else
			{
				Intent intent = new Intent(getActivity().getApplicationContext(),
						MediaPlayerActivity.class);
				intent.putExtra(STREAM_PUBLISHED_NAME,
						adapter.getItem(position).url);
				startActivity(intent);
			}


		}
	};
	private ArrayList<Stream> streamList;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		View v = inflater.inflate(R.layout.main, container, false);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		streamList = ((MainActivity)getActivity()).getStreamList();
		adapter = new StreamListAdapter(getActivity());

		setListAdapter(adapter);
		getListView().setOnItemClickListener(itemClickListener);
		
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

	public void streamListUpdated(ArrayList<Stream> streamList)
	{
		if (adapter != null) {
			adapter.clear();
			if (streamList != null) {
				adapter.addAll(streamList);
			}
			adapter.notifyDataSetChanged();
		}
	}

}
