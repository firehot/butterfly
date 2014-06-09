package com.butterfly.tasks;

import java.io.ByteArrayOutputStream;

import com.butterfly.listeners.IAsyncTaskListener;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.util.Log;
import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

/**
 * This is a task that sends a preview image to red5 in the background
 * 
 * @author murat
 * 
 */
public class SendPreviewTask extends AbstractAsyncTask<Object, Void, Boolean> {

	/**
	 * width of camera preview size
	 */
	private int width;
	/**
	 * height of camera preview size
	 */
	private int height;


	public SendPreviewTask(IAsyncTaskListener taskListener, Activity context) {
		super(taskListener, context);
	}

	public void setSize(int width, int height) {

		this.height = height;
		this.width = width;

	}

	@Override
	protected Boolean doInBackground(Object... params) {
		boolean result = false;
		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {
			amfConnection.connect(HTTP_GATEWAY_URL);

			byte[] imgData = (byte[]) params[0];
			String streamUrl = (String) params[1];

			ByteArrayOutputStream baos = getByteArrayOutputStreamFromYUV(
					imgData, this.width, this.height);

			result = (Boolean) amfConnection.call("savePreview", baos.toByteArray(), streamUrl);

		} catch (ClientStatusException e) {
			Log.e("butterfly", e.getMessage());
			e.printStackTrace();
		} catch (ServerStatusException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		amfConnection.close();

		return result;
	}

	/**
	 * This method creates a compressed and fixed size image from camera preview
	 * using yuvimage
	 * 
	 * @param data
	 * @param width
	 * @param height
	 * @return
	 */
	private ByteArrayOutputStream getByteArrayOutputStreamFromYUV(byte[] data,
			int width, int height) {

		YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height,
				null);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		yuvimage.compressToJpeg(new Rect(0, 0, width, height), 80, baos);
		byte[] jdata = baos.toByteArray();
		BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
		bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length,
				bitmapFatoryOptions);
		Bitmap scaled = Bitmap.createScaledBitmap(bmp, 176, 144, true);
		baos = new ByteArrayOutputStream();
		scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
		return baos;
	}

}
