package com.butterfly.view;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.butterfly.listener.OnPreviewListener;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

//---------------------------------------------
// camera thread, gets and encodes video data
//---------------------------------------------
public class CameraView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean isPreviewOn = false;
    private int imageWidth;
    private int imageHeight;
    private int frameRate;
    long startTime;
    OnPreviewListener previewListener;

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
        this.startTime = startTime;
        this.previewListener = (OnPreviewListener)context;
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
        this.previewListener.onPreviewChanged(data);
    }

}
