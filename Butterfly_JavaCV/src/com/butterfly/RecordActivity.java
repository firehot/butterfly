package com.butterfly;

import static com.googlecode.javacv.cpp.avcodec.AV_CODEC_ID_H264;

import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Build;
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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.androidsocialnetworks.lib.SocialNetwork;
import com.bugsense.trace.BugSenseHandler;
import com.butterfly.debug.BugSense;
import com.butterfly.fragment.ContactsListFragment;
import com.butterfly.listeners.IAsyncTaskListener;
import com.butterfly.message.GcmIntentService;
import com.butterfly.recorder.FFmpegFrameRecorder;
import com.butterfly.tasks.RegisterLocationForStreamTask;
import com.butterfly.tasks.RegisterStreamTask;
import com.butterfly.tasks.SendPreviewTask;
import com.butterfly.utils.LocationProvider;
import com.butterfly.utils.Utils;
import com.butterfly.view.CameraView;
import com.google.android.gms.location.LocationListener;
import com.googlecode.javacpp.BytePointer;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class RecordActivity extends BaseSocialMediaActivity implements OnClickListener,
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
	private String streamURL;
	private EditText streamNameEditText;
	private CheckBox publicVideoCheckBox;

	private static ProgressDialog mProgressDialog;
	private String mailsToBeNotified = null;
