package org.red5.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class BandwidthServer {
	
	private ServerSocket serverSocket;

	public BandwidthServer() {
		new Thread() {
			public void run() {
				try {
					serverSocket = new ServerSocket(53000);
					Socket socket;
					while (true) {
						socket = serverSocket.accept();
						handleSocket(socket);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			};
			
		}.start();
	}

	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void handleSocket(final Socket socket) {
		new Thread(){
			public void run() {
				try {
					InputStream inputStream = socket.getInputStream();
					OutputStream outputStream = socket.getOutputStream();
					byte[] data = new byte[1024];
					int length = 0;
					
					while ((length = inputStream.read(data, 0, data.length)) > 0) {
						
						if (data[length-1] == '\n') {
							break;
						}
						else if (data[length-1] == '\r') {
							outputStream.write("\n".getBytes());
							outputStream.flush();
						}
					}
					inputStream.close();
					outputStream.close();
					socket.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			};
			
		}.start();
		
		
	}
	

}
