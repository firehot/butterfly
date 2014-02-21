package org.red5.core;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.red5.core.dbModel.GcmUsers;
import org.red5.core.dbModel.Stream;
import org.red5.core.manager.StreamManager;
import org.red5.core.manager.UserManager;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.api.stream.ISubscriberStream;
import org.slf4j.Logger;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

/**
 * Sample application that uses the client manager.murat
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class Application extends MultiThreadedApplicationAdapter implements
IStreamListener {
	
	private static Logger log = Red5LoggerFactory.getLogger(Application.class);
	
	private static final String SENDER_ID = "AIzaSyCFmHIbJO0qCtPo6klp7Ade3qjeGLgtZWw";
	private static final String WEB_PLAY_URL = "http://www.butterflytv.net/player.html?videoId=";
	private Map<String, Stream> registeredStreams = new HashMap<String, Stream>();
	private ResourceBundle messagesTR;
	private ResourceBundle messagesEN;
	private BandwidthServer bandwidthServer;
	private java.util.Timer streamDeleterTimer;
	private static long MILLIS_IN_HOUR = 60 * 60 * 1000;
	public UserManager userManager;
	public StreamManager streamManager;
	private boolean mailsSent;

	public Application() {
		messagesTR = ResourceBundle.getBundle("resources/LanguageBundle",
				new Locale("tr"));
		messagesEN = ResourceBundle.getBundle("resources/LanguageBundle");
		bandwidthServer = new BandwidthServer();
		userManager = new UserManager(this);
		streamManager = new StreamManager(this);

		scheduleStreamDeleterTimer(6 * MILLIS_IN_HOUR, 24 * MILLIS_IN_HOUR);
		
		log.info("app started");
	}



	public void cancelStreamDeleteTimer() {
		if (streamDeleterTimer != null) {
			streamDeleterTimer.cancel();
		}
	}


	public void scheduleStreamDeleterTimer(long runPeriod, final long deleteTime) {
		TimerTask streamDeleteTask = new TimerTask() {

			@Override
			public void run() {
				File dir = new File("webapps/ButterFly_Red5/streams");
				String[] files = dir.list();
				if (files != null) {
					long timeMillis = System.currentTimeMillis();
					for (String fileName : files) {
						File f = new File(dir, fileName);
						if (f.isFile() == true && f.exists() == true) {
							
							String key = f.getName().substring(0, f.getName().indexOf(".flv"));
							if ((timeMillis - f.lastModified()) > deleteTime) {
								f.delete();
								if (registeredStreams.containsKey(key)) {
									registeredStreams.remove(key);
								}
							}
							
						}
					}	
				}
			}
		};

		streamDeleterTimer = new java.util.Timer();
		streamDeleterTimer.schedule(streamDeleteTask, 0, runPeriod);
	}




	@Override
	public void appStop(IScope arg0) {
		log.info("app stop");
		super.appStop(arg0);
		getBandwidthServer().close();
		cancelStreamDeleteTimer();

	}

	/** {@inheritDoc} */
	@Override
	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		log.info("app connect");
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect(IConnection conn, IScope scope) {
		super.disconnect(conn, scope);
	}

	public String getLiveStreams() {
		Set<Entry<String, Stream>> entrySet = getRegisteredStreams().entrySet();		
		return streamManager.getLiveStreams(entrySet);
	}

	public boolean isLiveStreamExist(String url) {

		IScope target = Red5.getConnectionLocal().getScope();
		Set<String> streamNames = getBroadcastStreamNames(target);

		return streamManager.isLiveStreamExist(url, streamNames);

	}

	public boolean registerLiveStream(String streamName, String url,
			String mailsToBeNotified, String broadcasterMail, boolean isPublic,
			String deviceLanguage) {

		return streamManager.registerLiveStream(streamName, url,
				mailsToBeNotified, broadcasterMail, isPublic, deviceLanguage);
	}

	public boolean registerLocationForStream(String url, double longitude,
			double latitude, double altitude) {
		return streamManager.registerLocationForStream(url, longitude, latitude, altitude);
	}

	public boolean registerUser(String register_id, String mail) {
		return userManager.registerUser(register_id, mail);

	}

	/**
	 * @param register_id
	 *            new register id
	 * @param mail
	 *            user mail
	 * @param oldRegID
	 *            old register id
	 * @return true if the user is updated succesfully , false if fails
	 */
	public boolean updateUser(String register_id, String mail, String oldRegID) {
		return userManager.updateUser(register_id, mail, oldRegID);

	}

	/**
	 * 
	 * @param mails
	 *            , The mail address to be notified that a video stream is
	 *            shared with them
	 * @param broadcasterMail
	 *            The mail address of broadcaster
	 * @param streamURL
	 *            published name of the stream in Red5
	 * @param deviceLanguage
	 *            language of the device. According to this parameter,
	 *            notification mail language is selected
	 * @param deviceLanguage
	 */
	public void sendNotificationsOrMail(String mails, String broadcasterMail,
			String streamURL, String streamName, String deviceLanguage) {

		GcmUsers result = null;

		// This List will be used for mails, which are not available on the
		// database
		ArrayList<String> mailListNotifiedByMail = new ArrayList<String>();

		// This List will be used for registerIds, which are available on the
		// database
		ArrayList<GcmUsers> userList = new ArrayList<GcmUsers>();
		if (mails != null) {
			String[] splits = mails.split(",");

			for (int i = 0; i < splits.length; i++) {
				result = getRegistrationIdList(splits[i]);
				if (result == null) {

					// using as a parameter for sendMail() function
					mailListNotifiedByMail.add(splits[i]);
				} else {
					// using as a parameter for sendNotification() function
					userList.add(result);
				}
			}

			if (!mailListNotifiedByMail.isEmpty())
				sendMail(mailListNotifiedByMail, broadcasterMail, streamName, streamURL,
						deviceLanguage);

			if (userList.size() > 0)
				sendNotification(userList, broadcasterMail, streamURL,
						streamName, deviceLanguage);
		}
	}

	/**
	 * @param mail
	 * @return user with reg ids of mail in the table if mail is not exist, null
	 *         returns else return GcmUsers of mail
	 */
	public GcmUsers getRegistrationIdList(String mail) {

		GcmUsers users = userManager.getRegistrationIdList(mail);
		return users;
	}

	public int getUserCount(String mail) {

		return userManager.getUserCount(mail);
	}

	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		String streamUrl = stream.getPublishedName();
		// getPublishedName means streamurl to us
		Stream streaming = getRegisteredStreams().get(streamUrl);

		streaming.isLive = false;
		streaming.close();
		super.streamBroadcastClose(stream);
	}

	@Override
	public void streamPublishStart(IBroadcastStream stream) {

		stream.addStreamListener(this);
		super.streamPublishStart(stream);
	}

	public boolean removeStream(String streamUrl) {
		return streamManager.removeStream(streamUrl);
	}

	public void sendMail(final ArrayList<String> email, String broadcasterMail,
			String streamName, String streamURL, String deviceLanguage) {
		setMailsSent(false);
		ResourceBundle messages = messagesEN;
		if (deviceLanguage != null && deviceLanguage.equals("tur")) {
			messages = messagesTR;
		}

		String webURL = WEB_PLAY_URL + streamURL;
		final String subject = messages.getString("mail_notification_subject");
		final String messagex = MessageFormat.format(
				messages.getString("mail_notification_message"),
				broadcasterMail, streamName, webURL);

		Thread mailSenderThread = new Thread() {
			@Override
			public void run() {
				final String username = "notification@butterflytv.net";
				final String password = "Nybn~Dx-E5-$";
				Properties props = new Properties();
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.starttls.enable", "true");
				props.put("mail.smtp.host", "mail.butterflytv.net");
				props.put("mail.smtp.port", "26");
				System.out.println("Done1");
				Session session = Session.getInstance(props,
						new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});

				try {
					javax.mail.Message message = new MimeMessage(session);
					message.setFrom(new InternetAddress(username));
					System.out.println("Done2");
					for (int i = 0; i < email.size(); i++) {
						System.out.println("Done3");
						message.setRecipients(javax.mail.Message.RecipientType.TO,
								InternetAddress.parse(email.get(i)));
						message.setSubject(subject);
						message.setContent(messagex, "text/html; charset=utf-8");
						// message.setText(broadcasterMail);
						System.out.println("Done4");
						Transport.send(message);
						System.out.println("Done5");
					}

					System.out.println("Done");
				} catch (MessagingException e) {
					System.out.println(e.getMessage());
				}
				setMailsSent(true);
			}
		};
		mailSenderThread.start();


	}

	private void sendNotification(final ArrayList<GcmUsers> androidTargets,
			final String broadcasterMail, final String streamURL, final String streamName,
			final String deviceLanguage) {
		Thread notifSender = new Thread() {
			public void run() {
				// Instance of com.android.gcm.server.Sender, that does the
				// transmission of a Message to the Google Cloud Messaging service.
				Sender sender = new Sender(SENDER_ID);

				// This Message object will hold the data that is being transmitted
				// to the Android client devices. For this demo, it is a simple text
				// string, but could certainly be a JSON object.
				Message message = new Message.Builder()

				// If multiple messages are sent using the same .collapseKey()
				// the android target device, if it was offline during earlier
				// message
				// transmissions, will only receive the latest message for that
				// key when
				// it goes back on-line.
				.collapseKey("1").timeToLive(30).delayWhileIdle(true)
				.addData("URL", streamURL)
				.addData("broadcaster", broadcasterMail)
				.addData("name", streamName).build();

				ArrayList<String> failedNotificationMails = new ArrayList<String>();

				try {
					// use this for multicast messages. The second parameter
					// of sender.send() will need to be an array of register ids.
					List<String> targetRegIDList = GcmUsers
							.fetchRegIDListbyUsers(androidTargets);
					MulticastResult result = sender.send(message, targetRegIDList, 1);

					List<Result> resultList = result.getResults();
					if (resultList != null) {
						int canonicalRegId = result.getCanonicalIds();
						for (int i = 0; i < resultList.size(); i++) {
							Result innerResult = resultList.get(i);
							if (innerResult.getMessageId() != null) {
								if (canonicalRegId != 0) {

									String canoID = innerResult
											.getCanonicalRegistrationId();
									String oldRegID = targetRegIDList.get(i);
									GcmUsers user = GcmUsers.fetchUserByRegID(oldRegID,
											androidTargets);
									updateUser(canoID, user.getEmail(), oldRegID);
								}

							} else {
								String error = innerResult.getErrorCodeName();
								if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
									// application has been removed from device -
									// unregister database
									String oldRegID = targetRegIDList.get(i);
									GcmUsers user = GcmUsers.fetchUserByRegID(oldRegID,
											androidTargets);
									if (!failedNotificationMails.contains(user
											.getEmail()))
										failedNotificationMails.add(user.getEmail());
									deleteUser(user);
								}
							}
						}

					} else {
						int error = result.getFailure();
						System.out.println("Broadcast failure: " + error);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				if (failedNotificationMails.size() > 0)
					sendMail(failedNotificationMails, broadcasterMail, streamName, streamURL,
							deviceLanguage);

				// We'll pass the CollapseKey and Message values back to index.jsp, only
				// so
				// we can display it in our form again
				System.out.println("OK");
			};
		};
		
		notifSender.start();

	}

	public boolean deleteUser(GcmUsers user) {
		return userManager.deleteUser(user.getId());
	}

	@Override
	public void streamPlayItemPlay(ISubscriberStream subscriberStream,
			IPlayItem item, boolean isLive) {
		super.streamPlayItemPlay(subscriberStream, item, isLive);

		String name = item.getName();
		if (getRegisteredStreams().containsKey(name)) {
			Stream stream = getRegisteredStreams().get(name);
			stream.addViewer(subscriberStream.getName());
			System.out
			.println("Application.streamPlayItemPlay() -- viewerCount "
					+ stream.getViewerCount());
			notifyUserAboutViewerCount(stream.getViewerCount(),
					stream.getBroadcasterGCMUsers());
		}

	}

	private void notifyUserAboutViewerCount(int viewerCount,
			GcmUsers broadcasterGCMUsers) {

		Sender sender = new Sender(SENDER_ID);

		Message message = new Message.Builder().collapseKey("1").timeToLive(30)
				.delayWhileIdle(true)
				.addData("viewerCount", String.valueOf(viewerCount)).build();

		try {
			MulticastResult result = sender.send(message,
					broadcasterGCMUsers.fetchRegIDStrings(), 1);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void streamSubscriberClose(ISubscriberStream subcriberStream) {
		super.streamSubscriberClose(subcriberStream);

		Set<Entry<String, Stream>> entrySet = getRegisteredStreams().entrySet();
		for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
			Entry<String, Stream> entry = (Entry<String, Stream>) iterator
					.next();
			Stream value = entry.getValue();
			if (value.containsViewer(subcriberStream.getName())) {
				value.removeViewer(subcriberStream.getName());
				notifyUserAboutViewerCount(value.getViewerCount(),
						value.getBroadcasterGCMUsers());
				break;
			}

		}
	}

	public int checkClientBandwidth(long startTime, int length, byte[] data) {
		System.out.println(" start time --> " + startTime);
		long currentTimeMillis = System.currentTimeMillis();
		System.out.println(" end time -->" + currentTimeMillis);

		long diff = currentTimeMillis - startTime;

		System.out.println("diff -> " + diff + " data length ->" + data.length);

		if (length == data.length) {
			int bandwidth = data.length / (int) diff;
			return bandwidth;

		}
		return 0;

	}

	public BandwidthServer getBandwidthServer() {
		return bandwidthServer;
	}

	@Override
	public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
		String streamUrl = stream.getPublishedName();

		if (registeredStreams.containsKey(streamUrl)) {
			Stream streamTemp = registeredStreams.get(streamUrl);
			java.util.Date date = new java.util.Date();
			streamTemp.timeReceived = new Timestamp(date.getTime());

			streamTemp.write(packet);
		}

	}

	/*
	 * en son alinan paket zamani ile mevcut zaman arasinda 5sn fark varsa bu
	 * stream silinir
	 */
	private Set<String> removeGhostBroadcasters(Set<String> streamNames) {
		List<String> toBeRemoved = new ArrayList<String>();

		for (String name : streamNames) {

			if (registeredStreams.containsKey(name)) {
				Stream stream = registeredStreams.get(name);
				java.util.Date date = new java.util.Date();
				Timestamp currentTime = new Timestamp(date.getTime());

				if (stream.timeReceived != null) {
					long diff = currentTime.getTime()
							- stream.timeReceived.getTime();
					if (diff > 5000) {
						toBeRemoved.add(name);
					}
				}
			}
		}

		for (String name : toBeRemoved) {
			Stream stream = registeredStreams.get(name);
			stream.isLive = false;
			stream.close();

			// File file = new File("webapps/ButterFly_Red5/"+name+".png");
			// file.delete();

		}

		return streamNames;
	}

	/**
	 * This methods gets image in byte array and saves as a file in the server
	 * 
	 * @param data
	 * @param streamURL
	 */
	public void savePreview(byte[] data, String streamURL) {
		try {
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
			File outputfile = new File("webapps/ButterFly_Red5/" + streamURL
					+ ".png");
			ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Gets the file name list in the path that the videos are recorded
	 * 
	 * @return file name list in json
	 */
	//	public String getRecordedVideoFileList() {
	//
	//		String path = "webapps/ButterFly_Red5/";
	//		String fileName;
	//		List<String> validFileNames = new ArrayList<String>();
	//		File folder = new File(path);
	//		File[] listOfFiles = folder.listFiles();
	//
	//		for (int i = 0; i < listOfFiles.length; i++) {
	//
	//			if (listOfFiles[i].isFile()) {
	//				fileName = listOfFiles[i].getName();
	//				if (fileName.endsWith(".png") || fileName.endsWith(".PNG")) {
	//
	//					// a file is valid if both flv and png files are exist
	//					if (isFlvFileExist(listOfFiles,
	//							fileName.substring(0, fileName.length() - 3)
	//							+ "flv")) {
	//						validFileNames.add(fileName);
	//					}
	//
	//				}
	//			}
	//		}
	//
	//		JSONArray jsonArray = new JSONArray();
	//		JSONObject jsonObject;
	//		for (String streamName : validFileNames) {
	//
	//			jsonObject = new JSONObject();
	//			jsonObject.put("streamName", streamName);
	//			jsonArray.add(jsonObject);
	//
	//		}
	//
	//		return jsonArray.toString();
	//
	//	}

	/**
	 * Checks whether a corresponding flv file is exist for a png
	 * 
	 * @param listOfFiles
	 * @param flvFileName
	 * @return
	 */
	//	public boolean isFlvFileExist(File[] listOfFiles, String flvFileName) {
	//		String fileName;
	//
	//		for (int i = 0; i < listOfFiles.length; i++) {
	//
	//			if (listOfFiles[i].isFile()) {
	//				fileName = listOfFiles[i].getName();
	//				if (fileName.equals(flvFileName)) {
	//
	//					return true;
	//				}
	//			}
	//		}
	//
	//		return false;
	//	}

	public Map<String, Stream> getRegisteredStreams() {
		return registeredStreams;
	}



	public boolean isMailsSent() {
		return mailsSent;
	}

	private void setMailsSent(boolean mailsSent) {
		this.mailsSent = mailsSent;
	}
}
