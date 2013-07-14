package com.mekya.streamserver.servers;

import java.net.Socket;

public class Client {
	
	private String addr;
	
	private int video_port;
	
	//default audio port number
	private int audio_port = 53008;
	
	
	public Client(String addr) {
		this.addr = addr;
	}
	
	public void setVideo_port(int video_port) {
		this.video_port = video_port;
	}
	
	public void setAudio_port(int audio_port) {
		this.audio_port = audio_port;
	}
	
	public int getVideo_port() {
		return video_port;
	}
	
	public int getAudio_port() {
		return audio_port;
	}

	public String getAddr() {
		return addr;
	}


	
	
}