//	private Size previewSize;
	private BytePointer bytePointer;
	private boolean snapshotSent = false;
	
	private LinearLayout twitLayout;
	private LinearLayout faceLayout;
	private CheckBox twitCheckbox;
	private CheckBox faceCheckbox;
	
	private IAsyncTaskListener mRegisterStreamTaskListener = new IAsyncTaskListener() {
		
		@Override
		public void onProgressUpdate(Object... progress) {}
		
		@Override
		public void onPreExecute() {
			
			
		}
		
		@Override
		public void onPostExecute(Object object) {
			Boolean result = (Boolean) object;
			if(mProgressDialog != null)
				mProgressDialog.dismiss();
			if (result == true) {
				Log.w(LOG_TAG, "Start Button Pushed");
				streamNameEditText.setVisibility(View.GONE);
				publicVideoCheckBox.setVisibility(View.GONE);
				resolutionsRadioGroup.setVisibility(View.GONE);
				btnRecorderControl
						.setBackgroundResource(R.drawable.bt_stop_record);
				RecordActivity.this.getLocation();
				shareonSocialMedia();
				setVisibilitySocialMedia(false);
			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.stream_registration_failed),
						Toast.LENGTH_LONG).show();
				streamNameEditText.setVisibility(View.VISIBLE);
				publicVideoCheckBox.setVisibility(View.VISIBLE);
				resolutionsRadioGroup.setVisibility(View.VISIBLE);
				setVisibilitySocialMedia(true);
			}

			mProgressDialog = null;
			btnRecorderControl.setClickable(true);
			
			
		}
	};


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
	private LocationProvider locationProvider;
	private List<Size> previewSizeList;
	private RadioGroup resolutionsRadioGroup;

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

		localBroadcastManager = LocalBroadcastManager
				.getInstance(getApplicationContext());
		locationProvider = new LocationProvider(this);
		initLayout();
		
		if (isArmCPU() == false) {
			showIncompatibleArchInfo();
		}
	}
	
	private void showIncompatibleArchInfo() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				RecordActivity.this);
 
			// set dialog message
			alertDialogBuilder
				.setMessage(getString(R.string.in_compatible_arch))
				.setCancelable(false)
				.setPositiveButton(getString(R.string.ok),new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						// if this button is clicked, close
						// current activity
						RecordActivity.this.finish();
					}
				  });
 
				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();
 
				// show it
				alertDialog.show();
	}
	
	
	private boolean isArmCPU()
	{
		boolean result = false;
		String arch = System.getProperty("os.arch");    
		String arc = arch.substring(0, 3).toUpperCase(Locale.ENGLISH);
		String cpuAbi = Build.CPU_ABI.substring(0, 3).toUpperCase(Locale.ENGLISH);
		String cpuAbi2 = Build.CPU_ABI2.substring(0, 3).toUpperCase(Locale.ENGLISH);
		if (arc.equals("ARM") || "ARM".equals(cpuAbi) || "ARM".equals(cpuAbi2)) {
			result = true;
	    }
	    return result;
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
		Crouton.cancelAllCroutons();

	}

	private void getLocation() {

		locationProvider.getLocation(new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {

				RegisterLocationForStreamTask registerLocationTask = new RegisterLocationForStreamTask(null, RecordActivity.this);
				registerLocationTask.setParams(RecordActivity.this.streamURL);
				registerLocationTask.execute(location.getLongitude(), location.getLatitude(),
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
		previewSizeList = parameters.getSupportedPreviewSizes();
		Collections.sort(previewSizeList, new Comparator<Size>() {

			@Override
			public int compare(Size lhs, Size rhs) {
				if (lhs.width == rhs.width) {
					return lhs.height == rhs.height ? 0 : (lhs.height > rhs.height ? 1 : -1);
				}
				else if (lhs.width > rhs.width) {
					return 1;
				}
				return -1;
			}
		});
		parameters.setPreviewSize(previewSizeList.get(0).width,  previewSizeList.get(0).height);

		

		cameraDevice.setParameters(parameters);
		cameraView.setCamera(cameraDevice);
		
		resolutionsRadioGroup = (RadioGroup) findViewById(R.id.resolutions_radio_group);
	
		int len = 3;
		if (previewSizeList.size() < 3) {
			len = previewSizeList.size();
		}
				
		LayoutParams lyp = new LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);

		for (int i = 0; i < len; i++) {
			RadioButton radioButton = new RadioButton(this);
			radioButton.setLayoutParams(lyp);
			Size size = previewSizeList.get(len-1-i);
			radioButton.setText(size.width + "x" + size.height);
			radioButton.setId(len-1-i);
			
			if (parameters.getPreviewSize().width == size.width && parameters.getPreviewSize().height == size.height) {
				radioButton.setChecked(true);
			}
			
			resolutionsRadioGroup.addView(radioButton);
		}
		resolutionsRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				Parameters parameters = cameraDevice.getParameters();
				parameters.setPreviewSize(previewSizeList.get(checkedId).width, previewSizeList.get(checkedId).width);
				if (cameraView.isPreviewOn()) {
					cameraDevice.stopPreview();
					cameraDevice.setParameters(parameters);
					cameraDevice.startPreview();
				} else {
					cameraDevice.setParameters(parameters);
				}
			}
		});
		 

		Log.i(LOG_TAG, "cameara preview start: OK");
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

		if (cameraDevice == null || cameraDevice.getParameters() == null) {
			return false;
		}

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

	public  boolean hasConnection() {
	    ConnectivityManager cm = (ConnectivityManager) this.getSystemService(
	        Context.CONNECTIVITY_SERVICE);

	    NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    if (wifiNetwork != null && wifiNetwork.isConnected()) {
	      return true;
	    }

	    NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
	    if (mobileNetwork != null && mobileNetwork.isConnected()) {
	      return true;
	    }

	    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
	    if (activeNetwork != null && activeNetwork.isConnected()) {
	      return true;
	    }

	    return false;
	  }
	
	@Override
	public void onClick(View v) {
		
		if (!recording) {
			
			if(!hasConnection())
			{
				Crouton.showText(RecordActivity.this,R.string.connectivityProblem, Style.ALERT);
				return;
			}
			btnRecorderControl.setClickable(false);

			streamName = streamNameEditText.getText().toString();

			// Check the video is public or not
			is_video_public = publicVideoCheckBox.isChecked();

			if (streamName != null && streamName.length() > 0) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(
						streamNameEditText.getWindowToken(), 0);
				
				if (mProgressDialog == null) {
					mProgressDialog = ProgressDialog.show(RecordActivity.this,
							null, getString(R.string.initializing), true);

				} else {
					mProgressDialog.setMessage(getString(R.string.initializing));
				}
				
				new Thread() {
						public void run() {
							initRecorder();
							boolean recordingStarted = startRecording();
							if (recordingStarted) {
								
								runOnUiThread(new Runnable() {
									
									@Override
									public void run() {
										new RegisterStreamTask(mRegisterStreamTaskListener, RecordActivity.this).
										execute(streamName,
												streamURL,mailsToBeNotified,
												Utils.getRegisteredMailList(RecordActivity.this), String.valueOf(is_video_public));
										
									}
								});
							
							}
							else {
								runOnUiThread(new Runnable() {
									
									@Override
									public void run() {
										if(mProgressDialog != null && 
												mProgressDialog.isShowing()==true) {
											mProgressDialog.dismiss();
										}
									}
								});
								stopRecording();
							}
						};
						
					}.start();
					
				 

			} else {
				
				Crouton.showText(this,R.string.write_name_of_stream, Style.INFO);
				btnRecorderControl.setClickable(true);
			}

		} else {
			// This will trigger the audio recording loop to stop and then set
			// isRecorderStart = false;
			btnRecorderControl
					.setBackgroundResource(R.drawable.bt_start_record);
			streamNameEditText.setVisibility(View.VISIBLE);
			publicVideoCheckBox.setVisibility(View.VISIBLE);
			resolutionsRadioGroup.setVisibility(View.VISIBLE);
			setVisibilitySocialMedia(true);
			stopRecording();
			runAudioThread = false;
			snapshotSent = false;
			Log.w(LOG_TAG, "Stop Button Pushed");
		}
	}

	public class StopRecordingTask extends	AsyncTask<FFmpegFrameRecorder, Void, Void> {

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
				Size size = cameraDevice.getParameters().getPreviewSize();
				recorder.record(bytePointer, size.width,
						size.height);
			} catch (FFmpegFrameRecorder.Exception e) {

				e.printStackTrace();
			}

			if (!snapshotSent) {
				// send a preview snapshot image to the red5 only for first time
				snapshotSent = true;
				Size size = arg1.getParameters().getPreviewSize();
				SendPreviewTask task = new SendPreviewTask(null, RecordActivity.this);
				task.setSize(size.width, size.height);
				task.execute(data, streamURL);
			}

		}

	}
	
	public String getStreamURL() {
		return streamURL;
	}
	
	@Override
	public void onSocialNetworkManagerInitialized() {

		for (SocialNetwork socialNetwork : mSocialNetworkManager
				.getInitializedSocialNetworks()) {
			socialNetwork.setOnLoginCompleteListener(this);
		}
		
		this.twitLayout = (LinearLayout) this.findViewById(R.id.twitLayout);
		this.twitCheckbox = (CheckBox) this.findViewById(R.id.checkBoxTwit);
		this.faceLayout = (LinearLayout) this.findViewById(R.id.faceLayout);
		this.faceCheckbox = (CheckBox) this.findViewById(R.id.checkBoxFace);
		
		setVisibilitySocialMedia(true);

	}

	private void setVisibilitySocialMedia(boolean visibility)
	{
		if(visibility)
		{
			if(mSocialNetworkManager.getTwitterSocialNetwork().isConnected())
				this.twitLayout.setVisibility(View.VISIBLE);
			else if(mSocialNetworkManager.getFacebookSocialNetwork().isConnected())
				this.faceLayout.setVisibility(View.VISIBLE);
		}
		else
		{
			this.twitLayout.setVisibility(View.GONE);
			this.faceLayout.setVisibility(View.GONE);
		}
	}
	
	private void shareonSocialMedia() {
		String message = RecordActivity.this.getString(R.string.social_media_message);
		message += streamURL;
		if(mSocialNetworkManager.getTwitterSocialNetwork().isConnected() 
				&& RecordActivity.this.twitCheckbox.isChecked())
			mSocialNetworkManager.getTwitterSocialNetwork().requestPostMessage(
					message,RecordActivity.this);
		else if(mSocialNetworkManager.getFacebookSocialNetwork().isConnected()
				&& RecordActivity.this.faceCheckbox.isChecked())
			mSocialNetworkManager.getFacebookSocialNetwork().requestPostMessage(
					message,RecordActivity.this);
	}
}
