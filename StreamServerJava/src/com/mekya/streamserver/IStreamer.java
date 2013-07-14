package com.mekya.streamserver;

import com.mekya.streamserver.servers.Client;

public interface IStreamer {

	public void startAudio(Client client);
	public void startVideo(Client client);
	
	public void stopVideo(Client client);
	public void stopAudio(Client client);
	public void stop();
}
