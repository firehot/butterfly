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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;

/**
 * Sample application that uses the client manager.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class Application extends MultiThreadedApplicationAdapter {

	private static final String SENDER_ID = "AIzaSyCFmHIbJO0qCtPo6klp7Ade3qjeGLgtZWw";
	Map<String, Stream> registeredStreams = new HashMap<String, Stream>();
	private EntityManager entityManager;
	private ResourceBundle messagesTR;
	private ResourceBundle messagesEN;

	public class Stream implements Serializable {
		public String streamName;
		public String streamUrl;
		public Long registerTime;
		public Stream(String streamName, String streamUrl, Long registerTime) {
			super();
			this.streamName = streamName;
			this.streamUrl = streamUrl;
			this.registerTime = registerTime;
		}

	}
	
	public Application() {
        messagesTR = ResourceBundle.getBundle("resources/LanguageBundle", new Locale("tr"));
        messagesEN = ResourceBundle.getBundle("resources/LanguageBundle");

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

	public HashMap<String, String> getLiveStreams() {
		HashMap<String, String> streams = new HashMap<String, String>();
		IScope target = null;

		target = Red5.getConnectionLocal().getScope();

		Set<String> streamNames =
				getBroadcastStreamNames(target);
		for (String name : streamNames) {
			if (registeredStreams.containsKey(name)) {
				Stream stream = registeredStreams.get(name);
				streams.put(stream.streamUrl, stream.streamName);
			}
		}
		return streams;
	}


	public boolean registerLiveStream(String streamName, String url) {
		boolean result = false;
		if (registeredStreams.containsKey(url) == false) {
			registeredStreams.put(url, new Stream(streamName,  url, System.currentTimeMillis()));
			result = true;
		}
		return result;
	}

	public boolean registerUser(String register_id, String mail) 
	{
		boolean result;
		try {
			beginTransaction();
			
			Query query = getEntityManager().createQuery("FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			if(results.size() > 0)
			{
				GcmUsers gcmUsers =  (GcmUsers) results.get(0);
				gcmUsers.setGcmRegId(register_id);
			}
			else
			{
				GcmUsers gcmUsers = new GcmUsers(register_id, mail);
				getEntityManager().persist(gcmUsers);
			}

			commit();
			closeEntityManager();
			result = true;
		}
		catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

	}

	public void sendNotificationsOrMail(String mails, String userMessage, String deviceLanguage) {

		ResourceBundle messages = messagesEN;
		if (deviceLanguage.equals("tur")) {
			messages = messagesTR;
		}
		
        String subject = messages.getString("mail_notification_subject");
        String message = messages.getString("mail_notification_message");
		
		String result = null;
		
		
	    ArrayList<String> mailListNotifiedByMail = new ArrayList<String>(); // This List will be used for mails, which are not available on the database
		ArrayList<String> registerIdList = new ArrayList<String>();// This List will be used for registerIds, which are available on the database
		String [] splits = mails.split(",");
		
		for(int i = 0; i<splits.length; i++){
			result = getRegistrationId(splits[i]);
			if (result == null){
				mailListNotifiedByMail.add(splits[i]); // using as a parameter for sendMail() function
			}			
			else {	
				registerIdList.add(result); // using as a parameter for sendNotification() function
			}
		}
			
		if(!mailListNotifiedByMail.isEmpty())
			sendMail(mailListNotifiedByMail, subject, message);
		if(!registerIdList.isEmpty())
			sendNotification(registerIdList, userMessage);
	}


	/**
	 * @param mail
	 * @return
	 * registration id of mail in the table
	 * if mail is not exist, 0 returns
	 * else return GcmRegId of mail
	 */
	public String getRegistrationId(String mail){

		String result = null;
		try{
			beginTransaction();
			Query query = getEntityManager().createQuery("FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			if(results.size() > 0)
			{
				GcmUsers gcmUsers =  (GcmUsers) results.get(0);
				result = gcmUsers.getGcmRegId();
			}

			commit();
			closeEntityManager();

		}
		catch (NoResultException e) {
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}

		return result;
	}

	
	public int getUserCount(String mail){

		int result = 0;
		try{
			beginTransaction();
			Query query = getEntityManager().createQuery("FROM GcmUsers where email= :email");
			query.setParameter("email", mail);
			List results = query.getResultList();
			result = results.size();
			commit();
			closeEntityManager();

		}
		catch (NoResultException e) {
			e.printStackTrace();
		}
		catch (Exception e){
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		String streamUrl = stream.getPublishedName();
		//getPublishedName means streamurl to us
		removeStream(streamUrl);
		super.streamBroadcastClose(stream);
	}

	public boolean removeStream(String streamUrl) {
		boolean result = false;
		if (registeredStreams.containsKey(streamUrl)) {
			Object object = registeredStreams.remove(streamUrl);
			if (object != null) {
				result = true;
			}
		}
		return result;
	}


	private void beginTransaction() {
		getEntityManager().getTransaction().begin();
	}

	private EntityManager getEntityManager() {
		if (entityManager == null) {
			EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("ButterFly_Red5");
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

	public boolean sendMail(ArrayList<String> email,String subject,String messagex)
	{
		boolean resultx = false;
		final String username = "butterfyproject@gmail.com";
		final String password = "123456Abc";
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try 
		{
			javax.mail.Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			
			for (int i = 0; i < email.size(); i++) {
				message.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(email.get(i)));
				message.setSubject(subject);
				message.setText(messagex);
				Transport.send(message);
			}
			
			resultx = true;
			System.out.println("Done");
		} catch (MessagingException e) 
		{
			resultx = false;
		}
		return resultx;

	}

	private boolean sendNotification(ArrayList<String> androidTargets, String userMessage)
	{
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
		.addData("notification", userMessage).build();


		try {
			// use this for multicast messages. The second parameter
			// of sender.send() will need to be an array of register ids.
			MulticastResult result = sender.send(message, androidTargets, 1);


			if (result.getResults() != null) {
				int canonicalRegId = result.getCanonicalIds();
				if (canonicalRegId != 0) {


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

}


