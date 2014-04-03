package org.red5.core.manager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.red5.core.Application;
import org.red5.core.dbModel.GcmUserMails;
import org.red5.core.dbModel.GcmUsers;
import org.red5.core.dbModel.StreamProxy;
import org.red5.core.dbModel.StreamViewers;
import org.red5.core.dbModel.Streams;
import org.red5.core.utils.JPAUtils;

public class StreamManager {

	Application red5App;

	public StreamManager(Application red5App)
	{
		this.red5App = red5App;
	}

	public String getLiveStreams( Map<String, StreamProxy> entrySet,List<String> mailList) {

		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject;

		java.util.Date date = new java.util.Date();
		Timestamp currentTime = new Timestamp(date.getTime());

		List<Streams> streamList;
		removeGhostStreams(entrySet, currentTime);

		streamList = getAllStreamList(mailList);
		for (Streams stream : streamList) {
			jsonObject = new JSONObject();
			jsonObject.put("url", stream.getStreamUrl());
			jsonObject.put("name", stream.getStreamName());
			jsonObject.put("viewerCount", this.red5App.getViewerCount(stream.getStreamUrl()));
			jsonObject.put("latitude", stream.getLatitude());
			jsonObject.put("longitude", stream.getLongitude());
			jsonObject.put("altitude", stream.getAltitude());
			jsonObject.put("isLive", stream.getIsLive());
			jsonObject.put("isPublic", stream.getIsPublic());
			jsonObject.put("isDeletable", isDeletable(stream, mailList));
			jsonObject.put("registerTime", stream.getRegisterTime().getTime());
			jsonArray.add(jsonObject);
		}
		

		return jsonArray.toString();
	}

	public boolean isDeletable(Streams stream,List<String> mailList)
	{
		for (String mail : mailList) {
			if(mail.equals(stream.getBroadcasterMail()))
				return true;
		}

		return false;
	}
	private void removeGhostStreams(Map<String, StreamProxy> entrySet,
			Timestamp currentTime) {
		List<Streams> streamList = getAllStreamList(null);
		if(streamList != null)
		{
			for (Streams stream : streamList) {

				StreamProxy streamProxy = null;
				if(entrySet.containsKey(stream.getStreamUrl()))
				{
					streamProxy = entrySet.get(stream.getStreamUrl());

					
					if(entrySet.containsKey(stream.getStreamUrl()))
					{
						streamProxy = entrySet.get(stream.getStreamUrl());

						if (streamProxy.timeReceived != null) {

							removeStream(stream.getStreamUrl());
						}
					}
				}
			}
		}
		

	}

	public boolean isLiveStreamExist(String url,Set<String> streamNames) {

		boolean result = false;
		if (streamNames.contains(url)) {
			result = true;
		}
		return result;
	}

	public boolean registerLiveStream(String streamName, String url,
			String mailsToBeNotified, String broadcasterMail, boolean isPublic,
			String deviceLanguage) {

		Map<String, StreamProxy> registeredStreams = this.red5App.getLiveStreamProxies();

		boolean result = false;
		if (registeredStreams.containsKey(url) == false) {
			JPAUtils.beginTransaction();
			String[] mailArray = broadcasterMail.split(",");

			Streams stream = new Streams(mailArray[0], Calendar.getInstance().getTime(), streamName, url);
			stream.setIsPublic(isPublic);
			JPAUtils.getEntityManager().persist(stream);

			//saveStream(stream);

			if (mailsToBeNotified != null) {
				String[] mails = mailsToBeNotified.split(",");
				List<String> mailList = new ArrayList<String>(Arrays.asList(mails));

				Query query = JPAUtils.getEntityManager().createQuery(
						"SELECT DISTINCT user FROM GcmUsers AS user "
								+ "JOIN user.gcmUserMailses AS userMails "
								+ "WHERE userMails.mail IN :email");
				query.setParameter("email", mailList);
				List<GcmUsers> results = query.getResultList();

				for (GcmUsers gcmUserMails : results) {
					JPAUtils.getEntityManager().persist(new StreamViewers(stream, gcmUserMails));
				}
			}

			JPAUtils.commit();
			JPAUtils.closeEntityManager();


			StreamProxy proxy = new StreamProxy(url, stream.getId());

			registeredStreams.put(url, proxy);
			this.red5App.sendNotificationsOrMail(mailsToBeNotified, broadcasterMail, url,
					streamName, deviceLanguage);
			// return true even if stream is not public
			result = true;
		}
		return result;
	}



