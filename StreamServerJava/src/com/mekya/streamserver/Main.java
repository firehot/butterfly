package com.mekya.streamserver;

import java.io.IOException;

import com.mekya.streamserver.servers.HttpServer;
import com.mekya.streamserver.servers.RtspServer;
import com.mekya.streamserver.servers.StreamTcpServer;
import com.mekya.streamserver.servers.StreamUdpServer;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		StreamTcpServer messenger = new StreamTcpServer(53007);
//		
//		HttpServer server = new HttpServer(24007);
//		server.setVideoStreamer(messenger);
//		server.setAudioStreamer(messenger);
//		
//		try {
//			messenger.start();
//			server.start();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		StreamUdpServer udpServer = new StreamUdpServer();
		RtspServer rtspServer = new RtspServer(udpServer);
		
		
	}

}
