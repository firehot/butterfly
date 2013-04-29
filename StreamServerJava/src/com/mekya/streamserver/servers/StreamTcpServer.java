package com.mekya.streamserver.servers;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import com.mekya.streamserver.IStreamListener;

/**
 * This server accepts stream data from clients
 * @author aomermerkaya
 *
 */
public class StreamTcpServer implements IVideoStreamPostman, IAudioStreamPostman{

	private ArrayList<IStreamListener> videoStreamListeners = new ArrayList<IStreamListener>();

	private ArrayList<IStreamListener> audioStreamListeners = new ArrayList<IStreamListener>();

	private ServerSocket serverVideoSocket;

	private int port;

	private ServerSocket serverAudioSocket;

	private VideoServerThread videoServerThread;

	private AudioServerThread audioServerThread;
	
	public StreamTcpServer(int port) {
		try {
			serverVideoSocket = new ServerSocket(port);
			serverAudioSocket = new ServerSocket(port+1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see com.mekya.streamserver.servers.IStreamPostman#register(com.mekya.streamserver.IStreamListener)
	 */
	@Override
	public void registerForVideo(IStreamListener streamListener) {
		videoStreamListeners.add(streamListener);
	}

	public void start() {
		videoServerThread = new VideoServerThread();
		audioServerThread = new AudioServerThread();
		
		videoServerThread.start();
		audioServerThread.start();

	}


	/* (non-Javadoc)
	 * @see com.mekya.streamserver.servers.IStreamPostman#removeListener(com.mekya.streamserver.IStreamListener)
	 */
	@Override
	public void removeVideoListener(IStreamListener streamListener) {
		videoStreamListeners.remove(streamListener);
	}

	private void feedVideoStreamListeners(byte[] data, int len) {
		int size = videoStreamListeners.size();
		for (int i = 0; i < size ; i++) {
			videoStreamListeners.get(i).dataReceived(data, len);
		}
	}
	

	public void stopServer() {
		try {
			serverVideoSocket.close();
			serverAudioSocket.close();
			videoServerThread.join();
			audioServerThread.join();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public class VideoServerThread extends Thread {
		@Override
		public void run() {
			try {
				Socket socket;
				while (true) {
					socket = serverVideoSocket.accept();
					new SessionHandler(socket){

						@Override
						public void feedStreamListeners(byte[] data, int length) {
							feedVideoStreamListeners(data, length);
							
						}
						
					}.start();
				}
			}catch (SocketException e) {
				//e.printStackTrace();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	
	public class AudioServerThread extends Thread {
		@Override
		public void run() {
			try {
				Socket socket;
				while (true) {
					socket = serverAudioSocket.accept();
					new SessionHandler(socket) {

						@Override
						public void feedStreamListeners(byte[] data, int length) {
							feedAudioStreamListeners(data, length);
							
						}
						
					}.start();
				}
			}catch (SocketException e) {
				//e.printStackTrace();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	
	public void feedAudioStreamListeners(byte[] data, int length) {
		int size = audioStreamListeners.size();
		for (int i = 0; i < size ; i++) {
			audioStreamListeners.get(i).dataReceived(data, length);
		}
	}

	public abstract class SessionHandler extends Thread {

		private final Socket socket;


		public SessionHandler(Socket socket) {
			this.socket = socket;
		}


		@Override
		public void run() {
			try {
				InputStream istream = socket.getInputStream();
				byte[] data = new byte[10240];
				int len;
				while ((len = istream.read(data, 0, data.length)) > 0) {
					feedStreamListeners(data, len);
				}
				feedStreamListeners(null, 0);
			} 
			catch (SocketException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public abstract void feedStreamListeners(byte[] data, int length);

	}

	@Override
	public void registerForAudio(IStreamListener streamListener) {
		audioStreamListeners.add(streamListener);
		
	}

	@Override
	public void removeAudioListener(IStreamListener streamListener) {
		audioStreamListeners.remove(streamListener);
	}


}