	public boolean registerLocationForStream(String url, double longitude,
			double latitude, double altitude) {

		Streams stream = getStream(url);

		boolean result = false;
		if (stream  != null) {

			stream.setLatitude(latitude);
			stream.setLongitude(longitude);
			stream.setAltitude(altitude);

			updateStream(stream);
			result = true;
		}
		return result;
	}

	public boolean removeStream(String streamUrl) {
		Map<String, StreamProxy> registeredLiveStreams = this.red5App.getLiveStreamProxies();
		boolean result = false;
		if (registeredLiveStreams.containsKey(streamUrl)) 
		{
			StreamProxy stream = registeredLiveStreams.remove(streamUrl);
			stream.close();

			Streams strm = getStream(streamUrl);
			strm.setIsLive(false);
			updateStream(strm);

			if (stream != null) {
				result = true;
			}
			stream = null;
		}
		return result;
	}

	public boolean saveStream(Streams stream) {
		boolean result;
		try {

			JPAUtils.beginTransaction();
			JPAUtils.getEntityManager().persist(stream);
			JPAUtils.commit();
			JPAUtils.closeEntityManager();

			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

	}

	public boolean updateStream(Streams stream) {
		boolean result;
		try {

			JPAUtils.beginTransaction();
			JPAUtils.getEntityManager().merge(stream);
			JPAUtils.commit();
			JPAUtils.closeEntityManager();

			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;

	}

	public boolean deleteStream(Streams stream) {
		boolean result;
		try {
			EntityManager em = JPAUtils.getEntityManager();
			JPAUtils.beginTransaction();	
			em.remove(em.contains(stream) ? stream : em.merge(stream));	
			JPAUtils.commit();
			JPAUtils.closeEntityManager();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}


	public Streams getStream(String streamUrl) {

		Streams resultStream = null;
		try {

			Query query = JPAUtils.getEntityManager().createQuery(
					"FROM Streams where streamUrl= :streamUrl");
			query.setParameter("streamUrl", streamUrl);
			resultStream = (Streams)query.getSingleResult();
			JPAUtils.closeEntityManager();

		} catch (NoResultException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultStream;
	}

	/**
	 * Function to get public and private streams from database. 
	 * 
	 * @param mailList
	 * list of mails that stream is shared with

	 * @return 
	 * public streams and private streams shared with the mailList
	 * if mailList is null then it returns only public streams
	 */
	public List<Streams> getAllStreamList(List<String> mailList) {
		List<Streams> results = null;
		try {
			Query query = null; 
			if (mailList != null) {
				query = JPAUtils.getEntityManager().
						createQuery("SELECT str FROM Streams AS str "
								+ "LEFT JOIN str.streamViewerses AS viewer "
								+ "WHERE ( (str.isPublic = :isPublic) " 
								+ 			" OR (viewer.gcmUsers.id IN "
								+ 			"		( SELECT gcmUsers.id FROM GcmUserMails userMails WHERE userMails.mail IN (:mails)))"
								+ 		")");
				query.setParameter("mails", mailList);
			}
			else {
				query = JPAUtils.getEntityManager().
						createQuery("SELECT str FROM Streams AS str "
								+ " WHERE (str.isPublic = :isPublic) ");
			}
			query.setParameter("isPublic", true);

			results = query.getResultList();

			JPAUtils.closeEntityManager();

		} catch (NoResultException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}

}
