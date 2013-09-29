package com.butterfly;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.debug.BugSense;
import com.butterfly.message.CloudMessaging;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class StreamList extends ListActivity {

	public static final String STREAM_PUBLISHED_NAME = "stream-name";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	String httpGatewayURL;
	private ArrayAdapter<Stream> adapter;

	public class Stream {
		public String name;
		public String url;

		public Stream(String name, String url) {
			super();
			this.name = name;
			this.url = url;
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
			Toast.makeText(getApplicationContext(),
					adapter.getItem(position).name, Toast.LENGTH_SHORT).show();

			Intent intent = new Intent(getApplicationContext(),
					ClientActivity.class);
			intent.putExtra(STREAM_PUBLISHED_NAME,
					adapter.getItem(position).url);
			startActivity(intent);

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		BugSenseHandler.initAndStartSession(this, BugSense.API_KEY);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		httpGatewayURL = getString(R.string.http_gateway_url);

		adapter = new ArrayAdapter<Stream>(StreamList.this,
				android.R.layout.simple_list_item_1);
		setListAdapter(adapter);

		getListView().setOnItemClickListener(itemClickListener);

		// Check device for Play Services APK.
		if (checkPlayServices()) {

			CloudMessaging msg = new CloudMessaging(this.getApplicationContext(), this);
		}

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		BugSenseHandler.closeSession(this);
	}

	@Override
	protected void onResume() {
		new GetStreamListTask().execute(httpGatewayURL);
		super.onResume();
		checkPlayServices();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_client, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.broadcast_live:
			Intent i = new Intent(getApplicationContext(), ContactsList.class);
			startActivity(i);
			return true;
		case R.id.refresh:
			new GetStreamListTask().execute(httpGatewayURL);
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {

				finish();
			}
			return false;
		}
		return true;
	}

	public class GetStreamListTask extends
			AsyncTask<String, Void, HashMap<String, String>> {

		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			super.onPreExecute();
		}

		@Override
		protected HashMap<String, String> doInBackground(String... params) {
			HashMap<String, String> streams = null;
			AMFConnection amfConnection = new AMFConnection();
			amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
			try {
				System.out.println(params[0]);
				amfConnection.connect(params[0]);
				streams = (HashMap<String, String>) amfConnection
						.call("getLiveStreams");
				System.out.println("result count -> " + streams.size());

			} catch (ClientStatusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ServerStatusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			amfConnection.close();

			return streams;
		}

		@Override
		protected void onPostExecute(HashMap<String, String> streams) {
			adapter.clear();

			if (streams != null) {
				if (streams.size() > 0) {

					Set<Entry<String, String>> entrySet = streams.entrySet();

					for (Iterator iterator = entrySet.iterator(); iterator
							.hasNext();) {
						Entry<String, String> entry = (Entry<String, String>) iterator
								.next();
						adapter.add(new Stream(entry.getValue(), entry.getKey()));
					}

					adapter.notifyDataSetChanged();
				} else {
					Toast.makeText(getApplicationContext(),
							getString(R.string.noLiveStream), Toast.LENGTH_LONG)
							.show();
				}
			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.connectivityProblem),
						Toast.LENGTH_LONG).show();

			}
			setProgressBarIndeterminateVisibility(false);
			super.onPostExecute(streams);
		}

	}

}
