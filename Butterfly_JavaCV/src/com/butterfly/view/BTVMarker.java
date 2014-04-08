package com.butterfly.view;

import com.butterfly.fragment.StreamListFragment.Stream;
import com.google.android.gms.maps.model.Marker;

public class BTVMarker {

	Marker marker;
	boolean isVisible;
	Stream stream;

	public BTVMarker(Marker marker,Stream stream) {
		this.marker = marker;
		this.stream = stream;
		this.isVisible = false;
	}

	public Stream getStream() {
		return stream;
	}

	public void setStream(Stream stream) {
		this.stream = stream;
	}

	public Marker getMarker() {
		return marker;
	}

	public void setMarker(Marker marker) {
		this.marker = marker;
	}

	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}
	
	public void showInfoWindow()
	{
		this.marker.showInfoWindow();
		this.isVisible = true;
	}
	
	public void hideInfoWindow()
	{
		this.marker.hideInfoWindow();
		this.isVisible = false;
	}

}
