package com.butterfly;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.adapter.StreamListAdapter;
import com.butterfly.debug.BugSense;
import com.butterfly.message.CloudMessaging;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class StreamList extends ListActivity {

	private static final String SHARED_PREFERENCE_FIRST_INSTALLATION = "firstInstallation";
	private static final String APP_SHARED_PREFERENCES = "applicationDetails";
	public static final String STREAM_PUBLISHED_NAME = "stream-name";
	String httpGatewayURL;
	private StreamListAdapter adapter;
	public static CloudMessaging msg;

	public static class Stream {
		public String name;
		public String url;
		public int viewerCount;

		public Stream(String name, String url, int viewerCount) {
			super();
			this.name = name;
			this.url = url;
			this.viewerCount = viewerCount;
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

		adapter = new StreamListAdapter(StreamList.this);
		setListAdapter(adapter);

		getListView().setOnItemClickListener(itemClickListener);

		final SharedPreferences applicationPrefs = getSharedPreferences(
				APP_SHARED_PREFERENCES, MODE_PRIVATE);
		Boolean firstInstallation = applicationPrefs.getBoolean(
				SHARED_PREFERENCE_FIRST_INSTALLATION, false);
		if (!firstInstallation) {
			showTermsOfUse(applicationPrefs);
		}

		// Check device for Play Services APK.
		if (checkPlayServices()) {

			CloudMessaging msg = new CloudMessaging(
					this.getApplicationContext(), this, httpGatewayURL);
		}
	}

	private void showTermsOfUse(final SharedPreferences applicationPrefs) {
		AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
		myAlertDialog.setTitle(R.string.terms_of_service_title);
		myAlertDialog.setMessage(R.string.terms_of_service);
		myAlertDialog.setPositiveButton(R.string.accept,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface arg0, int arg1) {
						// do something when the OK button is clicked
						SharedPreferences.Editor mInstallationEditor = applicationPrefs
								.edit();
						mInstallationEditor.putBoolean(
								SHARED_PREFERENCE_FIRST_INSTALLATION, true);
						mInstallationEditor.commit();
					}
				});
		myAlertDialog.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface arg0, int arg1) {
						// do something when the Cancel button is clicked
						StreamList.this.finish();
					}
				});
		myAlertDialog.show();
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
						CloudMessaging.PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {

				finish();
			}
			return false;
		}
		return true;
	}

	public class GetStreamListTask extends AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
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
													.getString("viewerCount"))));

						}

						adapter.notifyDataSetChanged();
					} else {
						Toast.makeText(getApplicationContext(),
								getString(R.string.noLiveStream),
								Toast.LENGTH_LONG).show();
					}

				} catch (JSONException e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(),
							getString(R.string.connectivityProblem),
							Toast.LENGTH_LONG).show();
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
