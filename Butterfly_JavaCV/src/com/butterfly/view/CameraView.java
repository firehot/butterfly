package com.butterfly.view;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

//---------------------------------------------
// camera thread, gets and encodes video data
//---------------------------------------------
public class CameraView extends ViewGroup implements SurfaceHolder.Callback {

	private SurfaceHolder mHolder;
	private Camera mCamera;
	private boolean isPreviewOn = false;
	Size mPreviewSize;
	List<Size> mSupportedPreviewSizes;
	SurfaceView mSurfaceView;

//	public CameraView(Context context, Camera camera) {
//		super(context);
//		Log.w("camera", "camera view");
//		mSurfaceView = new SurfaceView(context);
//		addView(mSurfaceView);
//		mCamera = camera;
//		mHolder = mSurfaceView.getHolder();
//		mHolder.addCallback(CameraView.this);
//		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//		mCamera.setPreviewCallback(CameraView.this);
//		this.previewListener = (OnPreviewListener) context;
//	}

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mSurfaceView = new SurfaceView(context);
		addView(mSurfaceView);
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//		this.previewListener = (OnPreviewListener) context;
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

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		stopPreview();
	//	Camera.Parameters camParams = mCamera.getParameters();
		// camParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
//		camParams.setPreviewSize(mSupportedPreviewSizes.get(0).width,
//				mSupportedPreviewSizes.get(0).height);
//		requestLayout();
	//	mCamera.setParameters(camParams);

		startPreview();
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
		//mCamera.setPreviewCallback(CameraView.this);
		if (mCamera != null) {
			mSupportedPreviewSizes = mCamera.getParameters()
					.getSupportedPreviewSizes();
			requestLayout();
		}
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
	
	public boolean isPreviewOn() {
		return isPreviewOn;
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
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// We purposely disregard child measurements because act as a
		// wrapper to a SurfaceView that centers the camera preview instead
		// of stretching it.
		final int width = resolveSize(getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(getSuggestedMinimumHeight(),
				heightMeasureSpec);
		setMeasuredDimension(width, height);

		if (mSupportedPreviewSizes != null) {
			mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width,
					height);
		}
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
				continue;
			}
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}

		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					System.out
							.println(" optimal size fit preview size width, height "
									+ size.width + " " + size.height);
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (changed && getChildCount() > 0) {
			final View child = getChildAt(0);

			final int width = r - l;
			final int height = b - t;

			int previewWidth = width;
			int previewHeight = height;
			if (mPreviewSize != null) {
				previewWidth = mPreviewSize.width;
				previewHeight = mPreviewSize.height;
			}

			// Center the child SurfaceView within the parent.
			if (width * previewHeight > height * previewWidth) {
				final int scaledChildWidth = previewWidth * height
						/ previewHeight;
				child.layout((width - scaledChildWidth) / 2, 0,
						(width + scaledChildWidth) / 2, height);
			} else {
				final int scaledChildHeight = previewHeight * width
						/ previewWidth;
				child.layout(0, (height - scaledChildHeight) / 2, width,
						(height + scaledChildHeight) / 2);
			}
		}
	}
}
