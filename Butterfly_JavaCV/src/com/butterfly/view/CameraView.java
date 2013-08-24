package com.butterfly.view;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.butterfly.listener.OnRecordStateListener;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

//---------------------------------------------
// camera thread, gets and encodes video data
//---------------------------------------------
public class CameraView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback,OnRecordStateListener {

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean isPreviewOn = false;
    private int imageWidth;
    private int imageHeight;
    private int frameRate;
    private IplImage yuvIplimage = null;
    private volatile FFmpegFrameRecorder recorder;
    long startTime;
    boolean recording;

    public CameraView(Context context, Camera camera,int imageWidth,int imageHeight,
    int frameRate,IplImage yuvIplimage,FFmpegFrameRecorder recorder,long startTime,boolean recording) {
        super(context);
        Log.w("camera","camera view");
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(CameraView.this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCamera.setPreviewCallback(CameraView.this);
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;
        this.yuvIplimage = yuvIplimage;
        this.recorder = recorder;
        this.startTime = startTime;
        this.recording = recording;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            stopPreview();
            mCamera.setPreviewDisplay(holder);
        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Camera.Parameters camParams = mCamera.getParameters();
        camParams.setPreviewSize(imageWidth, imageHeight);


        camParams.setPreviewFrameRate(frameRate);
        mCamera.setParameters(camParams);
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            mHolder.addCallback(null);
            mCamera.setPreviewCallback(null);
        } catch (RuntimeException e) {
            // The camera has probably just been released, ignore.
        }
    }

    public void startPreview() {
        if (!isPreviewOn && mCamera != null) {
            isPreviewOn = true;
            mCamera.startPreview();
        }
    }

    public void stopPreview() {
        if (isPreviewOn && mCamera != null) {
            isPreviewOn = false;
            mCamera.stopPreview();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        /* get video data */
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

	@Override
	public void onRecordStateChanged(boolean state) {
		this.recording = state;
		
	}
}
