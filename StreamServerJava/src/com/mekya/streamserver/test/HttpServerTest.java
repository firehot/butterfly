package com.mekya.streamserver.test;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.Marshaller.Listener;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mekya.streamserver.IStreamListener;
import com.mekya.streamserver.servers.HttpServer;
import com.mekya.streamserver.servers.IAudioStreamPostman;
import com.mekya.streamserver.servers.IVideoStreamPostman;

public class HttpServerTest {

	private static final int PORT = 24007;
	HttpServer server;
	protected int registerCount = 0;
	protected IStreamListener listener;

	@Before 
	public void before() {
		registerCount = 0;
		server = new HttpServer(PORT);
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@After
	public void after(){
		server.stop();
	}	

	@Test
	public void testVideoSeasonCount() {

		IVideoStreamPostman postman = new IVideoStreamPostman() {
			@Override
			public void removeVideoListener(IStreamListener streamListener) {

			}

			@Override
			public void registerForVideo(IStreamListener streamListener) {
				registerCount ++;
			}
		};

		server.setVideoStreamer(postman);

		Assert.assertEquals(registerCount, 0);

		for (int i = 0; i < 10; i++) {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet("http://127.0.0.1:"+PORT+"/liveVideo");
			HttpResponse response;
			try {
				response = client.execute(request);
				Assert.assertEquals(registerCount, i+1);

			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	
	@Test
	public void testAudioSeasonCount() {

		IAudioStreamPostman postman = new IAudioStreamPostman() {

			@Override
			public void registerForAudio(IStreamListener streamListener) {
				registerCount++;
				
			}

			@Override
			public void removeAudioListener(IStreamListener streamListener) {
				// TODO Auto-generated method stub
				
			}
	
		};

		server.setAudioStreamer(postman);

		Assert.assertEquals(registerCount, 0);

		for (int i = 0; i < 10; i++) {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet("http://127.0.0.1:"+PORT+"/liveAudio");
			HttpResponse response;
			try {
				response = client.execute(request);
				Assert.assertEquals(registerCount, i+1);

			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}


	@Test
	public void testAudioDataDelivers() {
		IAudioStreamPostman postman = new IAudioStreamPostman() {
			
			@Override
			public void removeAudioListener(IStreamListener streamListener) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void registerForAudio(IStreamListener streamListener) {
				listener = streamListener;
				
			}
		};
		
		server.setAudioStreamer(postman);

		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet("http://127.0.0.1:"+PORT+"/liveAudio");
		HttpResponse response;
		try {
			response = client.execute(request);
			InputStream inputStream = response.getEntity().getContent();
			int read = 0;
			byte[] data = new byte[1024];
			
			Assert.assertNotNull(listener);
			String testStr = "Merhaba RT";
			listener.dataReceived(testStr.getBytes(), testStr.length());
			while ((read = inputStream.read(data)) > 0) {
				Assert.assertEquals(testStr, new String(data, 0, read));
				listener.dataReceived(null, 0);
			}

		} catch (IOException e) {
			e.printStackTrace();

		}
	}
	
	@Test
	public void testVideoDataDelivers() {
		IVideoStreamPostman postman = new IVideoStreamPostman() {
			@Override
			public void removeVideoListener(IStreamListener streamListener) {

			}

			@Override
			public void registerForVideo(IStreamListener streamListener) {
				listener = streamListener;
			}
		};

		server.setVideoStreamer(postman);

		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet("http://127.0.0.1:"+PORT+"/liveVideo");
		HttpResponse response;
		try {
			response = client.execute(request);
			InputStream inputStream = response.getEntity().getContent();
			int read = 0;
			byte[] data = new byte[1024];
			
			Assert.assertNotNull(listener);
			String testStr = "Merhaba RT";
			listener.dataReceived(testStr.getBytes(), testStr.length());
			while ((read = inputStream.read(data)) > 0) {
				Assert.assertEquals(testStr, new String(data, 0, read));
				listener.dataReceived(null, 0);
			}

		} catch (IOException e) {
			e.printStackTrace();

		}
	}




}
