package com.butterfly.utils;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationProvider implements LocationListener, ConnectionCallbacks,
		OnConnectionFailedListener {

	private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private LocationRequest mLocationRequest;
	private LocationClient mLocationClient;
	private Activity mActivity;
	private int locationUpdateCount = 0;
	private LocationListener locationListener;

	/**
	 * Dont forget to check google play services is connected before using this
	 * class. How to use - create an object for this class, call getLocation
	 * function with location listener object. It will call onLocationChanged
	 * function after 15 seconds
	 * 
	 * @param activity
	 */
	public LocationProvider(Activity activity) {
		this.mActivity = activity;
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		mLocationRequest.setInterval(15000); // 15 seconds
		mLocationRequest.setFastestInterval(15000);

		mLocationClient = new LocationClient(activity, this, this);

	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (GooglePlayServicesUtil.isUserRecoverableError(connectionResult
				.getErrorCode())) {
			GooglePlayServicesUtil.getErrorDialog(
					connectionResult.getErrorCode(), mActivity,
					CONNECTION_FAILURE_RESOLUTION_REQUEST).show();
		} else {
			Toast.makeText(mActivity,
					"Unable to connect Google Location Service",
					Toast.LENGTH_LONG).show();
		}
	}

	public void getLocation(LocationListener listener) {
		locationUpdateCount = 0;
		mLocationClient.connect();
		this.locationListener = listener;
	}

	@Override
	public void onConnected(Bundle arg0) {
		mLocationClient.requestLocationUpdates(mLocationRequest, this);

	}

	@Override
	public void onDisconnected() {
		Toast.makeText(mActivity,
				"UpppsConnection to Play Service Location drops",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onLocationChanged(android.location.Location location) {
		locationUpdateCount++;
		if (locationUpdateCount >= 2) {
			mLocationClient.removeLocationUpdates(this);
			mLocationClient.disconnect();
			if (locationListener != null) {
				this.locationListener.onLocationChanged(location);
			}
		}

		Toast.makeText(
				mActivity,
				" provider -> " + location.getProvider() + "    latitude "
						+ location.getLatitude() + " longitude "
						+ location.getLongitude() + " accuray "
						+ location.getAccuracy() + " time "
						+ location.getTime(), Toast.LENGTH_SHORT).show();

	}
}
