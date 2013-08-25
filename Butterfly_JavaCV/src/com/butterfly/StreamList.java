package com.butterfly;

import java.util.Iterator;
import java.util.List;

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
import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;


public class StreamList extends ListActivity {

	
	public static final String STREAM_NAME = "stream-name";


	String httpGatewayURL;
			
			
	private ArrayAdapter<String> adapter;


	private OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long id) {
			Toast.makeText(getApplicationContext(), adapter.getItem(position), Toast.LENGTH_SHORT).show();
		
			Intent intent = new Intent(getApplicationContext(), ClientActivity.class);
			intent.putExtra(STREAM_NAME, adapter.getItem(position));
			startActivity(intent);
			
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		httpGatewayURL = getString(R.string.http_gateway_url);
		
		adapter = new ArrayAdapter<String>(StreamList.this, android.R.layout.simple_list_item_1);
		setListAdapter(adapter);
		
		getListView().setOnItemClickListener(itemClickListener );
		
		
	}
	
	@Override
	protected void onResume() {
		new GetStreamListTask().execute(httpGatewayURL);
		super.onResume();
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
			Intent i = new Intent(getApplicationContext(), RecordActivity.class);
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
	
	
	public class GetStreamListTask extends AsyncTask<String, Void, List<String>> {

		
		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			super.onPreExecute();
		}
		
		@Override
		protected List<String> doInBackground(String... params) {
			List<String> result = null;
			AMFConnection amfConnection = new AMFConnection();			
	    	amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
	    	try {
	    		System.out.println(params[0]);
	    		amfConnection.connect(params[0]);
	    		result = (List<String>) amfConnection.call("getLiveStreams");
				System.out.println("result count -> " + result.size());
				
			} catch (ClientStatusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ServerStatusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	amfConnection.close();
			
			
			return result;
		}
		
		@Override
		protected void onPostExecute(List<String> result) {
			adapter.clear();
			for (Iterator<String> iterator = result.iterator(); iterator.hasNext();) {
				adapter.add(iterator.next());
			}
			adapter.notifyDataSetChanged();
			setProgressBarIndeterminateVisibility(false);
			super.onPostExecute(result);
		}
		
		
		
	}

}
