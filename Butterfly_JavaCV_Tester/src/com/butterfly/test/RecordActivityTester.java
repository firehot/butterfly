package com.butterfly.test;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.EditText;

import com.butterfly.MainActivity;
import com.butterfly.MediaPlayerActivity;
import com.butterfly.R;
import com.butterfly.RecordActivity;
import com.butterfly.fragment.StreamListFragment;
import com.butterfly.utils.Utils;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class RecordActivityTester extends ActivityInstrumentationTestCase2 {

	private RecordActivity recordActivity;


	public RecordActivityTester() {
		super(RecordActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		recordActivity = (RecordActivity) getActivity();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		recordActivity = null;
	}
	
	/**
	 * 
	 */
	public void testLiveStreamReturnsFalse() {
		final EditText streamNameEditText = (EditText) recordActivity.findViewById(R.id.stream_name);
		final Button butRecorder = (Button) recordActivity.findViewById(R.id.recorder_control);
		
		recordActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				streamNameEditText.setText("auto-test");
				butRecorder.performClick();
			}
		});

		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		String streams = null;
		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {
			String url = recordActivity.getString(R.string.http_gateway_url);
			System.out.println(url);
			amfConnection.connect(url);
			String mails = Utils.getRegisteredMailList(recordActivity);
			streams = (String) amfConnection.call("getLiveStreams",mails);
		} catch (ClientStatusException e) {
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		}
		amfConnection.close();
		
		try {
			JSONArray jsonArray = new JSONArray(streams);
			System.out.println(streams);
			
			JSONObject jsonObject = (JSONObject) jsonArray.get(0);
			assertTrue(jsonObject.has("isLive"));
			assertTrue(jsonObject.getBoolean("isLive"));
			
			assertTrue(jsonObject.has("isPublic"));
			assertTrue(jsonObject.getBoolean("isPublic"));
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		recordActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				butRecorder.performClick();
			}
		});
		
	}

	
	public void testLiveStream() {

		final EditText streamNameEditText = (EditText) recordActivity.findViewById(R.id.stream_name);
		final Button butRecorder = (Button) recordActivity.findViewById(R.id.recorder_control);
		
		recordActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				streamNameEditText.setText("auto-test");
				butRecorder.performClick();
			}
		});

		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		recordActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				butRecorder.performClick();
			}
		});

		String streamUrl = recordActivity.getStreamURL();

		Intent i = new Intent(recordActivity, MediaPlayerActivity.class);
		i.putExtra(StreamListFragment.STREAM_PUBLISHED_NAME, streamUrl);
		recordActivity.startActivity(i);
		
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		fail("Have you watched the video? If you watch, it means this test passes");
	}


}
