package com.mekya.streamserver.servers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import com.mekya.streamserver.IStreamer;

public class StreamUdpServer implements IStreamer {

	private static final int DATAGRAM_AUDIO_PORT = 43008;
	private static final int DATAGRAM_VIDEO_PORT = 43007;
	protected static final int PORT = 50021;
	private static StreamUdpServer udpServer;
	protected DatagramSocket datagramVideoServerSocket;
	private Thread listenVideoThread;
	private Thread listenAudioThread;
	protected DatagramSocket datagramAudioServerSocket;
	List<Client> audioReceivers = new ArrayList<Client>();
	List<Client> videoReceivers = new ArrayList<Client>();

	public StreamUdpServer() {
		listenVideo();
		listenAudio();
	}

	public void listenVideo()
	{
		listenVideoThread = new Thread(){

			private int totalLength = 0;
			private DatagramPacket datagramSenderPacket;
			
			public void run() {
				try {
					datagramVideoServerSocket = new DatagramSocket(DATAGRAM_VIDEO_PORT);
					DatagramSocket datagramVideoClientSocket = new DatagramSocket();

					byte[] data = new byte[10*1024];
					DatagramPacket datagramPacket;

					while (true) {
						datagramPacket = new DatagramPacket(data, data.length);
						datagramVideoServerSocket.receive(datagramPacket);					
						totalLength += datagramPacket.getLength();
						
						for (int i = 0; i < videoReceivers.size(); i++) {
							InetAddress inetAddress = InetAddress.getByName(videoReceivers.get(i).address);
							datagramSenderPacket = new DatagramPacket(datagramPacket.getData(), datagramPacket.getLength(), inetAddress, videoReceivers.get(i).port);
							System.out
							.println("StreamUdpServer.listenVideo().new Thread() {...}.run()");
							datagramVideoClientSocket.send(datagramSenderPacket);
						}
						
					

					}
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		};
		listenVideoThread.start();

	}
	
	public void listenAudio() {
		listenAudioThread = new Thread(){


			private int totalLength = 0;
			public void run() {
				try {
					datagramAudioServerSocket = new DatagramSocket(DATAGRAM_AUDIO_PORT);
					DatagramSocket datagramAudioClientSocket = new DatagramSocket();

					byte[] data = new byte[10*1024];
					DatagramPacket datagramPacket;
					DatagramPacket datagramSenderPacket;

					while (true) {
						datagramPacket = new DatagramPacket(data, data.length);
						datagramAudioServerSocket.receive(datagramPacket);					
						totalLength += datagramPacket.getLength();
						
						System.out
								.println("StreamUdpServer.listenAudio().new Thread() {...}.run()");
						
						for (int i = 0; i < audioReceivers.size(); i++) {
							InetAddress inetAddress = InetAddress.getByName(audioReceivers.get(i).address);
							datagramSenderPacket = new DatagramPacket(datagramPacket.getData(), datagramPacket.getLength(), inetAddress, audioReceivers.get(i).port);
							datagramAudioClientSocket.send(datagramSenderPacket);
						}
					}
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		};
		listenAudioThread.start();
	}

	public void stop() {
		try {
			datagramVideoServerSocket.close();
			listenVideoThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void startAudio(String address, int port) {
		audioReceivers.add(new Client(address, port));
		
	}


	@Override
	public void startVideo(String address, int port) {
		videoReceivers.add(new Client(address, port));
	}


	@Override
	public void stopStreaming() {
	}
	
	public class Client {
		public String address;
		public int port;
		
		public Client(String address, int port) {
			this.address = address;
			this.port = port;
		}
 	}


}
