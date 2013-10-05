package com.butterfly;

import static com.googlecode.javacv.cpp.avcodec.AV_CODEC_ID_H264;

import java.nio.Buffer;
import java.nio.ShortBuffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.debug.BugSense;
import com.butterfly.listener.OnPreviewListener;
import com.butterfly.recorder.FFmpegFrameRecorder;
import com.butterfly.view.CameraView;
import com.googlecode.javacpp.BytePointer;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class RecordActivity extends Activity implements OnClickListener,
		OnPreviewListener, android.content.DialogInterface.OnClickListener {

	private final static String CLASS_LABEL = "RecordActivity";
	private final static String LOG_TAG = CLASS_LABEL;

	private PowerManager.WakeLock mWakeLock;

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

	private ProgressDialog m_ProgressDialog;
	private String mailsToBeNotified;
	private Size previewSize;
	private BytePointer bytePointer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		BugSenseHandler.initAndStartSession(this, BugSense.API_KEY);

		ffmpeg_link = getString(R.string.rtmp_url);

		Bundle extras = getIntent().getExtras();
		if (extras != null
				&& extras.containsKey(ContactsList.MAILS_TO_BE_NOTIFIED)) {
			mailsToBeNotified = getIntent().getExtras().getString(
					ContactsList.MAILS_TO_BE_NOTIFIED);
		}

		// Hide title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		setContentView(R.layout.main);

		httpGatewayURL = getString(R.string.http_gateway_url);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
				CLASS_LABEL);
		mWakeLock.acquire();

		initLayout();

		streamNameEditText = (EditText) findViewById(R.id.stream_name);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
					CLASS_LABEL);
			mWakeLock.acquire();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		BugSenseHandler.closeSession(this);

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

		if (cameraView != null) {
			cameraView.stopPreview();
			cameraDevice.release();
			cameraDevice = null;
		}

		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}

	private void initLayout() {

		/* add control button: start and stop */
		btnRecorderControl = (Button) findViewById(R.id.recorder_control);
		btnRecorderControl.setText("Start");
		btnRecorderControl.setOnClickListener(this);

		cameraView = (CameraView) findViewById(R.id.cam);
		cameraDevice = Camera.open(0);
		cameraView.setCamera(cameraDevice);

		Log.i(LOG_TAG, "cameara preview start: OK");
	}

	// ---------------------------------------
	// initialize ffmpeg_recorder
	// ---------------------------------------
	private void initRecorder() {

		Log.w(LOG_TAG, "init recorder");
		// if link is set, dont set it again
		if (ffmpeg_link.equals(getString(R.string.rtmp_url))) {
			streamURL = String.valueOf((int) (Math.random() * 100000)
					+ System.currentTimeMillis());
			ffmpeg_link += streamURL;
		}
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
		recorder.setVideoQuality(36);
		// Set in the surface changed method
		recorder.setFrameRate(frameRate);
	}

	public void startRecording() {

		previewSize = cameraDevice.getParameters().getPreviewSize();

		try {
			if (recorder == null)
				createRecorder();

			recorder.start();
			startTime = System.currentTimeMillis();

			recording = true;

		} catch (FFmpegFrameRecorder.Exception e) {
			e.printStackTrace();
		}
	}

	public void stopRecording() {

		runAudioThread = false;

		if (recorder != null && recording) {
			StopRecordingTask stopTask = new StopRecordingTask();
			stopTask.execute(recorder);
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
				Log.v(LOG_TAG, "recording? " + recording);
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
							Log.v(LOG_TAG, e.getMessage());
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

			String name = streamNameEditText.getText().toString();
			if (name != null && name.length() > 0) {

				m_ProgressDialog = ProgressDialog.show(this, "Please Wait",
						"Initializing...", true);
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(
						streamNameEditText.getWindowToken(), 0);
				streamNameEditText.setVisibility(View.GONE);
				initRecorder();
				new RegisterStreamTask().execute(httpGatewayURL, name,
						streamURL);
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setPositiveButton("OK", this);
				builder.setTitle("Error");
				builder.setMessage("Please enter a name...").show();
			}

		} else {
			// This will trigger the audio recording loop to stop and then set
			// isRecorderStart = false;
			m_ProgressDialog = ProgressDialog.show(this, "Please Wait",
					"Stopping...", true);
			stopRecording();
			runAudioThread = false;
			Log.w(LOG_TAG, "Stop Button Pushed");
			btnRecorderControl.setText("Start");
			streamNameEditText.setVisibility(View.VISIBLE);

		}
	}

	@Override
	public void onPreviewChanged(byte[] data) {

		if (/* bytePointer != null && */recording) {
			// yuvIplimage.getByteBuffer().put(data);
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
		}

	}

	public class RegisterStreamTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(String... params) {
			Boolean result = false;
			AMFConnection amfConnection = new AMFConnection();
			amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
			try {
				System.out.println(params[0]);
				amfConnection.connect(params[0]);
				result = (Boolean) amfConnection.call("registerLiveStream",
						params[1], params[2]);

			} catch (ClientStatusException e) {
				e.printStackTrace();
			} catch (ServerStatusException e) {

				e.printStackTrace();
			}
			amfConnection.close();

			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result == true) {
				startRecording();
				Log.w(LOG_TAG, "Start Button Pushed");
				btnRecorderControl.setText("Stop");
				if (mailsToBeNotified != null) {
					new SendNotificationTask().execute(httpGatewayURL,
							mailsToBeNotified);
				}
			} else {
				Toast.makeText(getApplicationContext(),
						"Debug: register failed", Toast.LENGTH_LONG).show();
			}
			super.onPostExecute(result);

			m_ProgressDialog.dismiss();
		}

	}

	public class SendNotificationTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(String... params) {
			Boolean result = false;
			AMFConnection amfConnection = new AMFConnection();
			amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
			try {
				System.out.println(params[0]);
				amfConnection.connect(params[0]);
				result = (Boolean) amfConnection.call("sendNotifications",
						params[1]);

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
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(FFmpegFrameRecorder... params) {

			try {
				params[0].stop();
			} catch (Exception e) {

			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			super.onPostExecute(result);

			m_ProgressDialog.dismiss();
		}

	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {

	}

}
