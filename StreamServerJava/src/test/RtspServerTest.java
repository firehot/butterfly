package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import test.mock.RtspClient;

import com.mekya.streamserver.IStreamer;
import com.mekya.streamserver.servers.Client;
import com.mekya.streamserver.servers.RtspServer;
import com.mekya.streamserver.servers.StreamUdpServer;

public class RtspServerTest {

	private RtspServer rtspServer;
	private Streamer streamer;
	private int clientCount = 1;

	public class Streamer implements IStreamer {


		private int startAudioCallCount = 0;
		private int startVideoCallCount = 0;
		private int stopVideoStreamingCallCount = 0;
		
		List<Integer> videoPortNoList = new ArrayList<Integer>();
		private int stopAudioStreamingCallCount;

		@Override
		public void startAudio(Client client) {
			startAudioCallCount ++;
			
		}

		@Override
		public void startVideo(Client client) {
			startVideoCallCount ++;
			System.out.println("Port no streamer " + client.getVideo_port());
			videoPortNoList.add(client.getVideo_port());
		}

		@Override
		public void stopVideo(Client client) {
			stopVideoStreamingCallCount ++;
		}
		
		@Override
		public void stopAudio(Client client) {
			stopAudioStreamingCallCount++;
		}

		@Override
		public void stop() {
			
			
		}
		
		

	}

	@Before
	public void setUp() throws Exception {
		streamer = new Streamer();
		rtspServer = new RtspServer(streamer);


	}

	@After
	public void tearDown() throws Exception {
		if (rtspServer != null) {
			rtspServer.stop();
			rtspServer = null;
		}
		streamer = null;
	}

	@Test
	public void testConnectRtspServer() {
		
		clientCount = 10;
		List<RtspClient> rtspClientList = letClientsConnectToRtspServer();
		
		assertEquals(streamer.startAudioCallCount, clientCount);
		assertEquals(streamer.startVideoCallCount, clientCount);
		
		assertEquals(clientCount, rtspServer.getConnectedClientCount());
			
		
		for (int i = 0; i < rtspClientList.size(); i++) {
			//checking port numbers are parsed ok
			assertEquals(rtspClientList.get(i).getRtpPort(), (int)streamer.videoPortNoList.get(i));
			try {
			//closing the connection on client side	
				rtspClientList.get(i).tearDown();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
			
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//check that stopStreaming called because teardown should make streaming stop
		assertEquals(streamer.stopVideoStreamingCallCount, clientCount);
		assertEquals(streamer.stopAudioStreamingCallCount, clientCount);
		
		assertEquals(0, rtspServer.getConnectedClientCount());
	
	}
	
	@Test
	public void testStreamToStreamUDPServer() {
		clientCount = 10;
		
		StreamUdpServer istreamer = new StreamUdpServer();
		rtspServer.setIStreamer(istreamer);
		List<RtspClient> rtspClientList = letClientsConnectToRtspServer();
		
		
		
		try {
			InetAddress addr = InetAddress.getByName("127.0.0.1");
			String data = "datadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadata";
			DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),addr, 43007);

			DatagramSocket socket = new DatagramSocket();
		
			int packetSendCount = 10;
			//sending data to stream udp server to deliver it to rtspclients
			for (int i = 0; i < packetSendCount; i++) {
				socket.send(packet);
				Thread.sleep(1000);
				for (int j = 0; j < 10; j++) {
					//check that data is received to rtspClients
					System.out.println("client : "+j+" "+rtspClientList.get(j).getTotalReceivedDataRTPLength());
				}
			}
			
			Thread.sleep(5000);
			
			int totalDataLength = data.length() * packetSendCount; 
			for (int i = 0; i < 10; i++) {
				//check that data is received to rtspClients
				assertEquals(totalDataLength, rtspClientList.get(i).getTotalReceivedDataRTPLength());
			}
			
			//check connected rtsp clients to server
			assertEquals(clientCount, rtspServer.getConnectedClientCount());
			
			//disconnect one rtsp client
			RtspClient rtspClient0 = rtspClientList.get(0);
			rtspClient0.tearDown();
			
			Thread.sleep(100);			
			//check that connected client count is updated
			assertEquals(clientCount-1, rtspServer.getConnectedClientCount());
			assertEquals(clientCount-1, istreamer.getVideoReceiverCount());
			
			
			//send packet again to stream udp server
			socket.send(packet);
			
			for (int j = 1; j < 10; j++) {
				//check that data is received to rtspClients
				System.out.println("client : "+j+" "+rtspClientList.get(j).getTotalReceivedDataRTPLength());
			}
			int oldtotalDataLength = totalDataLength;
			//update totalDatalength
			totalDataLength += packet.getLength();
			
			Thread.sleep(1000);		
			//rtspClient0 is disconnected so its received data length should not be updated
			//totalDatalength is not updated yet
			assertTrue(rtspClient0.getTotalReceivedDataRTPLength() != totalDataLength);
			assertTrue(rtspClient0.getTotalReceivedDataRTPLength() == oldtotalDataLength);
			
			

			
			for (int i = 1; i < rtspClientList.size(); i++) {
				//check that data is received to rtspClients
				assertEquals(totalDataLength, rtspClientList.get(i).getTotalReceivedDataRTPLength());
			}
			
			
			
		} catch (UnknownHostException | SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

				
		
	}
	
	

	private List<RtspClient> letClientsConnectToRtspServer() {
		List<RtspClient> rtspClientList = new ArrayList<RtspClient>();
		
		for (int i = 0; i < clientCount; i++) {
			
			RtspClient rtspClient = new RtspClient("127.0.0.1", 6454, "live.ts");

			rtspClient.start();
			
			rtspClientList.add(rtspClient);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		return rtspClientList;
	}

}
