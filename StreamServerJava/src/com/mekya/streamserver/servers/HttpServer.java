package com.mekya.streamserver.servers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.mekya.streamserver.IStreamListener;
import com.mekya.streamserver.servers.NanoHTTPD.Response.Status;

public class HttpServer extends NanoHTTPD {

	private IVideoStreamPostman videoStreamer;
	private IAudioStreamPostman audioStreamer;
	public HttpServer(int port) {
		super(port);
	}

	@Override
	public Response serve(IStreamListener streamListener, String uri, Method method,
			Map<String, String> header, Map<String, String> parms,
			Map<String, String> files) {
		// TODO Auto-generated method stub

		System.out.println(uri);
		
		Response res = null;
		if (uri.equals("/liveVideo")) {
			res = new NanoHTTPD.Response(Status.OK, NanoHTTPD.MIME_PLAINTEXT, true);
			
			getVideoStreamer().registerForVideo(streamListener);
		}
		else if (uri.equals("/liveAudio")) {
			res = new NanoHTTPD.Response(Status.OK, NanoHTTPD.MIME_PLAINTEXT, true);
			getAudioStreamer().registerForAudio(streamListener);
		}
		return res;
	}



	public void setVideoStreamer(IVideoStreamPostman messenger) {
		this.videoStreamer = messenger;
	}

	public IVideoStreamPostman getVideoStreamer() {
		return videoStreamer;
	}
	
	public IAudioStreamPostman getAudioStreamer() {
		return audioStreamer;
	}

	public void setAudioStreamer(IAudioStreamPostman audioMessenger) {
		this.audioStreamer = audioMessenger;
	}

}
