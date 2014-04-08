package com.butterfly;

import static com.googlecode.javacv.cpp.avcodec.AV_CODEC_ID_H264;

import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.debug.BugSense;
import com.butterfly.fragment.ContactsListFragment;
import com.butterfly.message.GcmIntentService;
import com.butterfly.recorder.FFmpegFrameRecorder;
import com.butterfly.tasks.SendPreviewTask;
import com.butterfly.utils.LocationProvider;
import com.butterfly.utils.Utils;
import com.butterfly.view.CameraView;
import com.google.android.gms.location.LocationListener;
import com.googlecode.javacpp.BytePointer;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class RecordActivity extends Activity implements OnClickListener,
		PreviewCallback {

	private final static String CLASS_LABEL = "RecordActivity";
	private final static String LOG_TAG = CLASS_LABEL;

	private String ffmpeg_link;
	// private String ffmpeg_link = "/mnt/sdcard/stream.flv";

	long startTime = 0;
	boolean recording = false;

	private volatile FFmpegFrameRecorder recorder;

	private int sampleAudioRateInHz = 44100;
	private int frameRate = 30;

	/* audio data getting thread */
	private AudioRecord audioRecord;
	private AudioRecordRunnable audioRecordRunnable;
	private Thread audioThread;
	volatile boolean runAudioThread = true;

	/* video data getting thread */
	private Camera cameraDevice;
	private CameraView cameraView;

	// private IplImage yuvIplimage = null;

	private Button btnRecorderControl;
	private String httpGatewayURL;
	private String streamURL;
	private EditText streamNameEditText;
	private CheckBox publicVideoCheckBox;

	private static ProgressDialog mProgressDialog;
	private String mailsToBeNotified = null;
	private Size previewSize;
	private BytePointer bytePointer;
	private boolean snapshotSent = false;

	BroadcastReceiver viewerCountReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			int viewCount = Integer.parseInt(intent.getExtras().getString(
					GcmIntentService.VIEWER_COUNT));
			if (viewCount == 0) {
				viewerCountView.setVisibility(View.INVISIBLE);
			} else {
				viewerCountView.setText(getResources().getQuantityString(
						R.plurals.viewers_count, viewCount, viewCount));
				viewerCountView.setVisibility(View.VISIBLE);
			}

		}
	};
	private LocalBroadcastManager localBroadcastManager;
	private TextView viewerCountView;
	private String streamName;
	private boolean is_video_public;
	private NetworkInfo activeNetwork;
	private LocationProvider locationProvider;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PackageManager packageManager = getPackageManager();
		boolean backCamera = packageManager
				.hasSystemFeature(PackageManager.FEATURE_CAMERA);
		boolean frontCamera = packageManager
				.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

		if (backCamera == false && frontCamera == false) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.nocamera), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		BugSenseHandler.initAndStartSession(this, BugSense.API_KEY);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// Hide title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);



		ffmpeg_link = getString(R.string.rtmp_url);



		setContentView(R.layout.activity_record);

		httpGatewayURL = getString(R.string.http_gateway_url);
		localBroadcastManager = LocalBroadcastManager
				.getInstance(getApplicationContext());
		locationProvider = new LocationProvider(this);

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		activeNetwork = cm.getActiveNetworkInfo();
		initLayout();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Bundle extras = getIntent().getExtras();
		
		publicVideoCheckBox.setChecked(true);
		if (extras != null
				&& extras.containsKey(ContactsListFragment.MAILS_TO_BE_NOTIFIED)) {
			mailsToBeNotified = getIntent().getExtras().getString(
					ContactsListFragment.MAILS_TO_BE_NOTIFIED);
			publicVideoCheckBox.setChecked(false);
		}
		
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopRecording();
		finish();

		if (cameraView != null) {
			cameraView.stopPreview();
			cameraDevice.setPreviewCallback(null);
			cameraDevice.release();
			cameraDevice = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		recording = false;
		runAudioThread = false;
		if (recorder != null) {
			try {
				recorder.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
			recorder = null;
		}
		BugSenseHandler.closeSession(this);

	}

	private void getLocation() {

		locationProvider.getLocation(new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {

				new RegisterLocationForStreamTask(httpGatewayURL,
						RecordActivity.this.streamURL).execute(
						location.getLongitude(), location.getLatitude(),
						location.getAltitude());

			}
		});
	}

	private void initLayout() {

		streamNameEditText = (EditText) findViewById(R.id.stream_name);
		publicVideoCheckBox = (CheckBox) findViewById(R.id.check_public);
		
		/* add control button: start and stop */
		btnRecorderControl = (Button) findViewById(R.id.recorder_control);
		btnRecorderControl.setBackgroundResource(R.drawable.bt_start_record);
		btnRecorderControl.setOnClickListener(this);

		viewerCountView = (TextView) findViewById(R.id.viewerCountView);

		viewerCountView.setVisibility(View.INVISIBLE);

		cameraView = (CameraView) findViewById(R.id.cam);
		cameraDevice = Camera.open(0);
		Parameters parameters = cameraDevice.getParameters();
		List<Size> sizes = parameters.getSupportedPreviewSizes();
		parameters.setPreviewSize(sizes.get(0).width, sizes.get(0).height);

	//	parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
	//	parameters.setSceneMode(Parameters.SCENE_MODE_AUTO);

		cameraDevice.setParameters(parameters);
		cameraView.setCamera(cameraDevice);

		Log.i(LOG_TAG, "cameara preview start: OK");
	}

	public boolean setCameraPreviewSize() {
		boolean result = false;
		if(cameraDevice == null)
			return result;
		Parameters parameters = cameraDevice.getParameters();
		List<Size> supportedPreviewSizes = parameters
				.getSupportedPreviewSizes();
		int likelyWidth = 0, likelyHeight = 0;

		int type = activeNetwork.getType();

		likelyWidth = 176;
		likelyHeight = 144;
		
		if (likelyWidth != 0 && likelyHeight != 0) {
			Size size = findPreviewSize(supportedPreviewSizes, likelyWidth,
					likelyHeight);
			parameters.setPreviewSize(size.width, size.height);
//			parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
//			parameters.setSceneMode(Parameters.SCENE_MODE_AUTO);
			
			if (cameraView.isPreviewOn()) {
				cameraDevice.stopPreview();
				cameraDevice.setParameters(parameters);
				cameraDevice.startPreview();
			} else {
				cameraDevice.setParameters(parameters);
			}
			result = true;
		}
		return result;
	}

	private Size findPreviewSize(List<Size> previewSizes, int width, int height) {
		int diff = Integer.MAX_VALUE;
		Size bestSize = previewSizes.get(0);

		for (Size size : previewSizes) {
			if (size.width <= width && size.height <= height) {
				int width2 = (int) Math.pow(width - size.width, 2);
				int height2 = (int) Math.pow(height - size.height, 2);
				int sizeDiff = (int) Math.sqrt(width2 + height2);
				if (sizeDiff < diff) {
					diff = sizeDiff;
					bestSize = size;
				}
			}
		}

		return bestSize;

	}

	// ---------------------------------------
	// initialize ffmpeg_recorder
	// ---------------------------------------
	private void initRecorder() {

		Log.w(LOG_TAG, "init recorder");
		// if link is set, dont set it again
		ffmpeg_link= getString(R.string.rtmp_url);
		streamURL = String.valueOf((int) (Math.random() * 100000)
					+ System.currentTimeMillis());
		ffmpeg_link += streamURL;
		
		Log.i(LOG_TAG, "ffmpeg_url: " + ffmpeg_link);

		createRecorder();

		Log.i(LOG_TAG, "recorder initialize success");

		runAudioThread = true;
		audioRecordRunnable = new AudioRecordRunnable();
		audioThread = new Thread(audioRecordRunnable);
		audioThread.start();
	}

	private void createRecorder() {
		// if recorder is created , dont create it again
		if (recorder == null) {
			Size previewSize = cameraDevice.getParameters().getPreviewSize();
			recorder = new FFmpegFrameRecorder(ffmpeg_link, previewSize.width,
					previewSize.height, 1);
		}
		recorder.setFormat("flv");
		recorder.setSampleRate(sampleAudioRateInHz);
		recorder.setVideoCodec(AV_CODEC_ID_H264);
		recorder.setVideoQuality(23);
		// Set in the surface changed method
		recorder.setFrameRate(frameRate);
	}

	public boolean startRecording() {

		if (cameraDevice != null && cameraDevice.getParameters() != null)
			previewSize = cameraDevice.getParameters().getPreviewSize();
		else
			return false;

		recording = false;
		IntentFilter intentFilter = new IntentFilter(
				GcmIntentService.ACTION_VIEWER_COUNT_UPDATED);
		localBroadcastManager.registerReceiver(viewerCountReceiver,
				intentFilter);

		try {
			if (recorder == null)
				createRecorder();

			cameraDevice.setPreviewCallback(this);
			recorder.start();
			startTime = System.currentTimeMillis();

			recording = true;

		} catch (FFmpegFrameRecorder.Exception e) {
			e.printStackTrace();
		}
		return recording;
	}

	public void stopRecording() {

		viewerCountView.setVisibility(View.INVISIBLE);
		runAudioThread = false;

		if (recorder != null && recording) {
			StopRecordingTask stopTask = new StopRecordingTask();
			stopTask.execute(recorder);
			recorder = null;
		}

		recording = false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (recording) {
				stopRecording();
			}

			finish();

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	// ---------------------------------------------
	// audio thread, gets and encodes audio data
	// ---------------------------------------------
	class AudioRecordRunnable implements Runnable {

		@Override
		public void run() {
			android.os.Process
					.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

			// Audio
			int bufferSize;
			short[] audioData;
			int bufferReadResult;

			bufferSize = AudioRecord
					.getMinBufferSize(sampleAudioRateInHz,
							AudioFormat.CHANNEL_IN_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					sampleAudioRateInHz, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, bufferSize);

			audioData = new short[bufferSize];

			Log.d(LOG_TAG, "audioRecord.startRecording()");
			audioRecord.startRecording();

			/* ffmpeg_audio encoding loop */
			while (runAudioThread) {
				// Log.v(LOG_TAG, "recording? " + recording);
				bufferReadResult = audioRecord.read(audioData, 0,
						audioData.length);
				if (bufferReadResult > 0) {
					// Log.v(LOG_TAG,"bufferReadResult: " + bufferReadResult);
					// If "recording" isn't true when start this thread, it
					// never get's set according to this if statement...!!!
					// Why? Good question...
					if (recording) {
						try {
							Buffer[] buffer = new Buffer[1];
							buffer[0] = ShortBuffer.wrap(audioData, 0,
									bufferReadResult);
							recorder.record(buffer);
							// Log.v(LOG_TAG,"recording " + 1024*i + " to " +
							// 1024*i+1024);
						} catch (FFmpegFrameRecorder.Exception e) {
							// Log.v(LOG_TAG, e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
			Log.v(LOG_TAG, "AudioThread Finished, release audioRecord");

			/* encoding finish, release recorder */

			stopAudioRecording();
		}

	}

	private void stopAudioRecording() {

		if (audioRecord != null) {
			audioRecord.stop();
			audioRecord.release();
			audioRecord = null;
			audioThread = null;
			audioRecordRunnable = null;
			Log.v(LOG_TAG, "audioRecord released");
		}
	}

	@Override
	public void onClick(View v) {
		
		if (!recording) {
			
			btnRecorderControl.setClickable(false);

			streamName = streamNameEditText.getText().toString();

			// Check the video is public or not
			is_video_public = publicVideoCheckBox.isChecked();

			if (streamName != null && streamName.length() > 0) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(
						streamNameEditText.getWindowToken(), 0);
				
				if (setCameraPreviewSize() == true) {

					initRecorder();
					new RegisterStreamTask().execute(httpGatewayURL, streamName,
							streamURL,
							Utils.getMailList(RecordActivity.this));
				} 

			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				// builder.setPositiveButton(R.string.ok, this);
				builder.setTitle(R.string.error);
				builder.setMessage(R.string.write_name_of_stream).show();
			}

		} else {
			// This will trigger the audio recording loop to stop and then set
			// isRecorderStart = false;
			btnRecorderControl
					.setBackgroundResource(R.drawable.bt_start_record);
			streamNameEditText.setVisibility(View.VISIBLE);
			publicVideoCheckBox.setVisibility(View.VISIBLE);
			stopRecording();
			runAudioThread = false;
			Log.w(LOG_TAG, "Stop Button Pushed");
		}
	}

	public class RegisterStreamTask extends AsyncTask<String, Void, Boolean> {

		String possibleMail;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			if (mProgressDialog == null) {
				mProgressDialog = ProgressDialog.show(RecordActivity.this,
						null, getString(R.string.initializing), true);

			} else {
				mProgressDialog.setMessage(getString(R.string.initializing));
			}

		}

		@Override
		protected Boolean doInBackground(String... params) {
			Boolean result = false;

			boolean recordingStarted = startRecording();
			if (recordingStarted == true) {
				this.possibleMail = params[3];

				AMFConnection amfConnection = new AMFConnection();
				amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
				try {
					System.out.println(params[0]);

					amfConnection.connect(params[0]);
					result = (Boolean) amfConnection.call("registerLiveStream",
							params[1], params[2], mailsToBeNotified,
							possibleMail, is_video_public, Locale.getDefault()
									.getISO3Language());

				} catch (ClientStatusException e) {
					e.printStackTrace();
				} catch (ServerStatusException e) {

					e.printStackTrace();
				}
				amfConnection.close();
			} else {
				stopRecording();
			}

			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(mProgressDialog != null)
				mProgressDialog.dismiss();
			if (result == true) {
				Log.w(LOG_TAG, "Start Button Pushed");
				streamNameEditText.setVisibility(View.GONE);
				publicVideoCheckBox.setVisibility(View.GONE);
				btnRecorderControl
						.setBackgroundResource(R.drawable.bt_stop_record);
			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.stream_registration_failed),
						Toast.LENGTH_LONG).show();
				streamNameEditText.setVisibility(View.VISIBLE);
				publicVideoCheckBox.setVisibility(View.VISIBLE);
			}

			mProgressDialog = null;
			btnRecorderControl.setClickable(true);
			RecordActivity.this.getLocation();
			super.onPostExecute(result);

		}
	}

	public class RegisterLocationForStreamTask extends
			AsyncTask<Double, Void, Boolean> {
		String httpGateway;
		private String streamURL;

		public RegisterLocationForStreamTask(String httpGateway, String url) {
			this.httpGateway = httpGateway;
			this.streamURL = url;
		}

		@Override
		protected Boolean doInBackground(Double... params) {
			Boolean result = false;

			AMFConnection amfConnection = new AMFConnection();
			amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
			try {
				System.out.println(params[0]);
				System.out.println(params[1]);
				System.out.println(params[2]);

				amfConnection.connect(this.httpGateway);
				result = (Boolean) amfConnection.call(
						"registerLocationForStream", this.streamURL, params[0],
						params[1], params[2]);

			} catch (ClientStatusException e) {
				e.printStackTrace();
			} catch (ServerStatusException e) {

				e.printStackTrace();
			}
			amfConnection.close();
			return result;
		}

	}

	public class StopRecordingTask extends
			AsyncTask<FFmpegFrameRecorder, Void, Void> {

		@Override
		protected Void doInBackground(FFmpegFrameRecorder... params) {
			try {
				localBroadcastManager.unregisterReceiver(viewerCountReceiver);
				cameraDevice.setPreviewCallback(null);
				params[0].stop();
			} catch (Exception e) {
			}
			return null;
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera arg1) {
		if (recording) {

			if (bytePointer == null) {
				bytePointer = new BytePointer(data.length);
			}
			bytePointer.put(data);

			try {
				long t = 1000 * (System.currentTimeMillis() - startTime);
				if (t > recorder.getTimestamp()) {
					recorder.setTimestamp(t);
				}
				recorder.record(bytePointer, previewSize.width,
						previewSize.height);
			} catch (FFmpegFrameRecorder.Exception e) {

				e.printStackTrace();
			}

			if (!snapshotSent) {
				// send a preview snapshot image to the red5 only for first time
				snapshotSent = true;
				Size size = arg1.getParameters().getPreviewSize();
				SendPreviewTask task = new SendPreviewTask(size.width,
						size.height);
				task.execute(httpGatewayURL, data, streamURL);
			}

		}

	}
	
	public String getStreamURL() {
		return streamURL;
	}

}
