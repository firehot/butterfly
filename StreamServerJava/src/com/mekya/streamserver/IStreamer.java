package com.mekya.streamserver;

public interface IStreamer {

	public void startAudio(String address, int port);
	public void startVideo(String address, int port);
	
	public void stopStreaming();
}
