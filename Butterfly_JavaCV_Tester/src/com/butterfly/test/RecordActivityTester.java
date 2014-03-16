package com.butterfly.test;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.EditText;

import com.butterfly.MediaPlayerActivity;
import com.butterfly.R;
import com.butterfly.RecordActivity;
import com.butterfly.fragment.StreamListFragment;

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
