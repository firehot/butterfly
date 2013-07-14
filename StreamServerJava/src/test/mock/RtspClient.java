package test.mock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class RtspClient {

	private Socket socket;
	private String file;
	private int port;
	private String addr;
	private BufferedReader reader;
	private BufferedWriter writer;
	private int rtpPort;
	private int rtcpPort;
	protected DatagramPacket datagramRTPPacket;
	protected int totalReceivedDataRTPLength;
	protected DatagramPacket datagramRTCPPacket;
	protected int totalReceivedDataRTCPLength;
	private Thread thread1;
	private Thread thread2;
	private DatagramSocket rtpSocket;
	private DatagramSocket rtcpSocket;

	public RtspClient(String addr, int port, String file) {
		this.addr = addr;
		this.port = port;
		this.file = file;

		rtpPort = (int) (Math.random() * 50000);
		rtcpPort = rtpPort + 1;
		System.out.println("rtpPort in rtsp client " + rtpPort);
	}

	public void start() {
		new Thread(){
			@Override
			public void run() {
				try {
					socket = new Socket(addr, port);

					reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

					sendDescripeRequest();


					parseReply();

					sendSetupRequest();

					parseReply();

					sendPlayRequest();

					parseReply();

					openUdpPorts();

				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

	}

	protected void openUdpPorts() {
		try {
			rtpSocket = new DatagramSocket(rtpPort);
			rtcpSocket = new DatagramSocket(rtcpPort);

			//receiving rtp packets.
			thread1 = new Thread() {
				public void run() {
					byte[] data = new byte[10240];
					datagramRTPPacket = new DatagramPacket(data, data.length);
					try {
						while (true) {
							rtpSocket.receive(datagramRTPPacket);
							totalReceivedDataRTPLength += datagramRTPPacket.getLength();

						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				};
			};
			thread1.start();



			//receiving rtcp packets.
			thread2 = new Thread() {
				public void run() {
					byte[] data = new byte[10240];
					datagramRTCPPacket = new DatagramPacket(data, data.length);
					try {
						while (true) {
							rtcpSocket.receive(datagramRTCPPacket);
							totalReceivedDataRTCPLength += datagramRTCPPacket.getLength();

						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}

				};
			};
			thread2.start();

		} catch (SocketException e) {
			e.printStackTrace();
		}

	}

	public void tearDown() throws IOException {
		String tearDownCommand = "TEARDOWN rtsp://"+ addr +":"+ port +"/"+ file + " RTSP/1.0" + "\r\n"
				+ "Session: 66c4afff"+ "\r\n"
				+ "CSeq: 4" + "\r\n"
				+ "\r\n";

		writer.write(tearDownCommand);
		writer.flush();
		try {
			if (rtpSocket != null) {
				rtpSocket.close();
			}
			if (rtcpSocket != null) {
				rtcpSocket.close();
			}
			thread1.join();
			thread2.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	protected void sendPlayRequest() throws IOException {
		String playCommand = "PLAY rtsp://"+ addr +":"+ port +"/"+ file + " RTSP/1.0" + "\r\n"
				+ "Session: 66c4afff"+ "\r\n"
				+ "CSeq: 3" + "\r\n"
				+ "\r\n";

		writer.write(playCommand);
		writer.flush();
	}

	protected void sendSetupRequest() throws IOException 
	{

		String setupCommand = "SETUP rtsp://"+ addr +":"+ port +"/"+ file + " RTSP/1.0" + "\r\n"
				+ "Transport: RTP/AVP/UDP;unicast;client_port="+ rtpPort +"-"+ rtcpPort + "\r\n"
				+ "CSeq: 2" + "\r\n"
				+ "\r\n";

		writer.write(setupCommand);
		writer.flush();

	}

	private void sendDescripeRequest() throws IOException 
	{
		String describeCommand = "DESCRIBE rtsp://"+ addr +":"+ port +"/"+ file + " RTSP/1.0" + "\r\n"
				+ "Accept: application/sdp"+ "\r\n"
				+ "CSeq: 1" + "\r\n"
				+ "\r\n";

		writer.write(describeCommand);
		writer.flush();
	}

	public DatagramPacket getDatagramRTPPacket() {
		return datagramRTPPacket;
	}

	public int getTotalReceivedDataRTPLength() {
		return totalReceivedDataRTPLength;
	}

	protected void parseReply() throws IOException {
		String line;
		int lineIndex = 0;
		while ((line = reader.readLine()).length() != 0) {

			//System.out.println(line);


			if (lineIndex == 1) {

			}
			else {

			}
			lineIndex++;
		}

	}


	public DatagramPacket getDatagramRTCPPacket() {
		return datagramRTCPPacket;
	}

	public int getTotalReceivedDataRTCPLength() {
		return totalReceivedDataRTCPLength;
	}

	public int getRtpPort() {
		return rtpPort;
	}

	public boolean isClosed() {
		return  socket.isClosed();
	}

	public void closeConnection() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
