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
	public ArrayList<Stream> streamList = new ArrayList<StreamListFragment.Stream>();    

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.activity_map, container, false);
		//this.addMarker(39.928958,32.798256,"Hello world");
		this.addMarkers();
		return rootView;
	}


	public Marker addMarker(double latitude, double longitude, String title) {


		mMap = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		//mMap.setOnMarkerClickListener(this);
		mapMarker = mMap.addMarker(new MarkerOptions()
		.position(new LatLng(latitude, longitude))
		.title(title));


		return mapMarker;

	}

	public void addMarkers() {

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


	/*public boolean onMarkerClick(final Marker marker) {
// TODO Auto-generated method stub
if (marker.equals(mapMarker)) 
        {
            //handle click here
Context context = getApplicationContext();
CharSequence text = "Hello toast!";
int duration = Toast.LENGTH_SHORT;

Toast toast = Toast.makeText(context, text, duration);
toast.show();
        }
return false;
}*/

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
							//addMarker
							streamList.add(new Stream(
									jsonObject.getString("name"), jsonObject
									.getString("url"), Integer
									.parseInt(jsonObject
											.getString("viewerCount")),
											Double .parseDouble(jsonObject.getString("latitude")),
											Double .parseDouble(jsonObject.getString("longitude")),
											Double .parseDouble(jsonObject.getString("altitude"))
									));
						}

						addMarkers();



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