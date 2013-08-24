package com.butterfly;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

import java.nio.Buffer;
import java.nio.ShortBuffer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.butterfly.listener.OnPreviewListener;
import com.butterfly.view.CameraView;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class RecordActivity extends Activity implements OnClickListener,OnPreviewListener {
	

    private final static String CLASS_LABEL = "RecordActivity";
    private final static String LOG_TAG = CLASS_LABEL;

    private PowerManager.WakeLock mWakeLock;

    private String ffmpeg_link = "rtmp://172.18.70.168/live/test";
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

    /* layout setting */
    private final int bg_screen_bx = 232;
    private final int bg_screen_by = 128;
    private final int bg_screen_width = 700;
    private final int bg_screen_height = 500;
    private final int bg_width = 1123;
    private final int bg_height = 715;
    private final int live_width = 640;
    private final int live_height = 480;
    private int screenWidth, screenHeight;
    private Button btnRecorderControl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Hide title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.main);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE); 
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, CLASS_LABEL); 
        mWakeLock.acquire(); 

        initLayout();
        initRecorder();
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

        if(cameraView != null)
    		cameraView.onRecordStateChanged(false);
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

        /* get size of screen */
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        screenWidth = display.getWidth();
        screenHeight = display.getHeight();
        RelativeLayout.LayoutParams layoutParam = null; 
        LayoutInflater myInflate = null; 
        myInflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout topLayout = new RelativeLayout(this);
        setContentView(topLayout);
        LinearLayout preViewLayout = (LinearLayout) myInflate.inflate(R.layout.main, null);
        layoutParam = new RelativeLayout.LayoutParams(screenWidth, screenHeight);
        topLayout.addView(preViewLayout, layoutParam);

        /* add control button: start and stop */
        btnRecorderControl = (Button) findViewById(R.id.recorder_control);
        btnRecorderControl.setText("Start");
        btnRecorderControl.setOnClickListener(this);

        /* add camera view */
        int display_width_d = (int) (1.0 * bg_screen_width * screenWidth / bg_width);
        int display_height_d = (int) (1.0 * bg_screen_height * screenHeight / bg_height);
        int prev_rw, prev_rh;
        if (1.0 * display_width_d / display_height_d > 1.0 * live_width / live_height) {
            prev_rh = display_height_d;
            prev_rw = (int) (1.0 * display_height_d * live_width / live_height);
        } else {
            prev_rw = display_width_d;
            prev_rh = (int) (1.0 * display_width_d * live_height / live_width);
        }
        layoutParam = new RelativeLayout.LayoutParams(prev_rw, prev_rh);
        layoutParam.topMargin = (int) (1.0 * bg_screen_by * screenHeight / bg_height);
        layoutParam.leftMargin = (int) (1.0 * bg_screen_bx * screenWidth / bg_width);

        cameraDevice = Camera.open(0);
        Log.i(LOG_TAG, "cameara open");
        cameraView = new CameraView(this, cameraDevice,imageWidth,imageHeight,frameRate,yuvIplimage,recorder,startTime,recording);
        topLayout.addView(cameraView, layoutParam);
        Log.i(LOG_TAG, "cameara preview start: OK");
    }

    //---------------------------------------
    // initialize ffmpeg_recorder
    //---------------------------------------
    private void initRecorder() {

        Log.w(LOG_TAG,"init recorder");

        if (yuvIplimage == null) {
            yuvIplimage = IplImage.create(imageWidth, imageHeight, IPL_DEPTH_8U, 2);
            Log.i(LOG_TAG, "create yuvIplimage");
        }

//        ffmpeg_link += (int) (Math.random()*100000) + System.currentTimeMillis();
        Log.i(LOG_TAG, "ffmpeg_url: " + ffmpeg_link);
        
        
        createRecorder();

        Log.i(LOG_TAG, "recorder initialize success");

        audioRecordRunnable = new AudioRecordRunnable();
        audioThread = new Thread(audioRecordRunnable);
        audioThread.start();
    }


	private void createRecorder() {
		recorder = new FFmpegFrameRecorder(ffmpeg_link, imageWidth, imageHeight, 1);
        recorder.setFormat("flv");
        recorder.setSampleRate(sampleAudioRateInHz);
        // Set in the surface changed method
        recorder.setFrameRate(frameRate);
	}

    public void startRecording() {

        try {
        	if(recorder == null)
        		createRecorder();
        	
            recorder.start();
            startTime = System.currentTimeMillis();
            if(cameraView != null)
        		cameraView.onRecordStateChanged(true);
            recording = true;
        //    audioThread.start();

        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {

        runAudioThread = false;

        if (recorder != null && recording) {
        	if(cameraView != null)
        		cameraView.onRecordStateChanged(false);
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
                    Log.v(LOG_TAG,"bufferReadResult: " + bufferReadResult);
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
            startRecording();
            Log.w(LOG_TAG, "Start Button Pushed");
            btnRecorderControl.setText("Stop");
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

}