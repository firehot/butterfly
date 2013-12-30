package net.butterflytv.server.feeder;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;


public class LiveFeed {
	
		private static final String RTMP_SERVER_URL = "rtmp://ec2-54-200-137-96.us-west-2.compute.amazonaws.com/ButterFly_Red5/";
		private static final String HTTP_GATEWAY_URL = "http://ec2-54-200-137-96.us-west-2.compute.amazonaws.com:5080/ButterFly_Red5/gateway";
		private String name;
		private String feedUrl;
		private String streamUrl;
		private Process exec;
		private InputStream errorStream;
		private String ffmpegPath = "ffmpeg";
		
		public LiveFeed(String name, String streamUrl, String feedUrl) {
			this.name = name;
			this.feedUrl = feedUrl;
			this.streamUrl = streamUrl;
		}

		public void start() {
			Thread startThread = new Thread() {

				@Override
				public void run() {
					boolean isRegistered = registerStream();
					try {
						if (isRegistered == true) {
							
							String shellCmd = ffmpegPath + " -re -i " + feedUrl +" -acodec copy -f flv -profile:v baseline -s 352x288 -vcodec libx264 -crf 30  " + RTMP_SERVER_URL + streamUrl ;
							System.out.println(shellCmd);
							
							exec = Runtime.getRuntime().exec(shellCmd);
							errorStream = exec.getErrorStream();
							byte[] data = new byte[1024];
							int length = 0;
							while ((length = errorStream.read(data, 0, data.length)) != -1) {
								System.out.println(new String(data, 0, length));
							}
						}
						else {
							System.out.println("not registered to server");
						}
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}
			};
			startThread.start();
			
		}
		
		private boolean registerStream() {
			AMFConnection amfConnection = new AMFConnection();
			amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
			boolean result = false;
			try {
				System.out.println(HTTP_GATEWAY_URL);

				amfConnection.connect(HTTP_GATEWAY_URL);
				result = (Boolean) amfConnection.call("registerLiveStream",
						name, streamUrl, null,
						"contact@butterflytv.net", true, Locale.getDefault()
						.getISO3Language());

			} catch (ClientStatusException e) {
				e.printStackTrace();
			} catch (ServerStatusException e) {

				e.printStackTrace();
			}
			amfConnection.close();
			System.out.println("register result -> " + result);
			return result;
		}
		
		public void stop() {
			if (exec != null) {
				exec.destroy();
			}
		}
		
		
	}