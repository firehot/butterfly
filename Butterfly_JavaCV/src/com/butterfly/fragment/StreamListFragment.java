package com.butterfly.fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.Int2;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.butterfly.ClientActivity;
import com.butterfly.R;
import com.butterfly.R.id;
import com.butterfly.R.layout;
import com.butterfly.R.string;
import com.butterfly.adapter.StreamListAdapter;
import com.butterfly.message.CloudMessaging;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class StreamListFragment extends ListFragment {

	public static final String STREAM_PUBLISHED_NAME = "stream-name";
	public static final String FRAGMENT_NAME = "STREAM LIST";
	String httpGatewayURL;
	private StreamListAdapter adapter;
	public static CloudMessaging msg;
	
	public static class Stream {
		public String name;
		public String url;
		public int viewerCount;
		public double latitude;
		public double longitude;
		public double altitude;

		public Stream(String name, String url, int viewerCount, double latitude, double longitude, double altitude) {
			super();
			this.name = name;
			this.url = url;
			this.viewerCount = viewerCount;
			this.latitude = latitude;
			this.longitude = longitude;
			this.altitude = altitude;
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

			Intent intent = new Intent(getActivity().getApplicationContext(),
					ClientActivity.class);
			intent.putExtra(STREAM_PUBLISHED_NAME,
					adapter.getItem(position).url);
			startActivity(intent);

		}
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState){
		
		View v = inflater.inflate(R.layout.main, container, false);
		return v;

	}
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
    		httpGatewayURL = getString(R.string.http_gateway_url);

    		adapter = new StreamListAdapter(StreamListFragment.this.getActivity());
    		
    		StreamListFragment.this.setListAdapter(adapter);
    		getListView().setOnItemClickListener(itemClickListener);

    		// Check device for Play Services APK.
    		if (checkPlayServices(getActivity())) {

    			CloudMessaging msg = new CloudMessaging(
    					this.getActivity().getApplicationContext(), getActivity(), httpGatewayURL);
    		}
    		new GetStreamListTask().execute(httpGatewayURL);

    		new GetStreamListTask().execute(httpGatewayURL);
    }

	@Override
	public void onResume() {
		super.onResume();
		checkPlayServices(this.getActivity());
	}
	
	public void refreshStreamList()
	{
		new GetStreamListTask().execute(httpGatewayURL);
	}
	
	public static boolean checkPlayServices(Activity activity) {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(activity);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
						CloudMessaging.PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				activity.finish();
			}
			return false;
		}

		return true;
	}

	public class GetStreamListTask extends AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {
			getActivity().setProgressBarIndeterminateVisibility(true);
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {
			String streams = null;
			AMFConnection amfConnection = new AMFConnection();
			amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
			try {
				System.out.println(params[0]);
				amfConnection.connect(params[0]);
				streams = (String) amfConnection.call("getLiveStreams");
			} catch (ClientStatusException e) {
				e.printStackTrace();
			} catch (ServerStatusException e) {
				e.printStackTrace();
			}
			amfConnection.close();

			return streams;
		}

		@Override
		protected void onPostExecute(String streams) {
			adapter.clear();

			if (streams != null) {
				// JSONObject jsonObject = new JSONObject(streams);
				JSONArray jsonArray;
				try {
					jsonArray = new JSONArray(streams);
					int length = jsonArray.length();
					if (length > 0) {

						JSONObject jsonObject;

						for (int i = 0; i < length; i++) {
							jsonObject = (JSONObject) jsonArray.get(i);
							adapter.add(new Stream(
									jsonObject.getString("name"), jsonObject
											.getString("url"), Integer
											.parseInt(jsonObject
													.getString("viewerCount")),
													Double .parseDouble(jsonObject.getString("latitude")),
													Double .parseDouble(jsonObject.getString("longitude")),
													Double .parseDouble(jsonObject.getString("altitude"))
													));

						}

						adapter.notifyDataSetChanged();
					} else {
						Toast.makeText(getActivity().getApplicationContext(),
								getString(R.string.noLiveStream),
								Toast.LENGTH_LONG).show();
					}

				} catch (JSONException e) {
					e.printStackTrace();
					Toast.makeText(getActivity().getApplicationContext(),
							getString(R.string.connectivityProblem),
							Toast.LENGTH_LONG).show();
				}

			} else {
				Toast.makeText(getActivity().getApplicationContext(),
						getString(R.string.connectivityProblem),
						Toast.LENGTH_LONG).show();

			}
			getActivity().setProgressBarIndeterminateVisibility(false);
			super.onPostExecute(streams);
		}
	}

}
