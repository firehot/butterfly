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
import java.io.Serializable;
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
import javax.management.timer.Timer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.mina.core.buffer.IoBuffer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.red5.io.ITag;
import org.red5.io.flv.impl.FLVWriter;
import org.red5.io.flv.impl.Tag;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.api.stream.ISubscriberStream;

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

	private static final String SENDER_ID = "AIzaSyCFmHIbJO0qCtPo6klp7Ade3qjeGLgtZWw";
	private Map<String, Stream> registeredStreams = new HashMap<String, Stream>();
	private EntityManager entityManager;
	private ResourceBundle messagesTR;
	private ResourceBundle messagesEN;
	private BandwidthServer bandwidthServer;
	private FLVWriter flvWriter;
	private java.util.Timer streamDeleterTimer;
	private static long MILLIS_IN_HOUR = 60*60*1000;

	public static class Stream implements Serializable {
		public String streamName;
		public String streamUrl;
		public Long registerTime;
		public ArrayList<String> viewerStreamNames = new ArrayList<String>();
		private GcmUsers gcmIdList;
		public Timestamp timeReceived;
		public double altitude;
		public double longtitude;
		public double latitude;
		public FLVWriter flvWriter;
		public boolean isLive = true;

		public Stream(String streamName, String streamUrl, Long registerTime) {
			super();
			this.streamName = streamName;
			this.streamUrl = streamUrl;
			this.registerTime = registerTime;
			this.isLive = true;

			try {
				File streamsFolder = new File("webapps/ButterFly_Red5/streams");
				if (streamsFolder.exists() == false) {
					streamsFolder.mkdir();
				}
				File file = new File(streamsFolder, streamUrl + ".flv");
				
				if (file.exists() == false) {
					file.createNewFile();
				}
				flvWriter = new FLVWriter(file, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void write(IStreamPacket packet) {
			IoBuffer data = packet.getData().asReadOnlyBuffer().duplicate();
			if (data.limit() == 0) {
				System.out.println("data limit -> 0");
				return;
			}

			ITag tag = new Tag();
			tag.setDataType(packet.getDataType());
			tag.setBodySize(data.limit());
			tag.setTimestamp(packet.getTimestamp());
			tag.setBody(data);

			try {
				flvWriter.writeTag(tag);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void close() {
			flvWriter.close();
		}

		public void addViewer(String streamName) {
			viewerStreamNames.add(streamName);
		}

		public boolean containsViewer(String streamName) {
			return viewerStreamNames.contains(streamName);
		}

		public void removeViewer(String streamName) {
			viewerStreamNames.remove(streamName);
		}

		public int getViewerCount() {
			return viewerStreamNames.size();
		}

		public void setGCMUser(GcmUsers registrationIdList) {
			this.gcmIdList = registrationIdList;
		}

		public GcmUsers getBroadcasterGCMUsers() {
			return gcmIdList;
		}

	}

	public Application() {
		messagesTR = ResourceBundle.getBundle("resources/LanguageBundle",
				new Locale("tr"));
		messagesEN = ResourceBundle.getBundle("resources/LanguageBundle");
		bandwidthServer = new BandwidthServer();
		
		scheduleStreamDeleterTimer(6* MILLIS_IN_HOUR, 24 * MILLIS_IN_HOUR);
	}
	
	
	public void scheduleStreamDeleterTimer(long runPeriod, final long deleteTime) {
		TimerTask streamDeleteTask = new TimerTask() {
			
			@Override
			public void run() {
				File dir = new File("webapps/ButterFly_Red5/streams");
				String[] files = dir.list();
				long timeMillis = System.currentTimeMillis();
				for (String fileName : files) {
					File f = new File(dir, fileName);
					if (f.isFile() == true) {
						if ((timeMillis - f.lastModified()) > deleteTime) {
							f.delete();
						}						
					}
				}				
			}
		};

		streamDeleterTimer = new java.util.Timer();
		streamDeleterTimer.schedule(streamDeleteTask, 0, runPeriod);
	}
	
	public void cancelStreamDeleteTimer() {
		if (streamDeleterTimer != null) {
			streamDeleterTimer.cancel();
		}
	}
	

	@Override
	public void appStop(IScope arg0) {
		super.appStop(arg0);
		getBandwidthServer().close();

	}

	/** {@inheritDoc} */
	@Override
	public boolean connect(IConnection conn, IScope scope, Object[] params) {
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect(IConnection conn, IScope scope) {
		super.disconnect(conn, scope);
	}

	public String getLiveStreams() {
		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject;
		
		Set<Entry<String, Stream>> entrySet = getRegisteredStreams().entrySet();
		for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
			Entry<String, Stream> entry = (Entry<String, Stream>) iterator
					.next();
			Stream stream = entry.getValue();
			
			jsonObject = new JSONObject();
			jsonObject.put("url", stream.streamUrl);
			jsonObject.put("name", stream.streamName);
			jsonObject.put("viewerCount", stream.getViewerCount());
			jsonObject.put("latitude", stream.latitude);
			jsonObject.put("longitude", stream.longtitude);
			jsonObject.put("altitude", stream.altitude);
			jsonObject.put("isLive", stream.isLive);
			jsonArray.add(jsonObject);
		}

		return jsonArray.toString();
	}

	public boolean isLiveStreamExist(String url) {
		IScope target = Red5.getConnectionLocal().getScope();
		Set<String> streamNames = getBroadcastStreamNames(target);
		boolean result = false;
		if (streamNames.contains(url)) {
			result = true;
		}
		return result;
	}

	public boolean registerLiveStream(String streamName, String url,
			String mailsToBeNotified, String broadcasterMail, boolean isPublic,
			String deviceLanguage) {
		boolean result = false;
		if (getRegisteredStreams().containsKey(url) == false) {
			if (isPublic == true) {
				Stream stream = new Stream(streamName, url,
						System.currentTimeMillis());
				stream.setGCMUser(getRegistrationIdList(broadcasterMail));

				registeredStreams.put(url, stream);
			}
			sendNotificationsOrMail(mailsToBeNotified, broadcasterMail, url,
					streamName, deviceLanguage);
			// return true even if stream is not public
			result = true;
		}
		return result;
	}

	public boolean registerLocationForStream(String url, double longitude,
			double latitude, double altitude) {
		boolean result = false;
		if (getRegisteredStreams().containsKey(url) == true) {
			Stream stream = getRegisteredStreams().get(url);
			stream.latitude = latitude;
			stream.longtitude = longitude;
			stream.altitude = altitude;
			result = true;
		}
		return result;
	}

	public boolean registerUser(String register_id, String mail) {
		boolean result;
		try {
			beginTransaction();

			Query query = getEntityManager().createQuery(
					"FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			if (results.size() > 0) {
				GcmUsers gcmUsers = (GcmUsers) results.get(0);
				RegIDs regid = new RegIDs(register_id);
				gcmUsers.addRegID(regid);
			} else {
				GcmUsers gcmUsers = new GcmUsers(mail);
				RegIDs regid = new RegIDs(register_id);
				gcmUsers.addRegID(regid);
				getEntityManager().persist(gcmUsers);
			}

			commit();
			closeEntityManager();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

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
		boolean result;
		try {
			beginTransaction();

			Query query = getEntityManager().createQuery(
					"FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();

			// if user is found
			if (results.size() > 0) {
				GcmUsers gcmUsers = (GcmUsers) results.get(0);

				// if reg id doesnt exist for the user
				if (gcmUsers.getRegIDs().size() == 0) {
					RegIDs regid = new RegIDs(register_id);
					gcmUsers.addRegID(regid);
				} else {
					// update the reg id of the user using the old reg id
					for (RegIDs regid : gcmUsers.getRegIDs()) {
						if (regid.getGcmRegId().equals(oldRegID)) {
							regid.setGcmRegId(register_id);
						}
					}
				}

			} else {
				// user doesnt exist, create user and add reg id
				GcmUsers gcmUsers = new GcmUsers(mail);
				RegIDs regid = new RegIDs(register_id);
				gcmUsers.addRegID(regid);
				getEntityManager().persist(gcmUsers);
			}

			commit();
			closeEntityManager();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

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
	private void sendNotificationsOrMail(String mails, String broadcasterMail,
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
				sendMail(mailListNotifiedByMail, broadcasterMail, streamName,
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

		GcmUsers result = null;
		try {
			beginTransaction();
			Query query = getEntityManager().createQuery(
					"FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			if (results.size() > 0) {
				GcmUsers gcmUsers = (GcmUsers) results.get(0);
				result = gcmUsers;
			}

			commit();
			closeEntityManager();

		} catch (NoResultException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public int getUserCount(String mail) {

		int result = 0;
		try {
			beginTransaction();
			Query query = getEntityManager().createQuery(
					"FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			result = results.size();
			commit();
			closeEntityManager();

		} catch (NoResultException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
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
		boolean result = false;
		if (getRegisteredStreams().containsKey(streamUrl)) {
			Stream stream = getRegisteredStreams().remove(streamUrl);
			stream.close();
			if (stream != null) {
				result = true;
			}
			stream = null;
			// File f = new File("webapps/ButterFly_Red5/"+streamUrl+".png");
			// f.delete();
		}
		return result;
	}

	private void beginTransaction() {
		getEntityManager().getTransaction().begin();
	}

	private EntityManager getEntityManager() {
		if (entityManager == null) {
			EntityManagerFactory entityManagerFactory = Persistence
					.createEntityManagerFactory("ButterFly_Red5");
			entityManager = entityManagerFactory.createEntityManager();
		}
		return entityManager;
	}

	private void commit() {
		getEntityManager().getTransaction().commit();
	}

	private void closeEntityManager() {
		getEntityManager().close();
		entityManager = null;
	}

	public boolean sendMail(ArrayList<String> email, String broadcasterMail,
			String streamName, String deviceLanguage) {

		ResourceBundle messages = messagesEN;
		if (deviceLanguage != null && deviceLanguage.equals("tur")) {
			messages = messagesTR;
		}

		String subject = messages.getString("mail_notification_subject");
		String messagex = MessageFormat.format(
				messages.getString("mail_notification_message"),
				broadcasterMail, streamName);

		boolean resultx = false;
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

			resultx = true;
			System.out.println("Done");
		} catch (MessagingException e) {
			resultx = false;
			System.out.println(e.getMessage());
		}
		return resultx;

	}

	private boolean sendNotification(ArrayList<GcmUsers> androidTargets,
			String broadcasterMail, String streamURL, String streamName,
			String deviceLanguage) {
		boolean resx = false;

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
			sendMail(failedNotificationMails, broadcasterMail, streamName,
					deviceLanguage);

		// We'll pass the CollapseKey and Message values back to index.jsp, only
		// so
		// we can display it in our form again
		System.out.println("OK");
		return resx;
	}

	public boolean deleteUser(GcmUsers user) {
		boolean result;
		try {
			beginTransaction();

			getEntityManager().remove(user);

			commit();
			closeEntityManager();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;
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
	 * @return file name list in json
	 */
	public String getRecordedVideoFileList() {

		String path = "webapps/ButterFly_Red5/";
		String fileName;
		List<String> validFileNames = new ArrayList<String>();
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {

			if (listOfFiles[i].isFile()) {
				fileName = listOfFiles[i].getName();
				if (fileName.endsWith(".png") || fileName.endsWith(".PNG")) {

					//a file is valid if both flv and png files are exist
					if (isFlvFileExist(listOfFiles,
							fileName.substring(0, fileName.length() - 3)
									+ "flv")) {
						validFileNames.add(fileName);
					}

				}
			}
		}

		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject;
		for (String streamName : validFileNames) {

			jsonObject = new JSONObject();
			jsonObject.put("streamName", streamName);
			jsonArray.add(jsonObject);

		}

		return jsonArray.toString();

	}

	/**
	 * Checks whether a corresponding flv file is exist for a png
	 * @param listOfFiles
	 * @param flvFileName
	 * @return
	 */
	public boolean isFlvFileExist(File[] listOfFiles, String flvFileName) {
		String fileName;

		for (int i = 0; i < listOfFiles.length; i++) {

			if (listOfFiles[i].isFile()) {
				fileName = listOfFiles[i].getName();
				if (fileName.equals(flvFileName)) {

					return true;
				}
			}
		}

		return false;
	}

	public Map<String, Stream> getRegisteredStreams() {
		return registeredStreams;
	}
}
