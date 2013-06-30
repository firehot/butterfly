package com.mekya.streamserver;

import com.mekya.streamserver.servers.RtspServer;
import com.mekya.streamserver.servers.StreamUdpServer;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		StreamUdpServer udpServer = new StreamUdpServer();
		RtspServer rtspServer = new RtspServer(udpServer);
		
		
	}

}
