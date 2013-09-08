package com.butterfly;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

import java.nio.Buffer;
import java.nio.ShortBuffer;

import android.app.Activity;
import android.content.Context;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.butterfly.listener.OnPreviewListener;
import com.butterfly.view.CameraView;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

public class RecordActivity extends Activity implements OnClickListener,OnPreviewListener {


	private final static String CLASS_LABEL = "RecordActivity";
	private final static String LOG_TAG = CLASS_LABEL;

	private PowerManager.WakeLock mWakeLock;

	private String ffmpeg_link; 
	//  private String ffmpeg_link = "/mnt/sdcard/stream.flv";

	long startTime = 0;
	boolean recording = false;

	private volatile FFmpegFrameRecorder recorder;

	private int sampleAudioRateInHz = 44100;
	private int imageWidth = 320;
	private int imageHeight = 240;
	private int frameRate = 30;

	/* audio data getting thread */
	private AudioRecord audioRecord;
	private AudioRecordRunnable audioRecordRunnable;
	private Thread audioThread;
	volatile boolean runAudioThread = true;

	/* video data getting thread */
	private Camera cameraDevice;
	private CameraView cameraView;

	private IplImage yuvIplimage = null;

	private Button btnRecorderControl;
	private String httpGatewayURL;
	private String streamURL;
	private EditText streamNameEditText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ffmpeg_link = getString(R.string.rtmp_url);

		//Hide title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		setContentView(R.layout.main);

		httpGatewayURL = getString(R.string.http_gateway_url);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE); 
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, CLASS_LABEL); 
		mWakeLock.acquire(); 

		initLayout();
		initRecorder();

		streamNameEditText = (EditText) findViewById(R.id.stream_name);
	}


	@Override
	protected void onResume() {
		super.onResume();

		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, CLASS_LABEL);
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

		recording = false;

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

	cameraView = (CameraView)findViewById(R.id.cam);
	cameraDevice = Camera.open(0);
	cameraView.setCamera(cameraDevice);

	Log.i(LOG_TAG, "cameara preview start: OK");
	}

	//---------------------------------------
	// initialize ffmpeg_recorder
	//---------------------------------------
	private void initRecorder() {

		Log.w(LOG_TAG,"init recorder");

		streamURL = String.valueOf((int) (Math.random()*100000) + System.currentTimeMillis());
		ffmpeg_link += streamURL;
		Log.i(LOG_TAG, "ffmpeg_url: " + ffmpeg_link);


		createRecorder();

		Log.i(LOG_TAG, "recorder initialize success");

		audioRecordRunnable = new AudioRecordRunnable();
		audioThread = new Thread(audioRecordRunnable);
		audioThread.start();
	}


	private void createRecorder() {
		Size previewSize = cameraDevice.getParameters().getPreviewSize();
		recorder = new FFmpegFrameRecorder(ffmpeg_link, previewSize.width, previewSize.height, 1);
		recorder.setFormat("flv");
		recorder.setSampleRate(sampleAudioRateInHz);
		// Set in the surface changed method
		recorder.setFrameRate(frameRate);
	}

	public void startRecording() {

		if (yuvIplimage == null) {
			Size previewSize = cameraDevice.getParameters().getPreviewSize();
			yuvIplimage = IplImage.create(previewSize.width, previewSize.height, IPL_DEPTH_8U, 2);
			Log.i(LOG_TAG, "create yuvIplimage");
		}

		try {
			if(recorder == null)
				createRecorder();

			recorder.start();
			startTime = System.currentTimeMillis();

			recording = true;
			//    audioThread.start();

		} catch (FFmpegFrameRecorder.Exception e) {
			e.printStackTrace();
		}
	}

	public void stopRecording() {

		runAudioThread = false;

		if (recorder != null && recording) {

			recording = false;
			Log.v(LOG_TAG,"Finishing recording, calling stop and release on recorder");
			try {
				recorder.stop();
				recorder.release();
			} catch (FFmpegFrameRecorder.Exception e) {
				e.printStackTrace();
			}
			recorder = null;

		}
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


	//---------------------------------------------
	// audio thread, gets and encodes audio data
	//---------------------------------------------
	class AudioRecordRunnable implements Runnable {

		@Override
		public void run() {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

			// Audio
			int bufferSize;
			short[] audioData;
			int bufferReadResult;

			bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz, 
					AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz, 
					AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

			audioData = new short[bufferSize];

			Log.d(LOG_TAG, "audioRecord.startRecording()");
			audioRecord.startRecording();

			/* ffmpeg_audio encoding loop */
			while (runAudioThread) {
				//Log.v(LOG_TAG,"recording? " + recording);
				bufferReadResult = audioRecord.read(audioData, 0, audioData.length);
				if (bufferReadResult > 0) {
					//Log.v(LOG_TAG,"bufferReadResult: " + bufferReadResult);
					// If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
					// Why?  Good question...
					if (recording) {
						try {
							Buffer[] buffer = new Buffer[1];
							buffer[0] = ShortBuffer.wrap(audioData, 0, bufferReadResult);
							recorder.record(buffer);
							//Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
						} catch (FFmpegFrameRecorder.Exception e) {
							Log.v(LOG_TAG,e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
			Log.v(LOG_TAG,"AudioThread Finished, release audioRecord");

			/* encoding finish, release recorder */
			if (audioRecord != null) {
				audioRecord.stop();
				audioRecord.release();
				audioRecord = null;
				Log.v(LOG_TAG,"audioRecord released");
			}
		}
	}



	@Override
	public void onClick(View v) {
		if (!recording) {
			String name = streamNameEditText.getText().toString();
			if (name != null && name.length() > 0) {
				new RegisterStreamTask().execute(httpGatewayURL, name, streamURL);
			}

		} else {
			// This will trigger the audio recording loop to stop and then set isRecorderStart = false;
			stopRecording();
			Log.w(LOG_TAG, "Stop Button Pushed");
			btnRecorderControl.setText("Start");
		}
	}


	@Override
	public void onPreviewChanged(byte[] data) {

		if (yuvIplimage != null && recording) {
			yuvIplimage.getByteBuffer().put(data);


			try {
				long t = 1000 * (System.currentTimeMillis() - startTime);
				if (t > recorder.getTimestamp()) {
					recorder.setTimestamp(t);
				}
				recorder.record(yuvIplimage);
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
				result = (Boolean) amfConnection.call("registerLiveStream", params[1], params[2]);

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
		protected void onPostExecute(Boolean result) {
			if (result == true) {
				startRecording();
				Log.w(LOG_TAG, "Start Button Pushed");
				btnRecorderControl.setText("Stop");
			}
			else {
				Toast.makeText(getApplicationContext(), "Debug: register failed", Toast.LENGTH_LONG).show();
			}
			super.onPostExecute(result);
		}

	}	

}