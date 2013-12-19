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

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
	Map<String, Stream> registeredStreams = new HashMap<String, Stream>();
	private EntityManager entityManager;
	private ResourceBundle messagesTR;
	private ResourceBundle messagesEN;
	private BandwidthServer bandwidthServer;

	public static class Stream implements Serializable {
		public String streamName;
		public String streamUrl;
		public Long registerTime;
		public ArrayList<String> viewerStreamNames = new ArrayList<String>();
		private String broadcasterGCMId;
		private GcmUsers gcmIdList;
		public Timestamp timeReceived;

		public Stream(String streamName, String streamUrl, Long registerTime) {
			super();
			this.streamName = streamName;
			this.streamUrl = streamUrl;
			this.registerTime = registerTime;

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
		IScope target = Red5.getConnectionLocal().getScope();

		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject;
		Set<String> streamNames = getBroadcastStreamNames(target);
		System.out.println("getLiveStreams count 1"+ streamNames.size());
		streamNames = removeGhostBroadcasters(streamNames);
		System.out.println("getLiveStreams count 2"+ streamNames.size());
		for (String name : streamNames) {
			if (registeredStreams.containsKey(name)) {
				Stream stream = registeredStreams.get(name);
				jsonObject = new JSONObject();
				jsonObject.put("url", stream.streamUrl);
				jsonObject.put("name", stream.streamName);
				jsonObject.put("viewerCount", stream.getViewerCount());
				jsonArray.add(jsonObject);
				// streams.put(stream.streamUrl, stream.streamName);
			}
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
		if (registeredStreams.containsKey(url) == false) {
			if (isPublic == true) {
				Stream stream = new Stream(streamName, url,
						System.currentTimeMillis());
				stream.setGCMUser(getRegistrationIdList(broadcasterMail));

				registeredStreams.put(url, stream);
			}
			sendNotificationsOrMail(mailsToBeNotified, broadcasterMail, url,
					deviceLanguage);
			// return true even if stream is not public
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
	 */
	private void sendNotificationsOrMail(String mails, String broadcasterMail,
			String streamURL, String deviceLanguage) {

		ResourceBundle messages = messagesEN;
		if (deviceLanguage != null && deviceLanguage.equals("tur")) {
			messages = messagesTR;
		}

		String subject = messages.getString("mail_notification_subject");
		String message = MessageFormat.format(
				messages.getString("mail_notification_message"),
				broadcasterMail);

		GcmUsers result = null;

		ArrayList<String> mailListNotifiedByMail = new ArrayList<String>(); // This
																			// List
																			// will
																			// be
																			// used
																			// for
																			// mails,
																			// which
																			// are
																			// not
																			// available
																			// on
																			// the
																			// database
		ArrayList<GcmUsers> userList = new ArrayList<GcmUsers>();// This List
																	// will be
																	// used for
																	// registerIds,
																	// which are
																	// available
																	// on the
																	// database
		if (mails != null) {
			String[] splits = mails.split(",");

			for (int i = 0; i < splits.length; i++) {
				result = getRegistrationIdList(splits[i]);
				if (result == null) {
					mailListNotifiedByMail.add(splits[i]); // using as a
															// parameter for
															// sendMail()
															// function
				} else {
					userList.add(result); // using as a parameter for
											// sendNotification() function
				}
			}

			if (!mailListNotifiedByMail.isEmpty())
				sendMail(mailListNotifiedByMail, subject, message,
						broadcasterMail);

			if (userList.size() > 0)
				sendNotification(userList, broadcasterMail, streamURL);
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
		removeStream(streamUrl);
		super.streamBroadcastClose(stream);
	}
	@Override
	public void streamBroadcastStart(IBroadcastStream stream) {
		System.out.println("streamBroadcastStart "+ stream.getPublishedName());
		stream.addStreamListener(this);
		super.streamBroadcastStart(stream);
	}
	

	@Override
	public void streamPublishStart(IBroadcastStream stream) {
		System.out.println("streamPublishStart "+ stream.getPublishedName());
		stream.addStreamListener(this);
		super.streamPublishStart(stream);
	}
	
	public boolean removeStream(String streamUrl) {
		boolean result = false;
		if (registeredStreams.containsKey(streamUrl)) {
			Object object = registeredStreams.remove(streamUrl);
			if (object != null) {
				result = true;
			}
			object = null;
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

	public boolean sendMail(ArrayList<String> email, String subject,
			String messagex, String broadcasterMail) {
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
				message.setText(messagex);
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
			String broadcasterMail, String streamURL) {
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
				.addData("broadcaster", broadcasterMail).build();

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
		if (registeredStreams.containsKey(name)) {
			Stream stream = registeredStreams.get(name);
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

		Set<Entry<String, Stream>> entrySet = registeredStreams.entrySet();
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
		
		System.out.println("packetReceived "+ streamUrl);
		
		if (registeredStreams.containsKey(streamUrl)) {
			System.out.println("packetReceived contains"+ streamUrl);
			Stream streamTemp = registeredStreams.get(streamUrl);
			java.util.Date date = new java.util.Date();
			streamTemp.timeReceived = new Timestamp(date.getTime());
			System.out.println("packetReceived time"+ streamUrl + streamTemp.timeReceived.toString());
		}
	}

	/*
	 * en son alýnan paket zamaný ile mevcut zaman arasinda 5sn fark varsa bu
	 * stream silinir
	 */
	private Set<String> removeGhostBroadcasters(Set<String> streamNames) {
		List<String> toBeRemoved = new ArrayList<String>();

		for (String name : streamNames) {

			if (registeredStreams.containsKey(name)) {
				System.out.println("registeredStreams.containsKey");
				Stream stream = registeredStreams.get(name);
				java.util.Date date = new java.util.Date();
				Timestamp currentTime = new Timestamp(date.getTime());

				if (stream.timeReceived != null) {
					System.out.println("timeReceived not null");
					System.out.println("current :" +currentTime.getTime());
					System.out.println("timeReceived :" +stream.timeReceived.getTime());
					if (currentTime.getTime() - stream.timeReceived.getTime() > 5000) {
						streamNames.add(name);
					}
				}
			}
		}

		for (String name : toBeRemoved) {
			registeredStreams.remove(name);
			streamNames.remove(name);
		}

		return streamNames;
	}
}
