/**
 * 
 */
package com.mekya.streamserver.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mekya.streamserver.IStreamListener;
import com.mekya.streamserver.servers.StreamTcpServer;

/**
 * @author aomermerkaya
 *
 */
public class StreamServerTest {

	private static final int PORT = 52147;
	private StreamTcpServer streamServer;

	/**
	 * Test method for {@link com.mekya.streamserver.servers.StreamTcpServer#registerForVideo(com.mekya.streamserver.IStreamListener)}.
	 */
	@Test
	public void testRegister() {
	}

	/**
	 * Test method for {@link com.mekya.streamserver.servers.StreamTcpServer#removeVideoListener(com.mekya.streamserver.IStreamListener)}.
	 */
	@Test
	public void testRemoveListener() {
	}

	/**
	 * Test method for {@link com.mekya.streamserver.servers.StreamTcpServer#stopServer()}.
	 */
	@Test
	public void testStopServer() {
	}
	
	@Before 
	public void before() {
		streamServer = new StreamTcpServer(PORT);
		streamServer.start();
	}
	
	@After
	public void after(){
		streamServer.stopServer();
		//streamServer = null;
	}
	
	@Test
	public void testStreamVideoAndAudio() {
		ArrayList<StreamListener> videoListeners = new ArrayList<StreamServerTest.StreamListener>();
		StreamListener listener;
		for (int i = 0; i < 50; i++) {
			listener = new StreamListener();
			streamServer.registerForVideo(listener);
			videoListeners.add(listener);
		}
		
		ArrayList<StreamListener> audioListeners = new ArrayList<StreamServerTest.StreamListener>();
		for (int i = 0; i < 50; i++) {
			listener = new StreamListener();
			streamServer.registerForAudio(listener);
			audioListeners.add(listener);
		}
		
		ClientStreamer videoStreamerClient = new ClientStreamer();
		ClientStreamer audioStreamerClient = new ClientStreamer();
		
		assertFalse(videoStreamerClient.isConnected());
		assertFalse(audioStreamerClient.isConnected());
		
		videoStreamerClient.connect(PORT);
		audioStreamerClient.connect(PORT + 1);

		assertTrue(videoStreamerClient.isConnected());
		assertTrue(audioStreamerClient.isConnected());
		
		for (int i = 0; i < 50; i++) {
			assertFalse(audioListeners.get(i).isDataReceived());
			assertFalse(videoListeners.get(i).isDataReceived());
		}
		
		String videoData = "videovideovideovideovideovideovideovideovideovideovideovideovideodatadatadatadatadatadatadatadatadatadatadatadatadatadatadatadata";
		videoStreamerClient.sendData(videoData.getBytes());
		String audioData = "audioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudioaudio";
		audioStreamerClient.sendData(audioData.getBytes());
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		for (int i = 0; i < 50; i++) {
			assertTrue(videoListeners.get(i).isDataReceived());
			assertTrue(audioListeners.get(i).isDataReceived());
			Assert.assertEquals(videoData.getBytes().length, videoListeners.get(i).getLength());
			Assert.assertEquals(audioData.getBytes().length, audioListeners.get(i).getLength());
			for (int j = 0; j < videoData.getBytes().length; j++) {
				Assert.assertEquals(videoData.getBytes()[j], videoListeners.get(i).getData()[j]);
			}
			for (int j = 0; j < audioData.getBytes().length; j++) {
				Assert.assertEquals(audioData.getBytes()[j], audioListeners.get(i).getData()[j]);
			}
		}
		
		
		for (int i = 0; i < 50; i++) {
			assertFalse(videoListeners.get(i).isDataReceived());
			assertFalse(audioListeners.get(i).isDataReceived());
		}
	}
	
	
	
	@Test 
	public void testStreamAudio() {
		ArrayList<StreamListener> listeners = new ArrayList<StreamServerTest.StreamListener>();
		StreamListener listener;
		for (int i = 0; i < 50; i++) {
			listener = new StreamListener();
			streamServer.registerForAudio(listener);
			listeners.add(listener);
		}
		
	
		ClientStreamer client = new ClientStreamer();
		client.connect(PORT+1);
		
		assertTrue(client.isConnected());
		
		for (int i = 0; i < 50; i++) {
			assertFalse(listeners.get(i).isDataReceived());
		}
		
		
		String data = "This is a audio test";
		//send data to stream server
		client.sendData(data.getBytes());
		
		//wait a little to let message delivered
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < 50; i++) {
			assertTrue(listeners.get(i).isDataReceived());
			Assert.assertEquals(data.getBytes().length, listeners.get(i).getLength());
			for (int j = 0; j < data.getBytes().length; j++) {
				Assert.assertEquals(data.getBytes()[j], listeners.get(i).getData()[j]);
			}
		}
		
		
		for (int i = 0; i < 50; i++) {
			assertFalse(listeners.get(i).isDataReceived());
		}
	}
	
	/**
	 * Test if StreamServer delivers messages to all registered members
	 */
	@Test
	public void testStreamVideo() {
		
		ArrayList<StreamListener> listeners = new ArrayList<StreamServerTest.StreamListener>();
		StreamListener listener;
		for (int i = 0; i < 50; i++) {
			listener = new StreamListener();
			streamServer.registerForVideo(listener);
			listeners.add(listener);
		}
		
	
		ClientStreamer client = new ClientStreamer();
		client.connect(PORT);
		
		assertTrue(client.isConnected());
		
		for (int i = 0; i < 50; i++) {
			assertFalse(listeners.get(i).isDataReceived());
		}
		
		
		String data = "This is a video stream test";
		//send data to stream server
		client.sendData(data.getBytes());
		
		//wait a little to let message delivered
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < 50; i++) {
			assertTrue(listeners.get(i).isDataReceived());
			Assert.assertEquals(data.getBytes().length, listeners.get(i).getLength());
			for (int j = 0; j < data.getBytes().length; j++) {
				Assert.assertEquals(data.getBytes()[j], listeners.get(i).getData()[j]);
			}
		}
		
		
		for (int i = 0; i < 50; i++) {
			assertFalse(listeners.get(i).isDataReceived());
		}
	}
	
	
	
	private class ClientStreamer {
		
		private Socket socket;
		private OutputStream outputStream;

		public void connect(int port) {
			try {
				socket = new Socket("127.0.0.1", port);
				outputStream = socket.getOutputStream();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
		
		public void sendData(byte[] data) {
			try {
				outputStream.write(data);
				outputStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public boolean isConnected() {
			if (socket == null) {
				return false;
			}
			return socket.isConnected();
		}
	}
	
	private class StreamListener implements IStreamListener {

		private boolean dataReceived = false;
		private byte[] data;
		private int length;

		@Override
		public int dataReceived(byte[] data, int length) {
			this.dataReceived = true;
			this.data = data;
			this.setLength(length);
			return length;
		}

		public boolean isDataReceived() {
			boolean received = dataReceived;
			dataReceived = false;
			return received;
		}

		/**
		 * @return the data
		 */
		public byte[] getData() {
			return data;
		}

		/**
		 * @param data the data to set
		 */
		public void setData(byte[] data) {
			this.data = data;
		}

		/**
		 * @return the length
		 */
		public int getLength() {
			return length;
		}

		/**
		 * @param length the length to set
		 */
		public void setLength(int length) {
			this.length = length;
		}
		
	}

}
